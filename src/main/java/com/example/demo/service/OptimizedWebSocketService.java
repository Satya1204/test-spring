package com.example.demo.service;

import com.example.demo.dto.OptimizedGameEvent;
import com.example.demo.dto.OptimizedEventData;
import com.example.demo.dto.GameResponse;
import com.example.demo.dto.GamePlayerDto;
import com.example.demo.dto.CardDto;
import com.example.demo.enums.GameDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Optimized WebSocket service that sends only changed data instead of entire
 * game state
 */
@Service
@RequiredArgsConstructor
public class OptimizedWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPlayerJoined(String gameCode, Long playerId, String playerName,
            Integer playerOrder, Integer totalPlayers) {
        OptimizedEventData.PlayerJoined eventData = new OptimizedEventData.PlayerJoined(
                playerName, playerOrder, totalPlayers);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "PLAYER_JOINED", gameCode, playerId, playerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastGameStarted(String gameCode, GameResponse gameState) {
        GamePlayerDto currentPlayer = getCurrentPlayer(gameState);
        OptimizedEventData.TopCard topCard = createTopCardData(gameState.getTopCard());

        OptimizedEventData.GameStarted eventData = new OptimizedEventData.GameStarted(
                gameState.getPlayers().size(),
                currentPlayer != null ? currentPlayer.getPlayer().getId() : null,
                currentPlayer != null ? currentPlayer.getPlayer().getPlayerName() : null,
                topCard);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "GAME_STARTED", gameCode, null, null, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastGameUpdate(String gameCode, GameResponse gameState) {
        GamePlayerDto currentPlayer = getCurrentPlayer(gameState);
        OptimizedEventData.TopCard topCard = createTopCardData(gameState.getTopCard());

        OptimizedEventData.GameUpdate eventData = new OptimizedEventData.GameUpdate(
                gameState.getPlayers().size(),
                currentPlayer != null ? currentPlayer.getPlayer().getId() : null,
                currentPlayer != null ? currentPlayer.getPlayer().getPlayerName() : null,
                topCard,
                gameState.getStatus().toString(),
                gameState.getDirection().toString());

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "GAME_UPDATE", gameCode, null, null, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastCardPlayed(String gameCode, Long playerId, String playerName,
            Long cardId, CardDto newTopCard, GameResponse gameState) {
        GamePlayerDto nextPlayer = getCurrentPlayer(gameState);
        GamePlayerDto currentPlayerData = findPlayer(gameState, playerId);

        OptimizedEventData.CardPlayed eventData = new OptimizedEventData.CardPlayed(
                cardId,
                createTopCardData(newTopCard),
                nextPlayer != null ? nextPlayer.getPlayer().getId() : null,
                nextPlayer != null ? nextPlayer.getPlayer().getPlayerName() : null,
                currentPlayerData != null ? currentPlayerData.getCardsCount() : 0,
                gameState.getDirection().toString(),
                gameState.getStatus().toString().equals("FINISHED"),
                gameState.getWinner() != null ? gameState.getWinner().getId() : null,
                gameState.getWinner() != null ? gameState.getWinner().getPlayerName() : null);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "CARD_PLAYED", gameCode, playerId, playerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastCardDrawn(String gameCode, Long playerId, String playerName,
            Integer cardsDrawn, Integer totalCardsInHand,
            Integer deckSize, GameResponse gameState) {
        GamePlayerDto nextPlayer = getCurrentPlayer(gameState);

        OptimizedEventData.CardDrawn eventData = new OptimizedEventData.CardDrawn(
                cardsDrawn,
                totalCardsInHand,
                nextPlayer != null ? nextPlayer.getPlayer().getId() : null,
                nextPlayer != null ? nextPlayer.getPlayer().getPlayerName() : null,
                deckSize);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "CARD_DRAWN", gameCode, playerId, playerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastUnoCalled(String gameCode, Long playerId, String playerName,
            Integer cardsRemaining) {
        OptimizedEventData.UnoCalled eventData = new OptimizedEventData.UnoCalled(
                playerName, cardsRemaining);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "UNO_CALLED", gameCode, playerId, playerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastGameWon(String gameCode, Long winnerId, String winnerName,
            Integer finalScore) {
        OptimizedEventData.GameWon eventData = new OptimizedEventData.GameWon(
                winnerName, finalScore);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "GAME_WON", gameCode, winnerId, winnerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastPlayerLeft(String gameCode, String playerName, Integer remainingPlayers,
            Long newCurrentPlayerId, String newCurrentPlayerName) {
        OptimizedEventData.PlayerLeft eventData = new OptimizedEventData.PlayerLeft(
                playerName, remainingPlayers, newCurrentPlayerId, newCurrentPlayerName);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "PLAYER_LEFT", gameCode, null, null, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastTurnSkipped(String gameCode, Long skippedPlayerId, String skippedPlayerName,
            Long nextPlayerId, String nextPlayerName) {
        OptimizedEventData.TurnSkipped eventData = new OptimizedEventData.TurnSkipped(
                skippedPlayerId, skippedPlayerName, nextPlayerId, nextPlayerName);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "TURN_SKIPPED", gameCode, skippedPlayerId, skippedPlayerName, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastDirectionChanged(String gameCode, GameDirection newDirection,
            Long nextPlayerId, String nextPlayerName) {
        OptimizedEventData.DirectionChanged eventData = new OptimizedEventData.DirectionChanged(
                newDirection.toString(), nextPlayerId, nextPlayerName);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "DIRECTION_CHANGED", gameCode, null, null, eventData);

        broadcastEvent(gameCode, event);
    }

    public void broadcastColorChanged(String gameCode, String newColor,
            Long nextPlayerId, String nextPlayerName) {
        OptimizedEventData.ColorChanged eventData = new OptimizedEventData.ColorChanged(
                newColor, nextPlayerId, nextPlayerName);

        OptimizedGameEvent event = OptimizedGameEvent.create(
                "COLOR_CHANGED", gameCode, null, null, eventData);

        broadcastEvent(gameCode, event);
    }

    public void sendPersonalMessage(String gameCode, Long playerId, String eventType, Object data) {
        OptimizedGameEvent event = OptimizedGameEvent.create(
                eventType, gameCode, playerId, null, data);

        // Send to specific player
        messagingTemplate.convertAndSend("/queue/game/" + gameCode + "/player/" + playerId, event);
    }

    private void broadcastEvent(String gameCode, OptimizedGameEvent event) {
        try {
            System.out.println("🔔 Broadcasting optimized WebSocket message:");
            System.out.println("   Topic: /topic/game/" + gameCode);
            System.out.println("   Event Type: " + event.getEventType());
            System.out.println("   Player: " + event.getPlayerName() + " (ID: " + event.getPlayerId() + ")");
            System.out.println("   Data Size: ~" + estimateDataSize(event) + " bytes");

            messagingTemplate.convertAndSend("/topic/game/" + gameCode, event);

            System.out.println("✅ Optimized WebSocket message sent successfully");

        } catch (Exception e) {
            System.err.println("❌ Error broadcasting optimized WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private OptimizedEventData.TopCard createTopCardData(CardDto card) {
        if (card == null)
            return null;
        return new OptimizedEventData.TopCard(
                card.getColor() != null ? card.getColor().toString() : null,
                card.getCardType() != null ? card.getCardType().toString() : null,
                card.getValue(),
                card.getDisplayName());
    }

    private GamePlayerDto getCurrentPlayer(GameResponse gameState) {
        if (gameState.getCurrentPlayerIndex() == null ||
                gameState.getCurrentPlayerIndex() >= gameState.getPlayers().size()) {
            return null;
        }
        return gameState.getPlayers().get(gameState.getCurrentPlayerIndex());
    }

    private GamePlayerDto findPlayer(GameResponse gameState, Long playerId) {
        return gameState.getPlayers().stream()
                .filter(p -> p.getPlayer().getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private int estimateDataSize(OptimizedGameEvent event) {
        // Rough estimation for logging purposes
        return event.toString().length();
    }
}
