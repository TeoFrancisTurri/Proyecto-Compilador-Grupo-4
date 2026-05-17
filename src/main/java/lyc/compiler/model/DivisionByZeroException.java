package lyc.compiler.model;

public class DivisionByZeroException extends CompilerException{

    public DivisionByZeroException(String message) {
        super(message);
    }
}
