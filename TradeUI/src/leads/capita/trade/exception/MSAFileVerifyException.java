package leads.capita.trade.exception;

public class MSAFileVerifyException extends Exception {
    
    public MSAFileVerifyException(Throwable throwable) {
        super(throwable);
    }

    public MSAFileVerifyException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public MSAFileVerifyException(String string) {
        super(string);
    }

    public MSAFileVerifyException() {
        super();
    }
}
