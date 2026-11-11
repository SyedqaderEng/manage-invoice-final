package com.manageinvoice.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
class ReceiptExtractor {
    private static final Pattern TOTAL = Pattern.compile("(?i)^\\s*(grand\\s+total|total|subtotal|sub\\s*total|tax|discount)\\s*:?\\s*\\$?\\s*(\\d{1,12}(?:\\.\\d{2}))\\s*$");
    // Tax lines often carry a rate, e.g. "TAX (8%) $5.52" or "TAX 1 6.000% 5.52"; take the trailing amount.
    private static final Pattern TAX_LINE = Pattern.compile("(?i)^\\s*(?:sales\\s+)?tax\\b.*?\\$?\\s*(\\d{1,12}\\.\\d{2})\\s*$");
    // Lines that are payment, change, or identifiers rather than purchased items.
    private static final Pattern NON_ITEM = Pattern.compile("(?i).*(balance|cash|change|visa|mastercard|amex|debit|credit|tend|payment|amount|due|tender|fee|savings|\\btax\\b|total).*");
    // Header lines that are not the merchant name.
    private static final Pattern NOT_VENDOR = Pattern.compile("(?i).*(receipt|chance|\\bwin\\b|\\bid\\s*#|save money|live better|manager|cashier|thank|welcome|survey|\\$|\\d{3}[-) ]\\s*\\d{3,4}).*");
    // A UPC / SKU of 8+ digits inside an item description.
    private static final Pattern UPC = Pattern.compile("\\s+\\d{8,14}\\b");
    private static final Pattern DATE = Pattern.compile("(?i)^\\s*(?:date|transaction date)\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})\\s*$");
    private static final Pattern SLASH_DATE = Pattern.compile("(?i)^\\s*(?:date|transaction date)\\s*:?\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s*$");
    private static final Pattern NUMBER = Pattern.compile("(?i)^\\s*(?:receipt|invoice)\\s*(?:number|no\\.?|#)\\s*:?\\s*(.{1,100}?)\\s*$");
    private static final Pattern ITEM = Pattern.compile("^\\s*(.{1,500}?)\\s+\\$?(\\d{1,12}\\.\\d{2})\\s*$");
    private static final Pattern QUANTITY_ITEM = Pattern.compile("^\\s*(.{1,500}?)\\s+(\\d{1,12}(?:\\.\\d{1,4})?)\\s*[xX@]\\s*\\$?(\\d{1,12}\\.\\d{2})\\s+\\$?(\\d{1,12}\\.\\d{2})\\s*$");
    private static final Pattern TIME = Pattern.compile("(?i)^\\s*(?:time|transaction time)\\s*:?\\s*(\\d{1,2}:\\d{2}(?:\\s*[ap]m)?)\\s*$");
    private static final Pattern CURRENCY = Pattern.compile("(?i)^\\s*currency\\s*:?\\s*([A-Z]{3})\\s*$");

    ReceiptData extract(String text) {
        String vendor = null;
        String number = null;
        String time = null;
        String currency = null;
        LocalDate date = null;
        boolean sawDollar = false;
        BigDecimal subtotal = null, tax = null, discount = null, total = null;
        var items = new ArrayList<ReceiptData.LineItem>();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String value = line.strip();
            if (value.isEmpty()) continue;
            Matcher amount = TOTAL.matcher(value);
            Matcher dateMatch = DATE.matcher(value);
            Matcher slashDate = SLASH_DATE.matcher(value);
            Matcher numberMatch = NUMBER.matcher(value);
            Matcher quantityItem = QUANTITY_ITEM.matcher(value);
            Matcher timeMatch = TIME.matcher(value);
            Matcher currencyMatch = CURRENCY.matcher(value);
            Matcher item = ITEM.matcher(value);
            Matcher taxLine = TAX_LINE.matcher(value);
            if (value.contains("$")) sawDollar = true;
            if (taxLine.matches()) {
                tax = new BigDecimal(taxLine.group(1));
            } else if (amount.matches()) {
                BigDecimal money = new BigDecimal(amount.group(2));
                String label = amount.group(1).replaceAll("\\s", "").toLowerCase(java.util.Locale.ROOT);
                switch (label) {
                    case "total", "grandtotal" -> total = money;
                    case "subtotal" -> subtotal = money;
                    case "tax" -> tax = money;
                    case "discount" -> discount = money;
                    default -> throw new IllegalStateException("Unexpected amount label");
                }
            } else if (dateMatch.matches()) {
                try { date = LocalDate.parse(dateMatch.group(1)); } catch (DateTimeParseException ignored) { date = null; }
            } else if (slashDate.matches()) {
                int first = Integer.parseInt(slashDate.group(1)), second = Integer.parseInt(slashDate.group(2));
                if (first > 12 || second > 12 || first == second) {
                    int month = first > 12 ? second : first;
                    int day = first > 12 ? first : second;
                    try { date = LocalDate.of(Integer.parseInt(slashDate.group(3)), month, day); }
                    catch (java.time.DateTimeException ignored) { date = null; }
                }
            } else if (numberMatch.matches()) {
                number = numberMatch.group(1).strip();
            } else if (timeMatch.matches()) {
                time = timeMatch.group(1);
            } else if (currencyMatch.matches()) {
                currency = currencyMatch.group(1).toUpperCase(java.util.Locale.ROOT);
            } else if (quantityItem.matches() && items.size() < 200) {
                String description = quantityItem.group(1).strip();
                items.add(new ReceiptData.LineItem(description, description, false, new BigDecimal(quantityItem.group(2)),
                    new BigDecimal(quantityItem.group(3)), new BigDecimal(quantityItem.group(4))));
            } else if (item.matches() && items.size() < 200) {
                String description = UPC.matcher(item.group(1).strip()).replaceAll("").strip();
                if (!description.isEmpty() && !NON_ITEM.matcher(description).matches()) {
                    items.add(new ReceiptData.LineItem(description, description, false, null, null, new BigDecimal(item.group(2))));
                }
            } else if (vendor == null && value.length() <= 255 && value.matches(".*[A-Za-z].*") && !NOT_VENDOR.matcher(value).matches()) {
                vendor = value;
            }
        }
        // A "$" with no explicit currency line is treated as USD; the editor tells the user to verify it.
        if (currency == null && sawDollar) currency = "USD";
        return new ReceiptData(vendor, number, date, time, currency, subtotal, tax, discount, total, null, items);
    }
}
