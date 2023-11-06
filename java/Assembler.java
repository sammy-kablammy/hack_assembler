import java.io.*;
import java.util.*;

enum COMMAND {
    A_COMMAND,
    C_COMMAND,
    L_COMMAND
}

/*
 * "why do the variables need to be handled on the second pass instead of the
 * first, along with all the labels?"
 *
 * see this code:
 *
 * // [stuff happens here]
 * 
 * @END_OF_PROGRAM
 * 
 * // [stuff happens here]
 * 
 * (END_OF_PROGRAM)
 * 
 * ...you need to know where the END_OF_PROGRAM label is located.
 * don't try to find a new memory location for "END_OF_PROGRAM" at the start.
 * its definition is coming later.
 */

// note for the inevitable rewrite of this:
// i think the parser class should have 'advanceToNextValidInstruction'

// As per chapter 6, the Parser class should encapsulate all access to the input
// file.
class Parser {
    private String inputFilename;
    private Scanner fileScanner;
    private String currentLine;

    public Parser(String inputFilename) {
        this.inputFilename = inputFilename;
        File f = new File(this.inputFilename);
        try {
            fileScanner = new Scanner(f);
        } catch (FileNotFoundException e) {
            System.out.println("file not found or something 💀");
            System.out.println(e);
            return;
        }
        this.advance();
    }

    public void close() {
        this.fileScanner.close();
    }

    public void reset() {
        // TODO figure out try-catch situation
        // ^^ that applies to this whole file, not just here 🫣
        try {
            this.fileScanner.close();
            this.fileScanner = new Scanner(new File(this.inputFilename));
            this.currentLine = null;
            this.advance();
        } catch (IOException e) {
            System.out.println("file not found or something 💀");
            System.out.println(e);
            System.exit(69);
        }
    }

    public boolean hasMoreCommands() {
        return this.fileScanner.hasNextLine() && !this.currentLine.isEmpty();
    }

    // Remove comments, remove whitespace, etc.
    private String doRegexSorcery(String s) {
        // WOOOOOOOH!!!! I LOVE REGULAR EXPRESSIONS!!!!!!!!
        s = s.replaceAll("(//.*$)| ", "");
        return s;
        // regexless way
        // int idx = s.indexOf("//");
        // if (idx == -1) {
        // return s;
        // }
        // return s.substring(0, idx);
    }

    public void advance() {
        if (!this.fileScanner.hasNextLine()) {
            return;
        }

        String formatted = doRegexSorcery(this.fileScanner.nextLine());
        while (formatted.isBlank() && this.fileScanner.hasNextLine()) {
            formatted = doRegexSorcery(this.fileScanner.nextLine());
        }

        this.currentLine = formatted;
    }

    public COMMAND commandType() {
        if (this.currentLine.charAt(0) == '@') {
            return COMMAND.A_COMMAND;
        }
        if (this.currentLine.charAt(0) == '(') {
            return COMMAND.L_COMMAND;
        }
        return COMMAND.C_COMMAND;
    }

    public String symbol() {
        if (this.commandType() == COMMAND.A_COMMAND) {
            return this.currentLine.substring(1);
        }
        if (this.commandType() == COMMAND.L_COMMAND) {
            return this.currentLine.substring(1, this.currentLine.length() - 1);
        }
        throw new UnsupportedOperationException("cannot find symbol of a non A or L instruction");
    }

    public String dest() {
        if (this.commandType() != COMMAND.C_COMMAND) {
            throw new UnsupportedOperationException("cannot get dest of non C inst :(");
        }
        int idx = this.currentLine.indexOf("=");
        if (idx == -1) {
            // this instruction has no destination.
            return "";
        }
        return this.currentLine.substring(0, idx);
    }

    public String comp() {
        if (this.commandType() != COMMAND.C_COMMAND) {
            throw new UnsupportedOperationException("cannot get comp of non C inst :(");
        }

        int idxOfEquals = this.currentLine.indexOf("=");
        int idxOfSemi = this.currentLine.indexOf(";");

        if (idxOfSemi == -1) {
            // this instruction has no jump.
            return this.currentLine.substring(idxOfEquals + 1);
        }

        if (idxOfEquals == -1) {
            // this instruction has no destination.
            return this.currentLine.substring(0, idxOfSemi);
        }

        // this instruction has both a jump and a destination.
        return this.currentLine.substring(idxOfEquals + 1, idxOfSemi);
    }

    public String jump() {
        if (this.commandType() != COMMAND.C_COMMAND) {
            throw new UnsupportedOperationException("cannot get jump of non C inst :(");
        }

        int idx = this.currentLine.indexOf(";");
        if (idx == -1) {
            // this instruction has no jump.
            return "";
        }
        return this.currentLine.substring(idx + 1);
    }
}

