package nin.transferpipe.util.java;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface JavaUtils {

    /**
     * 共通クラス取得
     */
    static Class<?> getCommonSuper(List<?> objects) {
        return objects.stream()
                .map(JavaUtils::getSupers)
                .min(Comparator.comparingInt(List::size)).get().stream()
                .filter(classToCheck -> objects.stream().allMatch(o -> classToCheck.isAssignableFrom(o.getClass())))
                .findFirst().get();
    }

    static List<Class<?>> getSupers(Object o) {
        var classes = new ArrayList<Class<?>>();

        var currentClass = o.getClass();
        while (currentClass != null) {
            classes.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }

        return classes;
    }

    @Nullable
    static Class<?> getCommonInterface(List<?> objects) {
        return getInterfaces(objects).stream()
                .filter(classToCheck -> objects.stream().allMatch(o -> classToCheck.isAssignableFrom(o.getClass())))
                .findFirst().orElse(null);
    }

    static List<Class<?>> getInterfaces(List<?> objects) {
        var interfaces = new ArrayList<Class<?>>();

        var currentInterfaces = new ArrayList<Class<?>>();
        var currentSupers = new ArrayList<Class<?>>();
        objects.forEach(o -> {
            currentInterfaces.addAll(Arrays.stream(o.getClass().getInterfaces()).toList());
            currentSupers.add(o.getClass().getSuperclass());
        });

        while (!(currentInterfaces.isEmpty() && currentSupers.isEmpty())) {
            interfaces.addAll(currentInterfaces);
            var interfacesToAdd = Stream.concat(currentInterfaces.stream(), currentSupers.stream()).flatMap(c -> Arrays.stream(c.getInterfaces())).collect(Collectors.toSet());
            var supersToAdd = currentSupers.stream().map(Class::getSuperclass).filter(Objects::nonNull).collect(Collectors.toSet());
            currentInterfaces.clear();
            currentInterfaces.addAll(interfacesToAdd);
            currentSupers.clear();
            currentSupers.addAll(supersToAdd);
        }

        return interfaces;
    }

    /**
     * Enumのループ検索
     */
    static <T extends Enum<T>> T front(T current, T[] values) {
        return current.ordinal() == values.length - 1 ? values[0] : values[current.ordinal() + 1];
    }

    static <T extends Enum<T>> T findNext(Collection<T> toSearch, T current, T[] values) {
        var searching = front(current, values);
        while (true) {
            if (toSearch.contains(searching))
                return searching;
            searching = front(searching, values);
        }
    }

    static <T extends Enum<T>> T back(T current, T[] values) {
        return current.ordinal() == 0 ? values[values.length - 1] : values[current.ordinal() - 1];
    }

    static <T extends Enum<T>> T findPrev(Collection<T> toSearch, T current, T[] values) {
        var searching = back(current, values);
        while (true) {
            if (toSearch.contains(searching))
                return searching;
            searching = back(searching, values);
        }
    }

    /**
     * 省略
     */
    static boolean fork(boolean forker, boolean ifTrue, boolean ifFalse) {
        if (forker)
            return ifTrue;
        else
            return ifFalse;
    }

    static double log(double base, double antilogarithm) {
        return Math.log(antilogarithm) / Math.log(base);
    }

    static <V> List<V> filter(List<V> list, Predicate<V> filter) {
        return list.stream().filter(filter).toList();
    }

    @Nullable
    static <V> V findFirst(List<V> list, Predicate<V> filter) {
        return list.stream().filter(filter).findFirst().orElse(null);
    }

    @Nullable
    static <V, Y> Y findFirst(List<V> list, Function<V, Y> mapper, Predicate<Y> filter) {
        return list.stream().map(mapper).filter(filter).findFirst().orElse(null);
    }

    static <V> Set<V> filter(Set<V> set, Predicate<V> filter) {
        return filter(set.stream(), filter);
    }

    static <V> Set<V> filter(Stream<V> stream, Predicate<V> filter) {
        return stream.filter(filter).collect(Collectors.toSet());
    }

    static <V, Y> List<Y> map(List<V> set, Function<V, Y> mapper) {
        return set.stream().map(mapper).toList();
    }

    static <V, Y> Set<Y> map(Set<V> set, Function<V, Y> mapper) {
        return map(set.stream(), mapper);
    }

    static <V, Y> Set<Y> map(Stream<V> stream, Function<V, Y> mapper) {
        return stream.map(mapper).collect(Collectors.toSet());
    }

    static void printStackTrace() {
        Arrays.stream(Thread.currentThread().getStackTrace()).forEach(System.out::println);
    }

    static <T> int recursion(Collection<T> looper, int initial, BiFunction<T, Integer, Integer> func) {
        var i = initial;
        for (T t : looper) {
            if (i <= 0)
                break;

            i = func.apply(t, i);
        }
        return i;
    }

    static <T> int decrementRecursion(Collection<T> looper, int initial, BiFunction<T, Integer, Integer> decrementGetter, BiConsumer<T, Integer> func) {
        var i = initial;
        for (T t : looper) {
            if (i <= 0)
                break;
            var decrement = decrementGetter.apply(t, i);
            if (decrement <= 0)
                break;

            func.accept(t, decrement);
            i -= decrement;
        }
        return i;
    }

    static <T> void forEach(Collection<T> looper, Supplier<Boolean> breaker, Consumer<T> func) {
        for (T t : looper) {
            if (breaker.get())
                break;
            func.accept(t);
        }
    }

    static <T> boolean isPresentAndThen(Optional<T> optional, Consumer<T> andThen) {
        optional.ifPresent(andThen);
        return optional.isPresent();
    }

    static boolean extraCondition(boolean premise, boolean extra) {
        return !(premise && !extra);
    }
}
