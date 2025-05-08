package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;
import java.awt.*;

import java.awt.event.*;

import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

// Physics
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

import tage.audio.*;

public class MyGame extends VariableFrameRateGame {
	JumpAction jumpAction;

	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter = 0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z;
	private ObjShape torS, ghostS, dolS, linxS, linyS, linzS;
	private TextureImage doltx, ghostT;
	private Light light;

	// ground
	private GameObject terr;
	private ObjShape terrS;
	private TextureImage groundTx, heightMapTx;

	// skybox
	private int flufflyClouds;

	// npcs
	private ObjShape npcShape;
	private TextureImage npcTex;

	public ObjShape getNPCshape() {
		return npcShape;
	}

	public TextureImage getNPCtexture() {
		return npcTex;
	}

	// physics
	private PhysicsEngine physicsEngine;
	private boolean running = true;
	private float vals[] = new float[16];
	private boolean showPhysicsWorld = false;

	// Throwable sphere
	private GameObject sphere;
	private ObjShape sphereS;
	private TextureImage sphereTx;
	private PhysicsObject sphereP;
	private boolean sphereCreated = false;

	public ObjShape getSphereShape() {
		return sphereS;
	}

	public TextureImage getSphereTexture() {
		return sphereTx;
	}

	// player stuff
	private int playerHealth = 3; // Start with 3 health
	private boolean isInvulnerable = false;
	private long invulnerabilityStartTime = 0;
	private final long INVULNERABILITY_DURATION = 1000;
	private boolean byeMessageSent = false;

	// shield stuff
	private boolean isShielding = false;
	private boolean shieldOnCooldown = false;
	private long shieldCooldownStartTime = 0;
	private final long SHIELD_COOLDOWN_DURATION = 1500;

	// sounds
	private IAudioManager audioMgr;
	private Sound blockSound;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	// hitbox stuff
	private GameObject hitbox;
	private boolean hitboxActive = false;
	private long hitboxActivationTime = 0;
	private final long HITBOX_DURATION = 200;

