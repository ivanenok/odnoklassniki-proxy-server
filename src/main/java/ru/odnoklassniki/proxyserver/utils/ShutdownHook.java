package ru.odnoklassniki.proxyserver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.odnoklassniki.proxyserver.ProxyServer;

/**
 * User: max
 * Date: 1/13/13
 * Time: 12:36 AM
 */
public class ShutdownHook extends Thread {
  private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

  private final ProxyServer proxyServer;

  public ShutdownHook(final ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Override
  public void run() {
    LOGGER.info("Proxy server will be stopped...");
    proxyServer.stop();
    LOGGER.info("Proxy server stopped.");
  }
}
