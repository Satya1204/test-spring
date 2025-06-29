package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.GameService;

import com.example.demo.service.OptimizedWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

        private final GameService gameService;
        private final OptimizedWebSocketService optimizedWebSocketService;

        @MessageMapping("/game/play-card")
        public void playCard(@Payload PlayCardRequest request) {
                try {
                        GameResponse response = gameService.playCard(
                                        request.getCardId(),
                                        request.getPlayerId(),
                                        request.getGameCode(),
                                        request.getChosenColor());

                        // Find player name
                        String playerName = response.getPlayers().stream()
                                        .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                                        .map(p -> p.getPlayer().getPlayerName())
                                        .findFirst()
                                        .orElse("Unknown Player");

                        // Broadcast optimized card played event
                        optimizedWebSocketService.broadcastCardPlayed(
                                        response.getGameCode(),
                                        request.getPlayerId(),
                                        playerName,
                                        request.getCardId(),
                                        response.getTopCard(),
                                        response);

                        // Check if game ended
                        if (response.getStatus().name().equals("FINISHED")) {
                                String winnerName = response.getWinner() != null ? response.getWinner().getPlayerName()
                                                : "Unknown";
                                optimizedWebSocketService.broadcastGameWon(
                                                response.getGameCode(),
                                                response.getWinner().getId(),
                                                winnerName,
                                                0); // TODO: Calculate final score
                        }

                } catch (RuntimeException e) {
                        // Send error message to the specific player
                        optimizedWebSocketService.sendPersonalMessage(
                                        request.getGameCode(),
                                        request.getPlayerId(),
                                        "ERROR",
                                        e.getMessage());
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

                        // Get player's current card count
                        Integer totalCardsInHand = response.getPlayers().stream()
                                        .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                                        .map(p -> p.getCardsCount())
                                        .findFirst()
                                        .orElse(0);

                        // Broadcast optimized card drawn event
                        optimizedWebSocketService.broadcastCardDrawn(
                                        response.getGameCode(),
                                        request.getPlayerId(),
                                        playerName,
                                        1, // cards drawn
                                        totalCardsInHand,
                                        response.getDeckSize(),
                                        response);

                } catch (RuntimeException e) {
                        // Send error message to the specific player
                        optimizedWebSocketService.sendPersonalMessage(
                                        request.getGameCode(),
                                        request.getPlayerId(),
                                        "ERROR",
                                        e.getMessage());
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

                        // Get player's current card count
                        Integer cardsRemaining = response.getPlayers().stream()
                                        .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                                        .map(p -> p.getCardsCount())
                                        .findFirst()
                                        .orElse(0);

                        // Broadcast optimized UNO call
                        optimizedWebSocketService.broadcastUnoCalled(
                                        response.getGameCode(),
                                        request.getPlayerId(),
                                        playerName,
                                        cardsRemaining);

                } catch (RuntimeException e) {
                        // Send error message to the specific player
                        optimizedWebSocketService.sendPersonalMessage(
                                        request.getGameCode(),
                                        request.getPlayerId(),
                                        "ERROR",
                                        e.getMessage());
                }
        }

        @MessageMapping("/game/join")
        public void joinGameViaWebSocket(@Payload JoinGameRequest request) {
                try {
                        System.out.println(
                                        "🎮 WebSocket JOIN received: " + request.getGameCode() + " - Player: "
                                                        + request.getPlayerId());

                        GameResponse response = gameService.joinGame(request);

                        System.out.println("🎮 Game service completed, response status: " + response.getStatus());

                        // Find the player who just joined
                        String playerName = response.getPlayers().stream()
                                        .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                                        .map(p -> p.getPlayer().getPlayerName())
                                        .findFirst()
                                        .orElse("Unknown Player");

                        System.out.println("🎮 Player name found: " + playerName);

                        // Broadcast optimized player joined event
                        System.out.println("🎮 About to broadcast player joined...");
                        Integer playerOrder = response.getPlayers().stream()
                                        .filter(p -> p.getPlayer().getId().equals(request.getPlayerId()))
                                        .map(p -> p.getPlayerOrder())
                                        .findFirst()
                                        .orElse(0);

                        optimizedWebSocketService.broadcastPlayerJoined(
                                        response.getGameCode(),
                                        request.getPlayerId(),
                                        playerName,
                                        playerOrder,
                                        response.getPlayers().size());
                        System.out.println("🎮 Player joined broadcast completed");

                        // If game started, broadcast that too
                        if (response.getStatus().name().equals("IN_PROGRESS")) {
                                System.out.println("🎮 Game started, broadcasting game started event...");
                                optimizedWebSocketService.broadcastGameStarted(response.getGameCode(), response);
                                System.out.println("🎮 Game started broadcast completed");
                        }

                } catch (Exception e) {
                        System.err.println("❌ ERROR in WebSocket JOIN: " + e.getMessage());
                        e.printStackTrace();

                        // Send error message to the specific player
                        optimizedWebSocketService.sendPersonalMessage(
                                        request.getGameCode(),
                                        request.getPlayerId(),
                                        "ERROR",
                                        e.getMessage());
                }
        }
}
