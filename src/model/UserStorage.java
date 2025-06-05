package model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class UserStorage implements Serializable {
    private String storagePath;
    private Folder rootFolder;

    public UserStorage(String storagePath) {
        this.storagePath = storagePath;
        this.rootFolder = new Folder();  // Root folder named after user
    }

    public UserStorage(String storagePath, Folder rootFolder) {
        this.storagePath = storagePath;
        this.rootFolder = rootFolder;
    }

    public UserStorage() {
        this.storagePath = "";
        this.rootFolder = new Folder();
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(Folder rootFolder) {
        this.rootFolder = rootFolder;
    }

    public void updateRootFolder() {
        File rootDir = new File(storagePath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("Invalid storage path: " + storagePath);
            return;
        }

        this.rootFolder = buildFolderFromDirectory(rootDir);
    }

    private Folder buildFolderFromDirectory(File dir) {
        Folder folder = new Folder(dir.getName());

        File[] children = dir.listFiles();
        if (children == null) return folder;

        for (File file : children) {
            if (file.isDirectory()) {
                folder.addSubFolder(buildFolderFromDirectory(file)); // recurse
            } else {
                folder.addFile(file.getName());
            }
        }

        return folder;
    }

    public static String getFolderTree(Path path) throws IOException {
        StringBuilder tree = new StringBuilder();
        walk(path, tree, "", true);
        return tree.toString();
    }

    private static void walk(Path path, StringBuilder tree, String indent, boolean isLast) throws IOException {
        tree.append(indent);
        if (isLast) {
            tree.append("└── ");
            indent += "    ";
        } else {
            tree.append("├── ");
            indent += "│   ";
        }
        tree.append(path.getFileName()).append("\n");

        File file = path.toFile();
        if (!file.isDirectory()) return;

        File[] files = file.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (int i = 0; i < files.length; i++) {
            boolean last = (i == files.length - 1);
            walk(files[i].toPath(), tree, indent, last);
        }
    }


    @Override
    public String toString() {
        return "model.UserStorage{" +
                ", storagePath='" + storagePath + '\'' +
                ", rootFolder=" + rootFolder +
                '}';
    }
}
