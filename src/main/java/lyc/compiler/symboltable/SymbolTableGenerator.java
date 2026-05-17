package lyc.compiler.symboltable;

import java.io.FileWriter;
import java.io.IOException;
import lyc.compiler.files.FileGenerator;

public class SymbolTableGenerator implements FileGenerator{
    
    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write(String.format("%-60s  %-15s  %-10s  %-50s  %-10s\n",
                "NOMBRE", "TIPOSIMBOLO", "TIPODATO", "VALOR", "LONGITUD"));

        String name, dataType, symbolType, value, length;

        for(SymbolEntry entry : SymbolTableManager.getSymbols().values()){
   
            name = entry.getName();
            dataType = entry.getDataType().toString();
            symbolType = entry.getSymbolType().toString();
            length = Integer.toString(entry.getLength());

            if(entry.getValue() != null)
                value = entry.getValue().toString();
            else
                value = "-";   

            if( entry.getDataType() != DataType.STRING || entry.getValue() == null)
                length = "-";
            
            
            fileWriter.write(String.format("%-60s  %-15s  %-10s  %-50s  %-10s\n",
                    name, symbolType, dataType, value, length));
        }
    }
}
