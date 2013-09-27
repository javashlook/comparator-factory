package org.javashlook.util.comparatorfactory;

public final class ComparatorHelper {

    private ComparatorHelper() {}

    public static int compare(Comparable x, Comparable y) {
        if (x == y) {
            return 0;
        }
        else if (x == null) {
            return Integer.MIN_VALUE;
        }
        else if (y == null) {
            return Integer.MAX_VALUE;
        }
        else {
            return x.compareTo(y);
        }
    }

}