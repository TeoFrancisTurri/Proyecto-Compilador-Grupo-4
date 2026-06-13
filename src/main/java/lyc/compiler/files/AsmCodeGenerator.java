package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import lyc.compiler.ast.*;
import lyc.compiler.symboltable.*;

public class AsmCodeGenerator implements FileGenerator {

    private final Node ast;
    private final StringBuilder dataSection = new StringBuilder();
    private final StringBuilder codeSection = new StringBuilder();
    private int labelCount = 0;
    private int tempCount = 0;
    private final Set<String> declaredTemps = new LinkedHashSet<>();
    private final Set<String> declaredStringConstants = new LinkedHashSet<>();

    public AsmCodeGenerator(Node ast) {
        this.ast = ast;
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        // Reset
        dataSection.setLength(0);
        codeSection.setLength(0);
        labelCount = 0;
        tempCount = 0;
        declaredTemps.clear();
        declaredStringConstants.clear();

        // Generate code section first (to discover temps needed)
        generateNode(ast);

        // Build the full ASM file
        StringBuilder asm = new StringBuilder();

        // Includes
        asm.append("include macros2.asm\r\n");
        asm.append("include number.asm\r\n");
        asm.append("\r\n");
        asm.append(".MODEL LARGE\r\n");
        asm.append(".386\r\n");
        asm.append(".STACK 200h\r\n");
        asm.append("\r\n");
        asm.append("MAXTEXTSIZE equ 50\r\n");
        asm.append("\r\n");

        // DATA section
        asm.append(".DATA\r\n");
        asm.append("\r\n");

        // Emit variables and constants from symbol table
        emitDataDeclarations(asm);

        // Emit temporaries
        for (String temp : declaredTemps) {
            asm.append("    ").append(temp).append("    dd ?\r\n");
        }

        // Emit string constants collected during code generation
        for (String strDecl : declaredStringConstants) {
            asm.append("    ").append(strDecl).append("\r\n");
        }

        // Aux variables
        asm.append("    _aux        db MAXTEXTSIZE dup (?),'$'\r\n");
        asm.append("    _msgPRESIONE    db 0DH,0AH,\"Presione una tecla para continuar...\",'$'\r\n");
        asm.append("    _NEWLINE        db 0DH,0AH,'$'\r\n");
        asm.append("    _msgDIV0        db 0DH,0AH,\"Error: division por cero\",'$'\r\n");
        asm.append("\r\n");

        // CODE section
        asm.append(".CODE\r\n");
        asm.append("\r\n");
        asm.append("START:\r\n");
        asm.append("    mov AX,@DATA\r\n");
        asm.append("    mov DS,AX\r\n");
        asm.append("    mov es,ax\r\n");

        // Append generated code
        asm.append(codeSection);

        // End: press key + exit
        asm.append("    mov dx,OFFSET _NEWLINE\r\n");
        asm.append("    mov ah,09\r\n");
        asm.append("    int 21h\r\n");
        asm.append("    mov dx,OFFSET _msgPRESIONE\r\n");
        asm.append("    mov ah,09\r\n");
        asm.append("    int 21h\r\n");
        asm.append("    mov ah, 1\r\n");
        asm.append("    int 21h\r\n");
        asm.append("    mov ax, 4C00h\r\n");
        asm.append("    int 21h\r\n");
        asm.append("END START\r\n");

        fileWriter.write(asm.toString());
    }

    // ── Data declarations ─────────────────────────────────────────

