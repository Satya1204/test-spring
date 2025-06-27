package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player; // null for system events
    
    @Column(name = "event_type", nullable = false)
    private String eventType; // CARD_PLAYED, CARD_DRAWN, UNO_CALLED, GAME_STARTED, etc.
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData; // JSON data with event details
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber; // Order of events in the game
    
    public GameEvent(Game game, Player player, String eventType, String eventData, Integer sequenceNumber) {
        this.game = game;
        this.player = player;
        this.eventType = eventType;
        this.eventData = eventData;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = LocalDateTime.now();
    }
}
