package trasm;

import java.util.ArrayList;

/**
 * Интерфейс элемента таблицы сегментов/идентификаторов
 */
interface TableItem {

    String getName();

    @Override
    String toString();
}

/**
 * Абстрактный класс для таблицы сегментов/идентификаторов
 */
abstract class AbstractTable {

    /**
     * Список для хранения элементов таблицы
     */
    protected final ArrayList<TableItem> list = new ArrayList<>();

    /**
     * Проверяет или элемент с заданным именем существует
     *
     * @param name Имя элемента
     * @return Ответ на главный вопрос
     */
    boolean isExist(String name) {
        for (TableItem tableItem : list) {
            if (name.equalsIgnoreCase(tableItem.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Добаляет элемент в таблицу, если он уже существует - ничего не происходит
     *
     * @param item Новый элемент
     */
    void add(TableItem item) {
        if (!isExist(item.getName())) {
            list.add(item);
        }
    }

    /**
     * Возвращает элемент с заданным именем. В случае если элемент не существует
     * - null.
     *
     * @param name Имя элемента
     * @return Элемент с заданым именем
     */
    TableItem get(String name) {
        for (TableItem tableItem : list) {
            if (name.equalsIgnoreCase(tableItem.getName())) {
                return tableItem;
            }
        }

        return null;
    }
}
