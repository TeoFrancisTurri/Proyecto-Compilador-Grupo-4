package lyc.compiler.intermediatecode;

import java.util.ArrayList;
import java.util.List;
import lyc.compiler.ast.*;
import lyc.compiler.symboltable.SymbolTableManager;


public final class IntermediateCode {

    private static final List<String> instructions = new ArrayList<>();
    private static int tempCount = 0;
    private static int labelCount = 0;

    private IntermediateCode() {}

    // ── API pública ──────────────────────────────────────────────────

    public static void generate(Node node) {
        if (node == null) return;
        switch (node.getType()) {
            case PROGRAM          -> generateProgram(node);
            case BLOCK            -> generateBlock(node);
            case INIT             -> generateBlock(node);
            case DECLARATION_LIST -> generateBlock(node);
            case VAR_DECLARATION  -> generateDeclaration(node);
            case ASSIG            -> generateAssig(node);
            case IF               -> generateIf(node);
            case IF_ELSE          -> generateIfElse(node);
            case WHILE            -> generateWhile(node);
            case READ             -> generateRead(node);
            case WRITE            -> generateWrite(node);
        }
    }

    public static List<String> getInstructions() {
        return instructions;
    }

    public static void reset() {
        instructions.clear();
        tempCount = 0;
        labelCount = 0;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String generateExpr(Node node) {
        if (node == null) return "";
        return switch (node.getType()) {
            case ID, CTE_INTEGER, CTE_FLOAT -> node.getValue();
            case CTE_STRING -> "\"" + node.getValue() + "\"";
            case ADD    -> generateBinaryOp(node, "+");
            case SUB    -> generateBinaryOp(node, "-");
            case MUL    -> generateBinaryOp(node, "*");
            case DIVIDE -> generateBinaryOpWithZeroCheck(node, "/");
            case NEGATE -> generateNegate(node);
            case DIV    -> generateBinaryOpWithZeroCheck(node, "DIV");
            case MOD    -> generateBinaryOpWithZeroCheck(node, "%");
            case GT     -> generateBinaryOp(node, ">");
            case LT     -> generateBinaryOp(node, "<");
            case GTE    -> generateBinaryOp(node, ">=");
            case LTE    -> generateBinaryOp(node, "<=");
            case EQ     -> generateBinaryOp(node, "==");
            case NEQ    -> generateBinaryOp(node, "!=");
            case AND    -> generateBinaryOp(node, "&&");
            case OR     -> generateBinaryOp(node, "||");
            case NOT    -> generateNot(node);
            default     -> "";
        };
    }

    private static void emit(String instruction) {
        instructions.add(instruction);
    }

    private static String newTemp() {
        return "t" + (++tempCount);
    }

    private static String newLabel() {
        return "L" + (++labelCount);
    }

    
    // ── Operaciones unarias ─────────────────────────────────────────
    private static String generateNegate(Node node) {
        String expr = generateExpr(node.getChild(0));
        String temp = newTemp();
        emit(temp + " = -" + expr);
        return temp;
    }

    // ── Operaciones binarias ─────────────────────────────────────────

    private static String generateBinaryOp(Node node, String op) {
        String left  = generateExpr(node.getChild(0));
        String right = generateExpr(node.getChild(1));
        String temp  = newTemp();
        emit(temp + " = " + left + " " + op + " " + right);
        return temp;
    }

    private static String generateBinaryOpWithZeroCheck(Node node, String op) {
        String left  = generateExpr(node.getChild(0));
        String right = generateExpr(node.getChild(1));

        String lError = newLabel();
        String lEnd   = newLabel();

        emit("if " + right + " == 0 goto " + lError); 
        String temp = newTemp();
        emit(temp + " = " + left + " " + op + " " + right);  
        emit("goto " + lEnd);                                  
        emit(lError + ":");
        emit("ERROR: division por cero");
        emit(lEnd + ":");

        return temp;
    }
    // ── Sentencias ───────────────────────────────────────────────────

    private static void generateProgram(Node node) {
        for (Node child : node.getChildren()) {
            generate(child);
        }
        emit("HALT"); 
    }

    private static void generateBlock(Node node) {
        for (Node child : node.getChildren()) {
            generate(child);
        }
    }

    private static void generateDeclaration(Node node) {
    for (Node child : node.getChildren()) {
        String id = child.getValue();
        String type = SymbolTableManager.getSymbols().get(id).getDataType().toString();
        emit("DECLARE " + id + " " + type);
        }
    }
    private static void generateAssig(Node node) {
        String id   = node.getChild(0).getValue();
        String expr = generateExpr(node.getChild(1));
        
        // si ya es un temporal no se crea otro
        if (expr.startsWith("t")) {
            emit(id + " = " + expr);
        } else {
            String temp = newTemp();
            emit(temp + " = " + expr);
            emit(id + " = " + temp);
        }
    }

    private static void generateIf(Node node) {
        String cond = generateExpr(node.getChild(0));
        String lEnd = newLabel();
        emit("if_false " + cond + " goto " + lEnd);
        generate(node.getChild(1));
        emit(lEnd + ":");
    }

    private static void generateIfElse(Node node) {
        String cond  = generateExpr(node.getChild(0));
        String lElse = newLabel();
        String lEnd  = newLabel();
        emit("if_false " + cond + " goto " + lElse);
        generate(node.getChild(1));
        emit("goto " + lEnd);
        emit(lElse + ":");
        generate(node.getChild(2));
        emit(lEnd + ":");
    }

    private static void generateWhile(Node node) {
        String lStart = newLabel();
        String lEnd   = newLabel();
        emit(lStart + ":");
        String cond = generateExpr(node.getChild(0));
        emit("if_false " + cond + " goto " + lEnd);
        generate(node.getChild(1));
        emit("goto " + lStart);
        emit(lEnd + ":");
    }

    private static void generateRead(Node node) {
        String id = node.getChild(0).getValue();
        emit("READ " + id);
    }

    private static void generateWrite(Node node) {
        String expr = generateExpr(node.getChild(0));
        emit("WRITE " + expr);
    }

    private static String generateNot(Node node) {
        String expr = generateExpr(node.getChild(0));
        String temp = newTemp();
        emit(temp + " = !" + expr);
        return temp;
    }
}