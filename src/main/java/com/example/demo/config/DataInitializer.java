package com.example.demo.config;

import com.example.demo.entity.Player;
import com.example.demo.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final PlayerRepository playerRepository;
    
    @Override
    public void run(String... args) throws Exception {
        if (playerRepository.count() == 0) {
            // Initialize with some sample data
            playerRepository.save(new Player("Alice", 1500));
            playerRepository.save(new Player("Bob", 2300));
            playerRepository.save(new Player("Charlie", 750));
            playerRepository.save(new Player("Diana", 3200));
            playerRepository.save(new Player("Eve", 950));
            
            System.out.println("Sample player data initialized!");
        }
    }
}
