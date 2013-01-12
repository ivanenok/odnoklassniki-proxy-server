package ru.odnoklassniki.proxyserver.utils;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * User: max
 * Date: 1/12/13
 * Time: 7:05 PM
 */
public class CyclicObjectsProvider<T> {
  private final ArrayDeque<T> objects = new ArrayDeque<T>();

  public synchronized void add(T object) {
    objects.add(object);
  }

  public synchronized T next() {
    T result = objects.poll();
    objects.add(result);
    return result;
  }

  public Collection<T> getAll(){
    return objects;
  }
}
