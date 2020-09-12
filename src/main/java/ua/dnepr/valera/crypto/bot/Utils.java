package ua.dnepr.valera.crypto.bot;

import ua.dnepr.valera.crypto.bot.model.MyPosition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Utils {

    public static final String HISTORY_FILE_HEADER = "id,price,qty,time,isBuyerMaker";
    public static final String COMMA_DELIMITER = ",";
    public static final String NEW_LINE_SEPARATOR = "\n";

    public static final int AMOUNT_PRECISION_BTC = 3;

    private static final DateTimeFormatter DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter formatter8 = DateTimeFormatter.ISO_INSTANT;

    private static final NumberFormat priceFormat = DecimalFormat.getInstance(Locale.ROOT);
    private static final NumberFormat amountFormat = DecimalFormat.getInstance(Locale.ROOT);


    static {
        priceFormat.setMaximumFractionDigits(2);
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setGroupingUsed(false);

        amountFormat.setMaximumFractionDigits(3);
        amountFormat.setMinimumFractionDigits(1);
        amountFormat.setGroupingUsed(false);
    }


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

    public static ZonedDateTime parseDateTime(long timeInMillis) {
        return Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC);
    }

    public static BigDecimal calcXPercentsFromY(BigDecimal xPercents, BigDecimal fromY) {
        return fromY.divide(new BigDecimal("100"), RoundingMode.DOWN).multiply(xPercents);
    }

    /* How match percents X stands of Y*/
    public static BigDecimal calcXOfYInPercents(BigDecimal xAmount, BigDecimal yAmount) {
        if (xAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal coef = yAmount.divide(xAmount, 6, RoundingMode.UP);
        return new BigDecimal("100").divide(coef, 6, RoundingMode.DOWN);
    }

    public static BigDecimal calcAmountToAverageAtPrice(BigDecimal initialAmount, BigDecimal initialPrice, BigDecimal desiredPrice, BigDecimal currentPrice) {
        BigDecimal pr1 = initialPrice.subtract(desiredPrice);
        BigDecimal pr2 = desiredPrice.subtract(currentPrice);
        BigDecimal pr3 = pr1.divide(pr2, AMOUNT_PRECISION_BTC, RoundingMode.UP); // TODO check scale for Satoshi coins
        return initialAmount.multiply(pr3).setScale(AMOUNT_PRECISION_BTC, RoundingMode.UP);
    }

    public static BigDecimal calcNewTakeForNewEntryAndAmount(MyPosition.Side positionSide, BigDecimal expectedPnL, BigDecimal newEntryPrice, BigDecimal newAmount) {
        if (positionSide.equals(MyPosition.Side.LONG)) {
            return expectedPnL.divide(newAmount, RoundingMode.DOWN).add(newEntryPrice);
        } else {
            return newEntryPrice.subtract(expectedPnL.divide(newAmount, RoundingMode.DOWN));
        }
    }

    public static BigDecimal calcNewStopForNewEntryAndAmount(MyPosition.Side positionSide, BigDecimal expectedPnL, BigDecimal newEntryPrice, BigDecimal newAmount) {
        if (positionSide.equals(MyPosition.Side.LONG)) {
            return newEntryPrice.subtract(expectedPnL.divide(newAmount, RoundingMode.DOWN));
        } else {
            return expectedPnL.divide(newAmount, RoundingMode.DOWN).add(newEntryPrice);
        }
    }

    public static String formatPrice(BigDecimal price) {
        synchronized (priceFormat) {
            return priceFormat.format(price);
        }
    }

    public static String formatAmount(BigDecimal amount) {
        synchronized (amountFormat) {
            return amountFormat.format(amount);
        }
    }

//    public static BigDecimal dropInAnHour() { // TODO implement
//
//    }
//
//    public static BigDecimal growthInAnHour() {
//
//    }

}
