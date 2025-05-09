package myGame;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient {
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game)
			throws IOException {
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}

	public UUID getID() {
		return id;
	}

	@Override
	protected void processPacket(Object message) {
		String strMessage = (String) message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");

		if (message == null) {
			System.out.println("Warning: Received null message in processPacket - ignoring");
			return;
		}

		// Game specific protocol to handle the message
		if (messageTokens.length > 0) {
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if (messageTokens[0].compareTo("join") == 0) {
				if (messageTokens[1].compareTo("success") == 0) {
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if (messageTokens[1].compareTo("failure") == 0) {
					System.out.println("join failure confirmed");
					game.setIsConnected(false);
				}
			}

			// Handle BYE message
			// Format: (bye,remoteId)
			if (messageTokens[0].compareTo("bye") == 0) { // remove ghost avatar with id = remoteId
															// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}

			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0)) { // create a
																											// new ghost
																											// avatar
																											// Parse out
																											// the id
																											// into a
																											// UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]));

				try {
					ghostManager.createGhostAvatar(ghostID, ghostPosition);
				} catch (IOException e) {
					System.out.println("error creating ghost avatar");
				}
			}

			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0) {
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}

			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0) {
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]));

				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
			}

			if (messageTokens[0].compareTo("rotate") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Matrix4f rotation = new Matrix4f();
				for (int i = 0; i < 16; i++) {
					rotation.set(i / 4, i % 4, Float.parseFloat(messageTokens[2 + i]));
				}
				ghostManager.updateGhostRotation(ghostID, rotation);
			}

			// ^ BALLS

			// Handle createBall messages
			if (messageTokens[0].compareTo("createBall") == 0) {
				// Format: createBall, senderId, ballId, x, y, z
				UUID senderId = UUID.fromString(messageTokens[1]);
				UUID ballId = UUID.fromString(messageTokens[2]);
				Vector3f ballPosition = new Vector3f(
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]));

				try {
					ghostManager.createGhostBall(ballId, senderId, ballPosition);
				} catch (IOException e) {
					System.out.println("Error creating ghost ball");
				}
			}

			// Handle moveBall messages
			if (messageTokens[0].compareTo("moveBall") == 0) {
				// Format: moveBall, senderId, ballId, x, y, z
				UUID ballId = UUID.fromString(messageTokens[2]);
				Vector3f ballPosition = new Vector3f(
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]));

				ghostManager.updateGhostBall(ballId, ballPosition);
			}

			// Handle removeBall messages
			if (messageTokens[0].compareTo("removeBall") == 0) {
				// Format: removeBall, senderId, ballId
				UUID ballId = UUID.fromString(messageTokens[2]);
				ghostManager.removeGhostBall(ballId);
			}

			if (messageTokens[0].compareTo("hit") == 0) {
				UUID targetID = UUID.fromString(messageTokens[1]);
				if (targetID.equals(id)) {
					game.handlePlayerHit();
				}
			}

			// ^ ============================== Shield Stuff ==============================
			if (messageTokens[0].compareTo("shield_activate") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.activateGhostShield(ghostID);
			}

			if (messageTokens[0].compareTo("shield_deactivate") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.deactivateGhostShield(ghostID);
			}

			if (messageTokens[0].compareTo("shield_hit") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.triggerGhostShieldHitEffect(ghostID);
			}
		}
	}

	// The initial message from the game client requesting to join the
	// server. localId is a unique identifier for the client. Recommend
	// a random UUID.
	// Message Format: (join,localId)

	public void sendJoinMessage() {
		try {
			sendPacket(new String("join," + id.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the client is leaving the server.
	// Message Format: (bye,localId)

	public void sendByeMessage() {
		try {
			sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the client�s Avatar�s position. The server
	// takes this message and forwards it to all other clients registered
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the
	// position

	public void sendCreateMessage(Vector3f position) {
		try {
			String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			sendPacket(message);
			sendRotateMessage(game.getAvatar().getWorldRotation());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the local avatar's position. The server then
	// forwards this message to the client with the ID value matching remoteId.
	// This message is generated in response to receiving a WANTS_DETAILS message
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position) {
		try {
			String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed position.
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendMoveMessage(Vector3f position) {
		try {
			String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRotateMessage(Matrix4f rotation) {
		try {
			// Convert Matrix4f to a string (e.g., "r0,r1,r2,...,r15")
			String message = "rotate," + id.toString();
			for (int i = 0; i < 16; i++) {
				message += "," + rotation.get(i / 4, i % 4);
			}
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ^ balls
	// Send a message to create a ball
	public void sendCreateBallMessage(UUID ballId, Vector3f position) {
		try {
			String message = new String("createBall," + id.toString() + "," + ballId.toString());
			message += "," + position.x() + "," + position.y() + "," + position.z();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send a message to update ball position
	public void sendUpdateBallMessage(UUID ballId, Vector3f position) {
		try {
			String message = new String("moveBall," + id.toString() + "," + ballId.toString());
			message += "," + position.x() + "," + position.y() + "," + position.z();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send a message to remove a ball
	public void sendRemoveBallMessage(UUID ballId) {
		try {
			String message = new String("removeBall," + id.toString() + "," + ballId.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHitPlayerMessage(UUID targetID) {
		try {
			System.out.println("Sending hit player message for target: " + targetID);
			String message = new String("hit," + targetID.toString());

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ^ ========================== Shield Stuff ========================== ^ //
	public void sendShieldActivateMessage() {
		try {
			String message = new String("shield_activate," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendShieldDeactivateMessage() {
		try {
			String message = new String("shield_deactivate," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Add method to send shield hit effect
	public void sendShieldHitMessage() {
		try {
			String message = new String("shield_hit," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
