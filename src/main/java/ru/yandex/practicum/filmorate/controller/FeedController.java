package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FeedEventDto;
import ru.yandex.practicum.filmorate.service.feed.FeedService;
import ru.yandex.practicum.filmorate.service.user.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class FeedController {

    private final FeedService feedService;
    private final UserService userService;

    @GetMapping("/{id}/feed")
    @ResponseStatus(HttpStatus.OK)
    public List<FeedEventDto> getUserFeed(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "100") Integer size) {

        log.debug("GET /users/{}/feed?from={}&size={}", id, from, size);

        userService.getUserById(id);

        return feedService.getUserFeed(id, from, size);
    }
}