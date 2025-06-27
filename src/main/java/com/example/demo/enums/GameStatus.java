package com.example.demo.enums;

public enum GameStatus {
    WAITING_FOR_PLAYERS,  // Game created, waiting for players to join
    IN_PROGRESS,          // Game is actively being played
    FINISHED,             // Game has ended
    CANCELLED             // Game was cancelled
}
