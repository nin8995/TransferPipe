package nin.transferpipe.util.java;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ExceptionPredicate {

    public static boolean succeeded(Runnable throwableFunc) {
        try {
            throwableFunc.run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean failed(Runnable throwableFunc) {
        try {
            throwableFunc.run();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static <T> Predicate<T> succeeded(Consumer<T> throwableFunc) {
        return t -> {
            try {
                throwableFunc.accept(t);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    public static <T> Predicate<T> failed(Consumer<T> throwableFunc) {
        return t -> {
            try {
                throwableFunc.accept(t);
                return false;
            } catch (Exception e) {
                return true;
            }
        };
    }
}
