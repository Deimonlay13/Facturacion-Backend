package com.gdl.facturacion_backend.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RutUtils {

    private static final Pattern RUT_PATTERN = Pattern.compile("^(\\d{1,8})-([\\dKk])$");

    private RutUtils() {}

    /**
     * Validates a Chilean RUT (format XXXXXXXX-D, dots are stripped before validation).
     */
    public static boolean isValid(String rut) {
        if (rut == null || rut.isBlank()) return false;
        Matcher m = RUT_PATTERN.matcher(clean(rut));
        if (!m.matches()) return false;
        return calculateDv(Integer.parseInt(m.group(1))).equals(m.group(2).toUpperCase());
    }

    /** Strips dots, trims whitespace, and uppercases the DV. */
    public static String clean(String rut) {
        return rut.trim().replace(".", "").toUpperCase();
    }

    private static String calculateDv(int number) {
        int sum = 0;
        int mult = 2;
        while (number > 0) {
            sum += (number % 10) * mult;
            number /= 10;
            mult = (mult == 7) ? 2 : mult + 1;
        }
        int remainder = 11 - (sum % 11);
        if (remainder == 11) return "0";
        if (remainder == 10) return "K";
        return String.valueOf(remainder);
    }
}
