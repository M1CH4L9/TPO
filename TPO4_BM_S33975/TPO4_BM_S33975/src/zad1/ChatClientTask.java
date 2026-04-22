/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class ChatClientTask implements Runnable {

  private final ChatClient client;
  private final List<String> messages;
  private final int wait;
  private final CountDownLatch done = new CountDownLatch(1);
  private volatile Throwable failure;

  private ChatClientTask(ChatClient client, List<String> messages, int wait) {
    this.client = Objects.requireNonNull(client);
    this.messages = List.copyOf(messages);
    this.wait = wait;
  }

  public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
    return new ChatClientTask(c, msgs, wait);
  }

  @Override
  public void run() {
    try {
      client.login();
      sleepIfNeeded();
      for (String message : messages) {
        client.send(message);
        sleepIfNeeded();
      }
      client.logout();
      sleepIfNeeded();
    } catch (Throwable exc) {
      failure = exc;
    } finally {
      done.countDown();
    }
  }

  public ChatClientTask get() throws InterruptedException, ExecutionException {
    done.await();
    if (failure != null) {
      throw new ExecutionException(failure);
    }
    return this;
  }

  public ChatClient getClient() {
    return client;
  }

  private void sleepIfNeeded() throws InterruptedException {
    if (wait != 0) {
      Thread.sleep(wait);
    }
  }
}
