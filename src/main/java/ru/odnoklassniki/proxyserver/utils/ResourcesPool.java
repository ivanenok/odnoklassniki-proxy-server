package ru.odnoklassniki.proxyserver.utils;

import java.util.ArrayDeque;

/**
 * User: max
 * Date: 1/13/13
 * Time: 4:20 AM
 */
public abstract class ResourcesPool<T> {
  protected ArrayDeque<T> cache;
  private int initialSize;

  public ResourcesPool(int initialSize) {
    this.initialSize = initialSize;
    cache = new ArrayDeque<>(initialSize);
  }

  protected void fillInitialCache(){
    for (int i = 0; i < initialSize; i++){
      cache.add(createItem());
    }
  }

  protected abstract T createItem();

  public synchronized T allocate(){
    T result = cache.poll();
    if (result == null){
      result = createItem();
    }
    return result;
  }

  public synchronized void release(T item){
    cache.add(item);
  }
}
