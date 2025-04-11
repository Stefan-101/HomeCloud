package server;

import messages.*;
import model.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private static final int PORT = 6060;
    private static final String STORAGE_DIR = "D:/Java/HomeCloud/serverStorage";    // TODO get storage_dir through constructor
    Set<User> users = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);     // TODO use try
        System.out.println("Server started");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected");

            new Thread(() -> {
                try{
                    socket.setSoTimeout(30000);
                    handleClient(socket);
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Client disconnected");
                }
                finally{
                    System.out.println("Client disconnected");
                }
            }).start();
        }
    }

    private static void handleClient(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        User user = null;
        Boolean isAuthenticated = false;

        while (true){
            Message message = (Message) objectInputStream.readObject();

            if (message == null){
                continue;
            }

            String hostInfo = ((user != null) ? user.getUsername() + " " : "") + socket.getInetAddress().getHostAddress();
            System.out.println("Received " + message.getType() + " from " + hostInfo);

            switch (message.getType()) {
                case "AUTH":
                    // Handle authentication
                    AuthMessage authMessage = (AuthMessage) message;
                    String username = authMessage.getUsername();
                    String password = authMessage.getPassword();

                    // TODO: Implement auth logic

                    user = new User(username, password, STORAGE_DIR);
                    isAuthenticated = true;

                    // Respond
                    objectOutputStream.writeObject(new ResponseMessage("AUTHENTICATED"));
                    System.out.println("User " + username + " authenticated");
                    break;

                case "UPLOAD":
                    // Handle file upload
                    if (!isAuthenticated) {
                        socket.close();
                        System.out.println("Not authenticated, aborted");
                        return;
                    }

                    // Receive upload intent
                    UploadFileMessage uploadFileMessage = (UploadFileMessage) message;

                    // Prepare storage
                    File file = new File(STORAGE_DIR + File.separator + uploadFileMessage.getFilename());
                    if (file.exists()) {
                        if (!file.delete()) {
                            System.out.println("Failed to delete existing file: " + file.getName());
                        }
                    }
                    else {
                        file.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true); // append mode

                    // Acknowledge the request
                    objectOutputStream.writeObject(new AckMessage());

                    // Receive the file
                    byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                    int bytesRead;
                    while ((bytesRead = objectInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        fileOutputStream.flush();
                    }

                    // After receiving all chunks, close the output stream
                    fileOutputStream.close();

                    // Respond with completion message
                    objectOutputStream.writeObject(new OkMessage());
                    System.out.println("File upload complete: " + file.getName());

                    break;

                default:
                    // Handle unknown message types
                    System.out.println("Unknown message type received: " + message);
                    socket.close();
                    break;
            }
        }
    }
}
