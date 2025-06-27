package com.example.demo.controller;

import com.example.demo.dto.CreateGameRequest;
import com.example.demo.dto.JoinGameRequest;
import com.example.demo.entity.Player;
import com.example.demo.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Transactional
@ActiveProfiles("test")
class GameControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Player testPlayer1;
    private Player testPlayer2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up and create test players
        playerRepository.deleteAll();
        testPlayer1 = playerRepository.save(new Player("GameTestPlayer1", 1000));
        testPlayer2 = playerRepository.save(new Player("GameTestPlayer2", 1000));
    }

    @Test
    void createGame_ShouldReturnCreatedGame() throws Exception {
        CreateGameRequest request = new CreateGameRequest(testPlayer1.getId(), 4, 2);

        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameCode", notNullValue()))
                .andExpect(jsonPath("$.status", is("WAITING_FOR_PLAYERS")))
                .andExpect(jsonPath("$.maxPlayers", is(4)))
                .andExpect(jsonPath("$.minPlayers", is(2)))
                .andExpect(jsonPath("$.players", hasSize(1)))
                .andExpect(jsonPath("$.createdBy.id", is(testPlayer1.getId().intValue())));
    }

    @Test
    void createGame_WithInvalidPlayer_ShouldReturnBadRequest() throws Exception {
        CreateGameRequest request = new CreateGameRequest(999L, 4, 2);

        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinGame_ShouldAddPlayerToGame() throws Exception {
        // First create a game
        CreateGameRequest createRequest = new CreateGameRequest(testPlayer1.getId(), 4, 2);
        String createResponse = mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract game code from response
        String gameCode = objectMapper.readTree(createResponse).get("gameCode").asText();

        // Join the game
        JoinGameRequest joinRequest = new JoinGameRequest(gameCode, testPlayer2.getId());

        mockMvc.perform(post("/api/games/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS"))) // Should start with 2 players
                .andExpect(jsonPath("$.startedAt", notNullValue()));
    }

    @Test
    void joinGame_WithInvalidGameCode_ShouldReturnBadRequest() throws Exception {
        JoinGameRequest request = new JoinGameRequest("INVALID", testPlayer1.getId());

        mockMvc.perform(post("/api/games/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGame_ShouldReturnGameDetails() throws Exception {
        // Create a game first
        CreateGameRequest createRequest = new CreateGameRequest(testPlayer1.getId(), 4, 2);
        String createResponse = mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String gameCode = objectMapper.readTree(createResponse).get("gameCode").asText();

        // Get the game
        mockMvc.perform(get("/api/games/{gameCode}", gameCode)
                .param("playerId", testPlayer1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameCode", is(gameCode)))
                .andExpect(jsonPath("$.status", is("WAITING_FOR_PLAYERS")));
    }

    @Test
    void getGame_WithInvalidGameCode_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/games/{gameCode}", "INVALID")
                .param("playerId", testPlayer1.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAvailableGames_ShouldReturnWaitingGames() throws Exception {
        // Create a game
        CreateGameRequest createRequest = new CreateGameRequest(testPlayer1.getId(), 4, 2);
        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Get available games
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].status", is("WAITING_FOR_PLAYERS")));
    }

    @Test
    void getPlayerGames_ShouldReturnPlayerActiveGames() throws Exception {
        // Create a game
        CreateGameRequest createRequest = new CreateGameRequest(testPlayer1.getId(), 4, 2);
        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Get player's games
        mockMvc.perform(get("/api/games/player/{playerId}", testPlayer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getPlayerGames_WithInvalidPlayer_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/games/player/{playerId}", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
