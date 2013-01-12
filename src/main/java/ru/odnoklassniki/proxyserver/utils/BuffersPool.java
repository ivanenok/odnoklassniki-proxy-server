package ru.odnoklassniki.proxyserver.utils;

import java.nio.ByteBuffer;

/**
 * User: max
 * Date: 1/13/13
 * Time: 4:06 AM
 */
public class BuffersPool extends ResourcesPool<ByteBuffer> {
  private int bufferSize;

  public BuffersPool(int initialSize, int bufferSize) {
    super(initialSize);
    this.bufferSize = bufferSize;
    fillInitialCache();
  }

  @Override
  protected ByteBuffer createItem() {
    return ByteBuffer.allocateDirect(bufferSize);
  }
}
