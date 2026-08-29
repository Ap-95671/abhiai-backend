package com.abhiai.abhiai_backend.news.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.abhiai.abhiai_backend.news.dto.NewsArticleResponse;
import com.abhiai.abhiai_backend.news.dto.NewsPageResponse;
import com.abhiai.abhiai_backend.news.service.NewsService;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<NewsPageResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(newsService.list(category, region, language, query, page, limit, refresh));
    }

    @GetMapping("/top")
    public ResponseEntity<NewsPageResponse> top(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(newsService.list("top", region, null, null, 0, limit, false));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<NewsPageResponse> category(
            @PathVariable String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(newsService.list(category, region, null, null, page, limit, false));
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<NewsPageResponse> region(
            @PathVariable String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(newsService.list(category, region, null, null, page, limit, false));
    }

    @GetMapping("/search")
    public ResponseEntity<NewsPageResponse> search(
            @RequestParam(name = "q") String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(newsService.list(category, region, null, query, page, limit, false));
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<NewsArticleResponse> get(@PathVariable String articleId) {
        return ResponseEntity.ok(newsService.get(articleId));
    }
}
