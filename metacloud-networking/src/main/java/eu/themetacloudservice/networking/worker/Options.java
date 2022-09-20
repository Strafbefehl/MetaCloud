package io.metacloud.worker;


public class Options<T> {

    public static final Options<Integer> TIMEOUT = new Options<>(-1);
    public static final Options<Integer> BUFFER_SIZE = new Options<>(1024);
    public static final Options<Boolean> PERFORMANCE_BOOST = new Options<>(true);
    public T value;

    protected Options(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        this.value = (T) value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
