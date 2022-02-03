import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommonConsts {
    public static double[] randMemory = new double[1000000];   // массив предгенерированных случайных чисел
    public static int randIdx = 0;                             // указатель текущего случайного числа
    static {                                                    // предгенерация массива случайных чисел
        for (int i = 0; i < randMemory.length; i++) {
            randMemory[i] = Math.random();
        }
    }
    public static void regenerate() {
        for (int i = 0; i < randMemory.length; i++) {
            randMemory[i] = Math.random();
        }
    }

    // families data
    public static Map<Integer, Integer> familiesCount = new HashMap<>();

    public static void clearFamilies() {
        familiesCount.clear();
    }
}
