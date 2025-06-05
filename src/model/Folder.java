package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    private String name;
    private List<Folder> subFolders;
    private List<String> fileNames;
    // You can add a List<File> files here later if you want to track files

    public Folder() {
        this.subFolders = new ArrayList<>();
        this.fileNames = new ArrayList<>();
    }

    public Folder(String name) {
        this.name = name;
        this.subFolders = new ArrayList<>();
        this.fileNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Folder> getSubFolders() {
        return subFolders;
    }

    public void addSubFolder(Folder folder) {
        subFolders.add(folder);
    }

    public void removeSubFolder(Folder folder) {
        subFolders.remove(folder);
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", subFolders=" + subFolders +
                '}';
    }

    public void addFile(String name) {
        fileNames.add(name);
    }
}
