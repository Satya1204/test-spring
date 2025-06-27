package com.example.demo.util;

import com.example.demo.entity.Card;
import com.example.demo.entity.Game;
import com.example.demo.entity.GamePlayer;
import com.example.demo.enums.CardColor;
import com.example.demo.enums.CardType;

import java.util.List;

public class UnoGameRules {
    
    public static boolean canPlayCard(Card cardToPlay, Card topCard) {
        if (cardToPlay == null || topCard == null) {
            return false;
        }
        
        // Wild cards can always be played
        if (cardToPlay.getCardType() == CardType.WILD || 
            cardToPlay.getCardType() == CardType.WILD_DRAW_FOUR) {
            return true;
        }
        
        // Can play if same color
        if (cardToPlay.getColor() == topCard.getColor()) {
            return true;
        }
        
        // Can play if same type (for action cards)
        if (cardToPlay.getCardType() == topCard.getCardType() && 
            cardToPlay.getCardType() != CardType.NUMBER) {
            return true;
        }
        
        // Can play if same number (for number cards)
        if (cardToPlay.getCardType() == CardType.NUMBER && 
            topCard.getCardType() == CardType.NUMBER) {
            return cardToPlay.getValue().equals(topCard.getValue());
        }
        
        return false;
    }
    
    public static boolean isValidColorChoice(CardColor color) {
        return color == CardColor.RED || color == CardColor.BLUE || 
               color == CardColor.GREEN || color == CardColor.YELLOW;
    }
    
    public static boolean requiresColorChoice(Card card) {
        return card.getCardType() == CardType.WILD || 
               card.getCardType() == CardType.WILD_DRAW_FOUR;
    }
    
    public static boolean isActionCard(Card card) {
        return card.getCardType() == CardType.SKIP || 
               card.getCardType() == CardType.REVERSE || 
               card.getCardType() == CardType.DRAW_TWO || 
               card.getCardType() == CardType.WILD || 
               card.getCardType() == CardType.WILD_DRAW_FOUR;
    }
    
    public static int getCardDrawCount(Card card) {
        switch (card.getCardType()) {
            case DRAW_TWO:
                return 2;
            case WILD_DRAW_FOUR:
                return 4;
            default:
                return 0;
        }
    }
    
    public static boolean causesSkip(Card card) {
        return card.getCardType() == CardType.SKIP || 
               card.getCardType() == CardType.DRAW_TWO || 
               card.getCardType() == CardType.WILD_DRAW_FOUR;
    }
    
    public static boolean causesReverse(Card card) {
        return card.getCardType() == CardType.REVERSE;
    }
    
    public static boolean shouldCallUno(GamePlayer player) {
        return player.getCardsCount() == 1 && !player.getHasCalledUno();
    }
    
    public static boolean hasWon(GamePlayer player) {
        return player.getCardsCount() == 0;
    }
    
    public static boolean canStartGame(Game game) {
        return game.getGamePlayers().size() >= game.getMinPlayers() && 
               game.getGamePlayers().size() <= game.getMaxPlayers();
    }
    
    public static boolean isGameFull(Game game) {
        return game.getGamePlayers().size() >= game.getMaxPlayers();
    }
    
    public static boolean hasPlayableCard(List<Card> hand, Card topCard) {
        return hand.stream().anyMatch(card -> canPlayCard(card, topCard));
    }
    
    public static int calculateScore(List<Card> hand) {
        return hand.stream().mapToInt(UnoGameRules::getCardPoints).sum();
    }
    
    public static int getCardPoints(Card card) {
        switch (card.getCardType()) {
            case NUMBER:
                return card.getValue();
            case SKIP:
            case REVERSE:
            case DRAW_TWO:
                return 20;
            case WILD:
            case WILD_DRAW_FOUR:
                return 50;
            default:
                return 0;
        }
    }
    
    public static boolean isValidFirstCard(Card card) {
        // First card cannot be a wild card or action card
        return card.getCardType() == CardType.NUMBER;
    }
    
    public static String getCardDescription(Card card) {
        if (card.getCardType() == CardType.NUMBER) {
            return card.getColor().name() + " " + card.getValue();
        } else if (card.getCardType() == CardType.WILD || 
                   card.getCardType() == CardType.WILD_DRAW_FOUR) {
            return card.getCardType().name().replace("_", " ");
        } else {
            return card.getColor().name() + " " + card.getCardType().name().replace("_", " ");
        }
    }
    
    public static boolean canChallengeWildDrawFour(Card playedCard, List<Card> previousPlayerHand, Card topCard) {
        // A Wild Draw Four can be challenged if the player who played it
        // had a card of the same color as the top card
        if (playedCard.getCardType() != CardType.WILD_DRAW_FOUR) {
            return false;
        }
        
        return previousPlayerHand.stream()
            .anyMatch(card -> card.getColor() == topCard.getColor());
    }
}
