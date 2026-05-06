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
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                System.out.println("Server started"); //wymagane przez polecenie

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(clientSocket);
                        //obsługa żądań odbywa się w wirtualnych wątkach
                        Thread.startVirtualThread(handler);
                    } catch (IOException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    public void stopServer() {
        isRunning = false;
        System.out.println("Server stopped"); //wymagane przez polecenie
        broadcastAndLog("ChatServer: chat closed");

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            //zamykamy połączenia wszystkich aktualnie podłączonych klientów
            for (ClientHandler client : clients) {
                client.close();
            }
        } catch (IOException e) {
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
                        //dodajemy klienta i natychmiast rozsyłamy jego logowanie
                        synchronized (ChatServer.this) {
                            clients.add(this);
                            broadcastAndLog(msg);
                        }
                    } else if (message.startsWith("LOGOUT:")) {
                        String msg = clientId + " logged out";
                        //rozsyłamy info i od razu usuwamy klienta z listy
                        synchronized (ChatServer.this) {
                            broadcastAndLog(msg);
                            clients.remove(this);
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
            if (out != null) {
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
