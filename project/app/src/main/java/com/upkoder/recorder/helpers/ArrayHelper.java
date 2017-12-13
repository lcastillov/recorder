package com.upkoder.recorder.helpers;

/**
 * Created by leandro on 4/9/2016.
 */
public class ArrayHelper {
    public static short[] segment(short[] data, int start, int end) {
        short[] result = new short[end - start];
        for (int i = 0; i < result.length; i++)
            result[i] = data[i + start];
        return result;
    }

    public static short[] concat(short[] a, short[] b) {
        short[] c = new short[a.length + b.length];
        int currentIndex = 0;
        for (int i = 0; i < a.length; i++) c[currentIndex++] = a[i];
        for (int i = 0; i < b.length; i++) c[currentIndex++] = b[i];
        return c;
    }
}
