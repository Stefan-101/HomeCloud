package model;

import java.sql.Timestamp;
import java.time.Instant;

public class Request {
    User user;
    String ip;
    String action;
    protected Timestamp timestamp;

    public Request(User user, String ip, String action) {
        this.user = user;
        this.ip = ip;
        this.action = action;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public User getUser() {
        return user;
    }

    public String getIp() {
        return ip;
    }

    public String getAction() {
        return action;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
