package messages;

import model.User;

public class CreateAccMessage extends Message{
    private User user;
    public CreateAccMessage(User user) {
        super("CREATE_ACCOUNT");
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
