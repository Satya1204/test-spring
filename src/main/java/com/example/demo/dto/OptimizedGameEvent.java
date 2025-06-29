package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Optimized WebSocket event that sends only changed data instead of entire game state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimizedGameEvent {
    
    private String eventType;
    private String gameCode;
    private Long playerId;
    private String playerName;
    private LocalDateTime timestamp;
    private Object eventData;
    
    public static OptimizedGameEvent create(String eventType, String gameCode, Long playerId, 
                                          String playerName, Object eventData) {
        return new OptimizedGameEvent(
            eventType,
            gameCode,
            playerId,
            playerName,
            LocalDateTime.now(),
            eventData
        );
    }
}
