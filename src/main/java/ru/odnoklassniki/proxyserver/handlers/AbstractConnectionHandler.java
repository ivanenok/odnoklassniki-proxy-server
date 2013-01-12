package ru.odnoklassniki.proxyserver.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * User: max
 * Date: 1/13/13
 * Time: 2:14 AM
 */
public abstract class AbstractConnectionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnectionHandler.class);

  public abstract void initialize(Selector selector) throws IOException;

  public abstract void handleKey(SelectionKey key);

  protected void closeChannel(Channel channel){
    try {
      channel.close();
    } catch (IOException e) {
      LOGGER.error("Problem on closing channel.");
    }
  }

}
