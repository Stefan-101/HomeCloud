package messages;

public class GetFolderTreeMessage extends Message {
    String path;

    public GetFolderTreeMessage(String path) {
        super("GET_FOLDER_TREE");
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
