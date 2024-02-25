import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MacroProcessor {
    private Map<String, Macro> macros;

    public MacroProcessor() {
        macros = new HashMap<>();
    }

    public void process(String inputFileName) {
        List<String> outputLines = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().contains("MACRO")) {
                    String[] lineSplited = line.split("\\s+");
                    String macroNameDetect = "MACRO.";
                    Pattern p = Pattern.compile(macroNameDetect);
                    int i;
                    ArrayList<String> argsList = new ArrayList<>();

                    for(i = 0; i < lineSplited.length; i++) {
                        Matcher m = p.matcher(lineSplited[i]);
                        if(m.find())
                            break;
                    }

                    for(int j = i + 1; j < lineSplited.length; j++) {
                        argsList.add(lineSplited[j]);
                    }

                    if(this.macros.containsKey(lineSplited[i]))
                        insertExistMacro(line, outputLines, this.macros, lineSplited[i], argsList);
                    else
                        processMacroDefinition(br, line);
                } else if (line.trim().startsWith("CALL")) {
                    String macroName = line.trim().substring(5).trim();
                    Macro macro = macros.get(macroName);
                    if (macro != null) {
                        List<String> arguments = parseArguments(line);
                        List<String> expandedMacro = macro.expand(arguments);
                        outputLines.addAll(expandedMacro);
                    }else {
                    throw new IllegalArgumentException("Erro: Macro '" + macroName + "' não definida. Linha: " + line);
                    }
                } else {
                    outputLines.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro de entrada/saída ao processar o arquivo.");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("lib/output.asm"))) {
            for (String outputLine : outputLines) {
                bw.write(outputLine);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erro de entrada/saída ao gravar o arquivo de saída.");
            e.printStackTrace();
        }
    }

    private void insertExistMacro(String currentLine, List<String> outputLines, Map<String, Macro> macros, String macroName, ArrayList<String> argsList){
        Macro macro  = macros.get(macroName);
        List<String> lines = macro.lines;
        int i = 0;

        String paramNameDetect = "PARA.";
        Pattern p = Pattern.compile(paramNameDetect);

        for(String line : lines) {
            String[] splitLine = line.split("\\s+");
            for(String part : splitLine){
                Matcher m = p.matcher(part);
                if(m.find()){
                    line = line.replace(part, argsList.get(i));
                    i++;
                }
            }
            outputLines.add(line.trim());
        }
    }

    private void processMacroDefinition(BufferedReader br, String macroDefinition) throws IOException {
        String[] trimmedDefinition = macroDefinition.split("\\s+");
        // verificar se ja existe MACRO com esse name
        // verificar se esta correta a definicao
        
        // if (!trimmedDefinition[0].toUpperCase().equals("MACRO")) {
        //     throw new IllegalArgumentException("Erro: Definição de macro inválida. Linha: " + trimmedDefinition);
        // }
        if (trimmedDefinition[0].startsWith("MACRO") && trimmedDefinition[1].startsWith("MACRO")) {
            String macroName = trimmedDefinition[0]; // Obtendo o nome da macro
            Macro macro = new Macro();
            List<String> macroLines = new ArrayList<>();
            
            String line;
            while ((line = br.readLine().trim()) != null) {
                if (line.trim().equals("MEND")) {
                    break;
                }
                macroLines.add(line);
            }
            
            macro.setLines(macroLines);
            macros.put(macroName, macro);
        }
    }
    
    
    private List<String> parseArguments(String line) {
        List<String> arguments = new ArrayList<>();
        String[] parts = line.trim().substring(5).trim().split(",");
        for (String part : parts) {
            arguments.add(part.trim());
        }
        return arguments;
    }

    public static void main(String[] args) {
        MacroProcessor processor = new MacroProcessor();
        processor.process("lib/input.asm");
    }
}

class Macro {
    public List<String> lines;

    public List<String> expand(List<String> arguments) {
        List<String> expandedMacro = new ArrayList<>();
        for (String line : lines) {
            String expandedLine = line;
            for (int i = 0; i < arguments.size(); i++) {
                expandedLine = expandedLine.replace("&&" + (i + 1), arguments.get(i));
            }
            expandedMacro.add(expandedLine);
        }
        return expandedMacro;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}
