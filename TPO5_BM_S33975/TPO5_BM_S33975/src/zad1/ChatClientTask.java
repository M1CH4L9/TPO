/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

//rozszerzamy klasę FutureTask by łatwo działała z metodą .get() i executorem w pliku Main
public class ChatClientTask extends FutureTask<ChatClient> {

    private ChatClient client;

    private ChatClientTask(Callable<ChatClient> callable, ChatClient client) {
        super(callable);
        this.client = client;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        Callable<ChatClient> callable = () -> {
            c.login();
            if (wait != 0) {
                Thread.sleep(wait);
            }
            for (String msg : msgs) {
                c.send(msg);
                if (wait != 0) {
                    Thread.sleep(wait);
                }
            }
            c.logout();
            if (wait != 0) {
                Thread.sleep(wait);
            }
            return c;
        };
        return new ChatClientTask(callable, c);
    }

    public ChatClient getClient() {
        return client;
    }
}