    private void emitDataDeclarations(StringBuilder asm) {
        Map<String, SymbolEntry> symbols = SymbolTableManager.getSymbols();

        for (Map.Entry<String, SymbolEntry> entry : symbols.entrySet()) {
            String key = entry.getKey();
            SymbolEntry sym = entry.getValue();
            String asmName = sanitizeName(key);

            switch (sym.getSymbolType()) {
                case ID:
                    if (sym.getDataType() == DataType.STRING) {
                        asm.append("    ").append(asmName)
                           .append("    db MAXTEXTSIZE dup (?),'$'\r\n");
                    } else {
                        // INT and FLOAT both stored as dd (32 bits, used with FPU)
                        asm.append("    ").append(asmName)
                           .append("    dd ?\r\n");
                    }
                    break;

                case CTE_INTEGER:
                    int intVal = ((Number) sym.getValue()).intValue();
                    asm.append("    ").append(asmName)
                       .append("    dd ").append(intVal).append(".0\r\n");
                    break;

                case CTE_FLOAT:
                    float floatVal = ((Number) sym.getValue()).floatValue();
                    asm.append("    ").append(asmName)
                       .append("    dd ").append(formatFloat(floatVal)).append("\r\n");
                    break;

                case CTE_STRING:
                    String strVal = sym.getValue().toString();
                    String safeStr = strVal.replace("\"", "'");
                    int padding = Math.max(0, 50 - safeStr.length());
                    asm.append("    ").append(asmName)
                       .append("    db \"").append(safeStr).append("\",'$'");
                    if (padding > 0) {
                        asm.append(", ").append(padding).append(" dup (?)");
                    }
                    asm.append("\r\n");
                    break;
            }
        }
        asm.append("\r\n");
    }

    // ── AST traversal (code generation) ───────────────────────────

    private void generateNode(Node node) {
        if (node == null) return;
        switch (node.getType()) {
            case PROGRAM          -> generateProgram(node);
            case BLOCK            -> generateBlock(node);
            case INIT             -> {} // declarations handled in data section
            case DECLARATION_LIST -> {} // handled above
            case VAR_DECLARATION  -> {} // handled above
            case ASSIG            -> generateAssig(node);
            case IF               -> generateIf(node);
            case IF_ELSE          -> generateIfElse(node);
            case WHILE            -> generateWhile(node);
            case READ             -> generateRead(node);
            case WRITE            -> generateWrite(node);
            default               -> {}
        }
    }

    private void generateProgram(Node node) {
        for (Node child : node.getChildren()) {
            generateNode(child);
        }
    }

    private void generateBlock(Node node) {
        for (Node child : node.getChildren()) {
            generateNode(child);
        }
    }

    // ── Assignment ────────────────────────────────────────────────

    private void generateAssig(Node node) {
        String id = node.getChild(0).getValue();
        String asmId = sanitizeName(id);
        Node expr = node.getChild(1);

        DataType idType = SymbolTableManager.getSymbol(id).getDataType();

        if (idType == DataType.STRING) {
            // String assignment: use STRCPY macro
            String exprName = generateStringExpr(expr);
            emit("    lea SI, " + exprName);
            emit("    lea DI, " + asmId);
            emit("    STRCPY");
        } else {
            // Numeric assignment: evaluate expression to FPU ST(0), then store
            generateNumericExpr(expr);
            emit("    fstp " + asmId);
        }
    }

    // ── Numeric expression → leaves result in ST(0) ───────────────

