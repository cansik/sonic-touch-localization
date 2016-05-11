package main.analyzer;

/**
 * Created by cansik on 11/05/16.
 */
@FunctionalInterface
public interface Function2 <A, B, R> {
    public R apply (A a, B b);
}
