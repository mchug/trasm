package trasm;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Типы лексем
 */
enum LexemeType {

    INSTRUCTION,
    /*1*/ DIRECTIVE,
    /*2*/ REGISTER_GENERAL,
    /*3*/ REGISTER_SEGMENT,
    /*4*/ DATA_TYPE,
    /*5*/ CONST_BIN,
    /*6*/ CONST_DEC,
    /*7*/ CONST_HEX,
    /*8*/ CONST_STRING,
    /*9*/ ONE_SYMBOL,
    /*10*/ USER_IDENTIFIER,
    /*11*/ ERROR_LEXEME;

    /**
     * Возвращает подробное описание типа лексемы
     *
     * @return Строка для печати
     */
    @Override
    public String toString() {
        final String[] typesDescription = {
            /*0*/"Ідентифікатор мнемокоду машинної інструкції",
            /*1*/ "Ідентифікатор директиви",
            /*2*/ "Ідентифікатор регістра загального призначення",
            /*3*/ "Ідентифікатор сегментного регістра",
            /*4*/ "Ідентифікатор дерективи даних",
            /*5*/ "Двійкова константа",
            /*6*/ "Десяткова константа",
            /*7*/ "Шістнадцяткова константа",
            /*8*/ "Текстова константа",
            /*9*/ "Односимвольна",
            /*10*/ "Ідентифікатор користувача або не визначений",
            /*11*/ "Недопустима лексема"
        };

        return typesDescription[this.ordinal()];
    }
}

/**
 * Контейнер для хранения информации про одну лексему
 */
class LexemeInfo {

    /**
     * Значение лексемы
     */
    final String value;
    /**
     * Тип лексемы
     */
    final LexemeType type;

    public LexemeInfo(String value, LexemeType type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Преобразование лексемы в удобный для печати вид
     *
     * @return Строка для печати
     */
    @Override
    public String toString() {
        return String.format("%1$-8s %2$-8d %3$s", value, value.length(), type.toString());
    }
}

/**
 * Лексический анализатор
 */
class LexicalAnalyzer {

    /**
     * Возвращает информацию про все лексемы в строке
     *
     * @param line Строчка для анализа
     * @return Массив лексем
     */
    static LexemeInfo[] getLexemeInfo(String line) {
        ArrayList<LexemeInfo> infoList = new ArrayList();

        final Pattern[] patterns = {
            /*0*/Pattern.compile("^(cli|inc|dec|add|cmp|xor|mov|or|jb|jmp)$", Pattern.CASE_INSENSITIVE),
            /*1*/ Pattern.compile("^(segment|ends|end|assume)$", Pattern.CASE_INSENSITIVE),
            /*2*/ Pattern.compile("^(al|cl|dl|bl|ah|ch|dh|bh|ax|cx|dx|bx|sp|bp|si|di|eax|ecx|edx|ebx|esp|ebp|esi|edi)$", Pattern.CASE_INSENSITIVE),
            /*3*/ Pattern.compile("^(es|cs|ss|ds|fs|gs)$", Pattern.CASE_INSENSITIVE),
            /*4*/ Pattern.compile("^(db|dw|dd)$", Pattern.CASE_INSENSITIVE),
            /*5*/ Pattern.compile("^([01]+b)$", Pattern.CASE_INSENSITIVE),
            /*6*/ Pattern.compile("^(\\d+d?)$", Pattern.CASE_INSENSITIVE),
            /*7*/ Pattern.compile("^(\\d+[A-F0-9]*h)$", Pattern.CASE_INSENSITIVE),
            /*8*/ Pattern.compile("^('[^']*')$", Pattern.CASE_INSENSITIVE),
            /*9*/ Pattern.compile("^[.,:\\[\\]]$"),
            /*10*/ Pattern.compile("^([a-z][a-z0-9]{0,7})$", Pattern.CASE_INSENSITIVE),
            /*11*/ Pattern.compile(".*")
        };

        line = line.trim();
        line = line.replaceAll(";.*", "");
        line = line.replaceAll("([,:\\[\\]])", " $1 ");

        String[] lexemes = line.split("[\\s\\t]+");

        for (String lexeme : lexemes) {
            for (LexemeType lexemeType : LexemeType.values()) {
                if (patterns[lexemeType.ordinal()].matcher(lexeme).matches()) {
                    infoList.add(new LexemeInfo(lexeme, lexemeType));
                    break;
                }
            }
        }

        return infoList.toArray(new LexemeInfo[infoList.size()]);
    }

    /**
     * Преобразовывает массив лексем в удобный для печати вид
     *
     * @param info Массив лексем
     * @return Строка для печати
     */
    static String getStringToPrint(LexemeInfo[] info) {
        StringBuilder outStr;
        outStr = new StringBuilder(String.format("%1$-8s %2$-8s %3$-8s %4$s\n", "№ п/п", "Лексема", "Довжина", "Тип лексеми"));

        for (int i = 0; i < info.length; i++) {
            LexemeInfo lexemeInfo = info[i];
            outStr = outStr.append(String.format("%1$-8d %2$s\n", i + 1, lexemeInfo.toString()));
        }
        return outStr.toString();
    }

    /**
     * Преобразовывает строку в число
     *
     * @param item Строка для преобразования (Bin, Dec, Hex)
     * @return Значение строки
     */
    static long getConstValue(String item) {
        if (item.toUpperCase().contains("H")) {
            return Long.parseLong(item.substring(0, item.length() - 1), 16);
        }
        if (item.toUpperCase().contains("B")) {
            return Long.parseLong(item.substring(0, item.length() - 1), 2);
        }
        if (item.toUpperCase().contains("D")) {
            return Long.parseLong(item.substring(0, item.length() - 1));
        }

        return Long.parseLong(item);
    }

    /**
     * Возвращает размер константы: 1,2 или 4 байта. В случае переполнения
     * возвращает -1;
     *
     * @param value Входное значение
     * @return Размер в байтах
     */
    static int getConstSize(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return -1;
        }
        return value < 256 ? 1 : (value < 256 * 256 ? 2 : 4);
    }

    /**
     * Возвращает размер константы: 1,2 или 4 байта. В случае переполнения
     * возвращает -1;
     *
     * @param value Строка для преобразования в число
     * @return Размер в байтах
     */
    static int getConstSize(String value) {
        return getConstSize((int) getConstValue(value));
    }
}
