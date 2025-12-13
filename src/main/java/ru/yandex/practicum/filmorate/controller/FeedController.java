package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FeedEventDTO;
import ru.yandex.practicum.filmorate.service.feed.FeedService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{id}/feed")
    public ResponseEntity<List<FeedEventDTO>> getUserFeed(@PathVariable Long id) {
        List<FeedEventDTO> feed = feedService.getUserFeed(id);
        return ResponseEntity.ok(feed);
    }
}