	long currentTime;

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args) {
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	public void loadSounds() {
		audioMgr = engine.getAudioManager();
		AudioResource blockResource = audioMgr.createAudioResource("block.wav", AudioResourceType.AUDIO_SAMPLE);
		blockSound = new Sound(blockResource, SoundType.SOUND_EFFECT, 100, false);
		blockSound.initialize(audioMgr);
		blockSound.setMaxDistance(10.0f);
		blockSound.setMinDistance(0.5f);
		blockSound.setRollOff(5.0f);
	}

	@Override
	public void loadShapes() {
		torS = new Torus(0.5f, 0.2f, 48);
		ghostS = new Sphere();
		dolS = new ImportedModel("dolphinHighPoly.obj");

		// terrain
		terrS = new TerrainPlane(1000);

		// npc
		npcShape = new ImportedModel("frog.obj");

		// sphere (throwable)
		sphereS = new Sphere();

		linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
		linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
		linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
	}

	@Override
	public void loadTextures() {
		doltx = new TextureImage("Dolphin_HighPolyUV.png");
		ghostT = new TextureImage("redDolphin.jpg");
		groundTx = new TextureImage("grass.png");
		npcTex = new TextureImage("frog.png");
		heightMapTx = new TextureImage("terrain.png");
		sphereTx = new TextureImage("water.jpg");
	}

	@Override
	public void loadSkyBoxes() {
		flufflyClouds = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(flufflyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects() {
		Matrix4f initialTranslation, initialRotation, initialScale;

		// build dolphin avatar
		avatar = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(-1f, 0f, 1f);
		avatar.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(135.0f));
		avatar.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.5f);
		avatar.setLocalScale(initialScale);

		// hitbox stuff
		hitbox = new GameObject(avatar, new Cube(), sphereTx);
		hitbox.propagateTranslation(true);
		hitbox.propagateRotation(true);
		hitbox.applyParentRotationToPosition(true);

		Vector3f localOffset = new Vector3f(0.0f, 0.15f, 0.5f);
		Matrix4f localTranslation = (new Matrix4f()).translation(localOffset);
		hitbox.setLocalTranslation(localTranslation);

		Matrix4f hitboxScale = (new Matrix4f()).scaling(0.25f);
		hitbox.setLocalScale(hitboxScale);

		hitbox.getRenderStates().disableRendering();

		// build torus along X axis
		tor = new GameObject(GameObject.root(), torS);
		initialTranslation = (new Matrix4f()).translation(1, 0, 0);
		tor.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.25f);
		tor.setLocalScale(initialScale);

		// Ground
		terr = new GameObject(GameObject.root(), terrS, groundTx);
		initialTranslation = (new Matrix4f()).translation(0f, 0f, 0f);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(20.0f, 1.0f, 20.0f);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(heightMapTx);

		// Set tiling for terrain texture
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(10);

		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
		(y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
		(z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
	}

	@Override
	public void initializeGame() {
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);

		setupNetworking();

		// ----------------- initialize camera ----------------
		positionCameraBehindAvatar();

		// Initialize physics system
		float[] gravity = { 0f, -5f, 0f };
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		// Create physics for the terrain
		float up[] = { 0, 1, 0 };
		double[] terrainTransform = toDoubleArray(terr.getLocalTranslation().get(vals));
		PhysicsObject terrainP = (engine.getSceneGraph()).addPhysicsStaticPlane(
				terrainTransform, up, 0.0f);
		terrainP.setBounciness(1.0f);
		terr.setPhysicsObject(terrainP);
		// Enable physics world rendering for debugging
		engine.enableGraphicsWorldRender();

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

		// build some action objects for doing things in response to user input
		FwdAction fwdAction = new FwdAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);
		BackwardAction backwardAction = new BackwardAction(this, protClient);
		jumpAction = new JumpAction(this, protClient);
		ShieldToggleAction shieldToggleAction = new ShieldToggleAction(this, protClient);

		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.Q,
				shieldToggleAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// attach the action objects to keyboard and gamepad components
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._1,
				fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.X,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.W,
				fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.A,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.S,
				backwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.D,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.SPACE,
				jumpAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	public GameObject getAvatar() {
		return avatar;
	}

	@Override
	public void update() {
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();

		// ----------------- Removing Ghosts of Clients that leave ----------------
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (protClient != null && isClientConnected && !byeMessageSent) {
				byeMessageSent = true;
				System.out.println("Sending bye message before shutdown...");
				protClient.sendByeMessage();
			}
		}));

		if (playerHealth <= 0) {
			System.exit(0);
		}

		currentTime = System.currentTimeMillis();

		// Check if invulnerability period is over
		if (isInvulnerable && System.currentTimeMillis() - invulnerabilityStartTime > INVULNERABILITY_DURATION) {
			isInvulnerable = false;
		}

		// Check if shield cooldown is over
		if (shieldOnCooldown && System.currentTimeMillis() - shieldCooldownStartTime > SHIELD_COOLDOWN_DURATION) {
			shieldOnCooldown = false;
		}

		// Calculate cooldowns
		long currentTime = System.currentTimeMillis();
		float throwCooldownRemaining = 0;
		if (currentTime - lastBallThrowTime < BALL_THROW_COOLDOWN) {
			throwCooldownRemaining = (BALL_THROW_COOLDOWN - (currentTime - lastBallThrowTime)) / 1000.0f;
		}

		float shieldCooldownRemaining = 0;
		if (shieldOnCooldown) {
			shieldCooldownRemaining = (SHIELD_COOLDOWN_DURATION - (currentTime - shieldCooldownStartTime)) / 1000.0f;
		}

		// Build and set HUD to show health and combined status
		String healthStr = "Health: " + playerHealth;

		// Construct the combined status string
		StringBuilder statusBuilder = new StringBuilder();

		// Add shield status
		if (isShielding) {
			statusBuilder.append("SHIELD ACTIVE");
		} else if (shieldOnCooldown) {
			statusBuilder.append("Shield: ").append(String.format("%.1f", shieldCooldownRemaining)).append("s");
		} else {
			statusBuilder.append("Shield Ready (Q)");
		}

		// Add separator
		statusBuilder.append(" | ");

		// Add throw status
		if (throwCooldownRemaining > 0) {
			statusBuilder.append("Throw: ").append(String.format("%.1f", throwCooldownRemaining)).append("s");
		} else {
			statusBuilder.append("Throw Ready (R)");
		}

		// Set colors - health is red, status is dynamic based on state
		Vector3f hud1Color = new Vector3f(1, 0, 0); // Red for health
		Vector3f hud2Color;

		// Determine color based on priority: shielding > on cooldown > ready
		if (isShielding) {
			hud2Color = new Vector3f(0, 1, 0); // Green when shield is active
		} else if (shieldOnCooldown || throwCooldownRemaining > 0) {
			hud2Color = new Vector3f(0.8f, 0.8f, 0); // Yellow when something is on cooldown
		} else {
			hud2Color = new Vector3f(0, 0.8f, 1); // Blue when everything is ready
		}

		// Set HUD elements
		(engine.getHUDmanager()).setHUD1(healthStr, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(statusBuilder.toString(), hud2Color, 15, 45);

		// update inputs and camera
		im.update((float) elapsedTime);

		if (jumpAction != null) {
			jumpAction.update();
		}

		if (hitboxActive) {
			checkHitboxStatus();
		}

		checkPlayerHits();

		if (isInvulnerable) {
			// Flash the avatar every 200ms during invulnerability
			long timeSinceInvulnerable = System.currentTimeMillis() - invulnerabilityStartTime;
			boolean shouldBeVisible = (timeSinceInvulnerable / 200) % 2 == 0;

			if (shouldBeVisible) {
				avatar.getRenderStates().enableRendering();
			} else {
				avatar.getRenderStates().disableRendering();
			}
		} else {
			// Make sure avatar is visible when not invulnerable
			avatar.getRenderStates().enableRendering();
		}

		checkBallBoundary();

		if (sphereCreated) {
			checkSphereLifetime();

			if (sphere != null && protClient != null && isClientConnected && sphereId != null) {
				protClient.sendUpdateBallMessage(sphereId, sphere.getWorldLocation());
			}

		}

		if (gm != null) {
			gm.cleanupExpiredBalls(SPHERE_LIFETIME);
		}

		// Update physics
		if (running) {
			AxisAngle4f aa = new AxisAngle4f();
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			Matrix4f mat3 = new Matrix4f().identity();

			checkForCollisions();
			physicsEngine.update((float) elapsedTime);

			for (GameObject go : engine.getSceneGraph().getGameObjects()) {
				if (go.getPhysicsObject() != null) {
					// Set translation
					mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3, 0, mat.m30());
					mat2.set(3, 1, mat.m31());
					mat2.set(3, 2, mat.m32());
					go.setLocalTranslation(mat2);

					// Set rotation
					mat.getRotation(aa);
					mat3.rotation(aa);
					go.setLocalRotation(mat3);
				}
			}
		}

		Vector3f loc = avatar.getWorldLocation();
		float height = terr.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		positionCameraBehindAvatar();
		processNetworking((float) elapsedTime);
	}

	private void positionCameraBehindAvatar() {
		Vector4f u = new Vector4f(-1f, 0f, 0f, 1f);
		Vector4f v = new Vector4f(0f, 1f, 0f, 1f);
		Vector4f n = new Vector4f(0f, 0f, 1f, 1f);
		u.mul(avatar.getWorldRotation());
		v.mul(avatar.getWorldRotation());
		n.mul(avatar.getWorldRotation());
		Matrix4f w = avatar.getWorldTranslation();
		Vector3f position = new Vector3f(w.m30(), w.m31(), w.m32());
		position.add(-n.x() * 2f, -n.y() * 2f, -n.z() * 2f);
		position.add(v.x() * .75f, v.y() * .75f, v.z() * .75f);
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		c.setLocation(position);
		c.setU(new Vector3f(u.x(), u.y(), u.z()));
		c.setV(new Vector3f(v.x(), v.y(), v.z()));
		c.setN(new Vector3f(n.x(), n.y(), n.z()));
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_R: {
				// Only allow throwing if not shielding and not on cooldown
				if (!isShielding && !shieldOnCooldown) {
					long currentTime = System.currentTimeMillis();
					// Check if throw cooldown has passed
					if (currentTime - lastBallThrowTime > BALL_THROW_COOLDOWN) {
						// Create and throw a sphere when R is pressed
						createThrowableSphere();
						lastBallThrowTime = currentTime; // Update the last throw time

						// Start physics if not already running
						if (!running) {
							running = true;
							System.out.println("starting physics");
						}
					} else {
						// Calculate remaining cooldown time in seconds
						float remainingCooldown = (BALL_THROW_COOLDOWN - (currentTime - lastBallThrowTime)) / 1000.0f;
						System.out.println("Throw on cooldown! Ready in " + remainingCooldown + " seconds");
					}
				} else if (isShielding) {
					System.out.println("Cannot throw while shielding!");
				} else if (shieldOnCooldown) {
					System.out.println("Cannot throw during shield cooldown!");
				}
				break;
			}
			case KeyEvent.VK_E: {
				// Only allow hitbox activation if not shielding and not on cooldown
				if (!isShielding && !shieldOnCooldown) {
					// Activate hitbox when E is pressed
					activateHitbox();
				} else if (isShielding) {
					System.out.println("Cannot attack while shielding!");
				} else if (shieldOnCooldown) {
					System.out.println("Cannot attack during shield cooldown!");
				}
				break;
			}
			case KeyEvent.VK_T: {
				// Existing code for physics world toggle
				showPhysicsWorld = !showPhysicsWorld;
				if (showPhysicsWorld) {
					engine.enablePhysicsWorldRender();
					System.out.println("showing physics world");
				} else {
					engine.disablePhysicsWorldRender();
					System.out.println("hiding physics world");
				}
				break;
			}
		}
		super.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public void toggleShield() {
		// Only toggle if not on cooldown
		if (!shieldOnCooldown) {
			isShielding = !isShielding;

			if (isShielding) {
				// Shield activated
				System.out.println("Shield activated!");
			} else {
				// Shield deactivated, start cooldown
				shieldOnCooldown = true;
				shieldCooldownStartTime = System.currentTimeMillis();
				System.out.println("Shield deactivated, on cooldown!");
			}
		} else {
			float cooldownRemaining = (SHIELD_COOLDOWN_DURATION
					- (System.currentTimeMillis() - shieldCooldownStartTime)) / 1000.0f;
			System.out.println("Shield on cooldown! Ready in " + String.format("%.1f", cooldownRemaining) + " seconds");
		}
	}

	private void activateHitbox() {
		// ! ADJUST THIS FOR AFTER YOU ADD ANIMATIONS Enable rendering to make it
		// visible
		hitbox.getRenderStates().enableRendering();

		// Start tracking activation time
		hitboxActivationTime = System.currentTimeMillis();
		hitboxActive = true;

		System.out.println("Hitbox activated");
	}

	private void checkHitboxStatus() {
		if (hitboxActive && System.currentTimeMillis() - hitboxActivationTime > HITBOX_DURATION) {
			// Deactivate the hitbox
			hitbox.getRenderStates().disableRendering();
			hitboxActive = false;
			System.out.println("Hitbox deactivated");
		}

		if (hitboxActive) {
			checkHitboxCollisions();
		}
	}

	private void checkHitboxCollisions() {
		if (hitboxActive && gm != null) {
			Vector<GhostAvatar> ghostAvatars = gm.getGhostAvatars();

			if (ghostAvatars != null && !ghostAvatars.isEmpty()) {
				Vector3f hitboxPos = hitbox.getWorldLocation();
				float hitboxRadius = 0.25f; // Try increasing this if detection is failing

				// Debug output
				System.out.println("Checking hitbox collisions at: " + hitboxPos.x() + ", " + hitboxPos.y() + ", "
						+ hitboxPos.z());
				System.out.println("Number of ghost avatars: " + ghostAvatars.size());

				for (GhostAvatar ghost : ghostAvatars) {
					if (ghost != null) {
						Vector3f ghostPos = ghost.getWorldLocation();
						float ghostRadius = 0.5f; // Approximate size of ghost avatar

						// Debug output for ghost positions
						System.out.println("Ghost at: " + ghostPos.x() + ", " + ghostPos.y() + ", " + ghostPos.z());

						// Simple distance-based collision detection
						float distance = hitboxPos.distance(ghostPos);
						System.out.println("Distance to ghost: " + distance + " (threshold: "
								+ (hitboxRadius + ghostRadius) + ")");

						if (distance < (hitboxRadius + ghostRadius)) {
							// We've hit a ghost avatar!
							System.out.println("HIT DETECTED! Distance: " + distance);
							handleGhostHit(ghost);
							break; // Only handle one hit per frame
						}
					}
				}
			} else {
				System.out.println("No ghost avatars found or list is empty");
			}
		}
	}

	private void handleGhostHit(GhostAvatar ghost) {
		System.out.println("Hit ghost avatar: " + ghost.getID());

		if (protClient != null && isClientConnected) {
			protClient.sendHitPlayerMessage(ghost.getID());
		}
	}

	public void handlePlayerHit() {
		if (isShielding) {
			System.out.println("Shield blocked the hit!");

			// Deactivate shield and start cooldown
			isShielding = false;
			shieldOnCooldown = true;
			shieldCooldownStartTime = System.currentTimeMillis();

			return;
		}

		if (!isInvulnerable) {
			playerHealth--;
			System.out.println("Player hit! Health reduced to: " + playerHealth);

			// Apply invulnerability
			isInvulnerable = true;
			invulnerabilityStartTime = System.currentTimeMillis();

			// Check for game over
			if (playerHealth <= 0) {
				System.out.println("GAME OVER - Player out of health!");
			}
		} else {
			System.out.println("Player is invulnerable, hit ignored!");
		}
	}

	// * ---------- Physics throw SECTION ----------------
	private long sphereCreationTime = 0;
	private final long SPHERE_LIFETIME = 2000; // 2 seconds
	private UUID sphereId = null;
	private long lastBallThrowTime = 0;
	private final long BALL_THROW_COOLDOWN = 3000;

	private void createThrowableSphere() {
		if (sphereCreated) {
			// If a sphere already exists, remove it from the scene graph and physics engine
			if (sphere.getPhysicsObject() != null) {
				int physicsObjectUID = sphere.getPhysicsObject().getUID();
				physicsEngine.removeObject(physicsObjectUID);
				sphere.setPhysicsObject(null);
			}
			if (sphereId != null && protClient != null && isClientConnected) {
				protClient.sendRemoveBallMessage(sphereId);
				sphereId = null;
			}
			sphereCreated = false;
		}

		// Create a new sphere
		sphere = new GameObject(GameObject.root(), sphereS, sphereTx);
		sphereId = UUID.randomUUID();

		// Get avatar position and direction
		Vector3f avatarPos = avatar.getWorldLocation();
		Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		fwdDirection.mul(avatar.getWorldRotation());
		fwdDirection.normalize();

		// Position the sphere at the avatar's position but slightly higher
		Vector3f spherePos = new Vector3f(
				avatarPos.x(),
				avatarPos.y() + 0.25f, // Position slightly above the avatar
				avatarPos.z());

		// Set sphere properties
		Matrix4f sphereTranslation = (new Matrix4f()).translation(spherePos);
		sphere.setLocalTranslation(sphereTranslation);
		Matrix4f sphereScale = (new Matrix4f()).scaling(0.07f);
		sphere.setLocalScale(sphereScale);

		// Create physics for the sphere
		float mass = 1.0f;
		float radius = 0.07f;
		double[] tempTransform = toDoubleArray(sphereTranslation.get(vals));

		// Add to physics engine with a unique ID
		int newUID = physicsEngine.nextUID();
		sphereP = physicsEngine.addSphereObject(newUID, mass, tempTransform, radius);

		physicsEngine.update(16);

		sphereP.setBounciness(0.5f);
		sphere.setPhysicsObject(sphereP);

		// Set up direction with upward component
		Vector3f tossDir = new Vector3f(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
		tossDir.add(0.0f, 0.30f, 0.0f);
		tossDir.normalize();

		// Apply force immediately for throwing
		float tossForce = 150.0f;
		float forceX = tossDir.x() * tossForce;
		float forceY = tossDir.y() * tossForce;
		float forceZ = tossDir.z() * tossForce;

		// Apply force at the center of the sphere
		sphereP.applyForce(forceX, forceY, forceZ, 0.0f, 0.0f, 0.0f);

		float[] initialVelocity = { tossDir.x() * 2.5f, tossDir.y() * 2.5f, tossDir.z() * 2.5f };
		sphereP.setLinearVelocity(initialVelocity);

		if (protClient != null && isClientConnected) {
			protClient.sendCreateBallMessage(sphereId, sphere.getWorldLocation());
		}

		// Start tracking the sphere's lifetime
		sphereCreationTime = System.currentTimeMillis();
		sphereCreated = true;

		System.out.println("Sphere created and thrown");
	}

	// Check and remove the sphere if its lifetime has expired
	private void checkSphereLifetime() {
		if (sphereCreated && System.currentTimeMillis() - sphereCreationTime > SPHERE_LIFETIME) {
			removeSphere();
		}
	}

	// Remove the sphere from the scene and physics engine
	private void removeSphere() {
		if (sphereCreated) {
			// Remove from physics engine
			if (sphere != null && sphere.getPhysicsObject() != null) {
				try {
					int physicsObjectUID = sphere.getPhysicsObject().getUID();
					System.out.println("Removing physics object with UID: " + physicsObjectUID);
					physicsEngine.removeObject(physicsObjectUID);
					sphere.setPhysicsObject(null);
					System.out.println("Physics object removed.");
				} catch (Exception e) {
					System.out.println("Error removing physics object: " + e.getMessage());
					e.printStackTrace();
				}
			}

			// Remove from scene graph
			if (sphere != null) {
				try {
					System.out.println("Attempting to remove sphere from scene graph...");
					engine.getSceneGraph().removeGameObject(sphere);
					System.out.println("Sphere removed from scene graph.");
				} catch (Exception e) {
					System.out.println("Error removing from scene graph: " + e.getMessage());
					e.printStackTrace();
				}
			}

			// Clear references
			sphereCreated = false;
			sphere = null;
			System.out.println("Sphere reference cleared.");
		}
	}

	private void checkBallBoundary() {
		if (sphereCreated && sphere != null) {
			Vector3f spherePos = sphere.getWorldLocation();

			// Check if the ball has hit or gone below y=0.1
			if (spherePos.y() <= 0.1f) {
				System.out.println("Ball hit ground! Removing...");
				removeSphere();
			}
		}
	}
	// ---------- PHYSICS UTILITY METHODS ----------------

	private void checkPlayerHits() {
		// Only check for hits if not invulnerable and ghost manager exists
		if (!isInvulnerable && gm != null) {
			Vector3f avatarPos = avatar.getWorldLocation();
			float avatarRadius = 0.5f; // Approximate size of avatar

			Vector<GhostBall> ghostBalls = gm.getGhostBalls();

			if (ghostBalls != null) {
				for (GhostBall ghostBall : ghostBalls) {
					if (ghostBall != null) {
						Vector3f ballPos = ghostBall.getWorldLocation();
						float ballRadius = 0.07f;

						float distance = ballPos.distance(avatarPos);

						if (distance < (ballRadius + avatarRadius)) {
							// A hit is detected, now check if shield is active
							if (isShielding) {
								System.out.println("Shield blocked the hit!");

								// Deactivate shield and start cooldown
								isShielding = false;
								shieldOnCooldown = true;
								shieldCooldownStartTime = System.currentTimeMillis();

								// Remove the ghost ball that hit the shield
								gm.removeGhostBall(ghostBall.getID());
							} else {
								// No shield active, so apply damage
								handlePlayerHit(ghostBall.getID(), ghostBall.getOwnerID());
							}
							break; // Only handle one hit per frame
						}
					}
				}
			}
		}
	}

	private void handlePlayerHit(UUID ballId, UUID throwerId) {
		// Reduce health
		playerHealth--;
		System.out.println("Player hit! Health reduced to: " + playerHealth);

		// Apply invulnerability
		isInvulnerable = true;
		invulnerabilityStartTime = System.currentTimeMillis();

		// Remove the ghost ball
		gm.removeGhostBall(ballId);

		// Check for game over
		if (playerHealth <= 0) {
			System.out.println("GAME OVER - Player out of health!");
			// You could add more game over handling here
		}
	}

	private void checkForCollisions() {
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine) physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();

		for (int i = 0; i < manifoldCount; i++) {
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody1();

			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			for (int j = 0; j < manifold.getNumContacts(); j++) {
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f) {
					System.out.println("---- hit between " + obj1 + " and " + obj2);
					break;
				}
			}
		}
	}

	private float[] toFloatArray(double[] arr) {
		if (arr == null)
			return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float) arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null)
			return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double) arr[i];
		}
		return ret;
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() {
		return ghostS;
	}

	public TextureImage getGhostTexture() {
		return ghostT;
	}

	public GhostManager getGhostManager() {
		return gm;
	}

	public Engine getEngine() {
		return engine;
	}

	private void setupNetworking() {
		isClientConnected = false;
		try {
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (protClient == null) {
			System.out.println("missing protocol host");
		} else { // Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}

	protected void processNetworking(float elapsTime) { // Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		this.isClientConnected = value;
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			if (protClient != null && isClientConnected == true) {
				protClient.sendByeMessage();
			}
		}
	}
}