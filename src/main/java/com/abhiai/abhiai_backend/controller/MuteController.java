package com.abhiai.abhiai_backend.controller;
import java.util.*; import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
import com.abhiai.abhiai_backend.dto.user.*; import com.abhiai.abhiai_backend.security.JwtPrincipal; import com.abhiai.abhiai_backend.service.MuteService; import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/mutes") public class MuteController{
 private final MuteService service; public MuteController(MuteService s){service=s;}
 @GetMapping public List<MuteResponse> list(@AuthenticationPrincipal JwtPrincipal p){return service.list(p.userId());}
 @PostMapping public ResponseEntity<MuteResponse> add(@AuthenticationPrincipal JwtPrincipal p,@Valid @RequestBody MuteRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.add(p.userId(),r));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@AuthenticationPrincipal JwtPrincipal p,@PathVariable UUID id){service.remove(p.userId(),id);}
}
