import messages.*;
import model.User;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    private String serverIp = "127.0.0.1";  // default to localhost
    private int serverPort = 6060;

    private User user;
    private SSLSocket socket;
    private ObjectOutputStream objOutStream;
    private ObjectInputStream objInStream;

    static {
        // demo purposes, trust my own cert
        System.setProperty("javax.net.ssl.trustStore", "client.truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    public Client(){}
    public Client(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void setUser(User user) {
        this.user = new User(user);
    }

    public void setServerIp(String serverIp) {
        if (socket != null) {
            return;
        }
        this.serverIp = serverIp;
    }

    public void setServerPort(int serverPort) {
        if (socket != null) {
            return;
        }
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverIp, serverPort);
        socket = sslSocket;
        sslSocket.setEnabledProtocols(sslSocket.getSupportedProtocols());

        objOutStream = new ObjectOutputStream(socket.getOutputStream());
        objInStream = new ObjectInputStream(socket.getInputStream());
        socket.setSoTimeout(10000);     // abort connection after 10 seconds of no response

        // insecure
//        socket = new Socket(serverIp, serverPort);
//        objOutStream = new ObjectOutputStream(socket.getOutputStream());
//        objInStream = new ObjectInputStream(socket.getInputStream());
//        socket.setSoTimeout(10000);     // abort connection after 10 seconds of no response
    }

    public int createAccount() throws IOException, ClassNotFoundException {
        User newUser;
        if (user != null){
            newUser = user;
        }
        else{
            throw new RuntimeException("User not set");
        }

        objOutStream.writeObject(new CreateAccMessage(newUser));

        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (response instanceof OkMessage){
            authenticate();
            return 1;
        } else if (response.getResponse().equals("WARN already exists")) {
            return 2;
        } else{
            throw new RuntimeException("Create account failed");
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
        if (!response.getResponse().equals("AUTHENTICATED")){
            throw new RuntimeException(response.getResponse());
        }
    }

    public void changePassword(String newPassword) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new ChangePasswordMessage(this.user, newPassword));

        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (!(response instanceof OkMessage)){
            throw new RuntimeException(response.getResponse());
        }

        this.user.setPassword(newPassword);
    }

    public void uploadFile(String filepath) throws IOException, ClassNotFoundException {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist");
        }

        // Announce server of upload intent
        UploadFileMessage uploadFileMessage = new UploadFileMessage(file.getName());
        objOutStream.writeObject(uploadFileMessage);

        // Wait for server to acknowledge
        Message response = (Message) objInStream.readObject();
        if (!(response instanceof AckMessage)) {
            throw new RuntimeException("Server not ready to receive file.");
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
        if (!(response instanceof OkMessage)) {
            throw new RuntimeException("Upload failed");
        }
    }

    public void downloadFile(String filepath) throws IOException, ClassNotFoundException {
        // file paths
        String currentPath = System.getProperty("user.dir") + File.separator + "downloads";
        Files.createDirectories(Path.of(currentPath));

        // send download request
        objOutStream.writeObject(new DownloadFileMessage(filepath));

        // Prepare storage context
        String fileName = Paths.get(filepath).getFileName().toString();
        File file = new File(currentPath + File.separator + fileName);
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Failed to delete file on client side");
            }
        } else {
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, true); // append mode

        // wait for server's acknowledgement
        Message response = (Message) objInStream.readObject();
        if (!(response instanceof AckMessage)){
            throw new RuntimeException(((ErrMessage) response).getResponse());
        }

        // receive the file
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        int bytesRead;
        while ((bytesRead = objInStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
            fileOutputStream.flush();
        }

        // After receiving all chunks, close the output stream
        fileOutputStream.close();
    }

    public void deleteFile(String filepath) throws IOException, ClassNotFoundException {
        // send delete request
        objOutStream.writeObject(new DeleteFileMessage(filepath));

        // receive server's confirmation
        ResponseMessage response = (ResponseMessage) objInStream.readObject();
        if (!(response instanceof OkMessage)) {
            throw new RuntimeException(response.getResponse());
        }
    }

    public void createFolder(String folderpath) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new UpdateFolderMessage(Action.CREATE, folderpath));
        ResponseMessage response = (ResponseMessage) objInStream.readObject();

        if (!(response instanceof OkMessage)) {
            throw new RuntimeException(response.getResponse());
        }
    }

    public void deleteFolder(String folderpath) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new UpdateFolderMessage(Action.DELETE, folderpath));
        ResponseMessage response = (ResponseMessage) objInStream.readObject();

        if (!(response instanceof OkMessage)) {
            throw new RuntimeException(response.getResponse());
        }
    }

    public void moveFile(String oldfilepath, String newfilepath) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new UpdateFolderMessage(Action.MOVE, oldfilepath, newfilepath));
        ResponseMessage response = (ResponseMessage) objInStream.readObject();

        if (!(response instanceof OkMessage)) {
            throw new RuntimeException(response.getResponse());
        }
    }

    public String listFolder(String folderpath) throws IOException, ClassNotFoundException {
        objOutStream.writeObject(new GetFolderTreeMessage(folderpath));
        Object response = objInStream.readObject();

        if (!(response instanceof String)){
            throw new RuntimeException(((ErrMessage) response).getResponse());
        }

        return (String) response;
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

        socket = null;
        objOutStream = null;
        objInStream = null;
    }
}
