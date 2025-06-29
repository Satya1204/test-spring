package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optimized event data classes that send only the necessary information
 */
public class OptimizedEventData {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerJoined {
        private String playerName;
        private Integer playerOrder;
        private Integer totalPlayers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameStarted {
        private Integer totalPlayers;
        private Long currentPlayerId;
        private String currentPlayerName;
        private TopCard topCard;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameUpdate {
        private Integer totalPlayers;
        private Long currentPlayerId;
        private String currentPlayerName;
        private TopCard topCard;
        private String gameStatus;
        private String direction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardPlayed {
        private Long cardId;
        private TopCard newTopCard;
        private Long nextPlayerId;
        private String nextPlayerName;
        private Integer cardsRemaining;
        private String direction; // CLOCKWISE or COUNTER_CLOCKWISE
        private Boolean gameEnded;
        private Long winnerId;
        private String winnerName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDrawn {
        private Integer cardsDrawn;
        private Integer totalCardsInHand;
        private Long nextPlayerId;
        private String nextPlayerName;
        private Integer deckSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnoCalled {
        private String playerName;
        private Integer cardsRemaining;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameWon {
        private String winnerName;
        private Integer finalScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerLeft {
        private String playerName;
        private Integer remainingPlayers;
        private Long newCurrentPlayerId;
        private String newCurrentPlayerName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCard {
        private String color;
        private String cardType;
        private Integer value;
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerUpdate {
        private Long playerId;
        private String playerName;
        private Integer cardsCount;
        private Boolean hasCalledUno;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnSkipped {
        private Long skippedPlayerId;
        private String skippedPlayerName;
        private Long nextPlayerId;
        private String nextPlayerName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectionChanged {
        private String newDirection;
        private Long nextPlayerId;
        private String nextPlayerName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorChanged {
        private String newColor;
        private Long nextPlayerId;
        private String nextPlayerName;
    }
}
