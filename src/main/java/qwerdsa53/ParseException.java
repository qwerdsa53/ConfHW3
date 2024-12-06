package qwerdsa53;

public class ParseException extends Exception {
    public ParseException(String message, int line) {
        super("На строке " + line + ": " + message);
    }
}
