package messages;

public class UpdateFolderMessage extends Message {
    Action action;
    String oldFolderPath;
    String folderPath;

    public UpdateFolderMessage(Action action, String folderPath) {
        super("UPDATE_FOLDER");
        this.action = action;
        this.folderPath = folderPath;
    }
    public UpdateFolderMessage(Action action, String oldFolderPath, String folderPath) {
        super("UPDATE_FOLDER");
        this.action = action;
        this.oldFolderPath = oldFolderPath;
        this.folderPath = folderPath;
    }

    public Action getAction() {
        return action;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getOldFolderPath() {
        return oldFolderPath;
    }

}
