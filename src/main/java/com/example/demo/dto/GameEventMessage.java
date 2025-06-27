package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEventMessage {
    
    private String eventType;
    private String gameCode;
    private Long playerId;
    private String playerName;
    private Object eventData;
    private LocalDateTime timestamp;
    private GameResponse gameState; // Updated game state after the event
    
    public static GameEventMessage create(String eventType, String gameCode, Long playerId, 
                                        String playerName, Object eventData, GameResponse gameState) {
        return new GameEventMessage(
            eventType,
            gameCode,
            playerId,
            playerName,
            eventData,
            LocalDateTime.now(),
            gameState
        );
    }
}
