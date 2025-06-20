import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    // Shared user list
    private static final Map<String, ClientHandler> userMap = Collections.synchronizedMap(new HashMap<>());
    private static final String LOG_FILE = "chatlog.txt";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Ask for username
            out.println("Enter your username:");
            username = in.readLine();

            synchronized (userMap) {
                while (userMap.containsKey(username) || username == null || username.trim().isEmpty()) {
                    out.println("❌ Username taken or invalid. Enter a different one:");
                    username = in.readLine();
                }
                userMap.put(username, this);
            }

            logAndBroadcast("🟢 " + username + " has joined the chat!");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/quit")) {
                    break;
                } else if (msg.equalsIgnoreCase("/list")) {
                    sendUserList();
                } else {
                    logAndBroadcast(username + ": " + msg);
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error with " + username);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}

            synchronized (userMap) {
                userMap.remove(username);
            }
            logAndBroadcast("🔴 " + username + " has left the chat.");
        }
    }

    private void logAndBroadcast(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String timeStampedMessage = "[" + time + "] " + message;

        // Send to all users
        synchronized (userMap) {
            for (ClientHandler client : userMap.values()) {
                client.out.println(timeStampedMessage);
            }
        }

        // Log to file
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(timeStampedMessage);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("⚠️ Failed to write to log file.");
        }
    }

    private void sendUserList() {
        StringBuilder sb = new StringBuilder("👥 Online users: ");
        synchronized (userMap) {
            for (String user : userMap.keySet()) {
                sb.append(user).append(" ");
            }
        }
        out.println(sb.toString().trim());
    }
}
