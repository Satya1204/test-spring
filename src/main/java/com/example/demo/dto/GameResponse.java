package com.example.demo.dto;

import com.example.demo.entity.Game;
import com.example.demo.enums.GameDirection;
import com.example.demo.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {
    
    private Long id;
    private String gameCode;
    private GameStatus status;
    private Integer maxPlayers;
    private Integer minPlayers;
    private Integer currentPlayerIndex;
    private GameDirection direction;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private PlayerResponse winner;
    private PlayerResponse createdBy;
    private List<GamePlayerDto> players;
    private CardDto topCard;
    private Integer deckSize;
    
    public static GameResponse fromEntity(Game game, Long currentPlayerId) {
        GameResponse response = new GameResponse();
        response.setId(game.getId());
        response.setGameCode(game.getGameCode());
        response.setStatus(game.getStatus());
        response.setMaxPlayers(game.getMaxPlayers());
        response.setMinPlayers(game.getMinPlayers());
        response.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        response.setDirection(game.getDirection());
        response.setCreatedAt(game.getCreatedAt());
        response.setStartedAt(game.getStartedAt());
        response.setFinishedAt(game.getFinishedAt());
        
        if (game.getWinner() != null) {
            response.setWinner(PlayerResponse.fromEntity(game.getWinner()));
        }
        
        if (game.getCreatedBy() != null) {
            response.setCreatedBy(PlayerResponse.fromEntity(game.getCreatedBy()));
        }
        
        // Convert game players, include hand only for current player
        response.setPlayers(game.getGamePlayers().stream()
            .map(gp -> GamePlayerDto.fromEntity(gp, 
                currentPlayerId != null && gp.getPlayer().getId().equals(currentPlayerId)))
            .toList());
        
        // Set top card
        if (game.getTopCard() != null) {
            response.setTopCard(CardDto.fromEntity(game.getTopCard()));
        }
        
        // Count deck size
        response.setDeckSize((int) game.getCards().stream()
            .filter(card -> card.getIsInDeck())
            .count());
        
        return response;
    }
}
