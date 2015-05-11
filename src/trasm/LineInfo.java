package trasm;

import java.nio.charset.StandardCharsets;
import trasm.IdTable.IdInfo;
import trasm.IdTable.IdType;
import trasm.SegTable.SegInfo;
import trasm.SegTable.SegRegister;

/**
 * Контейнер для хранения информации про исходную строчку
 */
class LineInfo {

    /**
     * Типы строчек
     */
    static enum LineType {

        BEGIN_SEGMENT,
        END_SEGMENT,
        DATA_DECLARATION,
        LABEL,
        ASSUME,
        INSTRUCTIONS,
        JUMP,
        END,
        ERROR_LINE,
        EMPTY
    }

    /**
     * Регистры общего назначения. (необходимы для генерации кода операции)
     */
    private enum Register {

        AL(0, 1), CL(1, 1), DL(2, 1), BL(3, 1), AH(4, 1), CH(5, 1), DH(6, 1), BH(7, 1),
        AX(0, 2), CX(1, 2), DX(2, 2), BX(3, 2), SP(4, 2), BP(5, 2), SI(6, 2), DI(7, 2),
        EAX(0, 4), ECX(1, 4), EDX(2, 4), EBX(3, 4), ESP(4, 4), EBP(5, 4), ESI(6, 4), EDI(7, 4);

        private final int num, size;

        public int getSize() {
            return size;
        }

        public int getNum() {
            return num;
        }

        public static String getModRM(int base, Register index, boolean isAddress) {
            int addr = (isAddress ? 0x80 : 0xC0) + 0x08 * base;

            if (index.name().contains("E")) {
                addr += index.getNum();
            }

            switch (index) {
                case SI:
                    addr += 4;
                    break;
                case DI:
                    addr += 5;
                    break;
                case BP:
                    addr += 6;
                    break;
                case BX:
                    addr += 7;
                    break;
                case ESP:
                    return IOLib.toHex(addr, 2) + " 24 ";
            }

            return IOLib.toHex(addr, 2) + " ";
        }

        public static String getModRM(Register base, Register index, boolean isAddress) {
            return Register.getModRM(base.getNum(), index, isAddress);
        }

        private Register(int num, int size) {
            this.num = num;
            this.size = size;
        }

    }

    /**
     * Исходное значение строки
     */
    final String value;
    /**
     * Размер строки в байтах (кода операции)
     */
    final int sizeInBytes;
    /**
     * Тип строки
     */
    final LineType type;
    /**
     * Код операции. Машинная трансляция строки
     */
    final String opCode;
    /**
     * Содержит ли строчка ошибку?
     */
    private boolean isCorrect;
    /**
     * Шаблон строчки (для внутринних нужд)
     */
    private String template;
    /**
     * Смещение строчки (для внутринних нужд)
     */
    private int address;

    //<editor-fold defaultstate="collapsed" desc="validTemplates">
    /**
     * Допустимые шаблоны строк
     */
    private static final String[] validTemplates = {
        /*0*/"ID SEGMENT",
        /*1*/ "ID ENDS",
        /*2*/ "ASSUME rS : ID",
        /*3*/ "END ID",
        /*4*/ "END",
        /*5*/ "ID :",
        /*6*/ "ID DB CONST",
        /*7*/ "ID DW CONST",
        /*8*/ "ID DD CONST",
        /*9*/ "ID DB C_STR",
        /*10*/ "CLI",
        /*11*/ "INC ID [ ADDR ]",
        /*12*/ "INC rS : ID [ ADDR ]",            
        /*13*/ "DEC reg",
        /*14*/ "ADD ID [ ADDR ] , CONST",
        /*15*/ "ADD rS : ID [ ADDR ] , CONST",
        /*16*/ "CMP reg , ID [ ADDR ]",
        /*17*/ "CMP reg , rS : ID [ ADDR ]",
        /*18*/ "XOR ID [ ADDR ] , reg",
        /*19*/ "XOR rS : ID [ ADDR ] , reg",
        /*20*/ "MOV reg , CONST",
        /*21*/ "OR reg , reg",
        /*22*/ "JB ID",
        /*23*/ "JMP ID",
        /*24*/ ""
    };
//</editor-fold>

