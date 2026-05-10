/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;

    //używamy bezpiecznych list, bo wirtualne wątki będą do nich sięgać równocześnie
    private List<String> serverLog = new CopyOnWriteArrayList<>();
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            // BUG FIX :))))
            //otwieramy port w głównym wątku żeby metoda startServer() nie zakończyła się, dopóki port nie będzie gotowy
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server started");

            //tutaj wątek czeka na klientów
            serverThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(clientSocket);
                        //dodajemy do listy, żeby móc od razu bezpiecznie zamknąć port klienta w stopServer()
                        clients.add(handler);
                        //obsługa żądań odbywa się w wirtualnych wątkach
                        Thread.startVirtualThread(handler);
                    } catch (IOException e) {
                        //ignorujemy wyjątek wyrzucany w momencie gdy stopServer() zamyka gniazdo
                    }
                }
            });
            serverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        isRunning = false;
        System.out.println("Server stopped"); //wymagane przez polecenie
        broadcastAndLog("ChatServer: chat closed");

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

            //zatrzymujemy ubicie gniazd TCP na 50ms, żeby "chat closed" na 100% dotarło do klientów
            Thread.sleep(50);

            //zamykamy połączenia wszystkich aktualnie podłączonych klientów
            for (ClientHandler client : clients) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getServerLog() {
        //zwracamy w postaci połączonego Stringa, dokładnie jak tego oczekuje test
        return String.join("\n", serverLog);
    }

    //metoda synchronizowana, żeby odpowiednio chronić kolejność wiadomości i logów
    private synchronized void broadcastAndLog(String message) {
        //generujemy format czasu HH:MM:SS.nnn
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn");
        String timeStr = LocalTime.now().format(dtf);

        serverLog.add(timeStr + " " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    //klasa pomocnicza dla obsługi pojedynczego klienta
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private Scanner in;
        private String clientId;
        private boolean isLoggedIn = false; //kinda ważne - flaga czy klient zdążył wejść

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new Scanner(socket.getInputStream());

                while (true) {
                    String message = null;
                    try {
                        //jeśli serwer nagle się zamknie, in.hasNextLine() rzuci wyjątkiem
                        if (!in.hasNextLine()) break;
                        message = in.nextLine();
                    } catch (IllegalStateException e) {
                        break; //wychodzimy z pętli po cichu, bo serwer został nagle zamknięty
                    }

                    if (message.startsWith("LOGIN:")) {
                        clientId = message.substring(6);
                        String msg = clientId + " logged in";
                        synchronized (ChatServer.this) {
                            isLoggedIn = true; //zamiast clients.add(this)
                            broadcastAndLog(msg);
                        }
                    } else if (message.startsWith("LOGOUT:")) {
                        String msg = clientId + " logged out";
                        synchronized (ChatServer.this) {
                            broadcastAndLog(msg);
                            isLoggedIn = false; //odznaczamy flagę
                        }
                        break;
                    } else if (message.startsWith("MSG:")) {
                        String text = message.substring(4);
                        String msg = clientId + ": " + text;
                        broadcastAndLog(msg);
                    }
                }
            } catch (Exception e) {
                //ignorujemy błędy wejścia/wyjścia
            } finally {
                clients.remove(this);
                close();
            }
        }

        public void sendMessage(String message) {
            //warunek - klient musi być zalogowany, żeby otrzymać powiadomienie
            if (isLoggedIn && out != null) {
                out.println(message);
            }
        }

        public void close() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                //ignorujemy
            }
        }
    }
}