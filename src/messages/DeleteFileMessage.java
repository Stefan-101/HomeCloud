package messages;

public class DeleteFileMessage extends Message {
    private String filepath;

    public DeleteFileMessage(String filepath) {
        super("DELETE");
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }
}
