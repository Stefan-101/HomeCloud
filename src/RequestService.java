import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Request;
import model.User;

public class RequestService {

    public void createTable() {
        try (Connection con = DbConnection.getInstance();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS requests (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100),
                    ip VARCHAR(100),
                    action VARCHAR(255),
                    timestamp TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int insert(Request request) {
        String sql = "INSERT INTO requests (username, ip, action, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, request.getUser().getUsername());
            ps.setString(2, request.getIp());
            ps.setString(3, request.getAction());
            ps.setTimestamp(4, request.getTimestamp());
            ps.executeUpdate();

            return ps.getUpdateCount();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<Request> getAll(UserService userService) {
        List<Request> list = new ArrayList<>();
        String sql = "SELECT * FROM requests ORDER BY timestamp DESC";
        try (Connection con = DbConnection.getInstance();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String username = rs.getString("username");
                User user = userService.getByUsername(username);
                if (user != null) {
                    Request request = new Request(
                            user,
                            rs.getString("ip"),
                            rs.getString("action")
                    );
                    // Overwrite timestamp to match DB value
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) {
                        // Workaround to set it after constructor
                        request = new Request(user, rs.getString("ip"), rs.getString("action")) {
                            { this.timestamp = ts; }
                        };
                    }
                    list.add(request);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int update(int reqId, Request request) {
        String sql = "UPDATE requests SET username = ?, ip = ?, action = ?, timestamp = ? WHERE id = ?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, request.getUser().getUsername());
            ps.setString(2, request.getIp());
            ps.setString(3, request.getAction());
            ps.setTimestamp(4, request.getTimestamp());
            ps.setInt(5, reqId);

            return ps.executeUpdate();  // returns number of rows affected
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int delete(int id) {
        String sql = "DELETE FROM requests WHERE id = ?";
        try (Connection con = DbConnection.getInstance();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            return ps.executeUpdate();  // returns number of rows affected
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


}
