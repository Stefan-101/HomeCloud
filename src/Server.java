import messages.*;
import model.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 6060;
    private static final String STORAGE_DIR;
    private static Map<String, User> users = new HashMap<>();       // store users and their passwordsd
    private static List<Request> requests = new ArrayList<>();      // some logging

    static {
        // should be done with dotenv, but I can't be bothered to install it for one line of text
        try (BufferedReader reader = new BufferedReader(new FileReader("storage_dir.txt"))) {
            String storage_dir = reader.readLine();
            STORAGE_DIR = Path.of(storage_dir).toAbsolutePath().toString();
            System.out.println(STORAGE_DIR);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // initialize the TLS keys
        System.setProperty("javax.net.ssl.keyStore", "./server.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "yourPassword");

        // Generate key
        //
        // keytool -genkeypair \
        // -alias serverkey \
        // -keyalg RSA \
        // -keysize 2048 \
        // -validity 365 \
        // -keystore server.keystore \
        // -storepass yourPassword

        // Export certificate
        // keytool -export -alias serverkey -keystore server.keystore -file server.crt -storepass yourPassword

        // Create truststore
        // keytool -import -alias servercert -file server.crt -keystore client.truststore -storepass changeit
    }

    public static void main(String[] args) throws IOException {
        // create a secure socket
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT);

        System.out.println("Secure TLS server listening on port " + PORT + "...");

        // Print requests list periodically
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(() -> {
//            System.out.println("== Printing requests list ==");
//            synchronized (requests) {
//                List<Request> sortedRequests = new ArrayList<>(requests);
//                sortedRequests.sort(Comparator.comparing(Request::getUser));
//                for (Request req : sortedRequests) {
//                    String userOrIp = req.getUser().getUsername() == "" ? req.getIp() : req.getUser().getUsername();
//                    System.out.println("  " + userOrIp + ": " + req.getAction() + " at " + req.getTimestamp());
//                }
//            }
//        }, 0, 30, TimeUnit.SECONDS);

        while (true) {
            // accept connections
            SSLSocket socket;
            ObjectInputStream objInStream;
            ObjectOutputStream objOutStream;
            try{
                socket = (SSLSocket) serverSocket.accept();
                objInStream = new ObjectInputStream(socket.getInputStream());
                objOutStream = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Client connected - " + socket.getInetAddress().getHostAddress());
                System.out.println("Protocol used: " + socket.getSession().getProtocol());
            }
            catch (Exception e){
                System.out.println("Something went wrong on client connection");
                continue;
            }

            new Thread(() -> {
                try{
                    socket.setSoTimeout(300_000);
                    handleClient(socket, objInStream, objOutStream);
                }
                catch (Exception e){
                    e.printStackTrace();

                    try {
                        objOutStream.writeObject(new DisconnectMessage());
                        objInStream.close();
                        objOutStream.close();
                        socket.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                finally{
                    System.out.println("Client disconnected");
                }
            }).start();
        }
    }

    private static User authenticate(User user) throws IOException {
        // returns the user if successfully authenticated, null otherwise
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

    private static void handleClient(Socket socket, ObjectInputStream objInStream, ObjectOutputStream objOutStream) throws IOException, ClassNotFoundException {
        User user = null;
        Boolean isAuthenticated = false;

        while (true){
            // receive messages from the client
            Message message = (Message) objInStream.readObject();

            if (message == null){
                continue;
            }

            // log each message received
            String hostInfo = ((user != null) ? user.getUsername() + " " : "") + socket.getInetAddress().getHostAddress();
            print("Received " + message.getType(), hostInfo);
            synchronized (requests) {
                requests.add(new Request(user != null ? user.stripPassword() : new User(), socket.getInetAddress().getHostAddress(), message.getType()));
            }

            // process message
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

                    userReq.setStoragePath(STORAGE_DIR + File.separator + userReq.getUsername());
                    users.put(userReq.getUsername(), userReq);

                    print("Created user " + userReq.getUsername(), hostInfo);

                    // respond
                    objOutStream.writeObject(new OkMessage());
                    break;
                }

                case "AUTH": {
                    // Handle authentication
                    AuthMessage authMessage = (AuthMessage) message;
                    User reqUser = authMessage.getUser();

                    if (!users.containsKey(reqUser.getUsername())) {
                        objOutStream.writeObject(new ErrMessage("User does not exist"));
                        print("User does not exist", hostInfo);
                        break;
                    }

                    user = authenticate(reqUser);
                    if (user == null) {
                        objOutStream.writeObject(new ErrMessage("Wrong password"));
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
                    if (reqUser == null || authenticate(changePwMsg.getUser()) == null) {
                        objOutStream.writeObject(new ErrMessage("User does not exist or wrong password!"));
                        print("User does not exist or wrong password!", hostInfo);
                        break;
                    }

                    // update password
                    reqUser.setPassword(changePwMsg.getNewPassword());
                    users.put(changePwMsg.getUser().getUsername(), reqUser);

                    objOutStream.writeObject(new OkMessage());

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
                    SecurePath path = new SecurePath(user.getStoragePath(), uploadFileMessage.getFilename());
                    File file = new File(path.getStringPath());
                    if (file.exists()) {
                        if (!file.delete()) {
                            print("Failed to delete existing file: " + file.getName(), hostInfo);
                            objOutStream.writeObject(new ErrMessage("Failed to delete existing file"));
                            break;
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
                    SecurePath path = new SecurePath(user.getStoragePath(), downloadFileMessage.getFilepath());
                    File file = new File(path.getStringPath());
                    if (!file.exists()) {
                        objOutStream.writeObject(new ErrMessage("File does not exist"));
                        print("Request file does not exist: " + downloadFileMessage.getFilepath(), hostInfo);
                        break;
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
                    SecurePath path = new SecurePath(user.getStoragePath(), deleteFileMessage.getFilepath());
                    File file = new File(path.getStringPath());

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
                    objOutStream.writeObject(new OkMessage());

                    print("File deleted: " + file.getPath(), hostInfo);

                    break;
                }

                case "UPDATE_FOLDER": {
                    if (!isAuthenticated || user == null) {
                        objOutStream.writeObject(new ErrMessage("Not Authenticated"));
                        socket.close();
                        print("Not authenticated, aborted", hostInfo);
                        return;
                    }

                    UpdateFolderMessage updFldMsg = (UpdateFolderMessage) message;

                    // the UPDATE_FOLDER message can have different subtypes
                    switch (updFldMsg.getAction()) {
                        case Action.CREATE: {
                            // create a new folder
                            SecurePath folderPath = new SecurePath(user.getStoragePath(), updFldMsg.getFolderPath());

                            try{
                                Files.createDirectories(folderPath.getPath());
                                objOutStream.writeObject(new OkMessage());
                                print("Folder created: " + folderPath, hostInfo);
                            }
                            catch (Exception e){
                                print("Failed to create folder: " + folderPath, hostInfo);
                                objOutStream.writeObject(new ErrMessage("Folder could not be created"));
                            }

                            break;
                        }

                        case Action.DELETE: {
                            // delete a folder recursively
                            String folderPath = (new SecurePath(user.getStoragePath(), updFldMsg.getFolderPath())).getStringPath();
                            File directory = new File(folderPath);

                            if (deleteDirectory(directory)){
                                print("Folder deleted: " + folderPath, hostInfo);
                                objOutStream.writeObject(new OkMessage());
                                break;
                            }

                            objOutStream.writeObject(new ErrMessage("Folder could not be deleted"));
                            print("Failed to delete folder: " + folderPath, hostInfo);
                            break;
                        }

                        case Action.MOVE: {
                            // move a folder and its contents
                            SecurePath oldFolderPath = new SecurePath(user.getStoragePath(), updFldMsg.getOldFolderPath());
                            SecurePath folderPath = new SecurePath(user.getStoragePath(), updFldMsg.getFolderPath());

                            try{
                                Files.move(oldFolderPath.getPath(), folderPath.getPath(), StandardCopyOption.REPLACE_EXISTING);
                                objOutStream.writeObject(new OkMessage());
                                print("Moved old folder: " + oldFolderPath, hostInfo);

                                break;
                            }
                            catch (Exception e){
                                print("Failed to move folder: " + oldFolderPath, hostInfo);
                                objOutStream.writeObject(new ErrMessage("Folder could not be moved"));
                                break;
                            }
                        }
                    }

                    break;
                }

                case "GET_FOLDER_TREE": {
                    // gets the structure of a folder
                    GetFolderTreeMessage getFldTreeMessage = (GetFolderTreeMessage) message;

                    SecurePath path = new SecurePath(user.getStoragePath(), getFldTreeMessage.getPath());
                    String response = getFolderTree(path.getPath());
                    objOutStream.writeObject(response);

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

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static String getFolderTree(Path path) throws IOException {
        StringBuilder tree = new StringBuilder();
        walk(path, tree, "", true);
        return tree.toString();
    }

    private static void walk(Path path, StringBuilder tree, String indent, boolean isLast) throws IOException {
        tree.append(indent);
        if (isLast) {
            tree.append("└── ");
            indent += "    ";
        } else {
            tree.append("├── ");
            indent += "│   ";
        }
        tree.append(path.getFileName()).append("\n");

        File file = path.toFile();
        if (!file.isDirectory()) return;

        File[] files = file.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (int i = 0; i < files.length; i++) {
            boolean last = (i == files.length - 1);
            walk(files[i].toPath(), tree, indent, last);
        }
    }

    // TODO checkAuthStatus function
}
