package com.manageinvoice.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record ReceiptData(
    @Size(max = 255) String vendor,
    @Size(max = 100) String receiptNumber,
    LocalDate transactionDate,
    @Size(max = 50) String transactionTime,
    @Pattern(regexp = "^[A-Z]{3}$") String currency,
    @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal subtotal,
    @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal tax,
    @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal discount,
    @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal total,
    @Size(max = 255) String paymentInfo,
    @NotNull @Size(max = 200) List<@NotNull @Valid LineItem> items,
    @Size(max = 100) String suggestedCategory,
    UUID categoryId
) {
    ReceiptData(String vendor, String receiptNumber, LocalDate transactionDate, String transactionTime,
                String currency, BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total,
                String paymentInfo, List<LineItem> items) {
        this(vendor, receiptNumber, transactionDate, transactionTime, currency, subtotal, tax, discount,
            total, paymentInfo, items, null, null);
    }
    record LineItem(
        @NotNull @Size(max = 500) String originalDescription,
        @NotNull @Size(max = 500) String description,
        boolean verified,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 4) BigDecimal quantity,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal lineTotal
    ) {}
}