    private void generateNumericExpr(Node node) {
        if (node == null) return;

        switch (node.getType()) {
            case ID -> {
                String asmName = sanitizeName(node.getValue());
                emit("    fld " + asmName);
            }
            case CTE_INTEGER -> {
                String constName = getConstantAsmName(node.getValue(), DataType.INT);
                emit("    fld " + constName);
            }
            case CTE_FLOAT -> {
                String constName = getConstantAsmName(node.getValue(), DataType.FLOAT);
                emit("    fld " + constName);
            }
            case ADD -> {
                generateNumericExpr(node.getChild(0));
                generateNumericExpr(node.getChild(1));
                emit("    fadd");
            }
            case SUB -> {
                generateNumericExpr(node.getChild(0));
                generateNumericExpr(node.getChild(1));
                emit("    fsub");
            }
            case MUL -> {
                generateNumericExpr(node.getChild(0));
                generateNumericExpr(node.getChild(1));
                emit("    fmul");
            }
            case DIVIDE -> {
                generateNumericExpr(node.getChild(0));
                generateNumericExpr(node.getChild(1));
                // Runtime division by zero check
                String lblOk = newLabel();
                String lblError = newLabel();
                emit("    ftst");
                emit("    fstsw ax");
                emit("    sahf");
                emit("    JNE " + lblOk);
                // divisor is zero
                emit("    displayString _msgDIV0");
                emit("    newLine 1");
                emit("    mov ax, 4C00h");
                emit("    int 21h");
                emit(lblOk + ":");
                emit("    fdiv");
            }
            case MOD -> {
                // MOD: a MOD b = a - (trunc(a/b) * b)  or use fprem
                generateNumericExpr(node.getChild(1)); // divisor in ST(0)
                generateNumericExpr(node.getChild(0)); // dividend in ST(0), divisor in ST(1)
                // Runtime division by zero check on divisor (now ST(1))
                String lblModOk = newLabel();
                String tempMod = newTemp();
                emit("    fstp " + tempMod);  // save dividend
                emit("    ftst");              // test divisor (now ST(0))
                emit("    fstsw ax");
                emit("    sahf");
                emit("    JNE " + lblModOk);
                emit("    displayString _msgDIV0");
                emit("    newLine 1");
                emit("    mov ax, 4C00h");
                emit("    int 21h");
                emit(lblModOk + ":");
                emit("    fld " + tempMod);   // reload dividend to ST(0), divisor in ST(1)
                String lblRepeat = newLabel();
                emit(lblRepeat + ":");
                emit("    fprem");
                emit("    fstsw ax");
                emit("    sahf");
                emit("    JP " + lblRepeat);    // repeat if C2 is set (partial remainder)
                emit("    fxch");
                emit("    ffree st(0)");
                emit("    fincstp");
            }
            case DIV -> {
                // Integer division: trunc(a / b)
                generateNumericExpr(node.getChild(0));
                generateNumericExpr(node.getChild(1));
                // Runtime division by zero check
                String lblDivOk = newLabel();
                emit("    ftst");
                emit("    fstsw ax");
                emit("    sahf");
                emit("    JNE " + lblDivOk);
                emit("    displayString _msgDIV0");
                emit("    newLine 1");
                emit("    mov ax, 4C00h");
                emit("    int 21h");
                emit(lblDivOk + ":");
                emit("    fdiv");
                // Truncate: save rounding mode, set to truncate, round, restore
                String tempCW1 = newTemp();
                String tempCW2 = newTemp();
                emit("    fstcw word ptr " + tempCW1);
                emit("    mov ax, word ptr " + tempCW1);
                emit("    or ax, 0C00h");           // set RC to truncate (11)
                emit("    mov word ptr " + tempCW2 + ", ax");
                emit("    fldcw word ptr " + tempCW2);
                emit("    frndint");
                emit("    fldcw word ptr " + tempCW1);
            }
            case NEGATE -> {
                generateNumericExpr(node.getChild(0));
                emit("    fchs");
            }
            default -> {
                // If it's a complex expression stored in a temp, load it
            }
        }
    }

    // ── String expression → returns ASM label name ────────────────

    private String generateStringExpr(Node node) {
        if (node.getType() == NodeType.ID) {
            return sanitizeName(node.getValue());
        } else if (node.getType() == NodeType.CTE_STRING) {
            return getConstantAsmName(node.getValue(), DataType.STRING);
        }
        return "_aux";
    }

    // ── Conditions ────────────────────────────────────────────────

