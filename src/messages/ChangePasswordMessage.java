package messages;

import model.User;

public class ChangePasswordMessage extends Message {
    User user;
    String newPassword;

    public ChangePasswordMessage(User user, String newPassword) {
        super("CHANGE_PW");
        this.user = user;
        this.newPassword = newPassword;
    }

    public User getUser() {
        return user;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
