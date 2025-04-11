package messages;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String type;

    public Message(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean equals(String str) {
        return type.equals(str);
    }
}
