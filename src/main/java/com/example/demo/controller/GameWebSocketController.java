package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.GameService;
import com.example.demo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {
    
    private final GameService gameService;
    private final WebSocketService webSocketService;
    
    @MessageMapping("/game/play-card")
    public void playCard(@Payload PlayCardRequest request) {
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
            
        } catch (RuntimeException e) {
            // Send error message to the specific player
            webSocketService.sendPersonalMessage(
                request.getGameCode(), 
                request.getPlayerId(), 
                "ERROR", 
                e.getMessage()
            );
        }
    }
    
    @MessageMapping("/game/draw-card")
    public void drawCard(@Payload DrawCardRequest request) {
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
            
        } catch (RuntimeException e) {
            // Send error message to the specific player
            webSocketService.sendPersonalMessage(
                request.getGameCode(), 
                request.getPlayerId(), 
                "ERROR", 
                e.getMessage()
            );
        }
    }
    
    @MessageMapping("/game/call-uno")
    public void callUno(@Payload UnoCallRequest request) {
        try {
            // This would be implemented in GameService
            // For now, just broadcast the UNO call
            GameResponse response = gameService.getGame(request.getGameCode(), request.getPlayerId());
            
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
            
        } catch (RuntimeException e) {
            // Send error message to the specific player
            webSocketService.sendPersonalMessage(
                request.getGameCode(), 
                request.getPlayerId(), 
                "ERROR", 
                e.getMessage()
            );
        }
    }
    
    @MessageMapping("/game/join")
    public void joinGameViaWebSocket(@Payload JoinGameRequest request) {
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
            
        } catch (RuntimeException e) {
            // Send error message to the specific player
            webSocketService.sendPersonalMessage(
                request.getGameCode(), 
                request.getPlayerId(), 
                "ERROR", 
                e.getMessage()
            );
        }
    }
}
