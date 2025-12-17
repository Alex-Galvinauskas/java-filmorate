package ru.yandex.practicum.filmorate.model;

public enum EventType {
    LIKE("LIKE"),
    REVIEW("REVIEW"),
    FRIEND("FRIEND");

    private final String russianName;

    EventType(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
}