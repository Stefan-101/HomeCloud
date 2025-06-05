import messages.Message;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    public void createTable() {
        try (Connection con = DbConnection.getInstance();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS requests (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    description VARCHAR(255)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(100),
                    data BLOB,
                    request_id INT,
                    FOREIGN KEY (request_id) REFERENCES requests(id)
                );
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(Message message, int requestId) {
        String sql = "INSERT INTO messages (type, data, request_id) VALUES (?, ?, ?)";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, message.getType());

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(message);
                oos.flush();
                ps.setBytes(2, baos.toByteArray());
            }

            ps.setInt(3, requestId);

            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages";

        try (Connection con = DbConnection.getInstance();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                byte[] data = rs.getBytes("data");
                try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                     ObjectInputStream ois = new ObjectInputStream(bais)) {
                    Object obj = ois.readObject();
                    if (obj instanceof Message) {
                        messages.add((Message) obj);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public void update(int id, Message newMessage, int requestId) {
        String sql = "UPDATE messages SET type=?, data=?, request_id=? WHERE id=?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newMessage.getType());

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(newMessage);
                oos.flush();
                ps.setBytes(2, baos.toByteArray());
            }

            ps.setInt(3, requestId);
            ps.setInt(4, id);

            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM messages WHERE id=?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
