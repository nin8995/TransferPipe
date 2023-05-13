package nin.transferpipe.util.java;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class OptionalStream<V> {

    public Stream<Optional<V>> stream;

    public OptionalStream(Stream<Optional<V>> stream) {
        this.stream = stream;
    }

    public static <V> OptionalStream<V> of(Collection<Optional<V>> collection) {
        return new OptionalStream<>(collection.stream());
    }

    public static <U, V> OptionalStream<V> of(Collection<U> collection, Function<U, Optional<V>> optionalMapper) {
        return new OptionalStream<>(collection.stream().map(optionalMapper));
    }

    public Optional<V> findFirst() {
        return stream.filter(Optional::isPresent)
                .findFirst().orElse(Optional.empty());
    }
}
