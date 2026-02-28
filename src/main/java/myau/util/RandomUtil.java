package myau.util;

import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;

import java.util.Random;

public class RandomUtil {
    private static final Random theRandom = new Random();

    public static long nextLong(long min, long max) {
        return (long) nextDouble((double) min, (double) (max + 1L));
    }

    public static long nextLong(IntProperty min, IntProperty max) {
        return nextLong(min.getValue().longValue(), max.getValue().longValue());
    }

    public static float nextFloat(float min, float max) {
        return theRandom.nextFloat() * (max - min) + min;
    }

    public static float nextFloat(FloatProperty min, FloatProperty max) {
        return nextFloat(min.getValue(), max.getValue());
    }

    public static double nextDouble(double min, double max) {
        return theRandom.nextDouble() * (max - min) + min;
    }
}
