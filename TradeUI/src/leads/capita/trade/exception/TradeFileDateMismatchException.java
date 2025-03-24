package leads.capita.trade.exception;

public class TradeFileDateMismatchException extends Exception {
    public TradeFileDateMismatchException(Throwable throwable) {
        super(throwable);
    }

    public TradeFileDateMismatchException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public TradeFileDateMismatchException(String string) {
        super(string);
    }

    public TradeFileDateMismatchException() {
        super();
    }
}
