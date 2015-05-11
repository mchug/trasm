package trasm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 * Класс для работы с вводом и выводом данных
 */
class IOLib {

    /**
     * Считывает текстовый файл в массив строк
     *
     * @param filePath Путь к текстовому файлу
     * @return Массив строк содержащий в себе каждую строку из входного файла
     * @throws FileNotFoundException
     * @throws IOException
     */
    static public String[] readAllLines(String filePath) throws FileNotFoundException, IOException {
        ArrayList<String> outList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                outList.add(scanner.nextLine());
            }
        }

        return outList.toArray(new String[outList.size()]);
    }

    /**
     * Записывает массив строк в поток
     *
     * @param lines Массив строк для записи
     * @param writer Райтер
     * @throws FileNotFoundException
     * @throws IOException
     */
    static public void writeAllLines(String[] lines, PrintStream writer) throws FileNotFoundException, IOException {

        String about = "Курсова робота студента КПІ ФПМ групи КВ-23 Чугаєвського Максима Варіант 1\n";
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/mm/yyyy HH:mm:ss");
        Date now = new Date();
        about += "Згенеровано: " + sdfDate.format(now);

        writer.println(about);

        for (String line : lines) {
            writer.println(line);
        }
        if (!writer.equals(System.out)) {
            writer.close();
        }
    }

    /**
     * Преобразовывает число в hex вид с заданной шириной
     *
     * @param i Число для преобразования
     * @param lenght Длина выходной строки
     * @return Число в hex формате
     */
    static String toHex(long i, int lenght) {
        StringBuilder outStr = new StringBuilder(Long.toHexString(i).toUpperCase());

        for (int j = outStr.length(); j < lenght; j++) {
            outStr.insert(0, "0");
        }

        return outStr.length() != lenght ? outStr.substring(outStr.length() - lenght, outStr.length()) : outStr.toString();
    }

    /**
     * Преобразовывает число в hex вид с заданной шириной
     *
     * @param value Строка для преобразования
     * @param lenght Длина выходной строки
     * @return Число в hex формате
     */
    static String toHex(String value, int lenght) {
        return toHex(LexicalAnalyzer.getConstValue(value), lenght);
    }
}
