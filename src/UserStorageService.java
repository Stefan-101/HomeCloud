import model.User;
import model.UserStorage;
import model.Folder;

import java.io.*;
import java.sql.*;

public class UserStorageService {

    public void createTable() {
        try (Connection con = DbConnection.getInstance();
             Statement stmt = con.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_storage (
                        username VARCHAR(100) PRIMARY KEY,
                        storage_path VARCHAR(255),
                        folder_structure BLOB,
                        FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                    )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(String username, UserStorage storage) {
        String sql = "INSERT INTO user_storage (username, storage_path, folder_structure) VALUES (?, ?, ?)";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, storage.getStoragePath());
            ps.setBytes(3, serialize(storage.getRootFolder()));
            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public UserStorage getByUsername(String username) {
        String sql = "SELECT * FROM user_storage WHERE username = ?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storagePath = rs.getString("storage_path");
                    byte[] folderBytes = rs.getBytes("folder_structure");
                    Folder rootFolder = deserialize(folderBytes);

                    return new UserStorage(storagePath, rootFolder);
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateStorage(String username, UserStorage storage) {
        String sql = "INSERT INTO user_storage (username, storage_path, folder_structure) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "storage_path = VALUES(storage_path), " +
                "folder_structure = VALUES(folder_structure)";

        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, storage.getStoragePath());
            ps.setBytes(3, serialize(storage.getRootFolder()));
            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


    public void delete(String username) {
        String sql = "DELETE FROM user_storage WHERE username=?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Serialize Folder object to byte array
    private byte[] serialize(Folder folder) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(folder);
            return bos.toByteArray();
        }
    }

    // Deserialize byte array back to Folder object
    private Folder deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Folder) ois.readObject();
        }
    }
}
