package lyc.compiler.symboltable;

import java.util.HashMap;
import java.util.Map;
import lyc.compiler.model.DuplicatedIdentifierException;

public final class SymbolTableManager {

    private static final Map<String, SymbolEntry> symbols = new HashMap<>();

    private SymbolTableManager() {}


    public static boolean isDeclared(String name) {
        return symbols.containsKey(name);
    }

    private static String buildConstantKey(Object value, DataType dataType) {
        return "_" + dataType + "_" + value;
    }

    public static SymbolEntry addConstant(Object value, DataType dataType) {
        String name = buildConstantKey(value, dataType);

        if( isDeclared(name) ){
            return symbols.get(name);
        }
        SymbolEntry symbol = new SymbolEntry(name, dataType, value);
        symbols.put(name, symbol);
        return symbol;

    }

    public static SymbolEntry addIdentifier(String name, DataType dataType) throws DuplicatedIdentifierException {
        if (isDeclared(name)) {
            throw new DuplicatedIdentifierException("El identificador " + name + " ya fue declarado.");
        }

        SymbolEntry symbol = new SymbolEntry(name, dataType);
        symbols.put(name, symbol);

        return symbol;
    }

    public static Map<String, SymbolEntry> getSymbols() {
        return symbols;
    }
    
    public static SymbolEntry getSymbol(String name) {
        return symbols.get(name);
    }
    public static void reset() {
        symbols.clear();
    }
}