    boolean isCorrect() {
        return isCorrect;
    }

    /**
     * Преобразование строчки в удобный для печати вид
     *
     * @return Строка для печати
     */
    @Override
    public String toString() {
        return String.format("%1$-20s %2$s", opCode, value);
    }

    /**
     * Конструктор для второго прохода. Специально для команда JMP и JB.
     *
     * @param info Результат превого прохода
     */
    public LineInfo(LineInfo info) {
        value = info.value;
        sizeInBytes = info.sizeInBytes;
        type = info.type;
        address = info.address;
        isCorrect = info.isCorrect;
        template = info.template;
        opCode = getOpCode(value);
    }

    /**
     * Конструктор для первого прохода.
     *
     * @param line Исходная строчка
     */
    public LineInfo(String line) {
        template = getLineTemplate(line);
        template = template.replaceAll("(\\[ r16a \\])|(\\[ r32 \\])", "[ ADDR ]").replaceAll("r8|r16a|r16|r32", "reg");
        if (template.contains("DB C_STR") == false) {
            template = template.replace("C_STR", "CONST");
        }
        if (template.contains("ASSUME")) {
            template = template.replaceAll(" , rS : ID", "");
        }

        this.value = line;
        this.address = SegTable.getInstance().getCurrentAddress();
        this.type = getTemplateType(template);
        this.isCorrect = type != LineType.ERROR_LINE;
        this.opCode = getOpCode(line);
        this.sizeInBytes = opCode.replaceAll("[|\\s]", "").length() / 2;
    }

    /**
     * Преобразование лексемы к шаблону
     *
     * @param lexeme Лексема
     * @return Шаблон
     */
    private String getLexemeTemplate(LexemeInfo lexeme) {
        String outStr = "So empty...";
        switch (lexeme.type) {
            case INSTRUCTION:
                outStr = lexeme.value.toUpperCase();
                break;
            case DIRECTIVE:
                outStr = lexeme.value.toUpperCase();
                break;
            case REGISTER_GENERAL:
                String reg = lexeme.value.toUpperCase();
                if (reg.contains("L") || reg.contains("H")) {
                    outStr = "r8";
                } else if (reg.contains("E")) {
                    outStr = "r32";
                } else if (reg.contains("B") || reg.contains("I")) {
                    outStr = "r16a";
                } else {
                    outStr = "r16";
                }
                break;
            case REGISTER_SEGMENT:
                outStr = "rS";
                break;
            case DATA_TYPE:
                outStr = lexeme.value.toUpperCase();
                break;
            case CONST_BIN:
            case CONST_DEC:
            case CONST_HEX:
                outStr = "CONST";
                break;
            case CONST_STRING:
                outStr = "C_STR";
                break;
            case ONE_SYMBOL:
                outStr = lexeme.value;
                break;
            case USER_IDENTIFIER:
                outStr = "ID";
                break;
            case ERROR_LEXEME:
                if (lexeme.value.isEmpty()) {
                    return "";
                }
                outStr = "<Error lexeme, sad... So sad... =( " + lexeme.value + ">";
                break;
            default:
                throw new AssertionError(lexeme.type.name());
        }

        return outStr;
    }

    /**
     * Преобразование строчки к шаблону
     *
     * @param line Исходная строчка
     * @return Шаблон
     */
    private String getLineTemplate(String line) {
        LexemeInfo[] lexemeInfo = LexicalAnalyzer.getLexemeInfo(line);
        StringBuilder template = new StringBuilder(getLexemeTemplate(lexemeInfo[0]));

        for (int i = 1; i < lexemeInfo.length; i++) {
            template = template.append(" ").append(getLexemeTemplate(lexemeInfo[i]));
        }

        return template.toString();
    }

