import model.User;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleClientService extends Client {
    Client client = new Client();
    Scanner scanner = new Scanner(System.in);

    ConsoleClientService() {
        super();
    }

    public void setUser(){
        System.out.println(" == Enter account details ==");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        super.setUser(new User(username, password));
    }

    public void setServerIp() {
        System.out.print("Server IP: ");
        String serverIP = scanner.nextLine();
        super.setServerIp(serverIP);
    }

    public void setServerPort() {
        System.out.print("Server Port: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        super.setServerPort(port);
    }

    public int createAccount() {
        try{
            int result = super.createAccount();
            if (result == 1){
                System.out.println("Account created successfully");
                return 1;
            }
            else {
                System.out.println("Account already exists, authenticated");
                return 1;
            }
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch (Exception e){
            System.out.println("Create account failed");
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public boolean auth(){
        try{
            super.authenticate();
            System.out.println("Authentication successful");
            return true;
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
            return false;
        }
        catch (Exception e){
            System.out.println("Authentication failed");
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void changePassword() {
        System.out.println("== Change password ==");
        System.out.print("New password: ");
        String newPassword = scanner.nextLine();
        try{
            super.changePassword(newPassword);
            System.out.println("Changed password successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch (Exception e){
            System.out.println("Change password failed");
            System.out.println(e.getMessage());
        }
    }

    public void uploadFile() {
        System.out.println("== Upload file ==");
        System.out.print("File path: ");
        String path = scanner.nextLine();
        try{
            super.uploadFile(path);
            System.out.println("File uploaded successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Upload failed");
            System.out.println(e.getMessage());
        }
    }

    public void downloadFile() {
        System.out.println("== Download file ==");
        System.out.print("File path: ");
        String path = scanner.nextLine();
        try{
            super.downloadFile(path);
            System.out.println("File downloaded successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Download failed");
            System.out.println(e.getMessage());
        }
    }

    public void deleteFile() {
        System.out.println("== Delete file ==");
        System.out.print("File path: ");
        String path = scanner.nextLine();
        try {
            super.deleteFile(path);
            System.out.println("File deleted successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Delete failed");
            System.out.println(e.getMessage());
        }
    }

    public void createFolder() {
        System.out.println("== Create folder ==");
        System.out.print("Folder path: ");
        String path = scanner.nextLine();
        try{
            super.createFolder(path);
            System.out.println("Folder created successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Create folder failed");
            System.out.println(e.getMessage());
        }
    }

    public void deleteFolder() {
        System.out.println("== Delete folder ==");
        System.out.print("Folder path: ");
        String path = scanner.nextLine();
        try{
            super.deleteFolder(path);
            System.out.println("Folder deleted successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Delete folder failed");
            System.out.println(e.getMessage());
        }
    }

    public void moveFolder() {
        System.out.println("== Move folder ==");
        System.out.print("Old folder path: ");
        String oldPath = scanner.nextLine();
        System.out.print("New folder path: ");
        String newPath = scanner.nextLine();
        try{
            super.moveFile(oldPath, newPath);
            System.out.println("Folder moved successfully");
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("Move failed");
            System.out.println(e.getMessage());
        }
    }

    public void listFolder() {
        System.out.println("== List Folder ==");
        System.out.print("Folder path (Enter for root): ");
        String path = scanner.nextLine();
        try{
            String tree = super.listFolder(path);
            System.out.println(tree);
        }
        catch (ClassCastException e){
            System.out.println("Server disconnected");
        }
        catch(Exception e){
            System.out.println("List failed");
            System.out.println(e.getMessage());
        }
    }

}
