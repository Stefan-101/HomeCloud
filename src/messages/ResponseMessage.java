package messages;

public class ResponseMessage extends Message {
    private String response;

    public ResponseMessage(String response) {
        super("RESPONSE");
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
