package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "game_players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    
    @Column(name = "player_order", nullable = false)
    private Integer playerOrder; // Order in which player joined (0, 1, 2, 3)
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Column(name = "is_active")
    private Boolean isActive = true; // false if player left the game
    
    @Column(name = "cards_count")
    private Integer cardsCount = 0; // Number of cards in hand
    
    @Column(name = "has_called_uno")
    private Boolean hasCalledUno = false; // true if player called UNO
    
    public GamePlayer(Game game, Player player, Integer playerOrder) {
        this.game = game;
        this.player = player;
        this.playerOrder = playerOrder;
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;
        this.cardsCount = 0;
        this.hasCalledUno = false;
    }
    
    public List<Card> getHand() {
        return game.getCards().stream()
            .filter(card -> card.getPlayer() != null && 
                           card.getPlayer().getId().equals(player.getId()) &&
                           !card.getIsInDeck() && 
                           !card.getIsTopCard())
            .sorted((c1, c2) -> {
                if (c1.getPositionInHand() == null) return 1;
                if (c2.getPositionInHand() == null) return -1;
                return c1.getPositionInHand().compareTo(c2.getPositionInHand());
            })
            .toList();
    }
    
    public boolean hasWon() {
        return cardsCount == 0;
    }
    
    public boolean shouldCallUno() {
        return cardsCount == 1 && !hasCalledUno;
    }
}
