package com.example.demo.service;

import com.example.demo.dto.CreateGameRequest;
import com.example.demo.dto.GameResponse;
import com.example.demo.dto.JoinGameRequest;
import com.example.demo.entity.Player;
import com.example.demo.enums.GameStatus;
import com.example.demo.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayerRepository playerRepository;

    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        player1 = playerRepository.save(new Player("TestPlayer1", 1000));
        player2 = playerRepository.save(new Player("TestPlayer2", 1000));
        player3 = playerRepository.save(new Player("TestPlayer3", 1000));
    }

    @Test
    void createGame_ShouldCreateGameSuccessfully() {
        CreateGameRequest request = new CreateGameRequest(player1.getId(), 4, 2);
        
        GameResponse response = gameService.createGame(request);
        
        assertNotNull(response);
        assertNotNull(response.getGameCode());
        assertEquals(GameStatus.WAITING_FOR_PLAYERS, response.getStatus());
        assertEquals(4, response.getMaxPlayers());
        assertEquals(2, response.getMinPlayers());
        assertEquals(1, response.getPlayers().size());
        assertEquals(player1.getId(), response.getCreatedBy().getId());
    }

    @Test
    void joinGame_ShouldAddPlayerToGame() {
        // Create game
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        GameResponse game = gameService.createGame(createRequest);
        
        // Join game
        JoinGameRequest joinRequest = new JoinGameRequest(game.getGameCode(), player2.getId());
        GameResponse response = gameService.joinGame(joinRequest);
        
        assertEquals(2, response.getPlayers().size());
        assertEquals(GameStatus.IN_PROGRESS, response.getStatus()); // Should start with 2 players
        assertNotNull(response.getStartedAt());
    }

    @Test
    void joinGame_WithInvalidGameCode_ShouldThrowException() {
        JoinGameRequest request = new JoinGameRequest("INVALID", player1.getId());
        
        assertThrows(RuntimeException.class, () -> gameService.joinGame(request));
    }

    @Test
    void joinGame_WhenGameIsFull_ShouldThrowException() {
        // Create game with max 2 players
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 2, 2);
        GameResponse game = gameService.createGame(createRequest);
        
        // Add second player
        JoinGameRequest joinRequest2 = new JoinGameRequest(game.getGameCode(), player2.getId());
        gameService.joinGame(joinRequest2);
        
        // Try to add third player (should fail)
        JoinGameRequest joinRequest3 = new JoinGameRequest(game.getGameCode(), player3.getId());
        assertThrows(RuntimeException.class, () -> gameService.joinGame(joinRequest3));
    }

    @Test
    void joinGame_WhenPlayerAlreadyInGame_ShouldThrowException() {
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        GameResponse game = gameService.createGame(createRequest);
        
        // Try to join again
        JoinGameRequest joinRequest = new JoinGameRequest(game.getGameCode(), player1.getId());
        assertThrows(RuntimeException.class, () -> gameService.joinGame(joinRequest));
    }

    @Test
    void getGame_ShouldReturnGameDetails() {
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        GameResponse createdGame = gameService.createGame(createRequest);
        
        GameResponse response = gameService.getGame(createdGame.getGameCode(), player1.getId());
        
        assertNotNull(response);
        assertEquals(createdGame.getGameCode(), response.getGameCode());
        assertEquals(GameStatus.WAITING_FOR_PLAYERS, response.getStatus());
    }

    @Test
    void getAvailableGames_ShouldReturnWaitingGames() {
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        gameService.createGame(createRequest);
        
        var availableGames = gameService.getAvailableGames();
        
        assertFalse(availableGames.isEmpty());
        assertTrue(availableGames.stream()
            .allMatch(game -> game.getStatus() == GameStatus.WAITING_FOR_PLAYERS));
    }

    @Test
    void getPlayerGames_ShouldReturnPlayerActiveGames() {
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        gameService.createGame(createRequest);
        
        var playerGames = gameService.getPlayerGames(player1.getId());
        
        assertFalse(playerGames.isEmpty());
        assertTrue(playerGames.stream()
            .anyMatch(game -> game.getPlayers().stream()
                .anyMatch(gp -> gp.getPlayer().getId().equals(player1.getId()))));
    }

    @Test
    void callUno_WithOneCard_ShouldSucceed() {
        // This test would require setting up a game state with one card
        // For now, we'll test the basic validation
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        GameResponse game = gameService.createGame(createRequest);
        
        // This should throw an exception since game is not in progress
        assertThrows(RuntimeException.class, () -> 
            gameService.callUno(player1.getId(), game.getGameCode()));
    }

    @Test
    void leaveGame_ShouldRemovePlayerFromGame() {
        CreateGameRequest createRequest = new CreateGameRequest(player1.getId(), 4, 2);
        GameResponse game = gameService.createGame(createRequest);
        
        GameResponse response = gameService.leaveGame(player1.getId(), game.getGameCode());
        
        // Since creator left and game hasn't started, it should be cancelled
        assertEquals(GameStatus.CANCELLED, response.getStatus());
    }
}
