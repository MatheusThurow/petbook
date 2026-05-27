package com.petbook.app.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgeFormatUtils {

    public static final class AgeValue {
        private final int years;
        private final int months;

        public AgeValue(int years, int months) {
            this.years = Math.max(years, 0);
            this.months = Math.max(months, 0);
        }

        public int getYears() {
            return years;
        }

        public int getMonths() {
            return months;
        }
    }

    private static final Pattern AGE_PATTERN = Pattern.compile(
            "(\\d+)\\s*ano[s]?|(?:(?:e\\s*)?(\\d+)\\s*mes(?:es)?)",
            Pattern.CASE_INSENSITIVE
    );

    private AgeFormatUtils() {
    }

    public static boolean hasAnyValue(String yearsValue, String monthsValue) {
        return !ValidationUtils.isEmpty(yearsValue) || !ValidationUtils.isEmpty(monthsValue);
    }

    public static Integer parseYears(String yearsValue) {
        if (ValidationUtils.isEmpty(yearsValue)) {
            return 0;
        }
        return Integer.parseInt(yearsValue.trim());
    }

    public static Integer parseMonths(String monthsValue) {
        if (ValidationUtils.isEmpty(monthsValue)) {
            return 0;
        }
        return Integer.parseInt(monthsValue.trim());
    }

    public static boolean isValidMonths(int months) {
        return months >= 0 && months <= 11;
    }

    public static String formatAge(int years, int months) {
        int normalizedYears = Math.max(years, 0);
        int normalizedMonths = Math.max(months, 0);

        if (normalizedYears > 0 && normalizedMonths > 0) {
            return normalizedYears + " ano" + (normalizedYears > 1 ? "s" : "")
                    + " e "
                    + normalizedMonths + " mes" + (normalizedMonths > 1 ? "es" : "");
        }
        if (normalizedYears > 0) {
            return normalizedYears + " ano" + (normalizedYears > 1 ? "s" : "");
        }
        return normalizedMonths + " mes" + (normalizedMonths > 1 ? "es" : "");
    }

    public static AgeValue parseAgeDescription(String ageDescription) {
        if (ValidationUtils.isEmpty(ageDescription)) {
            return new AgeValue(0, 0);
        }

        String normalized = ageDescription.trim();
        if (normalized.matches("\\d+")) {
            return new AgeValue(Integer.parseInt(normalized), 0);
        }
        if (normalized.matches("\\d+[\\.,]\\d+")) {
            String[] parts = normalized.replace(',', '.').split("\\.");
            int years = Integer.parseInt(parts[0]);
            int months = Integer.parseInt(parts[1]);
            return new AgeValue(years, Math.min(months, 11));
        }

        Matcher matcher = AGE_PATTERN.matcher(normalized);
        Integer years = null;
        Integer months = null;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                years = Integer.parseInt(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                months = Integer.parseInt(matcher.group(2));
            }
        }

        return new AgeValue(years == null ? 0 : years, months == null ? 0 : months);
    }
}
