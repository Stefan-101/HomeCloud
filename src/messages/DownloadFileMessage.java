package messages;

public class DownloadFileMessage extends Message {
    private String filepath;    // this would be the path inside the user's folder on the server

    public DownloadFileMessage(String filepath) {
        super("DOWNLOAD");
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
