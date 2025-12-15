package ru.yandex.practicum.filmorate.model;

public enum Operation {
    ADD("Добавить"),
    REMOVE("Удалить"),
    UPDATE("Обновить");

    private final String russianName;

    Operation(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
}