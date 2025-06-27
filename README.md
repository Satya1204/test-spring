# Multiplayer Uno Game - Backend API

A real-time multiplayer Uno card game built with Spring Boot, featuring WebSocket communication, game room management, and complete Uno game logic.

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Running the Application
```bash
# Clone and navigate to project
cd demo

# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The server will start on `http://localhost:8080`

## 📡 API Endpoints

### Player Management

#### Create Player
```http
POST /api/players
Content-Type: application/json

{
  "playerName": "JohnDoe"
}
```

**Response:**
```json
{
  "id": 1,
  "playerName": "JohnDoe",
  "coins": 1000
}
```

#### Get All Players
```http
GET /api/players
```

**Response:**
```json
[
  {
    "id": 1,
    "playerName": "JohnDoe",
    "coins": 1000
  }
]
```

### Game Management

#### Create Game
```http
POST /api/games
Content-Type: application/json

{
  "playerId": 1,
  "maxPlayers": 4,
  "minPlayers": 2
}
```

**Response:**
```json
{
  "id": 1,
  "gameCode": "ABC123",
  "status": "WAITING_FOR_PLAYERS",
  "maxPlayers": 4,
  "minPlayers": 2,
  "currentPlayerIndex": 0,
  "direction": "CLOCKWISE",
  "players": [
    {
      "id": 1,
      "player": {
        "id": 1,
        "playerName": "JohnDoe",
        "coins": 1000
      },
      "cardsCount": 0,
      "playerOrder": 0,
      "isActive": true,
      "hasCalledUno": false
    }
  ],
  "topCard": null,
  "createdBy": {
    "id": 1,
    "playerName": "JohnDoe",
    "coins": 1000
  },
  "createdAt": "2025-06-27T07:55:10.123Z",
  "startedAt": null,
  "finishedAt": null,
  "winner": null
}
```

#### Join Game
```http
POST /api/games/join
Content-Type: application/json

{
  "gameCode": "ABC123",
  "playerId": 2
}
```

#### Get Game Details
```http
GET /api/games/{gameCode}?playerId={playerId}
```

#### Get Available Games
```http
GET /api/games
```

#### Get Player's Active Games
```http
GET /api/games/player/{playerId}
```

## 🎮 Game Data Models

### Card Structure
```json
{
  "id": 1,
  "cardType": "NUMBER|SKIP|REVERSE|DRAW_TWO|WILD|WILD_DRAW_FOUR",
  "color": "RED|BLUE|GREEN|YELLOW|WILD",
  "value": 5,
  "positionInHand": 0
}
```

### Game Status Values
- `WAITING_FOR_PLAYERS` - Game created, waiting for minimum players
- `IN_PROGRESS` - Game is active and being played
- `FINISHED` - Game completed with a winner
- `CANCELLED` - Game was cancelled

### Card Types
- `NUMBER` - Regular number cards (0-9)
- `SKIP` - Skip next player's turn
- `REVERSE` - Reverse play direction
- `DRAW_TWO` - Next player draws 2 cards and skips turn
- `WILD` - Can be played on any card, player chooses color
- `WILD_DRAW_FOUR` - Next player draws 4 cards, player chooses color

### Card Colors
- `RED`, `BLUE`, `GREEN`, `YELLOW` - Standard colors
- `WILD` - For wild cards (color chosen by player)

## 🔌 WebSocket Integration

