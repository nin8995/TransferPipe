package nin.transferpipe.util.java;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
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
}
