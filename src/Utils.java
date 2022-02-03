import java.util.Arrays;
import java.util.List;

public class Utils {
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
