import messages.*;
import model.*;

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
    private static String STORAGE_DIR;
    private static Map<String, User> users = new HashMap<>();
    private static List<Request> requests = new ArrayList<>();

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
    }

        public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);     // TODO use try
        System.out.println("Server started");

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
            Socket socket = serverSocket.accept();
            ObjectInputStream objInStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Client connected");

            new Thread(() -> {
                try{
                    socket.setSoTimeout(300_000);
                    handleClient(socket, objInStream, objOutStream);
                }
                catch (Exception e){
                    e.printStackTrace();

                    try {
                        objOutStream.writeObject(new DisconnectMessage());  // client can't yet handle this
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
            Message message = (Message) objInStream.readObject();

            if (message == null){
                continue;
            }

            String hostInfo = ((user != null) ? user.getUsername() + " " : "") + socket.getInetAddress().getHostAddress();
            print("Received " + message.getType(), hostInfo);
            synchronized (requests) {
                requests.add(new Request(user != null ? user.stripPassword() : new User(), socket.getInetAddress().getHostAddress(), message.getType()));
            }

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
                    reqUser.setStoragePath(changePwMsg.getUser().getStoragePath());
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
                    File file = new File(user.getStoragePath() + File.separator + uploadFileMessage.getFilename());
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
                    File file = new File(user.getStoragePath() + File.separator + downloadFileMessage.getFilepath());
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
                    File file = new File(user.getStoragePath() + File.separator + deleteFileMessage.getFilepath());

                    if (file.getPath().contains("..")){
                        objOutStream.writeObject(new ErrMessage("Invalid path"));
                        print("Invalid path", hostInfo);
                        break;
                    }

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

                    if (updFldMsg.contains("..")){
                        objOutStream.writeObject(new ErrMessage("Invalid path"));
                        print("Invalid path", hostInfo);
                        break;
                    }

                    switch (updFldMsg.getAction()) {
                        case Action.CREATE: {
                            String folderPath = user.getStoragePath() + File.separator + updFldMsg.getFolderPath();

                            try{
                                Files.createDirectories(Path.of(folderPath));
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
                            String folderPath = user.getStoragePath() + File.separator + updFldMsg.getFolderPath();
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
                            String oldFolderPath = user.getStoragePath() + File.separator + updFldMsg.getOldFolderPath();
                            String folderPath = user.getStoragePath() + File.separator + updFldMsg.getFolderPath();

                            try{
                                Files.move(Path.of(oldFolderPath), Path.of(folderPath), StandardCopyOption.REPLACE_EXISTING);
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
                    GetFolderTreeMessage getFldTreeMessage = (GetFolderTreeMessage) message;

                    // don't allow user to exit their folder
                    if (getFldTreeMessage.getPath().contains("..")){
                        objOutStream.writeObject(new ErrMessage("Invalid path"));
                        print("Invalid path: " + getFldTreeMessage.getPath(), hostInfo);
                        break;
                    }

                    String response = getFolderTree(Path.of(user.getStoragePath() + File.separator + getFldTreeMessage.getPath()));
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
