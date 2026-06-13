package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.factories.ParserFactory;
import lyc.compiler.model.*;
import lyc.compiler.symboltable.SymbolTableManager;
import lyc.compiler.intermediatecode.IntermediateCode;
import org.apache.commons.io.IOUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;
import static lyc.compiler.Constants.EXAMPLES_ROOT_DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ParserTest {

    @BeforeEach
    public void reset() {
        
        IntermediateCode.reset();
        SymbolTableManager.reset();
    }

    @Test
    void init() throws Exception {
        compilationSuccessful("""
            init {
                a, a1, b1 : Float
                variable1, c, x, y, z : Int
                p1, p2, p3, b, var1 : String
            }
        """);

        assertThat(SymbolTableManager.isDeclared("a")).isTrue();
        assertThat(SymbolTableManager.isDeclared("a1")).isTrue();
        assertThat(SymbolTableManager.isDeclared("b1")).isTrue();
        assertThat(SymbolTableManager.isDeclared("variable1")).isTrue();
        assertThat(SymbolTableManager.isDeclared("c")).isTrue();
        assertThat(SymbolTableManager.isDeclared("x")).isTrue();
        assertThat(SymbolTableManager.isDeclared("y")).isTrue();
        assertThat(SymbolTableManager.isDeclared("z")).isTrue();
        assertThat(SymbolTableManager.isDeclared("p1")).isTrue();
        assertThat(SymbolTableManager.isDeclared("p2")).isTrue();
        assertThat(SymbolTableManager.isDeclared("p3")).isTrue();
        assertThat(SymbolTableManager.isDeclared("b")).isTrue();
        assertThat(SymbolTableManager.isDeclared("var1")).isTrue();
    }

    @Test
    void initDuplicatedIdentifier() {
        assertThrows(DuplicatedIdentifierException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                    x : Float
                }
            """);
        });
    }


    @Test
    public void assignmentWithExpression() throws Exception {
        compilationSuccessful("""
            init {
                c : Int
                d : Int
                e : Int
            }
            c := d * (e - 21) / 4
        """);
    }

    @Test
    public void syntaxError() {
        compilationError("1234");
    }

    @Test
    void assignments() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : String
            }
            a := 99999.99
            a := 99.
            a := .9999
            b := "@sdADaSjfla%dfg"
            b := "asldk  fh sjf"
        """);
    }

    @Test
    void write() throws Exception {
        compilationSuccessful("""
            init {
                var1 : Int
            }
            write("ewr")
            write(var1)
        """);
    }

    @Test
    void writeUndeclaredVariable() {
        assertThrows(UndeclaredVariableException.class, () -> {
            compilationSuccessful("""
                read(var2)
            """);
        });
    }

    @Test
    void read() throws Exception {
        compilationSuccessful("""
            init {
                base : String
            }
            read(base)
        """);
    }

    @Test
    void readUndeclaredVariable() {
        assertThrows(UndeclaredVariableException.class, () -> {
            compilationSuccessful("""
                read(undeclared)
            """);
        });
    }


    @Test
    void comment() throws Exception {
        compilationSuccessful(readFromFile("comment.txt"));
    }

    @Test
    void and() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : Float
                c : Float
            }
            if (a > b AND c > b)
            {
                write("a es mas grande que b y c es mas grande que b")
            }
        """);
    }

    @Test
    void or() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : Float
                c : Float
            }
            if (a > b OR c > b)
            {
                write("a es mas grande que b o c es mas grande que b")
            }
        """);
    }

    @Test
    void not() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : Float
            }
            if (NOT a > b)
            {
                write("a no es mas grande que b")
            }
        """);
    }

    @Test
    void ifElseStatement() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : Float
            }
            if (a > b)
            {
                write("a es mas grande que b")
            }
            else
            {
                write("a es mas chico o igual a b")
            }
        """);
    }

    @Test
    void whileStatement() throws Exception {
        compilationSuccessful("""
            init {
                a : Float
                b : Float
            }
            while (a > b)
            {
                write("a es mas grande que b")
                a := a + 1
            }
        """);
    }

    @Test
    void invalidLimitOverflowInteger() {
        assertThrows(InvalidNumberException.class, () -> {
            compilationSuccessful("c:=32768");
        });
    }

    @Test
    void arithmeticIncompatibleStringOperation() {
        assertThrows(IncompatibleTypesException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                    b : String
                }
                x := x + b
            """);
        });
    }

    @Test
    void arithmeticAssignFloatToInt() throws Exception {
        // INT and FLOAT are compatible numeric types - this should succeed
        compilationSuccessful("""
            init {
                x : Int
                a : Float
            }
            x := a + 1.0
        """);
    }

    @Test
    void assignStringToNumeric() {
        assertThrows(IncompatibleTypesException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                    b : String
                }
                x := b
            """);
        });
    }

    @Test
    void assignNumericToString() {
        assertThrows(IncompatibleTypesException.class, () -> {
            compilationSuccessful("""
                init {
                    b : String
                }
                b := 5
            """);
        });
    }

    @Test
    void arithmeticDivisionByZero() {
        assertThrows(DivisionByZeroException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                }
                x := x DIV 0
            """);
        });
    }

    @Test
    void longFunction() throws Exception {
        compilationSuccessful("""
            init {
                a : Int
            }
            a := long([1, 2, 3, 4, 5])
        """);
    }

    @Test
    void longFunctionWithExpressions() throws Exception {
        compilationSuccessful("""
            init {
                a : Int
                x : Int
                y : Int
            }
            a := long([x + y, x * y, x - y])
        """);
    }

    @Test
    void longFunctionInExpression() throws Exception {
        compilationSuccessful("""
            init {
                a : Int
                x : Int
            }
            a := long([1, 2, 3]) + x
        """);
    }

    @Test
    void conditionWithArithmetic() throws Exception {
        compilationSuccessful("""
            init {
                x : Int
                y : Int
                z : Int
            }
            if (x + y > z)
            {
                write("x + y es mayor que z")
            }
        """);
    }

    @Test
    void conditionWithArithmeticAndLogic() throws Exception {
        compilationSuccessful("""
            init {
                x : Int
                y : Int
                z : Int
            }
            if (x + y > z AND x * y > z)
            {
                write("ambas condiciones son verdaderas")
            }
        """);
    }

    @Test
    void whileWithArithmetic() throws Exception {
        compilationSuccessful("""
            init {
                x : Int
                y : Int
            }
            while (x * 2 > y)
            {
                x := x - 1
                write(x)
            }
        """);
    }

    @Test
    void modOperation() throws Exception {
        compilationSuccessful("""
            init {
                x : Int
                y : Int
                z : Int
            }
            z := x MOD y
            z := 10 MOD 3
            z := (x + 1) MOD (y + 2)
        """);
    }

    @Test
    void divideDivisionByZero() {
        assertThrows(DivisionByZeroException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                }
                x := x / 0
            """);
        });
    }

    @Test
    void modDivisionByZero() {
        assertThrows(DivisionByZeroException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                }
                x := x MOD 0
            """);
        });
    }
    @Test
    void modWithString() {
        assertThrows(IncompatibleTypesException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                    b : String
                }
                x := x MOD b
            """);
        });
    }
    @Test
    void divWithString() {
        assertThrows(IncompatibleTypesException.class, () -> {
            compilationSuccessful("""
                init {
                    x : Int
                    b : String
                }
                x := x DIV b
            """);
        });
    }



    private void compilationSuccessful(String input) throws Exception {
        assertThat(scan(input).sym).isEqualTo(ParserSym.EOF);
    }

    private void compilationError(String input){
        assertThrows(Exception.class, () -> scan(input));
    }

    private Symbol scan(String input) throws Exception {
        return ParserFactory.create(input).parse();
    }

    private String readFromFile(String fileName) throws IOException {
        URL url = new URL(EXAMPLES_ROOT_DIRECTORY + "/%s".formatted(fileName));
        assertThat(url).isNotNull();
        return IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
    }


}
