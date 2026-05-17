package lyc.compiler.model;

public class StringTooLongException extends CompilerException{

    public StringTooLongException(String message) {
        super(message);
    }
}
