package com.abhiai.abhiai_backend.controller;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.abhiai.abhiai_backend.dto.article.*;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.security.JwtPrincipal;
import com.abhiai.abhiai_backend.service.ArticleService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {
    private final ArticleService service;
    public ArticleController(ArticleService service){this.service=service;}
    @PostMapping public ResponseEntity<ArticleResponse> create(@AuthenticationPrincipal JwtPrincipal p,@Valid @RequestBody UpsertArticleRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(p.userId(),r));}
    @GetMapping public ResponseEntity<PageResponse<ArticleResponse>> list(@AuthenticationPrincipal JwtPrincipal p,@RequestParam(required=false) UUID authorId,@PageableDefault(size=12) Pageable pageable){return ResponseEntity.ok(service.list(p.userId(),authorId,pageable));}
    @GetMapping("/{articleId}") public ResponseEntity<ArticleResponse> get(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId){return ResponseEntity.ok(service.get(p.userId(),articleId));}
    @PutMapping("/{articleId}") public ResponseEntity<ArticleResponse> update(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId,@Valid @RequestBody UpsertArticleRequest r){return ResponseEntity.ok(service.update(p.userId(),articleId,r));}
    @DeleteMapping("/{articleId}") public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId){service.delete(p.userId(),articleId);return ResponseEntity.noContent().build();}
    @PostMapping("/{articleId}/likes") public ResponseEntity<ArticleResponse> like(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId){return ResponseEntity.status(HttpStatus.CREATED).body(service.like(p.userId(),articleId));}
    @DeleteMapping("/{articleId}/likes") public ResponseEntity<ArticleResponse> unlike(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId){return ResponseEntity.ok(service.unlike(p.userId(),articleId));}
    @GetMapping("/{articleId}/comments") public ResponseEntity<PageResponse<ArticleCommentResponse>> comments(@PathVariable UUID articleId,@PageableDefault(size=30) Pageable pageable){return ResponseEntity.ok(service.comments(articleId,pageable));}
    @PostMapping("/{articleId}/comments") public ResponseEntity<ArticleCommentResponse> comment(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId,@Valid @RequestBody CreateArticleCommentRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.comment(p.userId(),articleId,r));}
    @DeleteMapping("/{articleId}/comments/{commentId}") public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID articleId,@PathVariable UUID commentId){service.deleteComment(p.userId(),articleId,commentId);return ResponseEntity.noContent().build();}
    @PostMapping("/{articleId}/shares") public ResponseEntity<ArticleShareResponse> share(@PathVariable UUID articleId){return ResponseEntity.ok(service.share(articleId));}
}