    /**
     * Generates a condition and jumps to labelFalse if the condition is FALSE.
     */
    private void generateCondition(Node node, String labelFalse) {
        switch (node.getType()) {
            case GT, LT, GTE, LTE, EQ, NEQ -> {
                generateComparison(node, labelFalse);
            }
            case AND -> {
                // Short-circuit: if first is false, skip
                generateCondition(node.getChild(0), labelFalse);
                generateCondition(node.getChild(1), labelFalse);
            }
            case OR -> {
                // Short-circuit: if first is true, skip to after
                String labelTrue = newLabel();
                generateConditionTrue(node.getChild(0), labelTrue);
                generateCondition(node.getChild(1), labelFalse);
                emit(labelTrue + ":");
            }
            case NOT -> {
                // Negate: jump to labelFalse if inner condition is TRUE
                // We need a label for "inner true" which means outer false
                String labelInnerTrue = newLabel();
                generateConditionTrue(node.getChild(0), labelInnerTrue);
                emit("    JMP " + labelFalse);
                emit(labelInnerTrue + ":");
            }
            default -> {}
        }
    }

    /**
     * Generates a condition and jumps to labelTrue if the condition is TRUE.
     */
    private void generateConditionTrue(Node node, String labelTrue) {
        switch (node.getType()) {
            case GT, LT, GTE, LTE, EQ, NEQ -> {
                generateComparisonTrue(node, labelTrue);
            }
            case AND -> {
                String labelFail = newLabel();
                generateCondition(node.getChild(0), labelFail);
                generateConditionTrue(node.getChild(1), labelTrue);
                emit(labelFail + ":");
            }
            case OR -> {
                generateConditionTrue(node.getChild(0), labelTrue);
                generateConditionTrue(node.getChild(1), labelTrue);
            }
            case NOT -> {
                generateCondition(node.getChild(0), labelTrue);
            }
            default -> {}
        }
    }

    private void generateComparison(Node node, String labelFalse) {
        Node left = node.getChild(0);
        Node right = node.getChild(1);

        // Check if it's a string comparison
        if (left.getDataType() == DataType.STRING) {
            generateStringComparison(node, left, right, labelFalse, false);
            return;
        }

        // Numeric comparison using FPU
        generateNumericExpr(left);
        generateNumericExpr(right);
        emit("    fxch");
        emit("    fcomp");
        emit("    fstsw ax");
        emit("    ffree st(0)");
        emit("    fincstp");
        emit("    sahf");

        // Jump to labelFalse if condition is FALSE
        // FPU comparison: fcomp compares ST(0) with ST(1), pops ST(0)
        // After fxch: ST(0)=right, ST(1)=left. fcomp compares ST(0) [right] with ST(1) [left]
        // Actually we want to compare left vs right, so:
        // We load left first, then right. After fxch: ST(0)=left, ST(1)=right.
        // fcomp compares ST(0) [left] with ST(1) [right]
        switch (node.getType()) {
            case GT  -> emit("    JBE " + labelFalse);  // jump if left <= right
            case LT  -> emit("    JAE " + labelFalse);  // jump if left >= right
            case GTE -> emit("    JB " + labelFalse);   // jump if left < right
            case LTE -> emit("    JA " + labelFalse);   // jump if left > right
            case EQ  -> emit("    JNE " + labelFalse);  // jump if left != right
            case NEQ -> emit("    JE " + labelFalse);   // jump if left == right
            default -> {}
        }
    }

    private void generateComparisonTrue(Node node, String labelTrue) {
        Node left = node.getChild(0);
        Node right = node.getChild(1);

        if (left.getDataType() == DataType.STRING) {
            generateStringComparison(node, left, right, labelTrue, true);
            return;
        }

        generateNumericExpr(left);
        generateNumericExpr(right);
        emit("    fxch");
        emit("    fcomp");
        emit("    fstsw ax");
        emit("    ffree st(0)");
        emit("    fincstp");
        emit("    sahf");

        // Jump to labelTrue if condition is TRUE
        switch (node.getType()) {
            case GT  -> emit("    JA " + labelTrue);
            case LT  -> emit("    JB " + labelTrue);
            case GTE -> emit("    JAE " + labelTrue);
            case LTE -> emit("    JBE " + labelTrue);
            case EQ  -> emit("    JE " + labelTrue);
            case NEQ -> emit("    JNE " + labelTrue);
            default -> {}
        }
    }

