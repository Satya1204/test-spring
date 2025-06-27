package com.example.demo.entity;

import com.example.demo.enums.GameDirection;
import com.example.demo.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "game_code", unique = true, nullable = false)
    private String gameCode; // Unique code for players to join
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GameStatus status = GameStatus.WAITING_FOR_PLAYERS;
    
    @Column(name = "max_players")
    private Integer maxPlayers = 4;
    
    @Column(name = "min_players")
    private Integer minPlayers = 2;
    
    @Column(name = "current_player_index")
    private Integer currentPlayerIndex = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 20)
    private GameDirection direction = GameDirection.CLOCKWISE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_player_id")
    private Player createdBy;
    
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GamePlayer> gamePlayers = new ArrayList<>();
    
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Card> cards = new ArrayList<>();
    
    public Game(String gameCode, Player createdBy) {
        this.gameCode = gameCode;
        this.createdBy = createdBy;
        this.status = GameStatus.WAITING_FOR_PLAYERS;
        this.createdAt = LocalDateTime.now();
    }
    
    public boolean canStart() {
        return gamePlayers.size() >= minPlayers && gamePlayers.size() <= maxPlayers;
    }
    
    public boolean isFull() {
        return gamePlayers.size() >= maxPlayers;
    }
    
    public GamePlayer getCurrentPlayer() {
        if (gamePlayers.isEmpty() || currentPlayerIndex >= gamePlayers.size()) {
            return null;
        }
        return gamePlayers.get(currentPlayerIndex);
    }
    
    public void moveToNextPlayer() {
        if (gamePlayers.isEmpty()) return;
        
        if (direction == GameDirection.CLOCKWISE) {
            currentPlayerIndex = (currentPlayerIndex + 1) % gamePlayers.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex - 1 + gamePlayers.size()) % gamePlayers.size();
        }
    }
    
    public void reverseDirection() {
        direction = (direction == GameDirection.CLOCKWISE) ? 
            GameDirection.COUNTER_CLOCKWISE : GameDirection.CLOCKWISE;
    }
    
    public Card getTopCard() {
        return cards.stream()
            .filter(Card::getIsTopCard)
            .findFirst()
            .orElse(null);
    }
}
