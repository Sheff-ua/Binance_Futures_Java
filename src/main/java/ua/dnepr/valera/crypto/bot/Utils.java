package ua.dnepr.valera.crypto.bot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Utils {

    private static final DateTimeFormatter DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter formatter8 = DateTimeFormatter.ISO_INSTANT;

    public static synchronized String formatDateTimeUTC(long timeInMillis) {
        return formatter8.format(Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC));
    }

    public static synchronized String formatDateTimeUTCForPrint(Long timeInMillis) {
        if (timeInMillis == null) {
            return "";
        }
        return formatter8.format(Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC)).replaceAll("T", " ").replaceAll("Z", "");
    }

    public static synchronized LocalDate parseDate(String dateString) {
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }

    public static BigDecimal calcXPercentsFromY(BigDecimal xPercents, BigDecimal fromY) {
        return fromY.divide(new BigDecimal("100"), RoundingMode.DOWN).multiply(xPercents);
    }


}