    private void generateStringComparison(Node node, Node left, Node right,
                                           String targetLabel, boolean jumpOnTrue) {
        String leftName = generateStringExpr(left);
        String rightName = generateStringExpr(right);

        emit("    lea SI, " + leftName);
        emit("    lea DI, " + rightName);
        emit("    STRCMP");

        // STRCMP sets ZF=1 if equal, ZF=0 if not equal
        // We only support EQ and NEQ for strings
        if (jumpOnTrue) {
            switch (node.getType()) {
                case EQ  -> emit("    JE " + targetLabel);
                case NEQ -> emit("    JNE " + targetLabel);
                default -> emit("    JE " + targetLabel); // default to EQ
            }
        } else {
            switch (node.getType()) {
                case EQ  -> emit("    JNE " + targetLabel);
                case NEQ -> emit("    JE " + targetLabel);
                default -> emit("    JNE " + targetLabel);
            }
        }
    }

    // ── Control flow ──────────────────────────────────────────────

    private void generateIf(Node node) {
        Node condition = node.getChild(0);
        Node body = node.getChild(1);
        String labelEnd = newLabel();

        generateCondition(condition, labelEnd);
        generateNode(body);
        emit(labelEnd + ":");
    }

    private void generateIfElse(Node node) {
        Node condition = node.getChild(0);
        Node bodyTrue = node.getChild(1);
        Node bodyFalse = node.getChild(2);
        String labelElse = newLabel();
        String labelEnd = newLabel();

        generateCondition(condition, labelElse);
        generateNode(bodyTrue);
        emit("    JMP " + labelEnd);
        emit(labelElse + ":");
        generateNode(bodyFalse);
        emit(labelEnd + ":");
    }

    private void generateWhile(Node node) {
        Node condition = node.getChild(0);
        Node body = node.getChild(1);
        String labelStart = newLabel();
        String labelEnd = newLabel();

        emit(labelStart + ":");
        generateCondition(condition, labelEnd);
        generateNode(body);
        emit("    JMP " + labelStart);
        emit(labelEnd + ":");
    }

    // ── I/O ───────────────────────────────────────────────────────

    private void generateRead(Node node) {
        String id = node.getChild(0).getValue();
        String asmName = sanitizeName(id);
        DataType type = SymbolTableManager.getSymbol(id).getDataType();

        if (type == DataType.STRING) {
            emit("    getString " + asmName);
        } else {
            emit("    GetFloat " + asmName);
        }
        emit("    newLine 1");
    }

    private void generateWrite(Node node) {
        Node expr = node.getChild(0);

        if (expr.getDataType() == DataType.STRING) {
            String strName = generateStringExpr(expr);
            emit("    displayString " + strName);
        } else {
            // Evaluate numeric expression, store in temp, display
            String temp = newTemp();
            generateNumericExpr(expr);
            emit("    fstp " + temp);
            emit("    DisplayFloat " + temp + ", 2");
        }
        emit("    newLine 1");
    }

    // ── Utilities ─────────────────────────────────────────────────

    private String newLabel() {
        return "ET_" + (++labelCount);
    }

    private String newTemp() {
        String name = "@temp" + (++tempCount);
        declaredTemps.add(name);
        return name;
    }

    private void emit(String line) {
        codeSection.append(line).append("\r\n");
    }

    /**
     * Sanitize a symbol name for ASM (replace characters not valid in TASM labels).
     */
    private String sanitizeName(String name) {
        // Symbol table constant keys start with _ (e.g. _INT_5, _STRING_hello)
        // Variable names are just letters and digits — already valid
        return name.replace(".", "_").replace("-", "_").replace(" ", "_");
    }

    /**
     * Get the ASM name for a constant value by looking it up in the symbol table.
     */
    private String getConstantAsmName(String value, DataType type) {
        String key = "_" + type + "_" + value;
        return sanitizeName(key);
    }

    private String formatFloat(float f) {
        if (f == (int) f) {
            return String.valueOf((int) f) + ".0";
        }
        return String.valueOf(f);
    }
}