class Code {
    // Returns 3 bits.
    public static int dest(String mnemonic) {
        int result = 0x0000;

        if (mnemonic.contains("A")) {
            result |= 0x0020; // ____ ____ __10 0___
        }
        if (mnemonic.contains("D")) {
            result |= 0x0010; // ____ ____ __01 0___
        }
        if (mnemonic.contains("M")) {
            result |= 0x0008; // ____ ____ __00 1___
        }

        return result;
    }

    // Returns 7 bits.
    public static int comp(String mnemonic) {
        Map<String, Integer> map = new HashMap<>();
        // sorry the spacing is strange but the formatter doesn't like it otherwise
        /* ___0 1010 10__ ____ */ map.put("0", 0x0A80);
        /* ___0 1111 11__ ____ */ map.put("1", 0x0FC0);
        /* ___0 1110 10__ ____ */ map.put("-1", 0x0E80);
        /* ___0 0011 00__ ____ */ map.put("D", 0x0300);
        /* ___0 1100 00__ ____ */ map.put("A", 0x0C00);
        /* ___0 0011 01__ ____ */ map.put("!D", 0x0340);
        /* ___0 1100 01__ ____ */ map.put("!A", 0x0C40);
        /* ___0 0011 11__ ____ */ map.put("-D", 0x03C0);
        /* ___0 1100 11__ ____ */ map.put("-A", 0x0CC0);
        /* ___0 0111 11__ ____ */ map.put("D+1", 0x07C0);
        /* ___0 1101 11__ ____ */ map.put("A+1", 0x0DC0);
        /* ___0 0011 10__ ____ */ map.put("D-1", 0x0380);
        /* ___0 1100 10__ ____ */ map.put("A-1", 0x0C80);
        /* ___0 0000 10__ ____ */ map.put("D+A", 0x0080);
        /* ___0 0100 11__ ____ */ map.put("D-A", 0x04C0);
        /* ___0 0001 11__ ____ */ map.put("A-D", 0x01C0);
        /* ___0 0000 00__ ____ */ map.put("D&A", 0x0000);
        /* ___0 0101 01__ ____ */ map.put("D|A", 0x0540);

        /* ___1 1100 00__ ____ */ map.put("M", 0x1C00);
        /* ___1 1100 01__ ____ */ map.put("!M", 0x1C40);
        /* ___1 1100 11__ ____ */ map.put("-M", 0x1CC0);
        /* ___1 1101 11__ ____ */ map.put("M+1", 0x1DC0);
        /* ___1 1100 10__ ____ */ map.put("M-1", 0x1C80);
        /* ___1 0000 10__ ____ */ map.put("D+M", 0x1080);
        /* ___1 0100 11__ ____ */ map.put("D-M", 0x14C0);
        /* ___1 0001 11__ ____ */ map.put("M-D", 0x11C0);
        /* ___1 0000 00__ ____ */ map.put("D&M", 0x1000);
        /* ___1 0101 01__ ____ */ map.put("D|M", 0x1540);

        return map.get(mnemonic);
    }

    // Returns 3 bits.
    public static int jump(String mnemonic) {
        Map<String, Integer> map = new HashMap<>();
        map.put("", 0x0000);
        map.put("JGT", 0x0001);
        map.put("JEQ", 0x0002);
        map.put("JGE", 0x0003);
        map.put("JLT", 0x0004);
        map.put("JNE", 0x0005);
        map.put("JLE", 0x0006);
        map.put("JMP", 0x0007);

        return map.get(mnemonic);
    }
}

class SymbolTable {
    private Map<String, Integer> map;

    public SymbolTable() {
        this.map = new TreeMap<>();
        // Add all the predefined Hack language symbols
        // TODO are the symbols case-sensitive?
        this.addEntry("SP", 0);
        this.addEntry("LCL", 1);
        this.addEntry("ARG", 2);
        this.addEntry("THIS", 3);
        this.addEntry("THAT", 4);
        for (int i = 0; i <= 15; i++) {
            this.addEntry("R" + i, i);
        }
        this.addEntry("SCREEN", 16384);
        this.addEntry("KBD", 24576);
    }

    public void addEntry(String symbol, int address) {
        this.map.put(symbol, address);
    }

    public boolean contains(String symbol) {
        return this.map.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return this.map.get(symbol);
    }
}

public class Assembler {
    private static String instToStringNoSpacing(int instruction) {
        String binaryInst = Integer.toBinaryString(instruction);
        return String.format("%16s", binaryInst).replace(' ', '0');
    }

