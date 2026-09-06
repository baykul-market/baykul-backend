package by.baykulbackend.services.product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The existing eight-column, semicolon-separated CSV/TXT format. Identity strings are kept verbatim. */
public record PartCsvRow(String article, String name, Double weight, int minCount, Integer storageCount,
                         BigDecimal returnPart, BigDecimal price, String brand) {
    public static PartCsvRow parse(String raw) {
        String[] columns = raw.split(";", -1);
        if (columns.length != 8) {
            throw new IllegalArgumentException("Expected 8 semicolon-separated columns");
        }
        if (columns[0].isBlank() || columns[6].isBlank() || columns[7].isBlank()) {
            throw new IllegalArgumentException("Article, price and brand are required");
        }
        if (columns[0].length() > 50 || columns[7].length() > 50 || columns[1].length() > 255) {
            throw new IllegalArgumentException("Article/brand must fit 50 characters and name 255 characters");
        }
        try {
            Double weight = columns[2].isBlank() ? null : Double.valueOf(columns[2].replace(',', '.'));
            int min = columns[3].isBlank() ? 1 : Integer.parseInt(columns[3]);
            Integer stock = columns[4].isBlank() ? null : Integer.valueOf(columns[4]);
            if ((weight != null && (!Double.isFinite(weight) || weight < 0)) || min < 1 || (stock != null && stock < 0)) {
                throw new IllegalArgumentException("Weight/stock must be nonnegative and minimum count at least 1");
            }
            return new PartCsvRow(columns[0], columns[1], weight, min, stock,
                    decimal(columns[5].isBlank() ? "0" : columns[5]), decimal(columns[6]), columns[7]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value", ex);
        }
    }

    private static BigDecimal decimal(String value) {
        BigDecimal number = new BigDecimal(value.replace(',', '.'));
        if (number.signum() < 0 || number.scale() > 2 || number.precision() - number.scale() > 36) {
            throw new IllegalArgumentException("Price/return value must be nonnegative, with at most two decimal places");
        }
        return number.setScale(2, RoundingMode.UNNECESSARY);
    }
}
