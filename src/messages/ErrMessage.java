package messages;

public class ErrMessage extends ResponseMessage {
    public ErrMessage(String message) {
        super("ERROR", message);
    }
}
