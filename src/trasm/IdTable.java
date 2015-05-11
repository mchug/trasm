package trasm;

/**
 * Таблица идентификаторов
 */
class IdTable extends AbstractTable {
    
    
    /**
     * Типы идентификаторов
     */
    static enum IdType {

        DB(1), DW(2), DD(4), LABEL(0);

        private int size;

        private IdType(int i) {
            size = i;
        }

        /**
         * Возвращает тип идентификатора в байтах(для DB,DW,DD)
         *
         * @return Тип идентификатора
         */
        int getSize() {
            return size;
        }
    }

    /**
     * Добавляет в таблицу новый элемент. Если элемент существует - записывает
     * строчку как ошибочную.
     *
     * @param item Новый элемент
     */
    void add(TableItem item) {
        if (isExist(item.getName())) {
            ErrorList.AddError();
        } else {
            super.add(item);
        }
    }

    private static IdTable instance = null;

    private IdTable() {
    }

    /**
     * Возвращает единственный экземпляр таблицы идентификаторов
     *
     * @return Таблица идентификаторов
     */
    static IdTable getInstance() {
        if (instance == null) {
            instance = new IdTable();
        }
        return instance;
    }

    /**
     * Элемент таблицы идентификаторов
     */
    static class IdInfo implements TableItem {

        private final String name;
        private final String segment;
        private final int address;
        private final IdType type;

        /**
         * Конструктор элемента таблицы идентификаторов
         *
         * @param name Имя нового элемента
         * @param type Тип нового элемента
         */
        public IdInfo(String name, IdType type) {
            this.name = name;
            this.segment = SegTable.getInstance().getCurrentSegment();
            this.address = SegTable.getInstance().getCurrentAddress();
            this.type = type;
        }

        /**
         * Возвращает тип идентификатора
         *
         * @return Тип идентификатора
         */
        public IdType getType() {
            return type;
        }

        /**
         * Возвращает смещение элемента
         *
         * @return Смещение элемента
         */
        public int getAddress() {
            return address;
        }

        /**
         * Возвращает сегмент в котором объявлен элемент
         *
         * @return Сегмент элемента
         */
        public String getSegment() {
            return segment;
        }

        /**
         * Возвращает имя элемента
         *
         * @return Имя элемента
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Преобразовывает элемент в удобный для чтения вид
         *
         * @return Строка для печати
         */
        @Override
        public String toString() {
            return String.format("%1$-8s %2$-8s %3$s:%4$s\n", name, type.toString(), segment, IOLib.toHex(address, 4));
        }

    }

    /**
     * Возвращает таблицу идентификторов в удобном для чтения виде
     *
     * @return Строка для печати
     */
    @Override
    public String toString() {

        StringBuilder outStr;
        outStr = new StringBuilder("Ім'я     Тип      Адреса\n");

        for (TableItem tableItem : list) {
            outStr = outStr.append(tableItem.toString());
        }

        return outStr.toString();
    }
}
