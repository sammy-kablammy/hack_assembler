import java.io.*;
import java.util.*;

enum COMMAND {
    A_COMMAND,
    C_COMMAND,
    L_COMMAND
}

class Parser {

    private Scanner fileScanner;
    private String currentLine;
    private int lineNumber = 0;

    Parser(File f) {
        try {
            fileScanner = new Scanner(f);
        } catch (FileNotFoundException e) {
            System.out.println("file not found 💀");
            return;
        }
        this.advance();
    }

    boolean hasMoreCommands() {
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

    void advance() {
        if (!this.fileScanner.hasNextLine()) {
            return;
        }

        String formatted = doRegexSorcery(this.fileScanner.nextLine());
        while (formatted.isBlank() && this.fileScanner.hasNextLine()) {
            formatted = doRegexSorcery(this.fileScanner.nextLine());
        }

        this.currentLine = formatted;
        this.lineNumber++;
    }

    COMMAND commandType() {
        if (this.currentLine.charAt(0) == '@') {
            return COMMAND.A_COMMAND;
        }
        if (this.currentLine.charAt(0) == '(') {
            return COMMAND.L_COMMAND;
        }
        // TODO maybe do a check to see if it is a valid C-type command??
        // and like print an error otherwise???
        return COMMAND.C_COMMAND;
    }

    String symbol() {
        if (this.commandType() == COMMAND.A_COMMAND) {
            return this.currentLine.substring(1);
        }
        throw new UnsupportedOperationException("uh oh bucko. can't figure out"
                + "the symbol of a non-A-type instruction.");
        // TODO handle symbolic labels here
    }

    String dest() {
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

    String comp() {
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

    String jump() {
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

    void close() {
        this.fileScanner.close();
    }
}

class Code {
    // Returns 3 bits.
    static int dest(String mnemonic) {
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
    static int comp(String mnemonic) {
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
    static int jump(String mnemonic) {
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
    SymbolTable() {
        throw new UnsupportedOperationException("nope.");
    }

    void addEntry(String symbol, int address) {
        throw new UnsupportedOperationException("nope.");
    }

    boolean contains(String symbol) {
        throw new UnsupportedOperationException("nope.");
    }

    int getAddress(String symbol) {
        throw new UnsupportedOperationException("nope.");
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

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: 'java Assembler input_file.asm'");
            System.exit(1);
        }

        Parser p = new Parser(new File(args[0]));
        String outputFileName = args[0].substring(0, args[0].length() - 4) + ".hack";
        FileWriter writer = new FileWriter(new File(outputFileName));

        try {
            // TODO maybe don't do while(true) but for now it'll be fine... right???
            while (true) {
                int assembled = 0;
                COMMAND type = p.commandType();
                if (type == COMMAND.C_COMMAND) {
                    assembled |= 0xE000; // 111_ ____ ____ ____
                    assembled |= Code.dest(p.dest());
                    assembled |= Code.comp(p.comp());
                    assembled |= Code.jump(p.jump());
                    // writer.write(instToStringCType(assembled));
                } else if (type == COMMAND.A_COMMAND) {
                    assembled |= Integer.parseInt(p.symbol());
                    // writer.write(instToString4Nibbles(assembled));
                }
                writer.write(instToStringNoSpacing(assembled));
                writer.write("\n");
                if (p.hasMoreCommands()) {
                    p.advance();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("uh oh there bucko. some kinda file problem here:" + e);
        }

        p.close();
        writer.close();
    }
}
