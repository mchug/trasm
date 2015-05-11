package trasm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import trasm.LineInfo.LineType;

/**
 * Главный класс транслятора
 */
class Translator {

    /**
     * Глобальный флаг первого/второго прохода
     */
    static boolean isSecondPass = false;

    /**
     * Контейнер для хренения информации про строчку листинга
     */
    private static class LstLine {

        int address, lineNum;
        LineInfo info;

        public LstLine(int address, int lineNum, LineInfo info) {
            this.lineNum = lineNum;
            this.address = address;
            this.info = info;
        }

        @Override
        public String toString() {
            if (info.type == LineType.ASSUME) {
                return "        " + info.toString();
            }
            return String.format("%1$3d", lineNum) + " " + IOLib.toHex(address, 4) + "    " + info.toString();
        }
    }

    /**
     * Генерирует файл листинга
     *
     * @param asmFilePath Пусть к исходному файлу
     * @param lstFilePath Пусть для файла листинга
     * @param options Дополнительные опции генерации
     * @throws IOException
     */
    private static void makeLST(String asmFilePath, String lstFilePath, String options) throws IOException {

        boolean firstPassOut = options.contains("f");
        boolean lexicalOut = options.contains("l");
        boolean assumeOut = options.contains("a");
        boolean consoleOut = options.contains("c");

        if (!asmFilePath.toLowerCase().contains(".asm") && !asmFilePath.contains(".")) {
            asmFilePath += ".asm";
        }

        if (!lstFilePath.toLowerCase().contains(".lst")) {
            lstFilePath += ".lst";
        }

        ArrayList<LstLine> allLines = new ArrayList<>();
        String[] fileLines = IOLib.readAllLines(asmFilePath);
        ArrayList<LstLine> jumps = new ArrayList<>();
        for (String source_line : fileLines) {
            LineInfo line = new LineInfo(source_line);
            allLines.add(new LstLine(SegTable.getInstance().getCurrentAddress(), ErrorList.currentLine, line));
            if (!line.isCorrect()) {
                ErrorList.AddError();
            }
            if (line.type == LineType.JUMP && line.isCorrect()) {
                jumps.add(new LstLine(SegTable.getInstance().getCurrentAddress(), ErrorList.currentLine, line));
            }

            SegTable.getInstance().setCurrentAddress(SegTable.getInstance().getCurrentAddress() + line.sizeInBytes);
            ErrorList.currentLine++;
        }

        if (firstPassOut) {
            ArrayList<String> listing = new ArrayList<>();
            String firstPass = lstFilePath.toLowerCase().replace(".lst", ".flst");
            for (LstLine lstLine : allLines) {
                if (lstLine.info.type == LineType.EMPTY) {
                    listing.add("");
                } else if (!lstLine.info.isCorrect()) {
                    listing.add("Cинтаксична помилка! : " + lstLine.toString());
                } else {
                    listing.add(lstLine.toString());
                }
            }
            IOLib.writeAllLines(listing.toArray(new String[listing.size()]), new PrintStream(new File(firstPass)));
        }

        isSecondPass = true;
        for (LstLine lstLine : jumps) {
            LineInfo line = new LineInfo(lstLine.info);
            allLines.set(lstLine.lineNum - 1, new LstLine(lstLine.address, lstLine.lineNum, line));
            if (!line.isCorrect()) {
                ErrorList.AddError(lstLine.lineNum);
            }
        }

        ArrayList<String> listing = new ArrayList<>();
        for (LstLine lstLine : allLines) {
            if (lstLine.info.type == LineType.EMPTY) {
                listing.add("");
            } else if (!lstLine.info.isCorrect()) {
                listing.add("Cинтаксична помилка! : " + lstLine.toString());
            } else {

                listing.add(lstLine.toString());
                if (assumeOut && lstLine.info.type == LineType.ASSUME) {
                    listing.add(SegTable.getInstance().assumeToString());
                }
            }
        }

        listing.add("\n" + SegTable.getInstance().toString());
        listing.add(IdTable.getInstance().toString());
        listing.add(ErrorList.getStringToPrint());

        if (consoleOut) {
            IOLib.writeAllLines(listing.toArray(new String[listing.size()]), System.out);
        }
        IOLib.writeAllLines(listing.toArray(new String[listing.size()]), new PrintStream(new File(lstFilePath)));

        System.out.println("Вхідний файл: " + asmFilePath + "\nВихідний файл: " + lstFilePath);

        if (firstPassOut) {
            String firstPass = lstFilePath.toLowerCase().replace(".lst", ".flst");
            System.out.println("Файл першого проходу: " + firstPass);
        }

        if (lexicalOut) {
            String lexemes = lstFilePath.toLowerCase().replace(".lst", ".lex");
            try (PrintStream writer = new PrintStream(new File(lexemes))) {
                for (String source_line : fileLines) {
                    if (source_line.replaceAll(";.*", "").trim().isEmpty()) {
                        continue;
                    }
                    writer.println("Вхідна стрічка: " + source_line);
                    writer.println(LexicalAnalyzer.getStringToPrint(LexicalAnalyzer.getLexemeInfo(source_line)));
                }
            }
            System.out.println("Файл лексичного аналізу: " + lexemes);
        }
        System.out.println(ErrorList.getStringToPrint());
    }

    /**
     * Описание работы программы
     */
    private static void showHelp() {
        String jarName = new java.io.File(Translator.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();

        System.out.println("Використання: java -jar " + jarName + " [asmFile] [lstFile] [-options]");
        System.out.println("Довідка: ");
        System.out.println("[asmFile] - шлях до файлу з початковим кодом мовою ассемблер");
        System.out.println("[lstFile] - шлях до вихідного файлу лістингу");
        System.out.println("[-options] - додаткові опціі виконання программи:");
        System.out.println("    -f - генерація файлу першого проходу [lstFile].flst");
        System.out.println("    -l - генерація файлу лексичного аналізу за шляхом [lstFile].lex");
        System.out.println("    -a - виведення інформації(у файлі лістингу) про Assume");
        System.out.println("    -c - виведення лістингу на екран");
        System.out.println("\nПриклад: java -jar " + jarName + " source out");
        System.out.println("java -jar " + jarName + " src.asm out.lst -c");
        System.out.println("java -jar " + jarName + " test.asm test -af");
    }

    public static void main(String[] args) {

        if (args.length < 2 || args.length > 3) {
            showHelp();
            return;
        }

        try {
            if (args.length == 3) {
                if (!args[2].matches("^-c?l?a?f?$")) {
                    System.out.println("Помилкові опціі");
                    showHelp();
                    return;
                }
            }
            makeLST(args[0], args[1], args.length == 2 ? "" : args[2]);
        } catch (FileNotFoundException ex) {
            System.out.println("Файл не знайдено.");
        } catch (IOException ex) {
            System.out.println("Помилка виводу.");
        }
    }
}
