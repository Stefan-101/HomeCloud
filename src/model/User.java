package model;

public class User {
    private String username;
    private String password;
    private String storagePath;

    public User(){
        username = "";
        password = "";
        storagePath = "";
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
}
