package ru.odnoklassniki.proxyserver.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.odnoklassniki.proxyserver.utils.HandlerClosed;
import ru.odnoklassniki.proxyserver.utils.pools.BuffersPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * User: max
 * Date: 1/12/13
 * Time: 6:28 PM
 */
public class TunnellingConnectionHandler extends AbstractConnectionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TunnellingConnectionHandler.class);
  private static final int BUFFERS_SIZE = 2048;
  private static final int INITIAL_BUFFERS_POOL_SIZE = 512;

  private static final BuffersPool BUFFERS_POOL = new BuffersPool(INITIAL_BUFFERS_POOL_SIZE, BUFFERS_SIZE);

  private final InetSocketAddress address;
  private Selector registeredSelector;

  private final SocketChannel localChannel;
  private SocketChannel remoteChannel;

  private ByteBuffer localBuffer = BUFFERS_POOL.allocate();
  private ByteBuffer remoteBuffer = BUFFERS_POOL.allocate();

  private boolean localBufferEmpty = true;
  private boolean remoteBufferEmpty = true;

  public TunnellingConnectionHandler(SocketChannel localChannel, InetSocketAddress address) {
    this.localChannel = localChannel;
    this.address = address;
  }

  @Override
  public void initialize(Selector selector) throws IOException {
    this.remoteChannel = SocketChannel.open(address);
    this.remoteChannel.configureBlocking(false);

    registeredSelector = selector;

    LOGGER.debug("Handler {} assigned to selector {}", this, selector);

    updateChannelsInterests();
  }

  private void updateChannelsInterests() throws ClosedChannelException {
    updateInterestsByBuffersFilling(localBufferEmpty, remoteBufferEmpty, localChannel);
    updateInterestsByBuffersFilling(remoteBufferEmpty, localBufferEmpty, remoteChannel);
  }

  private void updateInterestsByBuffersFilling(boolean isReadBufferEmpty, boolean isWriteBufferEmpty, SocketChannel channel) throws ClosedChannelException {
    int ops = 0;
    if (isReadBufferEmpty) {
      ops |= SelectionKey.OP_READ;
    }
    if (!isWriteBufferEmpty) {
      ops |= SelectionKey.OP_WRITE;
    }
    channel.register(registeredSelector, ops, this);
  }

  @Override
  public void handleKey(SelectionKey key) {
    try {
      SocketChannel channel = (SocketChannel)key.channel();
      if (key.isReadable()) {
        tryRead(channel);
      } else if (key.isWritable()) {
        tryWrite(channel);
      }
      updateChannelsInterests();
    } catch (HandlerClosed ex) {
      LOGGER.debug("Local and remote channels will be closed and key will be invalidated. Handler is {}", this);
      closeChannelsAndKey(key);
      clearBuffers();
    } catch (ClosedChannelException e) {
      LOGGER.error("Unexpected channel closing. Local and remote channels will be closed and key will be invalidated.", e);
      closeChannelsAndKey(key);
      clearBuffers();
    }
  }

  private void clearBuffers() {
    localBuffer.clear();
    BUFFERS_POOL.release(localBuffer);
    localBufferEmpty = true;
    remoteBuffer.clear();
    BUFFERS_POOL.release(remoteBuffer);
    remoteBufferEmpty = true;
  }

  private void closeChannelsAndKey(SelectionKey key){
    closeChannels();
    key.cancel();
  }

  private void closeChannels(){
    closeChannel(localChannel);
    closeChannel(remoteChannel);
  }

  private boolean isRemoteChannel(SocketChannel channel) {
    return channel == remoteChannel;
  }

  private boolean isLocalChannel(SocketChannel channel) {
    return channel == localChannel;
  }

  private void tryRead(SocketChannel channel) throws HandlerClosed {
    if (isLocalChannel(channel)) {
      if (localBufferEmpty) {
        if (readToBuffer(channel, localBuffer)) {
          localBufferEmpty = false;
        }
      }
    } else if (isRemoteChannel(channel)) {
      if (remoteBufferEmpty) {
        if (readToBuffer(channel, remoteBuffer)) {
          remoteBufferEmpty = false;
        }
      }
    }
  }

  private boolean readToBuffer(SelectableChannel channel, ByteBuffer buffer) throws HandlerClosed {
    boolean success = false;
    ReadableByteChannel readableChannel = (ReadableByteChannel) channel;
    try {
      int read = readableChannel.read(buffer);
      if (read > 0) {
        buffer.flip();
        success = true;
      }
      if (read == -1) {
        throw new HandlerClosed();
      }
    } catch (IOException e) {
      throw new HandlerClosed(e);
    }
    return success;
  }

  private void tryWrite(SocketChannel channel) throws HandlerClosed {
    if (isLocalChannel(channel)) {
      if (!remoteBufferEmpty) {
        if (writeToChannel(localChannel, remoteBuffer)) {
          remoteBufferEmpty = true;
        }
      }
    } else if (isRemoteChannel(channel)) {
      if (!localBufferEmpty) {
        if (writeToChannel(remoteChannel, localBuffer)) {
          localBufferEmpty = true;
        }
      }
    }
  }

  private boolean writeToChannel(SocketChannel channel, ByteBuffer buffer) throws HandlerClosed {
    boolean success = false;
    try {
      channel.write(buffer);
      if (!buffer.hasRemaining()) {
        buffer.clear();
        success = true;
      }
    } catch (IOException e) {
      throw new HandlerClosed();
    }
    return success;
  }
}