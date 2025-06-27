package com.example.demo.controller;

import com.example.demo.dto.PlayerRequest;
import com.example.demo.dto.PlayerResponse;
import com.example.demo.dto.UpdateCoinsRequest;
import com.example.demo.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlayerController {
    
    private final PlayerService playerService;
    
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody PlayerRequest request) {
        try {
            PlayerResponse response = playerService.createPlayer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getAllPlayers() {
        List<PlayerResponse> players = playerService.getAllPlayers();
        return ResponseEntity.ok(players);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        try {
            PlayerResponse player = playerService.getPlayerById(id);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/name/{playerName}")
    public ResponseEntity<PlayerResponse> getPlayerByName(@PathVariable String playerName) {
        try {
            PlayerResponse player = playerService.getPlayerByName(playerName);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        try {
            PlayerResponse response = playerService.updatePlayer(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PatchMapping("/{id}/coins")
    public ResponseEntity<PlayerResponse> updatePlayerCoins(@PathVariable Long id, @Valid @RequestBody UpdateCoinsRequest request) {
        try {
            PlayerResponse response = playerService.updatePlayerCoins(id, request.getCoins());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PatchMapping("/{id}/add-coins")
    public ResponseEntity<PlayerResponse> addCoinsToPlayer(@PathVariable Long id, @Valid @RequestBody UpdateCoinsRequest request) {
        try {
            PlayerResponse response = playerService.addCoinsToPlayer(id, request.getCoins());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        try {
            playerService.deletePlayer(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/leaderboard")
    public ResponseEntity<List<PlayerResponse>> getLeaderboard() {
        List<PlayerResponse> players = playerService.getPlayersOrderByCoins();
        return ResponseEntity.ok(players);
    }
    
    @GetMapping("/rich")
    public ResponseEntity<List<PlayerResponse>> getRichPlayers(@RequestParam(defaultValue = "1000") Integer minCoins) {
        List<PlayerResponse> players = playerService.getPlayersWithMinCoins(minCoins);
        return ResponseEntity.ok(players);
    }
}
