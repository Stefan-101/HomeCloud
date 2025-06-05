package model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

public class User implements Serializable, Comparable<User> {
    private String username;
    private String password;
    private UserStorage storage;  // Added model.UserStorage reference

    public User() {
        username = "";
        password = "";
        storage = new UserStorage();  // initialize storage to null
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.storage = new UserStorage();
    }

    // Remove constructor with storagePath, add constructor with model.UserStorage
    public User(String username, String password, UserStorage storage) {
        this.username = username;
        this.password = password;
        this.storage = storage;
    }

    // Copy constructor updated
    public User(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.storage = user.getStorage();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserStorage getStorage() {
        return storage;
    }

    public void setStorage(UserStorage storage) {
        this.storage = storage;
    }

    public String getStoragePath() {
        return storage.getStoragePath();
    }

    public void setStoragePath(String storagePath) {
        storage.setStoragePath(storagePath);
    }

    public void updateStorage(){
        storage.updateRootFolder();
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public String getFolderTree(String path) throws IOException {
        return storage.getFolderTree(Path.of(path));
    }

    public User stripPassword() {
        User response = new User(this);
        response.setPassword("");
        return response;
    }

    @Override
    public int hashCode() {
        return username.hashCode() * 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof User)) return false;

        User user = (User) obj;
        return this.username.equals(user.getUsername());
    }

    @Override
    public String toString() {
        return username;
    }

    @Override
    public int compareTo(User otherUser) {
        return this.username.compareTo(otherUser.getUsername());
    }
}
