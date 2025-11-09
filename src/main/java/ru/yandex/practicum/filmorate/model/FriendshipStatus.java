package ru.yandex.practicum.filmorate.model;

import lombok.Getter;

@Getter
public enum FriendshipStatus {
    UNCONFIRMED("неподтверждённая"),
    CONFIRMED("подтверждённая");

    private final String description;

    FriendshipStatus(String description) {
        this.description = description;
    }
}