    private static String instToString4Nibbles(int instruction) {
        String binaryInst = Integer.toBinaryString(instruction);
        // Left pad with zeroes
        String padded = String.format("%16s", binaryInst).replace(' ', '0');
        String res = "";
        res += padded.substring(0, 4) + ' ';
        res += padded.substring(4, 8) + ' ';
        res += padded.substring(8, 12) + ' ';
        res += padded.substring(12, 16);
        return res;
    }

    private static String instToStringCType(int instruction) {
        String binaryInst = Integer.toBinaryString(instruction);
        // Left pad with zeroes
        String padded = String.format("%16s", binaryInst).replace(' ', '0');

        String res = "";
        // fun spacing fun time yayyyy 😊😊😊
        res += padded.substring(0, 3) + ' ';
        res += padded.substring(3, 4) + ' ';
        res += padded.substring(4, 10) + ' ';
        res += padded.substring(10, 13) + ' ';
        res += padded.substring(13, 16);
        return res;
    }

    private static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: 'java Assembler input_file.asm'");
            System.exit(1);
        }
        String inputFilename = args[0];
        if (!inputFilename.contains(".asm")) {
            System.out.print("Invalid filename '" + inputFilename + "'. ");
            System.out.println("Expected '<input_file>.asm'.");
            System.exit(2);
        }
        Parser p = new Parser(inputFilename);
        String outputFileName = inputFilename.substring(0, inputFilename.length() - 4) + ".hack";
        FileWriter writer = new FileWriter(new File(outputFileName));
        int outputFileLineNumber = 0;

        try {
            // First pass: find all symbols and map them to RAM or ROM addresses
            // NOPE! only map labels to their line numbers.
            // variables are all handled during the second pass.
            // Recall that all custom/user-defined symbols should begin at 16.
            // (15 is the maximum predefined symbol)
            int nextAvailableMemoryAddress = 16;
            SymbolTable symbolTable = new SymbolTable();
            boolean hasMoreCommands = true;
            while (hasMoreCommands) {
                // if this current line contains a symbol, then add it
                COMMAND type = p.commandType();
                if (type == COMMAND.L_COMMAND) {
                    // there is a (Label) symbol on this line
                    String symbol = p.symbol();
                    if (!symbolTable.contains(symbol)) {
                        symbolTable.addEntry(symbol, outputFileLineNumber);
                    }
                    // [don't increment the output file line number for L-type
                    // instructions. this line will disappear once assembled]
                } else if (type == COMMAND.A_COMMAND) {
                    outputFileLineNumber++;
                } else { // command is C-type
                    // no symbol business here.
                    // there shouldn't be any new symbols on C-type lines.
                    outputFileLineNumber++;
                }
                hasMoreCommands = p.hasMoreCommands();
                p.advance();
            }

            // since this is a two-pass assembler, we gotta restart from the
            // beginning :(
            p.reset();

            // Second pass: use the symbol table
            hasMoreCommands = true;
            while (hasMoreCommands) {
                int assembled = 0;
                COMMAND type = p.commandType();
                if (type == COMMAND.C_COMMAND) {
                    assembled |= 0xE000; // 111_ ____ ____ ____
                    assembled |= Code.dest(p.dest());
                    assembled |= Code.comp(p.comp());
                    assembled |= Code.jump(p.jump());
                    // writer.write(instToStringCType(assembled));
                    writer.write(instToStringNoSpacing(assembled));
                    writer.write("\n");
                } else if (type == COMMAND.A_COMMAND) {
                    String symbol = p.symbol();
                    if (isNumeric(symbol)) {
                        // example: @24
                        assembled |= Integer.parseInt(symbol);
                    } else {
                        // example: @LOOP_START
                        if (symbolTable.contains(symbol)) {
                            assembled |= symbolTable.getAddress(symbol);
                        } else {
                            // this symbol must represent a new variable.
                            assembled |= nextAvailableMemoryAddress;
                            symbolTable.addEntry(symbol, nextAvailableMemoryAddress++);
                        }
                    }

                    // writer.write(instToString4Nibbles(assembled));
                    writer.write(instToStringNoSpacing(assembled));
                    writer.write("\n");
                } else { // type must be L command
                    // we've already parsed this symbol on the previous pass.
                    // nothing to do here.
                }
                hasMoreCommands = p.hasMoreCommands();
                p.advance();
            }
        } catch (

        IOException e) {
            System.out.println("uh oh there bucko. some kinda file problem here:" + e);
        }
        p.close();
        writer.close();
    }
}
