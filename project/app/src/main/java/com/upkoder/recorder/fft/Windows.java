package com.upkoder.recorder.fft;

/**
 * Created by leandro on 4/10/2016.
 */
public class Windows {
    /**
     *
     * @param n Must to be greater than one (1).
     * @return Array of double of shape [n] containing signal multiplier.
     */
    public static double[] hanning(int n) {
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
            result[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1));
        return result;
    }

    /**
     *
     * @param n Must to be greater than one (1).
     * @return Array of double of shape [n] containing signal multiplier.
     */
    public static double[] hamming(int n) {
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
            result[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
        return result;
    }

    public static double[] identity(int n) {
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
            result[i] = 1;
        return result;
    }

    public static double weight(double[] values) {
        double w = 0;
        for (double u: values)
            w += u * u;
        return w / values.length;
    }

    public static double[] byName(String name, int n) {
        switch (name) {
            case "hamming":
                return hamming(n);
            case "hanning":
                return hanning(n);
            default:
                return identity(n);
        }
    }
}
