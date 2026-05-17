package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.ParserSym;
import lyc.compiler.model.*;
import static lyc.compiler.constants.Constants.*;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%throws CompilerException
%eofval{
  return symbol(ParserSym.EOF);
%eofval}


%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}
%state COMMENT
%state NESTED_COMMENT

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
Identation =  [ \t\f]


Init = "init"
Float = "Float"
Int = "Int"
String = "String"
Write = "write"
Read = "read"
While = "while"
If = "if"
Else = "else"
Not = "NOT"
Or = "OR"
And = "AND"
Long = "long"
Div = "DIV"
Mod = "MOD"

OpenComment = "#+"
CloseComment = "+#"
GreaterOrEqual = ">="
LessOrEqual = "<="
Equal = "=="
NotEqual = "!="


Plus = "+"
Mult = "*"
Sub = "-"
Divide = "/"
Assig = ":="
OpenParenthesis = "("
CloseParenteshis = ")"
OpenBracket = "["
CloseBracket = "]"
OpenBrace = "{"
CloseBrace = "}"
Comma = ","
Colon = ":"
Greater = ">"
Less = "<"


Letter = [a-zA-Z]
Digit = [0-9]


WhiteSpace = {LineTerminator} | {Identation}
Identifier = {Letter} ({Letter}|{Digit})*
IntegerConstant = {Digit}+
FloatConstant = {Digit}+\.{Digit}* | {Digit}*\.{Digit}+
StringConstant = \"[\x20-\x7E]*\" | “[\x20-\x7E]*”
%%


/* keywords */

<YYINITIAL> {

  /* keywords */
  {Init}                                    { return symbol(ParserSym.INIT); }
  {Float}                                   { return symbol(ParserSym.FLOAT); }
  {Int}                                     { return symbol(ParserSym.INT); }
  {String}                                  { return symbol(ParserSym.STRING); }
  {Read}                                    { return symbol(ParserSym.READ); }
  {Write}                                   { return symbol(ParserSym.WRITE); }
  {While}                                   { return symbol(ParserSym.WHILE); }
  {If}                                      { return symbol(ParserSym.IF); }
  {Else}                                    { return symbol(ParserSym.ELSE); }
  {Not}                                     { return symbol(ParserSym.NOT); }
  {Or}                                      { return symbol(ParserSym.OR); }
  {And}                                     { return symbol(ParserSym.AND); }
  {Long}                                    { return symbol(ParserSym.LONG); }
  {Mod}                                     { return symbol(ParserSym.MOD); }
  {Div}                                     { return symbol(ParserSym.DIV); }
  
   /* operators */
  {Greater}                                 { return symbol(ParserSym.GREATER); }
  {GreaterOrEqual}                          { return symbol(ParserSym.GREATER_OR_EQUAL); }
  {Less}                                    { return symbol(ParserSym.LESS); }
  {LessOrEqual}                             { return symbol(ParserSym.LESS_OR_EQUAL); }
  {Equal}                                   { return symbol(ParserSym.EQUAL); }
  {NotEqual}                                { return symbol(ParserSym.NOT_EQUAL); }

   
  {Plus}                                    { return symbol(ParserSym.PLUS); }
  {Sub}                                     { return symbol(ParserSym.SUB); }
  {Mult}                                    { return symbol(ParserSym.MULT); }
  {Divide}                                  { return symbol(ParserSym.DIVIDE); }
  {Assig}                                   { return symbol(ParserSym.ASSIG); }
  {OpenBracket}                             { return symbol(ParserSym.OPEN_BRACKET); }
  {CloseBracket}                            { return symbol(ParserSym.CLOSE_BRACKET); }
  {OpenBrace}                               { return symbol(ParserSym.OPEN_BRACE); }
  {CloseBrace}                              { return symbol(ParserSym.CLOSE_BRACE); }
  {OpenParenthesis}                         { return symbol(ParserSym.OPEN_PARENTHESIS); }
  {CloseParenteshis}                        { return symbol(ParserSym.CLOSE_PARENTHESIS); }
  {Comma}                                   { return symbol(ParserSym.COMMA); }
  {Colon}                                   { return symbol(ParserSym.COLON); }
  {OpenComment}                             { yybegin(COMMENT); }



  /* identifiers */
  {Identifier}                              { 
                                              if( yytext().length() > MAX_IDENTIFIER_LENGTH ){
                                                throw new InvalidFormatException("El identificador ingresado: " + yytext() + ", excede la longitud permitida.");
                                              }
                                              return symbol(ParserSym.IDENTIFIER, yytext()); 
                                            }
  /* Constants */
  {IntegerConstant}                         { 
                                              int valor;
                                              try{
                                                valor = Integer.parseInt(yytext());
                                              }
                                              catch( NumberFormatException e){
                                                throw new NumberOverflowException("El numero ingresado: " + yytext() + ", excede el rango permitido para un entero de 2 bytes.");
                                              }

                                              //Dejo pasar el numero 32768 ya que puede parsearse como negativo.
                                              if (valor > Short.MAX_VALUE + 1) {
                                                throw new NumberOverflowException(
                                                    "El numero ingresado: " + yytext() + " excede el rango permitido para un entero de 2 bytes.");
                                              }
                                              return symbol(ParserSym.CTE_INT, valor);
                                            }


  {FloatConstant}                           { 
                                              float valor;
                                              try{
                                                valor = Float.parseFloat(yytext());
                                                if (Float.isInfinite(valor) || Float.isNaN(valor)) {
                                                  throw new NumberOverflowException(
                                                    "El numero ingresado: " + yytext() + ", excede el rango permitido para float."
                                                  );
                                                }
                                              }
                                              catch( NumberFormatException e){
                                                throw new NumberOverflowException("El numero ingresado: " + yytext() + ", excede el rango permitido para float.");
                                              }
                                              return symbol(ParserSym.CTE_FLOAT, valor); 
                                            }

  {StringConstant}                          {
                                              String lexema = yytext().substring(1, yytext().length() - 1);
                                              
                                              if( lexema.length() > MAX_STRING_CONSTANT_LENGTH ){
                                                  throw new StringTooLongException("El string: " + lexema + " excede la longitud permitida de " + MAX_STRING_CONSTANT_LENGTH + " caracteres.");
                                              }
                                              
                                              return symbol(ParserSym.CTE_STRING, lexema);
                                            }


  /* whitespace */
  {WhiteSpace}                              { /* ignore */ }
}

<COMMENT> {
  {OpenComment}                             { yybegin(NESTED_COMMENT); }
  {CloseComment}                            { yybegin(YYINITIAL); } 
  [^]                                       { /* ignore */ }

}

<NESTED_COMMENT> {
  {OpenComment}                             { throw new InvalidNestedComment("No se permite anidar comentarios más de un nivel"); }
  {CloseComment}                            { yybegin(COMMENT); } 
  [^]                                       { /* ignore */ }
}

/* error fallback */
[^]                              { throw new UnknownCharacterException(yytext(), yyline, yycolumn); }
