package messages;

public class AuthMessage extends Message {
    private String username;
    private String password;

    public AuthMessage(String username, String password) {
        super("AUTH");
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
