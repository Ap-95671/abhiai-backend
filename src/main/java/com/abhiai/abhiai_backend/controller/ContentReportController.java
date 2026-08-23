package com.abhiai.abhiai_backend.controller;
import org.springframework.data.domain.Pageable; import org.springframework.data.web.PageableDefault; import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
import com.abhiai.abhiai_backend.dto.common.PageResponse; import com.abhiai.abhiai_backend.dto.report.*; import com.abhiai.abhiai_backend.security.JwtPrincipal; import com.abhiai.abhiai_backend.service.ContentReportService; import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/reports")
public class ContentReportController {private final ContentReportService service;public ContentReportController(ContentReportService service){this.service=service;}
 @PostMapping public ResponseEntity<ContentReportResponse> create(@AuthenticationPrincipal JwtPrincipal p,@Valid @RequestBody CreateReportRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(p.userId(),request));}
 @GetMapping("/mine") public ResponseEntity<PageResponse<ContentReportResponse>> mine(@AuthenticationPrincipal JwtPrincipal p,@PageableDefault(size=20) Pageable pageable){return ResponseEntity.ok(service.mine(p.userId(),pageable));}
}
