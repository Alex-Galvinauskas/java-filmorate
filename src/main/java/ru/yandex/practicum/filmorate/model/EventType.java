package ru.yandex.practicum.filmorate.model;

public enum EventType {
    LIKE("Лайк"),
    REVIEW("Отзыв"),
    FRIEND("Друг");

    private final String russianName;

    EventType(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
}