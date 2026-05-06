/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {
    private String host;
    private int port;
    private String id;
    private Socket socket;
    private PrintWriter out;
    private Scanner in;
    private Thread listenerThread;

    //lista zbierająca widok z perspektywy klienta
    private List<String> chatView = new CopyOnWriteArrayList<>();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void login() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());

            listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        try {
                            if (!in.hasNextLine()) break;
                            String msg = in.nextLine();
                            chatView.add(msg);
                        } catch (IllegalStateException e) {
                            break; //zamknięcie scannera, zamykamy wątek bez błędu
                        }
                    }
                } catch (Exception e) {
                    //ignorujemy zablokowanie wątku
                }
            });
            listenerThread.start();

            out.println("LOGIN:" + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        if (out != null) {
            out.println("LOGOUT:");
        }
    }

    public void send(String req) {
        if (out != null) {
            out.println("MSG:" + req);
        }
    }

    public String getChatView() {
        return String.join("\n", chatView);
    }

    public String getId() {
        return id;
    }
}
