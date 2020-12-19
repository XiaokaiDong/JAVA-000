package io.kimmking.rate;

public class RateUtil {
    public static final double USD_CNY_RATE = 6.8;

    public static int CNY2USD(int amount) {
        return (int) (amount / 6.8);
    }

    public static int USD2CNY(int amount) {
        return (int) (amount * 6.8);
    }
}
