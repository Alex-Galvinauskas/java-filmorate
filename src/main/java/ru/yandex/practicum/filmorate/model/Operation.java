package ru.yandex.practicum.filmorate.model;

public enum Operation {
    ADD("ADD"),
    REMOVE("REMOVE"),
    UPDATE("UPDATE");

    private final String russianName;

    Operation(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
}