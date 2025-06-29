package com.example.demo.service;

import com.example.demo.dto.GameEventMessage;
import com.example.demo.dto.GameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastGameEvent(String gameCode, String eventType, Long playerId,
            String playerName, Object eventData, GameResponse gameState) {
        try {
            GameEventMessage message = GameEventMessage.create(
                    eventType, gameCode, playerId, playerName, eventData, gameState);

            System.out.println("🔔 Broadcasting WebSocket message:");
            System.out.println("   Topic: /topic/game/" + gameCode);
            System.out.println("   Event Type: " + eventType);
            System.out.println("   Player: " + playerName + " (ID: " + playerId + ")");

            // Broadcast to all players in the game
            messagingTemplate.convertAndSend("/topic/game/" + gameCode, message);

            System.out.println("✅ WebSocket message sent successfully");

        } catch (Exception e) {
            System.err.println("❌ Error broadcasting WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPersonalMessage(String gameCode, Long playerId, String eventType, Object data) {
        GameEventMessage message = new GameEventMessage();
        message.setEventType(eventType);
        message.setGameCode(gameCode);
        message.setPlayerId(playerId);
        message.setEventData(data);

        // Send to specific player
        messagingTemplate.convertAndSend("/queue/game/" + gameCode + "/player/" + playerId, message);
    }

    public void broadcastGameUpdate(String gameCode, GameResponse gameState) {
        broadcastGameEvent(gameCode, "GAME_UPDATE", null, null, null, gameState);
    }

    public void broadcastPlayerJoined(String gameCode, Long playerId, String playerName, GameResponse gameState) {
        broadcastGameEvent(gameCode, "PLAYER_JOINED", playerId, playerName,
                new PlayerJoinedData(playerName), gameState);
    }

    public void broadcastCardPlayed(String gameCode, Long playerId, String playerName,
            String cardDescription, GameResponse gameState) {
        broadcastGameEvent(gameCode, "CARD_PLAYED", playerId, playerName,
                new CardPlayedData(cardDescription), gameState);
    }

    public void broadcastCardDrawn(String gameCode, Long playerId, String playerName,
            int cardCount, GameResponse gameState) {
        broadcastGameEvent(gameCode, "CARD_DRAWN", playerId, playerName,
                new CardDrawnData(cardCount), gameState);
    }

    public void broadcastUnoCall(String gameCode, Long playerId, String playerName, GameResponse gameState) {
        broadcastGameEvent(gameCode, "UNO_CALLED", playerId, playerName,
                new UnoCalledData(playerName), gameState);
    }

    public void broadcastGameWon(String gameCode, Long playerId, String playerName, GameResponse gameState) {
        broadcastGameEvent(gameCode, "GAME_WON", playerId, playerName,
                new GameWonData(playerName), gameState);
    }

    public void broadcastGameStarted(String gameCode, GameResponse gameState) {
        broadcastGameEvent(gameCode, "GAME_STARTED", null, null,
                new GameStartedData(gameState.getPlayers().size()), gameState);
    }

    // Inner classes for event data
    public static class PlayerJoinedData {
        public final String playerName;

        public PlayerJoinedData(String playerName) {
            this.playerName = playerName;
        }
    }

    public static class CardPlayedData {
        public final String cardDescription;

        public CardPlayedData(String cardDescription) {
            this.cardDescription = cardDescription;
        }
    }

    public static class CardDrawnData {
        public final int cardCount;

        public CardDrawnData(int cardCount) {
            this.cardCount = cardCount;
        }
    }

    public static class UnoCalledData {
        public final String playerName;

        public UnoCalledData(String playerName) {
            this.playerName = playerName;
        }
    }

    public static class GameWonData {
        public final String winnerName;

        public GameWonData(String winnerName) {
            this.winnerName = winnerName;
        }
    }

    public static class GameStartedData {
        public final int playerCount;

        public GameStartedData(int playerCount) {
            this.playerCount = playerCount;
        }
    }
}
