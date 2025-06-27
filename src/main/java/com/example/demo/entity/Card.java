package com.example.demo.entity;

import com.example.demo.enums.CardColor;
import com.example.demo.enums.CardType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 10)
    private CardColor color;
    
    @Column(name = "card_value")
    private Integer value; // For number cards (0-9), null for action/wild cards
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player; // null if card is in deck or discard pile
    
    @Column(name = "is_in_deck")
    private Boolean isInDeck = false; // true if card is in the draw deck
    
    @Column(name = "is_top_card")
    private Boolean isTopCard = false; // true if this is the current top card of discard pile
    
    @Column(name = "position_in_hand")
    private Integer positionInHand; // position in player's hand
    
    public Card(CardType cardType, CardColor color, Integer value) {
        this.cardType = cardType;
        this.color = color;
        this.value = value;
        this.isInDeck = true;
        this.isTopCard = false;
    }
    
    public boolean isPlayableOn(Card topCard) {
        return com.example.demo.util.UnoGameRules.canPlayCard(this, topCard);
    }
}
