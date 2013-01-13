package ru.odnoklassniki.proxyserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.odnoklassniki.proxyserver.handlers.ServerConnectionHandler;
import ru.odnoklassniki.proxyserver.utils.collections.CyclicObjectsProvider;
import ru.odnoklassniki.proxyserver.utils.config.Config;
import ru.odnoklassniki.proxyserver.workers.ProxyWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * User: max
 * Date: 1/12/13
 * Time: 6:13 PM
 */
public class ProxyServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);

  private final ArrayList<Future> workersFutures = new ArrayList<>();

  private final CyclicObjectsProvider<ProxyWorker> workers = new CyclicObjectsProvider<>();

  private static final int PROXY_WORKERS_COUNT = 16; // can be aligned to processors count

  private static final ExecutorService PROXY_WORKERS_EXECUTOR = new ThreadPoolExecutor(
                                                                  PROXY_WORKERS_COUNT,
                                                                  PROXY_WORKERS_COUNT,
                                                                  1,
                                                                  TimeUnit.MINUTES,
                                                                  new ArrayBlockingQueue<Runnable>(1));

  public void start() {
    try {
      initializeWorkers();
      initializeServerSockets();
      startWorkers();
    } catch (IOException e) {
      LOGGER.error("Can't initialize server correctly.");
      stop();
    }
  }

  private void startWorkers() {
    for (ProxyWorker proxyWorker : workers.getAll()) {
      workersFutures.add(PROXY_WORKERS_EXECUTOR.submit(proxyWorker));
    }
 }

  private void initializeServerSockets() throws IOException {
    Config config = Config.getConfig();
    for (Config.ProxyServerSocket proxyServerSocket : config.getProxyServerSockets()) {
      ServerConnectionHandler handler = new ServerConnectionHandler(
                                              workers,
                                              proxyServerSocket.getLocalPort(),
                                              proxyServerSocket.getRemoteHost(),
                                              proxyServerSocket.getRemotePort());
      handler.initialize(workers.next().getSelector());
    }
  }

  private void initializeWorkers() throws IOException {
    for (int i = 0; i < PROXY_WORKERS_COUNT; i++) {
      ProxyWorker worker = new ProxyWorker();
      workers.add(worker);
    }
  }

  public void stop() {
    for (Future worker : workersFutures) {
      worker.cancel(true);
    }

    PROXY_WORKERS_EXECUTOR.shutdown();

  }

  public static void main(String[] args) {
    ProxyServer proxyServer = new ProxyServer();
    proxyServer.start();
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(proxyServer));
  }


  private static class ShutdownHook extends Thread {
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

}
