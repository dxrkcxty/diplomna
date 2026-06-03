package exception;

public class ErrorResponse {
    public final int status;
    public final String error;
    public final String message;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
