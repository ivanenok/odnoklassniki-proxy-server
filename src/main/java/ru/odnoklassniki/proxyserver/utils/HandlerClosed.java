package ru.odnoklassniki.proxyserver.utils;

/**
 * User: max
 * Date: 1/12/13
 * Time: 8:26 PM
 */
public class HandlerClosed extends Exception {
  public HandlerClosed() {
  }

  public HandlerClosed(Throwable cause) {
    super(cause);
  }
}
