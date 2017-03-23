package util.lang;

import java.io.Closeable;

/**
 * An Iteration is a typed Iterator-like object that can throw (typed) Exceptions while iterating.
 * This is used in cases where the iteration is lazy and evaluates over a files.
 * In such cases an error can occur at any time and needs to be communicated through a checked exception,
 *    something Iterator can not do (it can only throw {@link RuntimeException)s.
 *
 * @param <X> Object type of objects contained in the iteration.
 * @param <E> Exception type that is thrown when a problem occurs during iteration.
 */
public interface ExceptionalIterator<X, E extends Exception> extends Closeable {
    boolean hasNext();
    X next() throws E;
    void remove();
}