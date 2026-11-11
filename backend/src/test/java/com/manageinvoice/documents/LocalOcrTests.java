package com.manageinvoice.documents;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

class LocalOcrTests {
    @TempDir Path temp;

    @Test
    void extractsImageAndMixedPdfLocally() throws Exception {
        String command = System.getProperty("ocr.test.command");
        assumeTrue(command != null && !command.isBlank(), "Set ocr.test.command to run native OCR tests");
        LocalOcr ocr = new LocalOcr(command);
        BufferedImage image = new BufferedImage(1000, 180, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial", Font.PLAIN, 50));
            graphics.drawString("RECEIPT TOTAL 12.34", 30, 95);
        } finally {
            graphics.dispose();
        }
        Path imagePath = temp.resolve("receipt.png");
        ImageIO.write(image, "png", imagePath.toFile());
        assertTrue(ocr.extract(Files.readAllBytes(imagePath), "image/png").contains("12.34"));

        Path pdfPath = temp.resolve("mixed.pdf");
        try (PDDocument pdf = new PDDocument()) {
            PDPage textPage = new PDPage();
            pdf.addPage(textPage);
            try (PDPageContentStream content = new PDPageContentStream(pdf, textPage)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 18);
                content.newLineAtOffset(50, 700);
                content.showText("DIGITAL INVOICE 55.25");
                content.endText();
            }
            PDPage scannedPage = new PDPage();
            pdf.addPage(scannedPage);
            try (PDPageContentStream content = new PDPageContentStream(pdf, scannedPage)) {
                content.drawImage(LosslessFactory.createFromImage(pdf, image), 30, 500, 500, 90);
            }
            pdf.save(pdfPath.toFile());
        }
        String text = ocr.extract(Files.readAllBytes(pdfPath), "application/pdf");
        assertTrue(text.contains("55.25"));
        assertTrue(text.contains("12.34"));
    }

    @Test
    void rejectsExcessivePdfPagesBeforeProcessing() throws Exception {
        Path pdfPath = temp.resolve("many-pages.pdf");
        try (PDDocument pdf = new PDDocument()) {
            for (int i = 0; i < 11; i++) pdf.addPage(new PDPage());
            pdf.save(pdfPath.toFile());
        }
        assertThrows(ResponseStatusException.class, () -> new LocalOcr("unused").extract(Files.readAllBytes(pdfPath), "application/pdf"));
    }
}
