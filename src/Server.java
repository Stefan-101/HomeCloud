import messages.*;
import model.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 6060;
    private static final String STORAGE_DIR = "D:/Java/HomeCloud/serverStorage";    // TODO get storage_dir through constructor
    private static Map<String, User> users = new HashMap<>();

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

    private static User authenticate(User user) throws IOException {
        if (users.containsKey(user.getUsername())){
            if (users.get(user.getUsername()).checkPassword(user.getPassword())){
                // set storage path for user
                user.setStoragePath(STORAGE_DIR + File.separator + user.getUsername());
                // create directory if it does not exist
                Files.createDirectories(Path.of(user.getStoragePath()));
                return user;
            }
        }
        return null;
    }

    private static void print(String msg, String hostInfo){
        System.out.println(msg + " - " + hostInfo);
    }

    private static void handleClient(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream objInStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
        User user = null;
        Boolean isAuthenticated = false;

        while (true){
            Message message = (Message) objInStream.readObject();

            if (message == null){
                continue;
            }

            String hostInfo = ((user != null) ? user.getUsername() + " " : "") + socket.getInetAddress().getHostAddress();
            print("Received " + message.getType(), hostInfo);

            switch (message.getType()) {
                case "CREATE_ACCOUNT": {
                    CreateAccMessage createAccMessage = (CreateAccMessage) message;
                    User userReq = createAccMessage.getUser();  // requested user account to be created

                    // check if the account exists
                    if (users.containsKey(userReq.getUsername())) {
                        user = authenticate(userReq);
                        if (user == null) {
                            objOutStream.writeObject(new ErrMessage("FAIL user already exists"));
                            print("User already exists", hostInfo);
                        } else {
                            objOutStream.writeObject(new ResponseMessage("WARN already exists"));
                            isAuthenticated = true;
                        }
                        break;
                    }
                    users.put(userReq.getUsername(), userReq);

                    print("Created user " + userReq.getUsername(), hostInfo);

                    // respond
                    objOutStream.writeObject(new ResponseMessage("OK"));
                    break;
                }

                case "AUTH": {
                    // Handle authentication
                    AuthMessage authMessage = (AuthMessage) message;
                    User reqUser = authMessage.getUser();

                    if (!users.containsKey(reqUser.getUsername())) {
                        objOutStream.writeObject(new ErrMessage("FAIL user does not exist"));
                        print("User does not exist", hostInfo);
                        break;
                    }

                    user = authenticate(reqUser);
                    if (user == null) {
                        objOutStream.writeObject(new ErrMessage("FAIL wrong password"));
                        print("Wrong password", hostInfo);
                        socket.close();
                        return;
                    }
                    isAuthenticated = true;

                    // Respond
                    objOutStream.writeObject(new ResponseMessage("AUTHENTICATED"));
                    print("User " + user.getUsername() + " authenticated", hostInfo);
                    break;
                }

                case "UPLOAD": {
                    // Handle file upload
                    if (!isAuthenticated || user == null) {
                        objOutStream.writeObject(new ErrMessage("Not Authenticated"));
                        socket.close();
                        print("Not authenticated, aborted", hostInfo);
                        return;
                    }

                    // Receive upload intent
                    UploadFileMessage uploadFileMessage = (UploadFileMessage) message;

                    // Prepare storage
                    File file = new File(user.getStoragePath() + File.separator + uploadFileMessage.getFilename());
                    if (file.exists()) {
                        if (!file.delete()) {
                            print("Failed to delete existing file: " + file.getName(), hostInfo);
                        }
                    } else {
                        file.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true); // append mode

                    // Acknowledge the request
                    objOutStream.writeObject(new AckMessage());

                    // Receive the file
                    byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                    int bytesRead;
                    while ((bytesRead = objInStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        fileOutputStream.flush();
                    }

                    // After receiving all chunks, close the output stream
                    fileOutputStream.close();

                    // Respond with completion message
                    objOutStream.writeObject(new OkMessage());
                    print("File upload complete: " + file.getName(), hostInfo);

                    break;
                }

                case "DOWNLOAD": {
                    // handle file download
                    if (!isAuthenticated || user == null) {
                        objOutStream.writeObject(new ErrMessage("Not Authenticated"));
                        socket.close();
                        print("Not authenticated, aborted", hostInfo);
                        return;
                    }

                    // check if the file exists
                    DownloadFileMessage downloadFileMessage = (DownloadFileMessage) message;
                    File file = new File(user.getStoragePath() + File.separator + downloadFileMessage.getFilepath());
                    if (!file.exists()) {
                        objOutStream.writeObject(new ErrMessage("File does not exist"));
                        print("Request file does not exist: " + downloadFileMessage.getFilepath(), hostInfo);
                    } else {
                        // acknowledge request, prepare for file transmission
                        objOutStream.writeObject(new AckMessage());
                    }

                    // transfer the file
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024 * 1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        objOutStream.write(buffer, 0, bytesRead);
                        objOutStream.flush();
                    }
                    objOutStream.writeObject(null);   // might not be a healthy approach to mark EOF

                    fileInputStream.close();

                    print("File sent: " + file.getPath(), hostInfo);
                    break;
                }

                case "DELETE": {
                    if (!isAuthenticated || user == null) {
                        objOutStream.writeObject(new ErrMessage("Not Authenticated"));
                        socket.close();
                        print("Not authenticated, aborted", hostInfo);
                        return;
                    }

                    // receive delete request
                    DeleteFileMessage deleteFileMessage = (DeleteFileMessage) message;
                    File file = new File(user.getStoragePath() + File.separator + deleteFileMessage.getFilepath());

                    // delete the file
                    try{
                        Files.delete(file.toPath());
                    }
                    catch (Exception e){
                        print("Failed to delete file: " + file.getName() + "\n  " + e.getClass().getName(), hostInfo);
                        objOutStream.writeObject(new ErrMessage("File could not be deleted"));
                        break;
                    }

                    // send confirmation
                    objOutStream.writeObject(new ResponseMessage("OK"));

                    print("File deleted: " + file.getPath(), hostInfo);

                    break;
                }

                case "CHANGE_PW": {
                    if (!isAuthenticated || user == null) {
                        objOutStream.writeObject(new ErrMessage("Not Authenticated"));
                        socket.close();
                        print("Not authenticated, aborted", hostInfo);
                        return;
                    }

                    ChangePasswordMessage changePwMsg = (ChangePasswordMessage) message;

                    // verify credentials
                    User reqUser = users.get(changePwMsg.getUser().getUsername());
                    if (reqUser == null || !reqUser.equals(changePwMsg.getUser()) || authenticate(changePwMsg.getUser()) == null) {
                        objOutStream.writeObject(new ErrMessage("User does not exist or wrong password!"));
                        print("User does not exist or wrong password!", hostInfo);
                        break;
                    }

                    // update password
                    reqUser.setPassword(changePwMsg.getNewPassword());
                    reqUser.setStoragePath(changePwMsg.getUser().getStoragePath());
                    users.put(changePwMsg.getUser().getUsername(), reqUser);

                    objOutStream.writeObject(new ResponseMessage("OK"));

                    break;
                }

                case "DISCONNECT": {
                    objOutStream.writeObject(new AckMessage());
                    objInStream.close();
                    objOutStream.close();
                    socket.close();
                    print("Disconnected", hostInfo);
                    return;
                }

                default:
                    // Handle unknown message types
                    print("Unknown message type received: " + message, hostInfo);
                    socket.close();
                    break;
            }
        }
    }

    // TODO checkAuthStatus function
}
