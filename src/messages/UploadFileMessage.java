package messages;

import java.io.IOException;

public class UploadFileMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String filename;

    public UploadFileMessage(String filename) throws IOException {
        super("UPLOAD");
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
