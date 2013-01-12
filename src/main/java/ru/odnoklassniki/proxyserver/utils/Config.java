package ru.odnoklassniki.proxyserver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * User: max
 * Date: 1/12/13
 * Time: 11:27 PM
 */
public class Config {
  private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

  private static final Config CONFIG = new Config();

  private static final String LOCAL_PORT_SUFFIX = ".localPort";
  private static final String REMOTE_PORT_SUFFIX = ".remotePort";
  private static final String REMOTE_HOST_SUFFIX = ".remoteHost";

  private final ArrayList<ProxyServerSocket> proxyServerSockets = new ArrayList<>();

  private Config() {
    load();
  }

  public static Config getConfig() {
    return CONFIG;
  }

  private void load() {
    InputStream resourceAsStream = getClass().getResourceAsStream("/proxy-config.properties");
    Properties properties = new Properties();
    try {
      properties.load(resourceAsStream);
      for (String property : properties.stringPropertyNames()) {
        if (property.contains(LOCAL_PORT_SUFFIX)) {
          processConfigurationNode(properties, property);
        }
      }
    } catch (Exception e) {
      ProxyServerSocket defaultServerSocket = new ProxyServerSocket(8080, "www.odnoklassniki.ru", 80);
      LOGGER.error("Problem on loading config. Default settings will be used. " + defaultServerSocket);
      this.proxyServerSockets.add(defaultServerSocket);
    }
  }

  private void processConfigurationNode(Properties properties, String property) {
    try {
      String socketNameBase = property.replace(LOCAL_PORT_SUFFIX, "");
      int localPort = Integer.parseInt((String) properties.get(socketNameBase + LOCAL_PORT_SUFFIX));
      int remotePort = Integer.parseInt((String) properties.get(socketNameBase + REMOTE_PORT_SUFFIX));
      String remoteHost = (String) properties.get(socketNameBase + REMOTE_HOST_SUFFIX);
      ProxyServerSocket serverSocket = new ProxyServerSocket(localPort, remoteHost, remotePort);
      this.proxyServerSockets.add(serverSocket);
    } catch (NumberFormatException e) {
      LOGGER.warn("Node of configuration file corrupted. Skip it.");
    }
  }

  public List<ProxyServerSocket> getProxyServerSockets() {
    return Collections.unmodifiableList(proxyServerSockets);
  }

  public static class ProxyServerSocket {
    private int localPort = 0;

    private String remoteHost = "";
    private int remotePort = 0;

    public ProxyServerSocket(int localPort, String remoteHost, int remotePort) {
      this.localPort = localPort;
      this.remoteHost = remoteHost;
      this.remotePort = remotePort;
    }

    public int getLocalPort() {
      return localPort;
    }

    public String getRemoteHost() {
      return remoteHost;
    }

    public int getRemotePort() {
      return remotePort;
    }

    @Override
    public String toString() {
      return "ProxyServerSocket{" +
          "localPort=" + localPort +
          ", remoteHost='" + remoteHost + '\'' +
          ", remotePort=" + remotePort +
          '}';
    }
  }

}
