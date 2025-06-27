package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.GameService;
import com.example.demo.service.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GameController {
    
    private final GameService gameService;
    private final WebSocketService webSocketService;
    
    @PostMapping
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        try {
            GameResponse response = gameService.createGame(request);
            
            // Broadcast game creation (though only creator will see it initially)
            webSocketService.broadcastGameUpdate(response.getGameCode(), response);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/join")
    public ResponseEntity<GameResponse> joinGame(@Valid @RequestBody JoinGameRequest request) {
        try {
            GameResponse response = gameService.joinGame(request);
            
            // Find the player who just joined
            String playerName = response.getPlayers().stream()
                .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                .map(p -> p.getPlayer().getPlayerName())
                .findFirst()
                .orElse("Unknown Player");
            
            // Broadcast player joined event
            webSocketService.broadcastPlayerJoined(
                response.getGameCode(), 
                request.getPlayerId(), 
                playerName, 
                response
            );
            
            // If game started, broadcast that too
            if (response.getStatus().name().equals("IN_PROGRESS")) {
                webSocketService.broadcastGameStarted(response.getGameCode(), response);
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{gameCode}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameCode, 
                                              @RequestParam(required = false) Long playerId) {
        try {
            GameResponse response = gameService.getGame(gameCode, playerId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<GameResponse>> getAvailableGames() {
        List<GameResponse> games = gameService.getAvailableGames();
        return ResponseEntity.ok(games);
    }
    
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<GameResponse>> getPlayerGames(@PathVariable Long playerId) {
        try {
            List<GameResponse> games = gameService.getPlayerGames(playerId);
            return ResponseEntity.ok(games);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/play-card")
    public ResponseEntity<GameResponse> playCard(@Valid @RequestBody PlayCardRequest request) {
        try {
            GameResponse response = gameService.playCard(
                request.getCardId(), 
                request.getPlayerId(), 
                request.getGameCode(), 
                request.getChosenColor()
            );
            
            // Find player name
            String playerName = response.getPlayers().stream()
                .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                .map(p -> p.getPlayer().getPlayerName())
                .findFirst()
                .orElse("Unknown Player");
            
            // Get card description
            String cardDescription = response.getTopCard() != null ? 
                response.getTopCard().getDisplayName() : "Unknown Card";
            
            // Broadcast card played event
            webSocketService.broadcastCardPlayed(
                response.getGameCode(), 
                request.getPlayerId(), 
                playerName, 
                cardDescription, 
                response
            );
            
            // Check if game ended
            if (response.getStatus().name().equals("FINISHED")) {
                String winnerName = response.getWinner() != null ? 
                    response.getWinner().getPlayerName() : "Unknown";
                webSocketService.broadcastGameWon(
                    response.getGameCode(), 
                    response.getWinner().getId(), 
                    winnerName, 
                    response
                );
            }
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/draw-card")
    public ResponseEntity<GameResponse> drawCard(@Valid @RequestBody DrawCardRequest request) {
        try {
            GameResponse response = gameService.drawCard(request.getPlayerId(), request.getGameCode());
            
            // Find player name
            String playerName = response.getPlayers().stream()
                .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                .map(p -> p.getPlayer().getPlayerName())
                .findFirst()
                .orElse("Unknown Player");
            
            // Broadcast card drawn event
            webSocketService.broadcastCardDrawn(
                response.getGameCode(), 
                request.getPlayerId(), 
                playerName, 
                1, 
                response
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/call-uno")
    public ResponseEntity<GameResponse> callUno(@Valid @RequestBody UnoCallRequest request) {
        try {
            GameResponse response = gameService.callUno(request.getPlayerId(), request.getGameCode());

            // Find player name
            String playerName = response.getPlayers().stream()
                .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                .map(p -> p.getPlayer().getPlayerName())
                .findFirst()
                .orElse("Unknown Player");

            // Broadcast UNO call
            webSocketService.broadcastUnoCall(
                response.getGameCode(),
                request.getPlayerId(),
                playerName,
                response
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/leave")
    public ResponseEntity<GameResponse> leaveGame(@RequestBody JoinGameRequest request) {
        try {
            GameResponse response = gameService.leaveGame(request.getPlayerId(), request.getGameCode());

            // Broadcast game update
            webSocketService.broadcastGameUpdate(response.getGameCode(), response);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
