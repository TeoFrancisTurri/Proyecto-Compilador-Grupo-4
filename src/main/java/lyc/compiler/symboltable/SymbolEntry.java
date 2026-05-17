package lyc.compiler.symboltable;

public final class SymbolEntry {

    private final String name;
    private final DataType dataType;
    private final SymbolType symbolType;
    private final Object value;
    private final int length;

    // Identificadores
    public SymbolEntry(String name, DataType dataType) {
        this.name = name;
        this.dataType = dataType;
        this.symbolType = SymbolType.ID;
        this.value = null;
        this.length = 0;
    }

    // Constantes
    public SymbolEntry(String name, DataType dataType, Object value) {
        this.name = "_" + name;
        this.dataType = dataType;
        this.value = value;

        switch (dataType) {
            case INT:
                this.symbolType = SymbolType.CTE_INTEGER;
                this.length = 0;
                break;

            case FLOAT:
                this.symbolType = SymbolType.CTE_FLOAT;
                this.length = 0;
                break;

            case STRING:
                this.symbolType = SymbolType.CTE_STRING;
                this.length = ((String) value).length();
                break;

            default:
                throw new IllegalArgumentException(
                    "Tipo de dato inválido para constante"
                );
        }
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public SymbolType getSymbolType() {
        return symbolType;
    }

    public Object getValue() {
        return value;
    }

    public int getLength() {
        return length;
    }
}