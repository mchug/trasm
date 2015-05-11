package trasm;

import java.util.ArrayList;

/**
 * Список ошибок при создании листинга.
 */
//p.s. Очень быдлокод...
class ErrorList {

    /**
     * Список с номерами строк
     */
    private static final ArrayList<Integer> errorLineList = new ArrayList<>();
    /**
     * Глобальная переменная хранящая текущий номер строки
     */
    static int currentLine = 1;

    /**
     * Добавить ошибку. (номер строки = currentLine)
     */
    static void AddError() {
        errorLineList.add(currentLine);
    }

    /**
     * Добавить ошибку с заданным номером строки
     *
     * @param line Номер строки
     */
    static void AddError(int line) {
        errorLineList.add(line);
    }

    /**
     * Строка для печати с количеством ошибок и номерами строчек с ошибками
     *
     * @return Строка для печати
     */
    static String getStringToPrint() {
        StringBuilder outStr;
        outStr = new StringBuilder("Помилки: ");

        outStr = outStr.append(errorLineList.size()).append((errorLineList.isEmpty()) ? "\n" : "\nРядки з помилками: ");

        for (Integer errorLine : errorLineList) {
            outStr = outStr.append(errorLine).append(" ");
        }

        return outStr.toString();
    }
}
