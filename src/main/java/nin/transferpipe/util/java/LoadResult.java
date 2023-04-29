package nin.transferpipe.util.java;

public sealed class LoadResult<V> permits LoadResult.A, LoadResult.NA, LoadResult.NL {

    public static <V> A<V> a(V v) {
        return new A<>(v);
    }

    public static <V> NA<V> na() {
        return new NA<>();
    }

    public static <V> NL<V> nl() {
        return new NL<>();
    }

    public static final class A<V> extends LoadResult<V> {

        public final V v;

        public A(V v) {
            this.v = v;
        }
    }

    public static final class NA<V> extends LoadResult<V> {
    }

    public static final class NL<V> extends LoadResult<V> {
    }
}
