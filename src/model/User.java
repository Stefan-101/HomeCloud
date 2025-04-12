package model;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private String storagePath;

    {
        storagePath = "";
    }

    public User(){
        username = "";
        password = "";
    }

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String storagePath) {
        this.username = username;
        this.password = password;
        this.storagePath = storagePath + "/" + username;
    }

    public User(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.storagePath = user.storagePath;
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

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public boolean checkPassword(String password){
        return this.password.equals(password);
    }

    @Override
    public int hashCode() {
        return username.hashCode() * 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        User user = (User) obj;
        return this.username.equals(user.getUsername());
    }

    @Override
    public String toString() {
        return username;
    }
}
