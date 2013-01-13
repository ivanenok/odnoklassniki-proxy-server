package ru.odnoklassniki.proxyserver.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.odnoklassniki.proxyserver.utils.collections.CyclicObjectsProvider;
import ru.odnoklassniki.proxyserver.workers.ProxyWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * User: max
 * Date: 1/12/13
 * Time: 6:42 PM
 */
public class ServerConnectionHandler extends AbstractConnectionHandler {

  private final CyclicObjectsProvider<ProxyWorker> workers;
  private final int localPort;
  private final String remoteHost;
  private final int remotePort;

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerConnectionHandler.class);
  private ServerSocketChannel serverSocketChannel;

  public ServerConnectionHandler(final CyclicObjectsProvider<ProxyWorker> workers, int localPort, String remoteHost, int remotePort) {
    this.workers = workers;
    this.localPort = localPort;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
  }

  private ProxyWorker resolveWorker() {
    return workers.next();
  }

  @Override
  public void initialize(Selector selector) throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(localPort));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);

    LOGGER.info("Proxy server socket handler started on local port: " + localPort + ", remote host: {}, remote port: {}", remoteHost, remotePort);
  }

  @Override
  public void handleKey(SelectionKey key) {
    if (key.isAcceptable()){
      LOGGER.info("Incoming request accepted on local port: {}", localPort);
      try {
        SocketChannel localChannel = ((ServerSocketChannel) key.channel()).accept();
        localChannel.configureBlocking(false);
        try {
          AbstractConnectionHandler connectionHandler = new TunnellingConnectionHandler(localChannel, new InetSocketAddress(InetAddress.getByName(remoteHost), remotePort));
          connectionHandler.initialize(resolveWorker().getSelector());
        } catch (IOException e) {
          closeChannel(localChannel);
          LOGGER.error("Problem on attachment connaction handler for tunnelling.");
        }
      } catch (IOException e) {
//      closeChannel(serverSocketChannel);
        key.cancel();
      }
    } else {
      LOGGER.error("Unexpected state of selection key.");
    }
  }
}
