package ru.odnoklassniki.proxyserver.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.odnoklassniki.proxyserver.handlers.AbstractConnectionHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * User: max
 * Date: 1/13/13
 * Time: 2:27 AM
 */
public class ProxyWorker implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyWorker.class);
  private static final int DEFAULT_SELECTION_TIMEOUT = 10;

  protected Selector selector = null;

  protected int selectionTimeOut = DEFAULT_SELECTION_TIMEOUT;

  private boolean interrupted = false;

  public ProxyWorker() throws IOException {
    selector = Selector.open();
  }

  public void interrupt() {
    interrupted = true;
  }

  public Selector getSelector() {
    return selector;
  }

  @Override
  public void run() {
    try {
      while (!interrupted && !Thread.interrupted()) {
        int select = selector.select(selectionTimeOut);
        if (select != 0) {
          for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
            SelectionKey selectionKey = i.next();
            try {
              i.remove();
              if (selectionKey.isValid()) {
                AbstractConnectionHandler handler = (AbstractConnectionHandler) selectionKey.attachment();
                handler.handleKey(selectionKey);
              }
            } catch (RuntimeException e) {
              selectionKey.cancel();
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Proxy worker interrupted abnormally.");
    } finally {
      try {
        if (selector != null) {
          selector.close();
        }
      } catch (IOException e) {
        LOGGER.error("Problem on closing proxy worker selector.");
      }
    }
  }
}
