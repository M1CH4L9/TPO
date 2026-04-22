/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {

  private final String host;
  private final int port;
  private final String id;
  private final StringBuilder chatView = new StringBuilder();
  private final Object chatLock = new Object();
  private final Queue<ByteBuffer> outbound = new ConcurrentLinkedQueue<>();
  private final CountDownLatch terminated = new CountDownLatch(1);
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean closeRequested = new AtomicBoolean(false);

  private volatile Selector selector;
  private volatile SocketChannel channel;
  private volatile SelectionKey key;
  private volatile Thread ioThread;

  public ChatClient(String host, int port, String id) {
    this.host = Objects.requireNonNull(host);
    this.port = port;
    this.id = Objects.requireNonNull(id);
    synchronized (chatLock) {
      chatView.append("=== ").append(id).append(" chat view\n");
    }
  }

  public void login() {
    if (!started.compareAndSet(false, true)) {
      return;
    }

    try {
      selector = Selector.open();
      channel = SocketChannel.open();
      channel.configureBlocking(false);
      key = channel.register(selector, SelectionKey.OP_CONNECT);
      channel.connect(new InetSocketAddress(host, port));
      ioThread = new Thread(this::runClientLoop, "chat-client-" + id);
      ioThread.start();
      enqueue("LOGIN\t" + id + "\n");
    } catch (IOException exc) {
      appendError(exc);
      closeResources();
      terminated.countDown();
    }
  }

  public void logout() {
    if (!started.get()) {
      return;
    }

    enqueue("LOGOUT\t" + id + "\n");
    closeRequested.set(true);
    wakeupSelector();

    try {
      terminated.await();
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      appendError(exc);
    }
  }

  public void send(String req) {
    if (!started.get()) {
      return;
    }
    enqueue("MSG\t" + req + "\n");
  }

  public String getChatView() {
    synchronized (chatLock) {
      return chatView.toString();
    }
  }

  private void enqueue(String text) {
    outbound.add(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    wakeupSelector();
  }

  private void wakeupSelector() {
    Selector currentSelector = selector;
    if (currentSelector != null) {
      currentSelector.wakeup();
    }
  }

  private void runClientLoop() {
    StringBuilder inbound = new StringBuilder();

    try {
      while (true) {
        updateInterestOps();
        Selector currentSelector = selector;
        if (currentSelector == null) {
          break;
        }

        currentSelector.select(100);
        Iterator<SelectionKey> iterator = currentSelector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey selectedKey = iterator.next();
          iterator.remove();

          if (!selectedKey.isValid()) {
            continue;
          }
          if (selectedKey.isConnectable()) {
            finishConnection();
          }
          if (selectedKey.isReadable()) {
            if (!readMessages(inbound)) {
              return;
            }
          }
          if (selectedKey.isWritable()) {
            writePending();
          }
        }

        if (closeRequested.get() && outbound.isEmpty()) {
          SocketChannel currentChannel = channel;
          if (currentChannel == null || !currentChannel.isOpen()) {
            break;
          }
        }
      }
    } catch (ClosedSelectorException ignored) {
    } catch (Exception exc) {
      appendError(exc);
    } finally {
      closeResources();
      terminated.countDown();
    }
  }

  private void finishConnection() throws IOException {
    SocketChannel currentChannel = channel;
    if (currentChannel != null && currentChannel.isConnectionPending()) {
      currentChannel.finishConnect();
    }
  }

  private boolean readMessages(StringBuilder inbound) throws IOException {
    SocketChannel currentChannel = channel;
    if (currentChannel == null) {
      return false;
    }

    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int read = currentChannel.read(buffer);
    if (read == -1) {
      return false;
    }
    if (read == 0) {
      return true;
    }

    buffer.flip();
    inbound.append(StandardCharsets.UTF_8.decode(buffer));

    int newlineIndex;
    while ((newlineIndex = inbound.indexOf("\n")) >= 0) {
      String line = inbound.substring(0, newlineIndex);
      inbound.delete(0, newlineIndex + 1);
      if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
        line = line.substring(0, line.length() - 1);
      }
      appendLine(line);
    }

    return true;
  }

  private void writePending() throws IOException {
    SocketChannel currentChannel = channel;
    if (currentChannel == null) {
      return;
    }

    while (true) {
      ByteBuffer buffer = outbound.peek();
      if (buffer == null) {
        return;
      }

      currentChannel.write(buffer);
      if (buffer.hasRemaining()) {
        return;
      }
      outbound.poll();
    }
  }

  private void updateInterestOps() {
    SelectionKey currentKey = key;
    SocketChannel currentChannel = channel;
    if (currentKey == null || !currentKey.isValid() || currentChannel == null) {
      return;
    }

    if (currentChannel.isConnectionPending()) {
      currentKey.interestOps(SelectionKey.OP_CONNECT);
      return;
    }

    int interestOps = SelectionKey.OP_READ;
    if (!outbound.isEmpty()) {
      interestOps |= SelectionKey.OP_WRITE;
    }
    currentKey.interestOps(interestOps);
  }

  private void appendLine(String line) {
    synchronized (chatLock) {
      chatView.append(line).append('\n');
    }
  }

  private void appendError(Exception exc) {
    synchronized (chatLock) {
      chatView.append("*** ").append(exc).append('\n');
    }
  }

  private void closeResources() {
    SelectionKey currentKey = key;
    if (currentKey != null) {
      currentKey.cancel();
    }

    SocketChannel currentChannel = channel;
    if (currentChannel != null) {
      try {
        currentChannel.close();
      } catch (IOException ignored) {
      }
    }

    Selector currentSelector = selector;
    if (currentSelector != null) {
      try {
        currentSelector.close();
      } catch (IOException ignored) {
      }
    }

    key = null;
    channel = null;
    selector = null;
  }
}
