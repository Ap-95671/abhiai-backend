package com.abhiai.abhiai_backend.controller;
import java.util.UUID; import org.springframework.http.ResponseEntity; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
import com.abhiai.abhiai_backend.dto.creator.*; import com.abhiai.abhiai_backend.security.JwtPrincipal; import com.abhiai.abhiai_backend.service.CreatorAnalyticsService;
@RestController @RequestMapping("/api/v1/creator/analytics")
public class CreatorAnalyticsController {
 private final CreatorAnalyticsService service; public CreatorAnalyticsController(CreatorAnalyticsService service){this.service=service;}
 @GetMapping public ResponseEntity<CreatorAnalyticsResponse> dashboard(@AuthenticationPrincipal JwtPrincipal p,@RequestParam(defaultValue="30")int days){return ResponseEntity.ok(service.dashboard(p.userId(),days));}
 @PostMapping("/posts/{postId}/impressions") public ResponseEntity<AnalyticsRecordedResponse> impression(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID postId){return ResponseEntity.ok(service.recordPostImpression(p.userId(),postId));}
 @PostMapping("/profiles/{username}/views") public ResponseEntity<AnalyticsRecordedResponse> profileView(@AuthenticationPrincipal JwtPrincipal p,@PathVariable String username){return ResponseEntity.ok(service.recordProfileView(p.userId(),username));}
}
