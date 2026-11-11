package com.manageinvoice.documents;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class LocalOcr {
    private static final int MAX_PAGES = 10;
    private static final int MAX_TEXT_BYTES = 1_000_000;
    private static final long MAX_PAGE_PIXELS = 20_000_000;
    private static final int TIMEOUT_SECONDS = 45;
    private final String command;

    LocalOcr(@Value("${ocr.tesseract-command:tesseract}") String command) {
        this.command = command;
    }

    String extract(byte[] content, String mimeType) {
        try {
            String text = mimeType.equals("application/pdf") ? extractPdf(content) : runTesseract(content);
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No readable text was found. Try a clearer image or a different document.");
            }
            return text;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "The document could not be read for OCR. Check the file and try again.", exception);
        }
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            if (pdf.isEncrypted() || pdf.getNumberOfPages() > MAX_PAGES) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "OCR supports unencrypted PDFs with at most " + MAX_PAGES + " pages.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(pdf);
            StringBuilder result = new StringBuilder();
            for (int page = 0; page < pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);
                String text = stripper.getText(pdf).strip();
                if (text.isEmpty()) {
                    var box = pdf.getPage(page).getCropBox();
                    double width = box.getWidth() * 150 / 72;
                    double height = box.getHeight() * 150 / 72;
                    if (width <= 0 || height <= 0 || width * height > MAX_PAGE_PIXELS) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A PDF page is too large to process.");
                    }
                    BufferedImage image = renderer.renderImageWithDPI(page, 150);
                    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                        if (!ImageIO.write(image, "png", bytes)) throw new IOException("PNG encoder unavailable");
                        text = runTesseract(bytes.toByteArray());
                    } finally {
                        image.flush();
                    }
                }
                result.append(text).append('\n');
                if (result.length() > MAX_TEXT_BYTES) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Extracted text is too large.");
                }
            }
            return result.toString().strip();
        }
    }

    private String runTesseract(byte[] input) {
        Process process;
        try {
            process = new ProcessBuilder(command, "stdin", "stdout", "-l", "eng")
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Local OCR is unavailable. Check that Tesseract is installed and configured.", exception);
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var output = executor.submit(() -> {
                try (var stream = process.getInputStream()) {
                    return stream.readNBytes(MAX_TEXT_BYTES + 1);
                }
            });
            var writer = executor.submit(() -> {
                try (var stream = process.getOutputStream()) {
                    stream.write(input);
                }
                return null;
            });
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "OCR timed out. Try a smaller or clearer document.");
            }
            writer.get();
            byte[] bytes = output.get();
            if (process.exitValue() != 0 || bytes.length > MAX_TEXT_BYTES) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR could not process this document.");
            }
            return new String(bytes, StandardCharsets.UTF_8).strip();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OCR was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR could not process this document.", exception);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
