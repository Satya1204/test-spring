package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "player_name", nullable = false, unique = true)
    private String playerName;
    
    @Column(name = "coins", nullable = false)
    private Integer coins;
    
    public Player(String playerName, Integer coins) {
        this.playerName = playerName;
        this.coins = coins;
    }
}
