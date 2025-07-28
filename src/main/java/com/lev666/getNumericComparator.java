package com.lev666;

import java.io.File;
import java.util.Comparator;

public class getNumericComparator {
    public static Comparator<File> get() {
        return (f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();

            String numStr1 = name1.replaceAll("\\D", "");
            String numStr2 = name2.replaceAll("\\D", "");

            if (numStr1.isEmpty() && numStr2.isEmpty()) {
                return name1.compareTo(name2);
            } else if (numStr1.isEmpty()) {
                return -1;
            } else if (numStr2.isEmpty()) {
                return 1;
            }

            try {
                int num1 = Integer.parseInt(numStr1);
                int num2 = Integer.parseInt(numStr2);

                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return name1.compareTo(name2);
            }
        };
    }
}
