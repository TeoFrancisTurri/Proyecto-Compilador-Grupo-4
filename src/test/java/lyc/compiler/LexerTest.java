package lyc.compiler;

import lyc.compiler.factories.LexerFactory;
import lyc.compiler.model.*;

import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static lyc.compiler.constants.Constants.MAX_IDENTIFIER_LENGTH;
import static lyc.compiler.constants.Constants.MAX_STRING_CONSTANT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertThrows;


//@Disabled
public class LexerTest {

  private Lexer lexer;


  @Test
  public void comment() throws Exception{
    scan("#+This is a comment+#");
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }

  @Test
  public void commentBetweenTokens() throws Exception {
    scan("a:=5 #+ comentario +#");
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_INT);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }

  @Test
  public void StringTooLong() {
    assertThrows(StringTooLongException.class, () -> {
      scan("\"%s\"".formatted(getRandomString()));
      nextToken();
    });
  }

  @Test
  public void invalidIdLength() {
    assertThrows(InvalidFormatException.class, () -> {
      scan(getRandomString());
      nextToken();
    });
  }

  @Test
  public void invalidPositiveIntegerConstantValue() {
    assertThrows(NumberOverflowException.class, () -> {
      scan("%d".formatted(9223372036854775807L));
      nextToken();
    });
  }
/*
  @Test
  public void invalidNegativeIntegerConstantValue() {
    assertThrows(InvalidNumberException.class, () -> {
      scan("%d".formatted(-9223372036854775807L));
      nextToken();
    });
  }
*/

  @Test
  public void assignmentWithExpressions() throws Exception {
    scan("c:=d*(e-21)/4");
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.MULT);
    assertThat(nextToken()).isEqualTo(ParserSym.OPEN_PARENTHESIS);
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.SUB);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_INT);
    assertThat(nextToken()).isEqualTo(ParserSym.CLOSE_PARENTHESIS);
    assertThat(nextToken()).isEqualTo(ParserSym.DIVIDE);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_INT);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }

  @Test
  public void unknownCharacter() {
    assertThrows(UnknownCharacterException.class, () -> {
      scan("#");
      nextToken();
    });
  }

  @Test
  public void validFloatConstant() throws Exception {
    scan("3.14 .5 2.");
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_FLOAT);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_FLOAT);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_FLOAT);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }

  @Test
  public void invalidFloatConstantValue() {
    assertThrows(NumberOverflowException.class, () -> {
      scan("999999999999999999999999999999999999999999999999999.0");
      nextToken();
    });
  }

  @Test
  public void negativeIntegerNumberAssignment() throws Exception {
    scan("c:=-32768");
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
    assertThat(nextToken()).isEqualTo(ParserSym.SUB);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_INT);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }



   @Test
  public void negativeFloatNumberAssignment() throws Exception{
    scan("c:=-5.2");
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
    assertThat(nextToken()).isEqualTo(ParserSym.SUB);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_FLOAT);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }


  @Test
  public void expressionWithStringAssignment() throws Exception {
    scan("msg:=\"hola\"");
    assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
    assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
    assertThat(nextToken()).isEqualTo(ParserSym.CTE_STRING);
    assertThat(nextToken()).isEqualTo(ParserSym.EOF);
  }

  @AfterEach
  public void resetLexer() {
    lexer = null;
  }

  private void scan(String input) {
    lexer = LexerFactory.create(input);
  }

  private int nextToken() throws IOException, CompilerException {
    return lexer.next_token().sym;
  }

  private static String getRandomString() {
    return new RandomStringGenerator.Builder()
            .filteredBy(CharacterPredicates.LETTERS)
            .withinRange('a', 'z')
            .build().generate(MAX_STRING_CONSTANT_LENGTH + 1);
  }

}