### Connection
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to game updates
    stompClient.subscribe('/topic/game/{gameCode}', function(message) {
        const gameEvent = JSON.parse(message.body);
        handleGameEvent(gameEvent);
    });
});
```

### Game Actions via WebSocket

#### Join Game
```javascript
stompClient.send('/app/game/join', {}, JSON.stringify({
    gameCode: 'ABC123',
    playerId: 1
}));
```

#### Play Card
```javascript
stompClient.send('/app/game/play-card', {}, JSON.stringify({
    gameCode: 'ABC123',
    playerId: 1,
    cardId: 5,
    chosenColor: 'RED' // Only for wild cards
}));
```

#### Draw Card
```javascript
stompClient.send('/app/game/draw-card', {}, JSON.stringify({
    gameCode: 'ABC123',
    playerId: 1
}));
```

#### Call Uno
```javascript
stompClient.send('/app/game/call-uno', {}, JSON.stringify({
    gameCode: 'ABC123',
    playerId: 1
}));
```

### WebSocket Event Types

#### Game Update Event
```json
{
  "eventType": "GAME_UPDATE",
  "gameCode": "ABC123",
  "playerId": null,
  "playerName": null,
  "message": "Game state updated",
  "timestamp": "2025-06-27T07:55:10.123Z",
  "gameData": {
    // Full game object with current state
  }
}
```

#### Player Action Events
```json
{
  "eventType": "CARD_PLAYED|CARD_DRAWN|PLAYER_JOINED|UNO_CALLED|GAME_STARTED|GAME_FINISHED",
  "gameCode": "ABC123",
  "playerId": 1,
  "playerName": "JohnDoe",
  "message": "Player played a card",
  "timestamp": "2025-06-27T07:55:10.123Z",
  "gameData": {
    // Updated game state
  }
}
```

## 🎯 Game Flow

### 1. Game Creation
1. Player creates game with min/max player limits
2. Game gets unique 6-digit code
3. Creator automatically joins as first player
4. Game status: `WAITING_FOR_PLAYERS`

### 2. Players Joining
1. Players join using game code
2. When minimum players reached, game auto-starts
3. Each player gets 7 cards
4. First card placed on discard pile
5. Game status: `IN_PROGRESS`

### 3. Gameplay
1. Players take turns in order
2. Must play matching card (color, number, or action)
3. If no playable card, must draw from deck
4. Special cards trigger effects (skip, reverse, draw, etc.)
5. Player must call "Uno" when down to 1 card

### 4. Game End
1. First player to play all cards wins
2. Game status: `FINISHED`
3. Winner recorded in game data

## 🛠️ Frontend Implementation Tips

### State Management
- Track current game state from WebSocket updates
- Maintain player hand separately from game state
- Handle real-time updates for all players

### UI Components Needed
- **Game Lobby**: List available games, create/join functionality
- **Game Board**: Display current card, other players, game info
- **Player Hand**: Show player's cards with play/draw actions
- **Game Controls**: Uno button, color picker for wild cards
- **Player List**: Show all players, current turn indicator

### Key Frontend Logic
```javascript
// Example game state handling
function handleGameEvent(event) {
    switch(event.eventType) {
        case 'GAME_UPDATE':
            updateGameState(event.gameData);
            break;
        case 'CARD_PLAYED':
            showCardPlayed(event.playerName, event.gameData.topCard);
            break;
        case 'PLAYER_JOINED':
            showPlayerJoined(event.playerName);
            break;
        // Handle other events...
    }
}

