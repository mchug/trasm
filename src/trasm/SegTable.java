package trasm;

/**
 * Таблица сегментов
 */
class SegTable extends AbstractTable {

    /**
     * Пустой сегмент
     */
    static final String NULL_SEG_NAME = "NOTHING";
    /**
     * Текущий сегмент
     */
    private static String currentSegment = SegTable.NULL_SEG_NAME;
    /**
     * Текущее смещение
     */
    private static int currentAddress = 0;
    /**
     * Состояние Assume-а
     */
    private SegInfo[] assumeSegs = {new SegInfo(), new SegInfo(), new SegInfo(),
        new SegInfo(), new SegInfo(), new SegInfo()}; //что бы не инициализировать
    private static SegTable instance = null;

    private SegTable() {

    }

    /**
     * Возвращает единственный экземпляр таблицы сегментов
     *
     * @return Таблица сегментов
     */
    static SegTable getInstance() {
        if (instance == null) {
            instance = new SegTable();
        }
        return instance;
    }

    /**
     * Сегментные регистры
     */
    static enum SegRegister {

        ES, CS, SS, DS, FS, GS
    }

    /**
     * Элемент таблицы сегментов
     */
    static class SegInfo implements TableItem {

        /**
         * Имя сегмента
         */
        private final String name;
        /**
         * Размер сегмента
         */
        private int size;

        public SegInfo() {
            name = NULL_SEG_NAME;
            size = 0;
        }

        public SegInfo(String name) {
            this.name = name;
            this.size = 0;
        }

        /**
         * Устанавливает размер сегмента
         *
         * @param size Новый размер сегмента
         */
        public void setSize(int size) {
            this.size = size;
        }

        /**
         * Возвращает размер сегмента
         *
         * @return Размер сегмента
         */
        public int getSize() {
            return size;
        }

        /**
         * Возвращает имя сегмента
         *
         * @return Имя сегмента
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Возвращает описание сегмента в удобном для печати виде
         *
         * @return Строка для печати
         */
        @Override
        public String toString() {
            return String.format("%1$-8s %2$-4s\n", name, IOLib.toHex(size, 4));
        }
    }

    /**
     * Возвращает текущий сегмент
     *
     * @return Текущий сегмент
     */
    String getCurrentSegment() {
        return currentSegment;
    }

    /**
     * Задает текущий сегмент
     *
     * @param newSegment Новый текущий сегмент
     */
    void setCurrentSegment(String newSegment) {
        currentSegment = newSegment;
    }

    /**
     * Возвращает текущее смещение
     *
     * @return текущее смещение
     */
    int getCurrentAddress() {
        return currentAddress;
    }

    /**
     * Задает текущее смещение
     *
     * @param newAddress Новое текущее смещение
     */
    void setCurrentAddress(int newAddress) {
        currentAddress = newAddress;
    }

    /**
     * Устанавливает новый размер сегмента
     *
     * @param segment Имя сегмента
     * @param size Новый размер сегмента
     */
    void setSize(String segment, int size) {
        if (isExist(segment)) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getName().equalsIgnoreCase(segment)) {
                    SegInfo info = (SegInfo) list.get(i);
                    info.setSize(size);
                    list.set(i, info);
                    break;
                }
            }
        }
    }

    /**
     * Assume
     *
     * @param assume Строчка для Assume-а
     */
    void assume(String assume) {
        LexemeInfo[] lexemes = LexicalAnalyzer.getLexemeInfo(assume.toUpperCase());
        int index = 0;

        String segName = NULL_SEG_NAME;
        for (LexemeInfo lexemeInfo : lexemes) {
            if (lexemeInfo.type == LexemeType.REGISTER_SEGMENT) {
                index = SegRegister.valueOf(lexemeInfo.value).ordinal();
            } else if (lexemeInfo.type == LexemeType.USER_IDENTIFIER) {
                segName = lexemeInfo.value.toUpperCase();
            } else if (lexemeInfo.value.equals(",")) {
                assumeSegs[index] = new SegInfo(segName);
            }
        }

        assumeSegs[index] = new SegInfo(segName);
    }

    /**
     * Возвращает удобное для печати состояние сегментных регистров
     *
     * @return Состояние сегментных регистров
     */
    String assumeToString() {
        StringBuilder outStr;
        outStr = new StringBuilder(String.format("Сегмент  Регістр\n"));

        for (SegRegister segReg : SegRegister.values()) {
            outStr = outStr.append(String.format("%1$-8s %2$s\n", assumeSegs[segReg.ordinal()].name, segReg.toString()));
        }

        return outStr.toString();
    }

    /**
     * Возвращает сегмент который сейчас "лежит" заданном регистре
     *
     * @param segment Сегментный регистр
     * @return Сегмент лежащий в этом регистре
     */
    SegRegister getSegmentReg(String segment) {
        for (SegRegister segReg : SegRegister.values()) {
            SegInfo segInfo = assumeSegs[segReg.ordinal()];
            if (segInfo.name.equalsIgnoreCase(segment)) {
                return segReg;
            }
        }
        return null;
    }

    /**
     * Возвращает таблицу сегментов в удобном для чтения виде
     *
     * @return Строка для печати
     */
    @Override
    public String toString() {
        StringBuilder outStr;
        outStr = new StringBuilder("Сегмент  Розмір\n");

        for (TableItem tableItem : list) {
            outStr = outStr.append(tableItem.toString());
        }

        return outStr.toString();
    }
}
