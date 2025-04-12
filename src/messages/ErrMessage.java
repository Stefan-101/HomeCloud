package messages;

public class ErrMessage extends Message {
    private String message;

    public ErrMessage(String message) {
        super("ERROR");
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
