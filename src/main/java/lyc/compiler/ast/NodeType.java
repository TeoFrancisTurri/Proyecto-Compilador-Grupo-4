package lyc.compiler.ast;

public enum NodeType {

    // ── Programa ────────────────────────────────────────────────────
    PROGRAM,

    // ── Bloques ─────────────────────────────────────────────────────
    BLOCK,
    INIT,
    DECLARATION_LIST,

    // ── Declaraciones y asignación ──────────────────────────────────
    VAR_DECLARATION,
    ASSIG,

    // ── Hojas (coinciden con SymbolType) ────────────────────────────
    ID,
    CTE_INTEGER,
    CTE_FLOAT,
    CTE_STRING,

    // ── Operaciones aritméticas ─────────────────────────────────────
    ADD,                // +
    SUB,                // -
    MUL,                // *
    DIVIDE,             // /
    DIV,                // división entera   expresion1 DIV expresion2
    MOD,                // resto de división expresion1 MOD expresion2
    NEGATE,             // negación unaria: -x

    // ── Comparaciones ───────────────────────────────────────────────
    GT,                 // >
    LT,                 // 
    GTE,                // >=
    LTE,                // <=
    EQ,                 // ==
    NEQ,                // !=

    // ── Lógicos ─────────────────────────────────────────────────────
    AND,
    OR,
    NOT,

    // ── Control de flujo ────────────────────────────────────────────
    IF,
    IF_ELSE,
    WHILE,

    // ── Funciones de I/O ────────────────────────────────────────────
    READ,
    WRITE,

    // ── Lista y longitud ────────────────────────────────────────────
    LIST,
    LIST_ELEM,
    LONG,

    // ── Funciones ───────────────────────────────────────────────────
    FUNC_DECLARATION,
    FUNC_CALL,
    PARAM,
    RETURN,
}