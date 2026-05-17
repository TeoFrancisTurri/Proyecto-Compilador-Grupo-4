package lyc.compiler.intermediatecode;

import java.io.FileWriter;
import java.io.IOException;
import lyc.compiler.files.FileGenerator;

public class IntermediateCodeGenerator implements FileGenerator {

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        for (String instruction : IntermediateCode.getInstructions()) {
            fileWriter.write(instruction + "\n");
        }
    }
}