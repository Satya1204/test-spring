package com.example.demo.service;

import com.example.demo.dto.CreateGameRequest;
import com.example.demo.dto.GameResponse;
import com.example.demo.dto.JoinGameRequest;
import com.example.demo.entity.*;
import com.example.demo.enums.*;
import com.example.demo.repository.*;
import com.example.demo.util.UnoGameRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final CardRepository cardRepository;
    private final GameEventRepository gameEventRepository;
    private final PlayerRepository playerRepository;
    private final OptimizedWebSocketService optimizedWebSocketService;
    
    public GameResponse createGame(CreateGameRequest request) {
        Player creator = playerRepository.findById(request.getPlayerId())
            .orElseThrow(() -> new RuntimeException("Player not found"));
        
        String gameCode = generateGameCode();
        Game game = new Game(gameCode, creator);
        game.setMaxPlayers(request.getMaxPlayers());
        game.setMinPlayers(request.getMinPlayers());
        
        game = gameRepository.save(game);

        // Add creator as first player
        GamePlayer gamePlayer = new GamePlayer(game, creator, 0);
        gamePlayer = gamePlayerRepository.save(gamePlayer);
        game.getGamePlayers().add(gamePlayer);
        
        // Log game creation event
        logGameEvent(game, creator, "GAME_CREATED", 
            String.format("{\"gameCode\":\"%s\",\"maxPlayers\":%d}", gameCode, request.getMaxPlayers()));
        
        return GameResponse.fromEntity(game, creator.getId());
    }
    
    public GameResponse joinGame(JoinGameRequest request) {
        Game game = gameRepository.findByGameCode(request.getGameCode())
            .orElseThrow(() -> new RuntimeException("Game not found"));
        
        Player player = playerRepository.findById(request.getPlayerId())
            .orElseThrow(() -> new RuntimeException("Player not found"));
        
        // Check if game is joinable
        if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new RuntimeException("Game is not accepting new players");
        }
        
        if (game.isFull()) {
            throw new RuntimeException("Game is full");
        }
        
        // Check if player already in game
        if (gamePlayerRepository.existsByGameIdAndPlayerId(game.getId(), player.getId())) {
            throw new RuntimeException("Player already in this game");
        }
        
        // Add player to game
        int playerOrder = gamePlayerRepository.countActivePlayersByGame(game.getId());
        GamePlayer gamePlayer = new GamePlayer(game, player, playerOrder);
        gamePlayer = gamePlayerRepository.save(gamePlayer);
        game.getGamePlayers().add(gamePlayer);
        
        // Log join event
        logGameEvent(game, player, "PLAYER_JOINED", 
            String.format("{\"playerName\":\"%s\",\"playerOrder\":%d}", player.getPlayerName(), playerOrder));
        
        // Get updated game state for WebSocket broadcast
        Game updatedGame = gameRepository.findById(game.getId()).orElse(game);
        GameResponse gameResponse = GameResponse.fromEntity(updatedGame, player.getId());
        
        // Broadcast PLAYER_JOINED event
        optimizedWebSocketService.broadcastPlayerJoined(
                game.getGameCode(),
                player.getId(),
                player.getPlayerName(),
                playerOrder,
                gameResponse.getPlayers().size());
        
        // Broadcast GAME_UPDATE event with current player information
        optimizedWebSocketService.broadcastGameUpdate(game.getGameCode(), gameResponse);
        
        // Start game if we have enough players
        if (game.canStart() && gamePlayerRepository.countActivePlayersByGame(game.getId()) >= game.getMinPlayers()) {
            startGame(game);
        }
        
        return gameResponse;
    }
    
    private void startGame(Game game) {
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        
        // Create and shuffle deck
        createDeck(game);
        
        // Deal cards to players
        dealInitialCards(game);
        
        // Set first card
        setFirstCard(game);
        
        gameRepository.save(game);
        
        // Log game start event
        logGameEvent(game, null, "GAME_STARTED", 
            String.format("{\"playerCount\":%d}", game.getGamePlayers().size()));
        
        // Get updated game state for WebSocket broadcast
        Game updatedGame = gameRepository.findById(game.getId()).orElse(game);
        GameResponse gameResponse = GameResponse.fromEntity(updatedGame, null);
        
        // Broadcast GAME_STARTED event with current player information
        optimizedWebSocketService.broadcastGameStarted(game.getGameCode(), gameResponse);
    }
    
    private void createDeck(Game game) {
        List<Card> deck = new ArrayList<>();
        
        // Create number cards (0-9) for each color
        for (CardColor color : Arrays.asList(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)) {
            // One 0 card per color
            deck.add(new Card(CardType.NUMBER, color, 0));
            
            // Two of each number 1-9 per color
            for (int i = 1; i <= 9; i++) {
                deck.add(new Card(CardType.NUMBER, color, i));
                deck.add(new Card(CardType.NUMBER, color, i));
            }
            
            // Two of each action card per color
            deck.add(new Card(CardType.SKIP, color, null));
            deck.add(new Card(CardType.SKIP, color, null));
            deck.add(new Card(CardType.REVERSE, color, null));
            deck.add(new Card(CardType.REVERSE, color, null));
            deck.add(new Card(CardType.DRAW_TWO, color, null));
            deck.add(new Card(CardType.DRAW_TWO, color, null));
        }
        
        // Add wild cards
        for (int i = 0; i < 4; i++) {
            deck.add(new Card(CardType.WILD, CardColor.WILD, null));
            deck.add(new Card(CardType.WILD_DRAW_FOUR, CardColor.WILD, null));
        }
        
        // Set game reference and save
        deck.forEach(card -> card.setGame(game));
        cardRepository.saveAll(deck);
    }
    
    private void dealInitialCards(Game game) {
        List<Card> deckCards = cardRepository.findDeckCardsRandomOrder(game.getId());
        List<GamePlayer> players = gamePlayerRepository.findActivePlayersByGame(game.getId());
        
        int cardIndex = 0;
        
        // Deal 7 cards to each player
        for (GamePlayer gamePlayer : players) {
            for (int i = 0; i < 7; i++) {
                if (cardIndex < deckCards.size()) {
                    Card card = deckCards.get(cardIndex++);
                    card.setPlayer(gamePlayer.getPlayer());
                    card.setIsInDeck(false);
                    card.setPositionInHand(i);
                    cardRepository.save(card);
                }
            }
            gamePlayer.setCardsCount(7);
            gamePlayerRepository.save(gamePlayer);
        }
    }
    
    private void setFirstCard(Game game) {
        List<Card> deckCards = cardRepository.findDeckCardsRandomOrder(game.getId());
        
        // Find first non-wild card to start with
        Card firstCard = deckCards.stream()
            .filter(card -> card.getCardType() != CardType.WILD && 
                           card.getCardType() != CardType.WILD_DRAW_FOUR)
            .findFirst()
            .orElse(deckCards.get(0)); // Fallback to any card
        
        firstCard.setIsInDeck(false);
        firstCard.setIsTopCard(true);
        firstCard.setPlayer(null);
        cardRepository.save(firstCard);
    }
    
    private String generateGameCode() {
        String code;
        do {
            code = String.format("%06d", new Random().nextInt(1000000));
        } while (gameRepository.existsByGameCode(code));
        return code;
    }
    
    private void logGameEvent(Game game, Player player, String eventType, String eventData) {
        Integer sequenceNumber = gameEventRepository.getLastSequenceNumber(game.getId()) + 1;
        GameEvent event = new GameEvent(game, player, eventType, eventData, sequenceNumber);
        gameEventRepository.save(event);
    }
    
    @Transactional(readOnly = true)
    public GameResponse getGame(String gameCode, Long playerId) {
        Game game = gameRepository.findByGameCode(gameCode)
            .orElseThrow(() -> new RuntimeException("Game not found"));
        return GameResponse.fromEntity(game, playerId);
    }
    
    @Transactional(readOnly = true)
    public List<GameResponse> getAvailableGames() {
        return gameRepository.findAvailableGames(GameStatus.WAITING_FOR_PLAYERS)
            .stream()
            .map(game -> GameResponse.fromEntity(game, null))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<GameResponse> getPlayerGames(Long playerId) {
        List<GameStatus> activeStatuses = Arrays.asList(
            GameStatus.WAITING_FOR_PLAYERS,
            GameStatus.IN_PROGRESS
        );

        return gameRepository.findPlayerActiveGames(playerId, activeStatuses)
            .stream()
            .map(game -> GameResponse.fromEntity(game, playerId))
            .collect(Collectors.toList());
    }

    public GameResponse playCard(Long cardId, Long playerId, String gameCode, CardColor chosenColor) {
        Game game = gameRepository.findByGameCode(gameCode)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is not in progress");
        }

        GamePlayer currentGamePlayer = game.getCurrentPlayer();
        if (currentGamePlayer == null || !currentGamePlayer.getPlayer().getId().equals(playerId)) {
            throw new RuntimeException("It's not your turn");
        }

        Card cardToPlay = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        // Validate card belongs to player
        if (!cardToPlay.getPlayer().getId().equals(playerId)) {
            throw new RuntimeException("Card does not belong to you");
        }

        // Validate card can be played
        Card topCard = game.getTopCard();
        if (!UnoGameRules.canPlayCard(cardToPlay, topCard)) {
            throw new RuntimeException("Card cannot be played on current top card");
        }

        // Validate wild card color choice
        if (UnoGameRules.requiresColorChoice(cardToPlay) && !UnoGameRules.isValidColorChoice(chosenColor)) {
            throw new RuntimeException("Valid color choice required for wild cards");
        }

        // Play the card
        playCardAction(game, cardToPlay, currentGamePlayer, chosenColor);

        return GameResponse.fromEntity(gameRepository.findById(game.getId()).orElse(game), playerId);
    }

    private void playCardAction(Game game, Card cardToPlay, GamePlayer gamePlayer, CardColor chosenColor) {
        // Remove card from player's hand
        cardToPlay.setPlayer(null);
        cardToPlay.setPositionInHand(null);

        // Set as new top card
        Card oldTopCard = game.getTopCard();
        if (oldTopCard != null) {
            oldTopCard.setIsTopCard(false);
            cardRepository.save(oldTopCard);
        }

        cardToPlay.setIsTopCard(true);

        // Handle wild card color choice
        if ((cardToPlay.getCardType() == CardType.WILD || cardToPlay.getCardType() == CardType.WILD_DRAW_FOUR)
            && chosenColor != null) {
            cardToPlay.setColor(chosenColor);
        }

        cardRepository.save(cardToPlay);

        // Update player's card count
        gamePlayer.setCardsCount(gamePlayer.getCardsCount() - 1);
        gamePlayer.setHasCalledUno(false); // Reset UNO call
        gamePlayerRepository.save(gamePlayer);

        // Log the card play
        String eventData = String.format("{\"cardType\":\"%s\",\"color\":\"%s\",\"value\":%s,\"chosenColor\":\"%s\"}",
            cardToPlay.getCardType(), cardToPlay.getColor(), cardToPlay.getValue(), chosenColor);
        logGameEvent(game, gamePlayer.getPlayer(), "CARD_PLAYED", eventData);

        // Handle special card effects
        handleCardEffect(game, cardToPlay, gamePlayer);

        // Check for win condition
        if (UnoGameRules.hasWon(gamePlayer)) {
            endGame(game, gamePlayer.getPlayer());
            return;
        }

        // Move to next player (unless it's a skip card)
        if (!UnoGameRules.causesSkip(cardToPlay)) {
            game.moveToNextPlayer();
        } else {
            // Skip current player, move to next
            game.moveToNextPlayer();
            game.moveToNextPlayer();
        }

        gameRepository.save(game);
    }

    private void handleCardEffect(Game game, Card playedCard, GamePlayer currentPlayer) {
        if (UnoGameRules.causesReverse(playedCard)) {
            game.reverseDirection();
            logGameEvent(game, currentPlayer.getPlayer(), "DIRECTION_REVERSED", "{}");
        }

        int drawCount = UnoGameRules.getCardDrawCount(playedCard);
        if (drawCount > 0) {
            GamePlayer nextPlayer = getNextPlayer(game);
            drawCardsForPlayer(game, nextPlayer, drawCount);
            logGameEvent(game, nextPlayer.getPlayer(), "FORCED_DRAW",
                String.format("{\"count\":%d}", drawCount));
            // Skip the next player's turn
            game.moveToNextPlayer();
        }
    }

    private GamePlayer getNextPlayer(Game game) {
        int nextIndex;
        if (game.getDirection() == GameDirection.CLOCKWISE) {
            nextIndex = (game.getCurrentPlayerIndex() + 1) % game.getGamePlayers().size();
        } else {
            nextIndex = (game.getCurrentPlayerIndex() - 1 + game.getGamePlayers().size()) % game.getGamePlayers().size();
        }
        return game.getGamePlayers().get(nextIndex);
    }

    public GameResponse drawCard(Long playerId, String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is not in progress");
        }

        GamePlayer currentGamePlayer = game.getCurrentPlayer();
        if (currentGamePlayer == null || !currentGamePlayer.getPlayer().getId().equals(playerId)) {
            throw new RuntimeException("It's not your turn");
        }

        drawCardsForPlayer(game, currentGamePlayer, 1);

        // Move to next player
        game.moveToNextPlayer();
        gameRepository.save(game);

        return GameResponse.fromEntity(game, playerId);
    }

    private void drawCardsForPlayer(Game game, GamePlayer gamePlayer, int count) {
        List<Card> deckCards = cardRepository.findDeckCardsRandomOrder(game.getId());

        if (deckCards.size() < count) {
            // Reshuffle discard pile into deck if needed
            reshuffleDiscardPile(game);
            deckCards = cardRepository.findDeckCardsRandomOrder(game.getId());
        }

        for (int i = 0; i < count && i < deckCards.size(); i++) {
            Card card = deckCards.get(i);
            card.setPlayer(gamePlayer.getPlayer());
            card.setIsInDeck(false);
            card.setPositionInHand(gamePlayer.getCardsCount() + i);
            cardRepository.save(card);
        }

        gamePlayer.setCardsCount(gamePlayer.getCardsCount() + count);
        gamePlayerRepository.save(gamePlayer);

        logGameEvent(game, gamePlayer.getPlayer(), "CARD_DRAWN", String.format("{\"count\":%d}", count));
    }

    private void reshuffleDiscardPile(Game game) {
        List<Card> discardCards = cardRepository.findDiscardPileCards(game.getId());
        discardCards.forEach(card -> {
            card.setIsInDeck(true);
            card.setPlayer(null);
            cardRepository.save(card);
        });

        logGameEvent(game, null, "DECK_RESHUFFLED", String.format("{\"cardCount\":%d}", discardCards.size()));
    }

    private void endGame(Game game, Player winner) {
        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishedAt(LocalDateTime.now());
        gameRepository.save(game);

        logGameEvent(game, winner, "GAME_WON", String.format("{\"winnerName\":\"%s\"}", winner.getPlayerName()));
    }

    public GameResponse callUno(Long playerId, String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game is not in progress");
        }

        GamePlayer gamePlayer = gamePlayerRepository.findByGameIdAndPlayerId(game.getId(), playerId)
            .orElseThrow(() -> new RuntimeException("Player not in this game"));

        if (gamePlayer.getCardsCount() != 1) {
            throw new RuntimeException("You can only call UNO when you have exactly 1 card");
        }

        gamePlayer.setHasCalledUno(true);
        gamePlayerRepository.save(gamePlayer);

        logGameEvent(game, gamePlayer.getPlayer(), "UNO_CALLED",
            String.format("{\"playerName\":\"%s\"}", gamePlayer.getPlayer().getPlayerName()));

        return GameResponse.fromEntity(game, playerId);
    }

    public GameResponse leaveGame(Long playerId, String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode)
            .orElseThrow(() -> new RuntimeException("Game not found"));

        GamePlayer gamePlayer = gamePlayerRepository.findByGameIdAndPlayerId(game.getId(), playerId)
            .orElseThrow(() -> new RuntimeException("Player not in this game"));

        gamePlayer.setIsActive(false);
        gamePlayerRepository.save(gamePlayer);

        logGameEvent(game, gamePlayer.getPlayer(), "PLAYER_LEFT",
            String.format("{\"playerName\":\"%s\"}", gamePlayer.getPlayer().getPlayerName()));

        // If game hasn't started and creator left, cancel the game
        if (game.getStatus() == GameStatus.WAITING_FOR_PLAYERS &&
            game.getCreatedBy().getId().equals(playerId)) {
            game.setStatus(GameStatus.CANCELLED);
            gameRepository.save(game);
        }

        // If game is in progress and not enough active players, end the game
        Integer activePlayers = gamePlayerRepository.countActivePlayersByGame(game.getId());
        if (game.getStatus() == GameStatus.IN_PROGRESS && activePlayers < 2) {
            game.setStatus(GameStatus.FINISHED);
            game.setFinishedAt(LocalDateTime.now());

            // Find a winner from remaining active players
            List<GamePlayer> remainingPlayers = gamePlayerRepository.findActivePlayersByGame(game.getId());
            if (!remainingPlayers.isEmpty()) {
                game.setWinner(remainingPlayers.get(0).getPlayer());
            }

            gameRepository.save(game);
            logGameEvent(game, null, "GAME_ENDED_INSUFFICIENT_PLAYERS", "{}");
        }

        return GameResponse.fromEntity(game, playerId);
    }
}
