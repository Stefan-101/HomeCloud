package messages;

import model.User;

public class AuthMessage extends Message {
    private User user;

    public AuthMessage(String username, String password) {
        super("AUTH");
        this.user = new User(username, password);
    }

    public AuthMessage(User user) {
        super("AUTH");
        this.user = user;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getPassword() {
        return user.getPassword();
    }

    public User getUser() {
        return user;
    }
}
