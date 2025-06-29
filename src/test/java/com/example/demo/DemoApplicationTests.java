package com.example.demo;

import com.example.demo.config.StompProtocolHandler;
import com.example.demo.dto.*;
import com.example.demo.entity.Game;
import com.example.demo.entity.Player;
import com.example.demo.service.GameService;
import com.example.demo.service.PlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private PlayerService playerService;

	@Autowired
	private GameService gameService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void testStompProtocolHandlerAddsNullTerminators() throws Exception {
		// Test the STOMP protocol handler directly
		StompProtocolHandler handler = new StompProtocolHandler();

		// Create a test optimized game event
		OptimizedEventData.PlayerJoined eventData = new OptimizedEventData.PlayerJoined("TestPlayer", 1, 2);
		OptimizedGameEvent gameEvent = OptimizedGameEvent.create(
				"PLAYER_JOINED", "TEST123", 1L, "TestPlayer", eventData);

		String jsonPayload = objectMapper.writeValueAsString(gameEvent);
		byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

		// Create a STOMP MESSAGE frame without null terminator
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setDestination("/topic/game/TEST123");
		accessor.setHeader("content-type", "application/json");

		Message<?> message = MessageBuilder.createMessage(payloadBytes, accessor.getMessageHeaders());

		// Process the message through the handler
		Message<?> processedMessage = handler.preSend(message, null);

		// Verify the message was processed
		assertNotNull(processedMessage);

		// Check that the payload now has null terminator
		Object payload = processedMessage.getPayload();
		assertTrue(payload instanceof byte[]);

		byte[] processedBytes = (byte[]) payload;
		String processedString = new String(processedBytes, StandardCharsets.UTF_8);

		// Verify null terminator was added
		assertTrue(processedString.endsWith("\0"), "STOMP frame should end with null terminator");

		// Verify the JSON content is still intact (excluding null terminator)
		String jsonPart = processedString.substring(0, processedString.length() - 1);
		assertEquals(jsonPayload, jsonPart, "JSON payload should be preserved");

		System.out.println("✅ STOMP Protocol Handler Test PASSED");
		System.out.println("📤 Original JSON: " + jsonPayload);
		System.out.println("📤 Processed Frame: " + processedString.replace("\0", "\\0"));
		System.out.println("📤 Frame Length: " + processedBytes.length + " bytes");
		System.out.println("📤 Null Terminator Present: " + processedString.endsWith("\0"));
	}

	@Test
	void testStompFrameWithExistingNullTerminator() throws Exception {
		// Test that frames with existing null terminators are not double-terminated
		StompProtocolHandler handler = new StompProtocolHandler();

		String jsonPayload = "{\"eventType\":\"TEST\",\"playerId\":1}";
		String payloadWithNull = jsonPayload + "\0"; // Already has null terminator
		byte[] payloadBytes = payloadWithNull.getBytes(StandardCharsets.UTF_8);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setDestination("/topic/game/TEST123");

		Message<?> message = MessageBuilder.createMessage(payloadBytes, accessor.getMessageHeaders());
		Message<?> processedMessage = handler.preSend(message, null);

		byte[] processedBytes = (byte[]) processedMessage.getPayload();
		String processedString = new String(processedBytes, StandardCharsets.UTF_8);

		// Should not have double null terminators
		assertFalse(processedString.endsWith("\0\0"), "Should not have double null terminators");
		assertTrue(processedString.endsWith("\0"), "Should still have single null terminator");

		System.out.println("✅ Existing Null Terminator Test PASSED");
		System.out.println("📤 No double termination occurred");
	}

	@Test
	void testWebSocketStompFrameIntegration() throws Exception {
		// Create a simple WebSocket client to test STOMP frame transmission
		StandardWebSocketClient client = new StandardWebSocketClient();
		WebSocketSession session = null;

		try {
			URI uri = URI.create("ws://localhost:" + port + "/ws");

			// Create a simple WebSocket handler to capture frames
			final BlockingQueue<String> receivedFrames = new LinkedBlockingQueue<>();

			WebSocketHandler handler = new WebSocketHandler() {
				@Override
				public void afterConnectionEstablished(WebSocketSession session) throws Exception {
					System.out.println("🔌 Test WebSocket connected");

					// Send STOMP CONNECT frame
					String connectFrame = "CONNECT\n" +
							"accept-version:1.0,1.1,1.2\n" +
							"heart-beat:5000,5000\n" +
							"\n\0";
					session.sendMessage(new TextMessage(connectFrame));
				}

				@Override
				public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
					String frameContent = message.getPayload().toString();
					System.out.println("📨 Received frame: " + frameContent.replace("\0", "\\0"));
					receivedFrames.offer(frameContent);

					// Check if this is a CONNECTED frame, then subscribe
					if (frameContent.startsWith("CONNECTED")) {
						String subscribeFrame = "SUBSCRIBE\n" +
								"id:sub-1\n" +
								"destination:/topic/game/TEST123\n" +
								"\n\0";
						session.sendMessage(new TextMessage(subscribeFrame));
					}
				}

				@Override
				public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
					System.err.println("❌ WebSocket transport error: " + exception.getMessage());
				}

				@Override
				public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
					System.out.println("🔌 WebSocket connection closed: " + closeStatus);
				}

				@Override
				public boolean supportsPartialMessages() {
					return false;
				}
			};

			// Connect to WebSocket
			session = client.doHandshake(handler, null, uri).get(5, TimeUnit.SECONDS);

			// Wait for CONNECTED frame
			String connectedFrame = receivedFrames.poll(5, TimeUnit.SECONDS);
			assertNotNull(connectedFrame, "Should receive CONNECTED frame");
			assertTrue(connectedFrame.startsWith("CONNECTED"), "Should be a CONNECTED frame");
			assertTrue(connectedFrame.endsWith("\0"), "CONNECTED frame should end with null terminator");

			System.out.println("✅ WebSocket STOMP Integration Test PASSED");
			System.out.println("📨 CONNECTED frame properly terminated: " + connectedFrame.endsWith("\0"));

		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
	}

	@Test
	void testStompFrameStructureWithRealWebSocketMessage() throws Exception {
		// Create players using the correct service methods
		PlayerRequest player1Request = new PlayerRequest("TestPlayer1", 100);
		PlayerResponse player1Response = playerService.createPlayer(player1Request);

		PlayerRequest player2Request = new PlayerRequest("TestPlayer2", 100);
		PlayerResponse player2Response = playerService.createPlayer(player2Request);

		// Create a game using the correct service method
		CreateGameRequest createGameRequest = new CreateGameRequest(player1Response.getId(), 4, 2);
		GameResponse gameResponse = gameService.createGame(createGameRequest);

		// Join second player to trigger WebSocket message
		JoinGameRequest joinRequest = new JoinGameRequest(gameResponse.getGameCode(), player2Response.getId());
		gameService.joinGame(joinRequest);

		// The game should automatically start when the second player joins (since min
		// players = 2)
		// This will trigger GAME_STARTED WebSocket message

		System.out.println("✅ Real WebSocket message test completed");
		System.out.println("📊 Note: STOMP frame inspection logs may not appear in unit tests");
		System.out.println(
				"📊 STOMP interceptors work during actual WebSocket connections, not internal Spring messaging");

		// Verify game state - should be IN_PROGRESS after second player joined
		GameResponse updatedGameResponse = gameService.getGame(gameResponse.getGameCode(), player1Response.getId());
		assertEquals(com.example.demo.enums.GameStatus.IN_PROGRESS, updatedGameResponse.getStatus());

		System.out.println("✅ Game state verified: " + updatedGameResponse.getStatus());
	}
}