// Check if card can be played
function canPlayCard(card, topCard) {
    return card.color === topCard.color || 
           card.value === topCard.value || 
           card.cardType === topCard.cardType ||
           card.cardType === 'WILD' || 
           card.cardType === 'WILD_DRAW_FOUR';
}
```

### Error Handling
- Handle WebSocket disconnections
- Validate moves before sending to server
- Show appropriate error messages for invalid actions
- Implement reconnection logic

## 🧪 Testing

Run the test suite:
```bash
./mvnw test
```

**Test Coverage**: 48 tests covering all game logic, API endpoints, and WebSocket functionality.

## 📝 Database

Uses H2 in-memory database for development. Data includes:
- Players with coins system
- Games with full state tracking
- Cards with positions and ownership
- Game events for history/replay

## 🎲 Complete Game Rules Implementation

### Card Play Rules
- **Number Cards**: Must match color or number of top card
- **Skip Cards**: Next player loses their turn
- **Reverse Cards**: Changes direction of play
- **Draw Two**: Next player draws 2 cards and loses turn
- **Wild Cards**: Can be played anytime, player chooses new color
- **Wild Draw Four**: Next player draws 4 cards, player chooses color

### Special Rules
- **Uno Call**: Player must call "Uno" when playing second-to-last card
- **Penalty**: If caught not calling Uno, player draws 2 cards
- **Stacking**: Draw cards can be stacked (if house rules enabled)
- **Valid First Card**: Game starts with a number card only

### Scoring System
- **Number Cards**: Face value (0-9 points)
- **Action Cards**: 20 points each (Skip, Reverse, Draw Two)
- **Wild Cards**: 50 points each (Wild, Wild Draw Four)

## 🎨 Frontend Implementation Examples

### React Component Structure
```jsx
// Main Game Component
function UnoGame({ gameCode, playerId }) {
    const [gameState, setGameState] = useState(null);
    const [playerHand, setPlayerHand] = useState([]);
    const [socket, setSocket] = useState(null);

    useEffect(() => {
        // Initialize WebSocket connection
        const stompClient = connectToGame(gameCode, handleGameUpdate);
        setSocket(stompClient);

        return () => stompClient.disconnect();
    }, [gameCode]);

    const handleGameUpdate = (event) => {
        setGameState(event.gameData);
        // Update player hand from game data
        const currentPlayer = event.gameData.players.find(p => p.player.id === playerId);
        if (currentPlayer) {
            setPlayerHand(currentPlayer.cards || []);
        }
    };

    const playCard = (card, chosenColor = null) => {
        socket.send('/app/game/play-card', {}, JSON.stringify({
            gameCode,
            playerId,
            cardId: card.id,
            chosenColor
        }));
    };

    return (
        <div className="uno-game">
            <GameBoard gameState={gameState} />
            <PlayerHand cards={playerHand} onPlayCard={playCard} topCard={gameState?.topCard} />
            <GameControls gameState={gameState} playerId={playerId} socket={socket} />
        </div>
    );
}
```

### Card Component
```jsx
function Card({ card, isPlayable, onClick, isTopCard = false }) {
    const getCardColor = () => {
        const colorMap = {
            'RED': '#ff4444',
            'BLUE': '#4444ff',
            'GREEN': '#44ff44',
            'YELLOW': '#ffff44',
            'WILD': '#333333'
        };
        return colorMap[card.color] || '#cccccc';
    };

    const getCardDisplay = () => {
        if (card.cardType === 'NUMBER') return card.value;
        if (card.cardType === 'SKIP') return '⊘';
        if (card.cardType === 'REVERSE') return '↻';
        if (card.cardType === 'DRAW_TWO') return '+2';
        if (card.cardType === 'WILD') return 'W';
        if (card.cardType === 'WILD_DRAW_FOUR') return '+4';
        return '?';
    };

    return (
        <div
            className={`card ${isPlayable ? 'playable' : ''} ${isTopCard ? 'top-card' : ''}`}
            style={{ backgroundColor: getCardColor() }}
            onClick={() => isPlayable && onClick(card)}
        >
            <span className="card-value">{getCardDisplay()}</span>
        </div>
    );
}
```

### Game State Management
```javascript
// Game state utilities
export const GameUtils = {
    isPlayerTurn: (gameState, playerId) => {
        if (!gameState || gameState.status !== 'IN_PROGRESS') return false;
        const currentPlayer = gameState.players[gameState.currentPlayerIndex];
        return currentPlayer?.player.id === playerId;
    },

    canPlayCard: (card, topCard) => {
        if (!topCard) return true;
        return card.color === topCard.color ||
               card.value === topCard.value ||
               card.cardType === topCard.cardType ||
               card.cardType === 'WILD' ||
               card.cardType === 'WILD_DRAW_FOUR';
    },

    getPlayableCards: (hand, topCard) => {
        return hand.filter(card => GameUtils.canPlayCard(card, topCard));
    },

    needsColorChoice: (card) => {
        return card.cardType === 'WILD' || card.cardType === 'WILD_DRAW_FOUR';
    }
};
```

### WebSocket Service
```javascript
// websocket-service.js
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

export class UnoWebSocketService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
    }

    connect(gameCode, onGameUpdate) {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;

            // Subscribe to game updates
            this.stompClient.subscribe(`/topic/game/${gameCode}`, (message) => {
                const event = JSON.parse(message.body);
                onGameUpdate(event);
            });
        }, (error) => {
            console.error('WebSocket connection error:', error);
            this.connected = false;
        });

        return this.stompClient;
    }

    playCard(gameCode, playerId, cardId, chosenColor = null) {
        if (this.connected) {
            this.stompClient.send('/app/game/play-card', {}, JSON.stringify({
                gameCode, playerId, cardId, chosenColor
            }));
        }
    }

    drawCard(gameCode, playerId) {
        if (this.connected) {
            this.stompClient.send('/app/game/draw-card', {}, JSON.stringify({
                gameCode, playerId
            }));
        }
    }

    callUno(gameCode, playerId) {
        if (this.connected) {
            this.stompClient.send('/app/game/call-uno', {}, JSON.stringify({
                gameCode, playerId
            }));
        }
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
        }
    }
}
```

## 🎯 Recommended Frontend Tech Stack

- **React/Vue/Angular**: For component-based UI
- **SockJS + StompJS**: For WebSocket communication
- **Axios**: For REST API calls
- **CSS/Styled-Components**: For card animations and styling
- **State Management**: Redux/Zustand/Pinia for complex state

## 🚀 Deployment

### Backend Deployment
```bash
# Build production JAR
./mvnw clean package -DskipTests

# Run with production profile
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Environment Variables
```bash
# Database configuration (for production)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uno_game
SPRING_DATASOURCE_USERNAME=uno_user
SPRING_DATASOURCE_PASSWORD=your_password

# Server configuration
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com
```

---

**🎮 Ready to build your frontend!** This backend provides all the APIs and real-time communication needed for a complete multiplayer Uno experience. The game logic is fully implemented and tested - just focus on creating an engaging user interface!
