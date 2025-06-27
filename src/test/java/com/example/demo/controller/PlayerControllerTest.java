package com.example.demo.controller;

import com.example.demo.dto.PlayerRequest;
import com.example.demo.dto.UpdateCoinsRequest;
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
class PlayerControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        playerRepository.deleteAll();
    }

    @Test
    void createPlayer_ShouldReturnCreatedPlayer() throws Exception {
        PlayerRequest request = new PlayerRequest("TestPlayer", 1000);

        mockMvc.perform(post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName", is("TestPlayer")))
                .andExpect(jsonPath("$.coins", is(1000)))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void createPlayer_WithDuplicateName_ShouldReturnBadRequest() throws Exception {
        playerRepository.save(new Player("ExistingPlayer", 500));
        
        PlayerRequest request = new PlayerRequest("ExistingPlayer", 1000);

        mockMvc.perform(post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPlayers_ShouldReturnAllPlayers() throws Exception {
        playerRepository.save(new Player("Player1", 100));
        playerRepository.save(new Player("Player2", 200));

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName", is("Player1")))
                .andExpect(jsonPath("$[1].playerName", is("Player2")));
    }

    @Test
    void getPlayerById_ShouldReturnPlayer() throws Exception {
        Player savedPlayer = playerRepository.save(new Player("TestPlayer", 1000));

        mockMvc.perform(get("/api/players/{id}", savedPlayer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName", is("TestPlayer")))
                .andExpect(jsonPath("$.coins", is(1000)));
    }

    @Test
    void getPlayerById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/players/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePlayerCoins_ShouldUpdateCoins() throws Exception {
        Player savedPlayer = playerRepository.save(new Player("TestPlayer", 1000));
        UpdateCoinsRequest request = new UpdateCoinsRequest(1500);

        mockMvc.perform(patch("/api/players/{id}/coins", savedPlayer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins", is(1500)));
    }

    @Test
    void addCoinsToPlayer_ShouldAddCoins() throws Exception {
        Player savedPlayer = playerRepository.save(new Player("TestPlayer", 1000));
        UpdateCoinsRequest request = new UpdateCoinsRequest(500);

        mockMvc.perform(patch("/api/players/{id}/add-coins", savedPlayer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins", is(1500)));
    }

    @Test
    void deletePlayer_ShouldDeletePlayer() throws Exception {
        Player savedPlayer = playerRepository.save(new Player("TestPlayer", 1000));

        mockMvc.perform(delete("/api/players/{id}", savedPlayer.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/players/{id}", savedPlayer.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLeaderboard_ShouldReturnPlayersOrderedByCoins() throws Exception {
        playerRepository.save(new Player("Player1", 100));
        playerRepository.save(new Player("Player2", 300));
        playerRepository.save(new Player("Player3", 200));

        mockMvc.perform(get("/api/players/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].coins", is(300)))
                .andExpect(jsonPath("$[1].coins", is(200)))
                .andExpect(jsonPath("$[2].coins", is(100)));
    }
}
