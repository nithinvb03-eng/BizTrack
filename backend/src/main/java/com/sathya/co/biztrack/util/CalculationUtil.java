package com.sathya.co.biztrack.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculationUtil {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private CalculationUtil() {
    }

    /** Add: total of a list of amounts. */
    public static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            total = total.add(v);
        }
        return total;
    }

    /** Subtract: profit = inflow - outflow. */
    public static BigDecimal profit(BigDecimal inflow, BigDecimal outflow) {
        return inflow.subtract(outflow);
    }

    /** Percent: part / whole * 100, one decimal. Returns 0 when whole is 0 (no divide-by-zero). */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    /** Percent change: (current - previous) / previous * 100. */
    public static BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        return percent(current.subtract(previous), previous);
    }

    /** Average: total / count, rounded to a whole number. Returns 0 when count is 0. */
    public static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    /** Ratio: a / b, two decimals. Returns 0 when b is 0. */
    public static BigDecimal ratio(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return a.divide(b, 2, RoundingMode.HALF_UP);
    }
}