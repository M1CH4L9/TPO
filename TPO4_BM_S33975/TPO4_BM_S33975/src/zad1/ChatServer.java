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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  private final String host;
  private final int port;
  private final StringBuilder serverLog = new StringBuilder();
  private final Object logLock = new Object();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final CountDownLatch started = new CountDownLatch(1);
  private final Map<SocketChannel, ClientSession> sessions = new HashMap<>();

  private volatile Selector selector;
  private volatile ServerSocketChannel serverChannel;
  private volatile Thread serverThread;
  private volatile RuntimeException startupFailure;

  public ChatServer(String host, int port) {
    this.host = Objects.requireNonNull(host);
    this.port = port;
  }

  public void startServer() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    serverThread = new Thread(this::runServer, "chat-server");
    serverThread.start();

    try {
      started.await();
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while starting server", exc);
    }

    if (startupFailure != null) {
      running.set(false);
      throw startupFailure;
    }

    System.out.println("Server started");
  }

  public void stopServer() {
    if (!running.compareAndSet(true, false)) {
      if (serverThread == null) {
        return;
      }
    }

    Selector currentSelector = selector;
    if (currentSelector != null) {
      currentSelector.wakeup();
    }

    Thread thread = serverThread;
    if (thread != null && thread != Thread.currentThread()) {
      try {
        thread.join();
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while stopping server", exc);
      }
    }

    System.out.println("Server stopped");
  }

  public String getServerLog() {
    synchronized (logLock) {
      return serverLog.toString();
    }
  }

  private void runServer() {
    try (Selector openedSelector = Selector.open();
         ServerSocketChannel openedServerChannel = ServerSocketChannel.open()) {
      selector = openedSelector;
      serverChannel = openedServerChannel;

      openedServerChannel.configureBlocking(false);
      openedServerChannel.bind(new InetSocketAddress(host, port));
      openedServerChannel.register(openedSelector, SelectionKey.OP_ACCEPT);
      started.countDown();

      while (running.get()) {
        openedSelector.select();
        handleSelectedKeys(openedSelector.selectedKeys().iterator());
      }
    } catch (ClosedSelectorException ignored) {
      started.countDown();
    } catch (IOException | RuntimeException exc) {
      startupFailure = new IllegalStateException("Failed to start server", exc);
      started.countDown();
    } finally {
      closeAllSessions();
      selector = null;
      serverChannel = null;
    }
  }

  private void handleSelectedKeys(Iterator<SelectionKey> iterator) {
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();

      if (!key.isValid()) {
        continue;
      }

      try {
        if (key.isAcceptable()) {
          acceptClient();
        }
        if (key.isReadable()) {
          readFromClient(key);
        }
        if (key.isValid() && key.isWritable()) {
          writeToClient(key);
        }
      } catch (IOException exc) {
        closeSession((SocketChannel) key.channel(), false);
      }
    }
  }

  private void acceptClient() throws IOException {
    SocketChannel clientChannel = serverChannel.accept();
    if (clientChannel == null) {
      return;
    }

    clientChannel.configureBlocking(false);
    ClientSession session = new ClientSession(clientChannel);
    sessions.put(clientChannel, session);
    clientChannel.register(selector, SelectionKey.OP_READ, session);
  }

  private void readFromClient(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ClientSession session = (ClientSession) key.attachment();
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int read = channel.read(buffer);

    if (read == -1) {
      closeSession(channel, false);
      return;
    }

    if (read == 0) {
      return;
    }

    buffer.flip();
    session.inbound.append(StandardCharsets.UTF_8.decode(buffer));
    processInboundLines(session);
  }

  private void processInboundLines(ClientSession session) throws IOException {
    int newlineIndex;
    while ((newlineIndex = session.inbound.indexOf("\n")) >= 0) {
      String line = session.inbound.substring(0, newlineIndex);
      session.inbound.delete(0, newlineIndex + 1);

      if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
        line = line.substring(0, line.length() - 1);
      }

      handleCommand(session, line);
    }
  }

  private void handleCommand(ClientSession session, String line) throws IOException {
    String[] parts = line.split("\t", 2);
    String command = parts[0];
    String payload = parts.length > 1 ? parts[1] : "";

    switch (command) {
      case "LOGIN" -> handleLogin(session, payload);
      case "MSG" -> handleMessage(session, payload);
      case "LOGOUT" -> handleLogout(session);
      default -> {
      }
    }
  }

  private void handleLogin(ClientSession session, String id) {
    if (session.loggedIn || id.isBlank()) {
      return;
    }

    session.id = id;
    session.loggedIn = true;
    logEntry(id + " logged in");
    broadcast(id + " logged in");
  }

  private void handleMessage(ClientSession session, String message) {
    if (!session.loggedIn) {
      return;
    }

    logEntry(session.id + ": " + message);
    broadcast(session.id + ": " + message);
  }

  private void handleLogout(ClientSession session) {
    if (!session.loggedIn || session.logoutQueued) {
      return;
    }

    logEntry(session.id + " logged out");
    broadcast(session.id + " logged out");
    session.loggedIn = false;
    session.logoutQueued = true;
    updateInterestOps(session);
  }

  private void broadcast(String message) {
    byte[] payload = (message + "\n").getBytes(StandardCharsets.UTF_8);
    List<ClientSession> recipients = new ArrayList<>();
    for (ClientSession session : sessions.values()) {
      if (session.loggedIn) {
        recipients.add(session);
      }
    }

    for (ClientSession recipient : recipients) {
      recipient.outbound.add(ByteBuffer.wrap(payload.clone()));
      updateInterestOps(recipient);
    }
  }

  private void writeToClient(SelectionKey key) throws IOException {
    ClientSession session = (ClientSession) key.attachment();
    SocketChannel channel = (SocketChannel) key.channel();

    while (!session.outbound.isEmpty()) {
      ByteBuffer buffer = session.outbound.peek();
      channel.write(buffer);
      if (buffer.hasRemaining()) {
        break;
      }
      session.outbound.poll();
    }

    if (session.outbound.isEmpty()) {
      if (session.logoutQueued) {
        closeSession(channel, true);
        return;
      }
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private void updateInterestOps(ClientSession session) {
    SelectionKey key = session.channel.keyFor(selector);
    if (key == null || !key.isValid()) {
      return;
    }

    int interestOps = SelectionKey.OP_READ;
    if (!session.outbound.isEmpty()) {
      interestOps |= SelectionKey.OP_WRITE;
    }
    key.interestOps(interestOps);
  }

  private void closeSession(SocketChannel channel, boolean logoutAlreadyHandled) {
    ClientSession session = sessions.remove(channel);
    if (session == null) {
      closeChannel(channel);
      return;
    }

    if (!logoutAlreadyHandled && session.loggedIn) {
      logEntry(session.id + " logged out");
      broadcast(session.id + " logged out");
      session.loggedIn = false;
    }

    SelectionKey key = channel.keyFor(selector);
    if (key != null) {
      key.cancel();
    }
    closeChannel(channel);
  }

  private void closeAllSessions() {
    List<SocketChannel> channels = new ArrayList<>(sessions.keySet());
    for (SocketChannel channel : channels) {
      closeSession(channel, true);
    }
  }

  private void closeChannel(SocketChannel channel) {
    try {
      channel.close();
    } catch (IOException ignored) {
    }
  }

  private void logEntry(String text) {
    synchronized (logLock) {
      serverLog.append(LocalTime.now().format(TIME_FORMAT)).append(' ').append(text).append('\n');
    }
  }

  private static final class ClientSession {
    private final SocketChannel channel;
    private final StringBuilder inbound = new StringBuilder();
    private final ArrayDeque<ByteBuffer> outbound = new ArrayDeque<>();
    private String id;
    private boolean loggedIn;
    private boolean logoutQueued;

    private ClientSession(SocketChannel channel) {
      this.channel = channel;
    }
  }
}
