package Utils;

import java.util.Arrays;

public class CommonUtils {
    public static void joinSafe(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static double AverageOfArray(Integer [] array) {
        return Arrays.stream(array).mapToInt(Integer::intValue).average().orElse(Double.NaN);
    }
}