    /**
     * Возвращает тип шаблона
     *
     * @param template Шаблон
     * @return Тип шаблона
     */
    private LineType getTemplateType(String template) {

        if (!template.equals("ID :") && template.startsWith("ID :")) {
            template = template.substring(template.indexOf(":") + 1).trim();
        }

        int index = -1;
        for (int i = 0; i < validTemplates.length; i++) {
            if (validTemplates[i].equals(template)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return LineType.ERROR_LINE;
        }

        switch (index) {
            case 0:
                return LineType.BEGIN_SEGMENT;
            case 1:
                return LineType.END_SEGMENT;
            case 2:
                return LineType.ASSUME;
            case 3:
            case 4:
                return LineType.END;
            case 5:
                return LineType.LABEL;
            case 22:
            case 23:
                return LineType.JUMP;
            case 24:
                return LineType.EMPTY;
            default:
                if (index < 10) {
                    return LineType.DATA_DECLARATION;
                }
                return LineType.INSTRUCTIONS;
        }
    }

    /**
     * Генерация кода операции для строчки
     *
     * @param input Исходная строчка
     * @return Код операции
     */
    private String getOpCode(String input) {

        SegTable segTable = SegTable.getInstance();
        IdTable idTable = IdTable.getInstance();

        while (!template.equals("ID :") && template.startsWith("ID :")) {
            String label = input.substring(0, input.indexOf(":")).trim();
            input = input.substring(input.indexOf(":") + 1).trim();
            idTable.add(new IdInfo(label, IdType.LABEL));
            template = getLineTemplate(input);
        }

        LexemeInfo[] lexemes = LexicalAnalyzer.getLexemeInfo(input);

        switch (type) {
            case BEGIN_SEGMENT:
                if (!segTable.getCurrentSegment().equals(SegTable.NULL_SEG_NAME)) {
                    isCorrect = false;
                } else {
                    segTable.setCurrentSegment(lexemes[0].value);
                    if (!segTable.isExist(lexemes[0].value)) {
                        segTable.add(new SegInfo(lexemes[0].value));
                    }
                    segTable.setCurrentAddress(((SegInfo) segTable.get(lexemes[0].value)).getSize());
                }
                return "";
            case END_SEGMENT:
                if (!segTable.getCurrentSegment().equalsIgnoreCase(lexemes[0].value)) {
                    isCorrect = false;
                } else {
                    segTable.setSize(segTable.getCurrentSegment(), address);
                }
                segTable.setCurrentSegment(SegTable.NULL_SEG_NAME);
                return "";
            case DATA_DECLARATION:
                if (lexemes[2].type == LexemeType.CONST_STRING) {
                    if (!lexemes[1].value.equalsIgnoreCase("DB")) {
                        isCorrect = false;
                        return "";
                    }
                    idTable.add(new IdInfo(lexemes[0].value, IdType.DB));
                    
                    String constStr = lexemes[2].value.substring(1, lexemes[2].value.length() - 1);
                    String constHex = "";
                    for (byte b : constStr.getBytes(StandardCharsets.UTF_8)) {
                        constHex += Integer.toHexString(b & 0xFF) + " ";
                    }


                    return constHex.trim().toUpperCase();
                }
                int immSize = LexicalAnalyzer.getConstSize(lexemes[2].value);

                IdType idType = IdType.valueOf(lexemes[1].value.toUpperCase());

                idTable.add(new IdInfo(lexemes[0].value, idType));

                if (idType.getSize() < immSize || immSize == -1) {
                    isCorrect = false;
                    return "";
                }

                return IOLib.toHex(lexemes[2].value, immSize * 2);
            case LABEL:
                idTable.add(new IdInfo(lexemes[0].value, IdType.LABEL));
                return "";
            case ASSUME:
                segTable.assume(input);
                return "";
            case INSTRUCTIONS:
            case JUMP:
                return getInstructionCode(lexemes);
            case ERROR_LINE:
                isCorrect = false;
            default:
                return "";
        }
    }

    /**
     * Генерация кода операции для операций.
     *
     * @param lexemes Массив лексем
     * @return Код операции
     */
    private String getInstructionCode(LexemeInfo[] lexemes) {

        SegTable segTable = SegTable.getInstance();
        IdTable idTable = IdTable.getInstance();

        final String addrPrefix = "66| ";
        final String regPrefix = "67| ";
        boolean isAddrPref, isRegPref, isSegPref;
        String segPrefix = "";
        int immSize = 0;
        IdInfo idInfo = null;
        Register reg = null;
        SegRegister idSeg = null;
        switch (lexemes[0].value.toUpperCase()) {
            case "INC": {
                //FE /0 — INC r/m8
                //FF /0 — INC r/m16
                //FF /0 — INC r/m32
                isSegPref = lexemes[1].type == LexemeType.REGISTER_SEGMENT;
                // INC(0) ID(1) [(2) REG(3) ](4)                    
                // INC(0) S_REG(1) :(2) ID(3) [(4) REG(5) ](5)

                if (isSegPref) {
                    segPrefix = getSegPrefix(lexemes[1].value);
                }

                idInfo = (IdInfo) (idTable.get(lexemes[isSegPref ? 3 : 1].value));

                if (idInfo == null) {
                    isCorrect = false;
                    return "";
                }

                idSeg = segTable.getSegmentReg(idInfo.getSegment());
                if (idSeg != SegTable.SegRegister.DS && !isSegPref) {
                    segPrefix = getSegPrefix(idSeg.name());
                }

                isAddrPref = idInfo.getType() == IdType.DD;
                reg = Register.valueOf(lexemes[isSegPref ? 5 : 3].value.toUpperCase());
                isRegPref = reg.getSize() == 4;

                return (isAddrPref ? addrPrefix : "") + segPrefix + (isRegPref ? regPrefix : "")
                        + "FE " + Register.getModRM(0, reg, true) + IOLib.toHex(idInfo.getAddress(), (isRegPref ? 8 : 4));

            }
            case "DEC": {
                //FE /1 — DEC r/m8
                //48+rw — DEC r16
                //48+rd — DEC r32
                // DEC(0) REG(1)                    

                reg = Register.valueOf(lexemes[1].value.toUpperCase());
                isRegPref = reg.getSize() == 4;

                return (isRegPref ? regPrefix : "") + (reg.getSize() == 1 ? "FE "
                        + IOLib.toHex(0xC8 + reg.getNum(), 2) : IOLib.toHex(0x48 + reg.getNum(), 2));
            }
            case "ADD": {
                //80 /0 ib — ADD r/m8,imm8
                //81 /0 iw — ADD r/m16,imm16
                //81 /0 id — ADD r/m32,imm32
                //83 /0 ib — ADD r/m16,imm8
                //83 /0 ib — ADD r/m32,imm8

                isSegPref = lexemes[1].type == LexemeType.REGISTER_SEGMENT;
                // ADD(0) ID(1) [(2) REG(3) ](4) ,(5) CONST(6)                    
                // ADD(0) S_REG(1) :(2) ID(3) [(4) REG(5) ](6) ,(7) CONST(8)

                if (isSegPref) {
                    segPrefix = getSegPrefix(lexemes[1].value);
                }

                idInfo = (IdInfo) (idTable.get(lexemes[isSegPref ? 3 : 1].value));
                if (idInfo == null || idInfo.getType().getSize() < immSize || immSize == -1) {
                    isCorrect = false;
                    return "";
                }

                idSeg = segTable.getSegmentReg(idInfo.getSegment());
                if (idSeg != SegTable.SegRegister.DS && !isSegPref) {
                    segPrefix = getSegPrefix(idSeg.name());
                }
                long imm = LexicalAnalyzer.getConstValue(lexemes[isSegPref ? 8 : 6].value);
                immSize = LexicalAnalyzer.getConstSize(imm);

                isAddrPref = idInfo.getType() == IdType.DD;
                reg = Register.valueOf(lexemes[isSegPref ? 5 : 3].value.toUpperCase());
                isRegPref = reg.getSize() == 4;

                return (isAddrPref ? addrPrefix : "") + segPrefix + (isRegPref ? regPrefix : "")
                        + (immSize == 1 && idInfo.getType().getSize() != 1 ? "83 " : idInfo.getType().getSize() == 1 ? "80 " : "81 ")
                        + Register.getModRM(0, reg, true) + IOLib.toHex(idInfo.getAddress(), (isRegPref ? 8 : 4))
                        + " " + IOLib.toHex(imm, immSize != 1 ? idInfo.getType().getSize() * 2 : 2);

            }
            case "CMP": {
                //3A /r — CMP r8,r/m8
                //3B /r — CMP r16,r/m16
                //3B /r — CMP r32,r/m32

                isSegPref = lexemes[3].type == LexemeType.REGISTER_SEGMENT;
                // CMP(0) FIRST_REG(1) ,(2) ID(3) [(4) REG(5) ](6)                    
                // CMP(0) FIRST_REG(1) ,(2) S_REG(3) :(4) ID(5) [(6) REG(7) ](8)                    

                if (isSegPref) {
                    segPrefix = getSegPrefix(lexemes[3].value);
                }

                idInfo = (IdInfo) (idTable.get(lexemes[isSegPref ? 5 : 3].value));
                Register firstReg = Register.valueOf(lexemes[1].value.toUpperCase());
                if (idInfo == null || idInfo.getType().getSize() != firstReg.getSize()) {
                    isCorrect = false;
                    return "";
                }

                idSeg = segTable.getSegmentReg(idInfo.getSegment());
                if (idSeg != SegTable.SegRegister.DS && !isSegPref) {
                    segPrefix = getSegPrefix(idSeg.name());
                }

                isAddrPref = idInfo.getType() == IdType.DD;
                reg = Register.valueOf(lexemes[isSegPref ? 7 : 5].value.toUpperCase());
                isRegPref = reg.getSize() == 4;

                return (isAddrPref ? addrPrefix : "") + segPrefix + (isRegPref ? regPrefix : "")
                        + (idInfo.getType() == IdType.DB ? "3A " : "3B ") + Register.getModRM(firstReg, reg, true)
                        + IOLib.toHex(idInfo.getAddress(), (isRegPref ? 8 : 4));
            }
            case "XOR": {
                //30 /r — XOR r/m8,r8
                //31 /r — XOR r/m16,r16
                //31 /r — XOR r/m32,r32

                isSegPref = lexemes[1].type == LexemeType.REGISTER_SEGMENT;
                // XOR(0) ID(1) [(2) REG(3) ](4) ,(5) SECOND_REG(6)
                // XOR(0) S_REG(1) :(2) ID(3) [(4) REG(5) ](6) ,(7) SECOND_REG(8)

                if (isSegPref) {
                    segPrefix = getSegPrefix(lexemes[1].value);
                }

                idInfo = (IdInfo) (idTable.get(lexemes[isSegPref ? 3 : 1].value));
                Register secondReg = Register.valueOf(lexemes[isSegPref ? 8 : 6].value.toUpperCase());
                if (idInfo == null || idInfo.getType().getSize() != secondReg.getSize()) {
                    isCorrect = false;
                    return "";
                }

                idSeg = segTable.getSegmentReg(idInfo.getSegment());
                if (idSeg != SegTable.SegRegister.DS && !isSegPref) {
                    segPrefix = getSegPrefix(idSeg.name());
                }

                isAddrPref = idInfo.getType() == IdType.DD;
                reg = Register.valueOf(lexemes[isSegPref ? 5 : 3].value.toUpperCase());
                isRegPref = reg.getSize() == 4;

                return (isAddrPref ? addrPrefix : "") + segPrefix + (isRegPref ? regPrefix : "")
                        + (idInfo.getType() == IdType.DB ? "30 " : "31 ") + Register.getModRM(secondReg, reg, true)
                        + IOLib.toHex(idInfo.getAddress(), (isRegPref ? 8 : 4));
            }
            case "MOV": {
                //B0+rb — MOV r8,imm8
                //B8+rw — MOV r16,imm16
                //B8+rd — MOV r32,imm32                
                // MOV(0) REG(1) ,(2) imm(3)

                immSize = LexicalAnalyzer.getConstSize(lexemes[3].value);
                reg = Register.valueOf(lexemes[1].value.toUpperCase());
                isAddrPref = reg.getSize() == 4;

                if (reg.getSize() < immSize) {
                    isCorrect = false;
                    return "";
                }

                return (isAddrPref ? addrPrefix : "")
                        + (reg.getSize() == 1 ? IOLib.toHex(0xB0 + reg.getNum(), 2) : IOLib.toHex(0xB8 + reg.getNum(), 2))
                        + " " + IOLib.toHex(lexemes[3].value, reg.getSize() * 2);
            }
            case "OR": {
                //0A /r — OR r8,r/m8
                //0B /r — OR r16,r/m16
                //0B /r — OR r32,r/m32              
                // OR(0) REG(1) ,(2) SECOND_REG(3)

                reg = Register.valueOf(lexemes[1].value.toUpperCase());
                Register secondReg = Register.valueOf(lexemes[3].value.toUpperCase());
                isAddrPref = reg.getSize() == 4;

                if (reg.getSize() != secondReg.getSize()) {
                    isCorrect = false;
                    return "";
                }

                return (isAddrPref ? addrPrefix : "") + (reg.getSize() == 1 ? IOLib.toHex(0x0A, 2) : IOLib.toHex(0x0B, 2))
                        + " " + Register.getModRM(reg, secondReg, false);
            }
            case "JB": {
                //72 cb — JB rel8
                //0F 82 cw/cd — JB rel16/32
                //JB(0) ID(1)

                idInfo = (IdInfo) (idTable.get(lexemes[1].value));

                if (idInfo == null) {
                    if (Translator.isSecondPass) {
                        isCorrect = false;
                        return "";
                    }
                    return "90 90 90 90";
                }
                int jumpWidth = idInfo.getAddress() - (address + 2);
                if (jumpWidth > -128 && jumpWidth < 0) {
                    return "72 " + IOLib.toHex(jumpWidth, 2);
                }
                return jumpWidth < 127 && jumpWidth > -128 ? "72 " + IOLib.toHex(jumpWidth, 2) + " 90 90" : "0F 82 " + IOLib.toHex(jumpWidth - 2, 4);
            }
            case "JMP": {
                //EB cb — JMP rel8
                //E9 cw — JMP rel16
                //JMP(0) ID(1)

                idInfo = (IdInfo) (idTable.get(lexemes[1].value));

                if (idInfo == null) {
                    if (Translator.isSecondPass) {
                        isCorrect = false;
                        return "";
                    }
                    return "90 90 90";
                }
                int jumpWidth = idInfo.getAddress() - (address + 2);
                if (jumpWidth > -128 && jumpWidth < 0) {
                    return "EB " + IOLib.toHex(jumpWidth, 2);
                }
                return jumpWidth < 127 && jumpWidth > -128 ? "EB " + IOLib.toHex(jumpWidth, 2) + " 90" : "E9 " + IOLib.toHex(jumpWidth - 1, 4);
            }
            default: //CLI
                return "FA";
        }
    }

    /**
     * Возвращает машинное представление префикса замены сегмента
     *
     * @param reg Сегментный регистр
     * @return Префикс замены сегмента
     */
    private String getSegPrefix(String reg) {
        switch (reg.toUpperCase()) {
            case "ES":
                return "26: ";
            case "CS":
                return "2E: ";
            case "SS":
                return "36: ";
            case "DS":
                return "3E: ";
            case "FS":
                return "64: ";
            case "GS":
                return "65: ";
            default:
                return "ERROR";
        }
    }
}
