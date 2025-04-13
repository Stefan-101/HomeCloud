import messages.*;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Client {
    private String serverIp = "127.0.0.1";  // default to localhost
    private int serverPort = 6060;

    private User user;
    private Socket socket;
    private ObjectOutputStream objOutStream;
    private ObjectInputStream objInStream;

    public Client(){}
    public Client(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void setUser(User user) {
        this.user = new User(user);
    }

    public void connect() throws IOException {
        socket = new Socket(serverIp, serverPort);
        objOutStream = new ObjectOutputStream(socket.getOutputStream());
        objInStream = new ObjectInputStream(socket.getInputStream());
        socket.setSoTimeout(10000);     // abort connection after 10 seconds of no response
    }

    public void createAccount() throws IOException, ClassNotFoundException {
        User newUser;
        if (user != null){
            newUser = user;
        }
        else{
            String username;
            String password;

            System.out.println("= Create account =");
            Scanner scanner = new Scanner(System.in);
            System.out.print("Username: ");
            username = scanner.nextLine();
            System.out.print("Password: ");
            password = scanner.nextLine();

            newUser = new User(username, password);
        }

        setUser(newUser);

        objOutStream.writeObject(new CreateAccMessage(newUser));

        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (response.getResponse().equals("OK")){
            System.out.println("Created account successfully");
            authenticate();
        } else if (response.getResponse().equals("WARN already exists")) {
            System.out.println("Account already exists, authenticated");
        } else{
            System.out.println("Create account failed");
        }
    }

    public void authenticate() throws IOException, ClassNotFoundException {
        if (user == null){
            throw new RuntimeException("User is null");
        }
        if (socket == null){
            throw new RuntimeException("Socket is null");
        }

        // send auth request
        // not done properly, everything is in plain text!!
        AuthMessage authMessage = new AuthMessage(user);
        objOutStream.writeObject(authMessage);

        // await response
        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (response.getResponse().equals("AUTHENTICATED")){
            System.out.println("Authenticated");
        }
        else{
            // TODO handle server response (incorrect password, etc)
            System.out.println("Authentication failed");
        }
    }

    public void uploadFile(String filepath) throws IOException, ClassNotFoundException {
        File file = new File(filepath);
        if (!file.exists()) {
            System.out.println("File does not exist");
            return;
        }

        // Announce server of upload intent
        UploadFileMessage uploadFileMessage = new UploadFileMessage(filepath, file.getName());
        objOutStream.writeObject(uploadFileMessage);

        // Wait for server to acknowledge
        Message response = (Message) objInStream.readObject();
        if (!(response instanceof AckMessage)) {
            System.out.println("Server not ready to receive file.");
            return;
        }

        // Open the file for reading
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        int bytesRead;

        // Send the file in chunks
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            // Send the chunk to the server
            objOutStream.write(buffer, 0, bytesRead);
            objOutStream.flush(); // Ensure the data is sent immediately
        }
        objOutStream.writeObject(null); // might not be a very healthy approach to mark EOF

        // Close the file input stream
        fileInputStream.close();

        // Wait for the server to acknowledge
        response = (Message) objInStream.readObject();
        if (response instanceof OkMessage) {
            System.out.println("File uploaded successfully.");
        } else {
            System.out.println("File upload failed.");
        }
    }

    public void downloadFile(String filepath) throws IOException, ClassNotFoundException {
        // file paths
        String currentPath = System.getProperty("user.dir") + File.separator + "downloads";
        Files.createDirectories(Path.of(currentPath));

        // send download request
        objOutStream.writeObject(new DownloadFileMessage(filepath));

        // Prepare storage context
        File file = new File(currentPath + File.separator + filepath);
        if (file.exists()) {
            if (!file.delete()) {
                System.out.println("Failed to delete file");
            }
        } else {
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, true); // append mode

        // wait for server's acknowledgement
        AckMessage ackMessage = (AckMessage) objInStream.readObject();

        // receive the file
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        int bytesRead;
        while ((bytesRead = objInStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
            fileOutputStream.flush();
        }

        // After receiving all chunks, close the output stream
        fileOutputStream.close();

        System.out.println("Downloaded file successfully");
    }

    public void deleteFile(String filepath) throws IOException, ClassNotFoundException {
        // file paths
        String currentPath = System.getProperty("user.dir") + File.separator + "downloads";
        Files.createDirectories(Path.of(currentPath));

        // send delete request
        objOutStream.writeObject(new DeleteFileMessage(filepath));

        // receive server's confirmation
        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (response.getResponse().equals("OK")){
            System.out.println("Deleted file successfully");
        }
        else{
            System.out.println("Deletion failed: " + response.getResponse());
        }
    }


    public void changePassword(String newPassword) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new ChangePasswordMessage(this.user, newPassword));

        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (response.getResponse().equals("OK")){
            System.out.println("Changed password successfully");
            this.user.setPassword(newPassword);
        }
        else{
            System.out.println("Changed password failed");
        }
    }

    public void disconnect() throws IOException, ClassNotFoundException {
        // send disconnect request
        objOutStream.writeObject(new DisconnectMessage());
        AckMessage ackMessage = (AckMessage) objInStream.readObject();

        objOutStream.close();
        objInStream.close();

        if (socket.isClosed()) {
            System.out.println("Disconnected");
        }
    }
}
