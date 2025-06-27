package com.example.demo.service;

import com.example.demo.dto.PlayerRequest;
import com.example.demo.dto.PlayerResponse;
import com.example.demo.entity.Player;
import com.example.demo.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {
    
    private final PlayerRepository playerRepository;
    
    public PlayerResponse createPlayer(PlayerRequest request) {
        if (playerRepository.existsByPlayerName(request.getPlayerName())) {
            throw new RuntimeException("Player with name '" + request.getPlayerName() + "' already exists");
        }
        
        Player player = new Player(request.getPlayerName(), request.getCoins());
        Player savedPlayer = playerRepository.save(player);
        return PlayerResponse.fromEntity(savedPlayer);
    }
    
    @Transactional(readOnly = true)
    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(PlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
        return PlayerResponse.fromEntity(player);
    }
    
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerByName(String playerName) {
        Player player = playerRepository.findByPlayerName(playerName)
                .orElseThrow(() -> new RuntimeException("Player not found with name: " + playerName));
        return PlayerResponse.fromEntity(player);
    }
    
    public PlayerResponse updatePlayer(Long id, PlayerRequest request) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
        
        // Check if the new name already exists (and it's not the same player)
        if (!player.getPlayerName().equals(request.getPlayerName()) && 
            playerRepository.existsByPlayerName(request.getPlayerName())) {
            throw new RuntimeException("Player with name '" + request.getPlayerName() + "' already exists");
        }
        
        player.setPlayerName(request.getPlayerName());
        player.setCoins(request.getCoins());
        
        Player updatedPlayer = playerRepository.save(player);
        return PlayerResponse.fromEntity(updatedPlayer);
    }
    
    public PlayerResponse updatePlayerCoins(Long id, Integer coins) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
        
        player.setCoins(coins);
        Player updatedPlayer = playerRepository.save(player);
        return PlayerResponse.fromEntity(updatedPlayer);
    }
    
    public PlayerResponse addCoinsToPlayer(Long id, Integer coinsToAdd) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
        
        player.setCoins(player.getCoins() + coinsToAdd);
        Player updatedPlayer = playerRepository.save(player);
        return PlayerResponse.fromEntity(updatedPlayer);
    }
    
    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new RuntimeException("Player not found with id: " + id);
        }
        playerRepository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public List<PlayerResponse> getPlayersOrderByCoins() {
        return playerRepository.findAllOrderByCoinsDesc()
                .stream()
                .map(PlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PlayerResponse> getPlayersWithMinCoins(Integer minCoins) {
        return playerRepository.findPlayersWithMinCoins(minCoins)
                .stream()
                .map(PlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
