/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;


import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.yaml.snakeyaml.*;

public class MainChatVT {

  public static Map<String, String> doChat( Map<String, List<Integer>> opts) throws InterruptedException {
    List<Integer> pd = opts.remove("portAndDelays"); 
    int port = pd.get(0);
    int startTaskDelay = pd.get(1);
    int stopServerDelay = pd.get(2); 


    ChatServer s = new ChatServer(port);
    s.startServer();

    ExecutorService es = Executors.newCachedThreadPool();
    List<ChatClientTask> ctasks = new ArrayList<>();

    // Creating and starting clients
    opts.forEach((k, v) -> {
      String cid = k;
      int wait = v.get(0);
      List<String> msgs = IntStream.rangeClosed(1, v.get(1)).mapToObj(n -> "msg_"+n).toList();
      ChatClient c = new ChatClient("localhost", port, cid);
      ChatClientTask ctask = ChatClientTask.create(c, msgs, wait);
      ctasks.add(ctask);
      if (startTaskDelay > 0)
        try {
          Thread.sleep(startTaskDelay);
        } catch (InterruptedException exc) {
          throw new RuntimeException(exc);
        }
      es.execute(ctask);
    });

    ctasks.forEach( task -> {
      try {
        task.get();
      } catch (InterruptedException | ExecutionException exc) {
        System.out.println("*** " + exc);
      }
    });
    if (stopServerDelay > 0)  Thread.sleep(stopServerDelay);
    s.stopServer();
    es.shutdown();
    Thread.sleep(100);

    Map<String, String> results = new LinkedHashMap<>();
    results.put("#server#", s.getServerLog());
    ctasks.forEach(task -> { 
      ChatClient c = task.getClient();
      results.put(c.getId(), c.getChatView());
    });
    return results;
  }


  public static void main(String[] args) throws Exception {
    String testFileName = System.getProperty("user.home") + "/ChatTestVT.yaml";
    String options = Files.readString(Path.of(testFileName), Charset.forName("UTF-8"));
    System.out.println("=== Options\n" + options);

    Map<String, List<Integer>> imap = (Map<String, List<Integer>>) new Yaml().load(options);
    doChat(imap).forEach ( (k, v) -> {
      if (k.equals("#server#")) System.out.println("\n=== Server log\n" + v);
      else System.out.println("=== " + k + " chat view\n" + v);
    });
  }
}
