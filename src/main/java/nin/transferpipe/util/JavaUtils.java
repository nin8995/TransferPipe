package nin.transferpipe.util;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaUtils {

    public static Class<?> getCommonSuper(List<?> objects) {
        return objects.stream()
                .map(JavaUtils::getSupers)
                .min(Comparator.comparingInt(List::size)).get().stream()
                .filter(classToCheck -> objects.stream().allMatch(o -> classToCheck.isAssignableFrom(o.getClass())))
                .findFirst().get();
    }

    public static List<Class<?>> getSupers(Object o) {
        var classes = new ArrayList<Class<?>>();

        var currentClass = o.getClass();
        while (currentClass != null) {
            classes.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }

        return classes;
    }

    @Nullable
    public static Class<?> getCommonInterface(List<?> objects) {
        return getInterfaces(objects).stream()
                .filter(classToCheck -> objects.stream().allMatch(o -> classToCheck.isAssignableFrom(o.getClass())))
                .findFirst().orElse(null);
    }

    public static List<Class<?>> getInterfaces(List<?> objects) {
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
}
