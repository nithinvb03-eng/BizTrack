package com.sathya.co.biztrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sathya.co.biztrack.util.CalculationUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculationUtilTests {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @Test
    void sumAddsAllAmounts() {
        assertEquals(0, d("26000").compareTo(CalculationUtil.sum(List.of(d("18500"), d("7500")))));
    }

    @Test
    void profitIsInflowMinusOutflow() {
        assertEquals(0, d("21900").compareTo(CalculationUtil.profit(d("26000"), d("4100"))));
    }

    @Test
    void marginIsProfitOverInflowTimes100() {
        assertEquals(0, d("84.2").compareTo(CalculationUtil.percent(d("21900"), d("26000"))));
    }

    @Test
    void percentChangeComparesWithLastMonth() {
        assertEquals(0, d("28.7").compareTo(CalculationUtil.percentChange(d("121000"), d("94000"))));
    }

    @Test
    void averageDividesByCount() {
        assertEquals(0, d("93667").compareTo(CalculationUtil.average(d("562000"), 6)));
    }

    @Test
    void ratioDividesInflowByOutflow() {
        assertEquals(0, d("1.48").compareTo(CalculationUtil.ratio(d("562000"), d("379000"))));
    }

    @Test
    void dividingByZeroGivesZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(CalculationUtil.percent(d("100"), BigDecimal.ZERO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(CalculationUtil.ratio(d("100"), BigDecimal.ZERO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(CalculationUtil.average(d("100"), 0)));
    }
}