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
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter = 0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z;
	private ObjShape torS, ghostS, dolS, linxS, linyS, linzS;
	private TextureImage doltx, ghostT, metalTx;
	private Light light, redLight, redLight1, redLight2, redLight3;

	private AnimatedShape frogS;
	private TextureImage frogTx, bearTx;
	private float frogHeightAdjust = 0.65f;

	public AnimatedShape getFrogShape() {
		return frogS;
	}

	public TextureImage getFrogTexture() {
		return frogTx;
	}

	public TextureImage getBearTexture() {
		return bearTx;
	}

	public String getPlayerTexture() {
		return playerTexture;
	}

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

	// crates
	private ObjShape crateS;
	private TextureImage crateTx;
	private GameObject crate1, crate2, crate3, crate4;
	private boolean cratesPositioned = false;

	// physics
	private PhysicsEngine physicsEngine;
	private boolean running = true;
	private float vals[] = new float[16];
	private boolean showPhysicsWorld = false;

	// Throwables
	private GameObject sphere;
	private ObjShape sphereS;
	private TextureImage sphereTx;
	private PhysicsObject sphereP;
	private boolean sphereCreated = false;

	private int projectileAmmo = 3; // starting ammo
	private final int MAX_AMMO = 3;
	private boolean isReloading = false;
	private long reloadStartTime = 0;
	private final long RELOAD_TIME = 2000; // 2 seconds to reload
	private GameObject nearestCrate = null;
	private float crateInteractionDistance = 3.0f;
	private boolean fKeyHeld = false;
	private int screenWidth = 1900;
	private int screenHeight = 1000;

	// Dodge
	private boolean isDashing = false;
	private long dashStartTime = 0;
	private final long DASH_DURATION = 200; // 0.2 seconds for the dash
	private final float DASH_DISTANCE = 3.5f; // Total distance of the dash
	private final long DASH_COOLDOWN = 3000; // 1.5 seconds cooldown
	private long lastDashTime = 0;
	private Vector3f dashDirection = new Vector3f(0, 0, 0);
	private Vector3f dashStartPosition = new Vector3f();
	private Vector3f dashEndPosition = new Vector3f();

	private boolean wKeyHeld = false;
	private boolean aKeyHeld = false;
	private boolean sKeyHeld = false;
	private boolean dKeyHeld = false;

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
	private final long HITBOX_DURATION = 100;
	private long lastAttackTime = 0;
	private final long ATTACK_COOLDOWN = 1000;

	// shield stuff
	private GameObject shield;
	private boolean shieldActive = false;
	private long shieldActivationTime = 0;
	private final long SHIELD_DURATION = 5000;
	private boolean qKeyHeld = false;

	private long shieldDeactivationTime = 0;
	private final long SHIELD_COOLDOWN = 1500; // 1.5 seconds cooldown
	private boolean shieldOnCooldown = false;
	private long shieldHitTime = 0;
	private final long SHIELD_HIT_FLASH_DURATION = 200;

	private ObjShape shieldS;
	private TextureImage shieldTx;

	private GameObject sword;
	private ObjShape swordS;
	private TextureImage swordTx;
	private float swordAnimationProgress = 0.0f;
	private boolean swordAnimating = false;
	private final float SWORD_ANIMATION_SPEED = 3.0f;

	public ObjShape getSwordShape() {
		return swordS;
	}

	public TextureImage getSwordTexture() {
		return swordTx;
	}

	public TextureImage getShieldTexture() {
		return shield.getTextureImage();
	}

	public ObjShape getShieldShape() {
		return shield.getShape();
	}

	// Sounds
	private IAudioManager audioMgr;
	private Sound blockSound;
	private Sound slapSound, oofSound, musicSound;
	private Sound footstepSound;
	private boolean isWalking = false;

	public MyGame(String serverAddress, int serverPort, String protocol, String playerTexture) {
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.playerTexture = playerTexture;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	private String playerTexture = "frog.png"; // Default texture
	private TextureImage avatarTx;

	public static void main(String[] args) {
		TextureSelectionWindow selectionWindow = new TextureSelectionWindow();
		String selectedTexture = selectionWindow.waitForSelection();

		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2], selectedTexture);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	public void loadSounds() {
		AudioResource blockResource, slapResource, oofResource, musicResource, footstepResource;
		audioMgr = engine.getAudioManager();

		musicResource = audioMgr.createAudioResource("music.wav", AudioResourceType.AUDIO_SAMPLE);
		musicSound = new Sound(musicResource, SoundType.SOUND_EFFECT, 60, true);
		musicSound.initialize(audioMgr);
		musicSound.play();

		blockResource = audioMgr.createAudioResource("shield.wav", AudioResourceType.AUDIO_SAMPLE);
		blockSound = new Sound(blockResource, SoundType.SOUND_EFFECT, 50, false);
		blockSound.initialize(audioMgr);

		blockSound.setMaxDistance(10.0f);
		blockSound.setMinDistance(0.5f);
		blockSound.setRollOff(5.0f);

		slapResource = audioMgr.createAudioResource("slap.wav", AudioResourceType.AUDIO_SAMPLE);
		slapSound = new Sound(slapResource, SoundType.SOUND_EFFECT, 60, false);
		slapSound.initialize(audioMgr);

		slapSound.setMaxDistance(10.0f);
		slapSound.setMinDistance(0.5f);
		slapSound.setRollOff(5.0f);

		oofResource = audioMgr.createAudioResource("oof.wav", AudioResourceType.AUDIO_SAMPLE);
		oofSound = new Sound(oofResource, SoundType.SOUND_EFFECT, 60, false);
		oofSound.initialize(audioMgr);

		oofSound.setMaxDistance(10.0f);
		oofSound.setMinDistance(0.5f);
		oofSound.setRollOff(5.0f);

		footstepResource = audioMgr.createAudioResource("pmpt.wav", AudioResourceType.AUDIO_SAMPLE);
		footstepSound = new Sound(footstepResource, SoundType.SOUND_EFFECT, 25, true);
		footstepSound.initialize(audioMgr);

		footstepSound.setMaxDistance(5.0f);
		footstepSound.setMinDistance(0.2f);
		footstepSound.setRollOff(5.0f);

	}

	@Override
	public void loadShapes() {
		torS = new Torus(0.5f, 0.2f, 48);

		frogS = new AnimatedShape("frog.rkm", "frog.rks");
		frogS.loadAnimation("RUN", "run.rka");
		frogS.loadAnimation("HELLO", "hello.rka");

		// ! CHANGE THIS
		// ghost models
		ghostS = frogS;

		// terrain
		terrS = new TerrainPlane(1000);

		// npc
		npcShape = new ImportedModel("frog.obj");

		// sphere (throwable)
		sphereS = new ImportedModel("axe.obj");

		// shield
		shieldS = new ImportedModel("shield.obj");

		// sword
		swordS = new ImportedModel("sword.obj");

		// crates
		crateS = new ImportedModel("crate.obj");
	}

	@Override
	public void loadTextures() {
		groundTx = new TextureImage("grass.png");
		npcTex = new TextureImage("frog.png");
		heightMapTx = new TextureImage("terrain.png");
		sphereTx = new TextureImage("metal.jpg");
		metalTx = new TextureImage("metal.jpg");
		shieldTx = new TextureImage("metal.jpg");
		swordTx = new TextureImage("metal.jpg"); // ! change ?
		frogTx = new TextureImage("frog.png");
		bearTx = new TextureImage("bear.png");
		crateTx = new TextureImage("crate.png");

		ghostT = new TextureImage("water.jpg"); // default
		avatarTx = playerTexture.equals("frog.png") ? frogTx : bearTx;
	}

	@Override
	public void loadSkyBoxes() {
		flufflyClouds = (engine.getSceneGraph()).loadCubeMap("lakeIslands");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(flufflyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects() {
		Matrix4f initialTranslation, initialRotation, initialScale;
		float quadrantSize = 10.0f;

		// Frog avatar
		avatar = new GameObject(GameObject.root(), frogS, avatarTx);
		initialTranslation = (new Matrix4f()).translation(-1f, frogHeightAdjust, 1f);
		avatar.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(135.0f));
		avatar.setLocalRotation(initialRotation);
		initialScale = (new Matrix4f()).scaling(0.20f);
		avatar.setLocalScale(initialScale);

		avatar.getRenderStates().setModelOrientationCorrection(
				(new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90.0f)));

		// hitbox stuff
		hitbox = new GameObject(avatar, new Cube(), sphereTx);
		hitbox.propagateTranslation(true);
		hitbox.propagateRotation(true);
		hitbox.applyParentRotationToPosition(true);

		Vector3f hitboxOffset = new Vector3f(0.0f, 0.0f, 0.15f);
		Matrix4f hitboxLocalTranslation = (new Matrix4f()).translation(hitboxOffset);
		hitbox.setLocalTranslation(hitboxLocalTranslation);

		Matrix4f hitboxScale = (new Matrix4f()).scaling(1.25f);
		hitbox.setLocalScale(hitboxScale);

		// make hitbox invisible
		hitbox.getRenderStates().disableRendering();

		// shield stuff
		shield = new GameObject(avatar, shieldS, shieldTx);
		shield.propagateTranslation(true);
		shield.propagateRotation(true);
		shield.applyParentRotationToPosition(true);

		Vector3f shieldLocalOffset = new Vector3f(0.3f, -0.25f, 0.1f);
		Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldLocalOffset);
		shield.setLocalTranslation(shieldLocalTranslation);
		initialRotation = (new Matrix4f()).rotateY((float) java.lang.Math.toRadians(30.0f));
		shield.setLocalRotation(initialRotation);
		Matrix4f shieldHitboxScale = (new Matrix4f()).scaling(0.75f);
		shield.setLocalScale(shieldHitboxScale);

		// sword stuff
		sword = new GameObject(avatar, swordS, swordTx);
		sword.propagateTranslation(true);
		sword.propagateRotation(true);
		sword.applyParentRotationToPosition(true);

		Vector3f swordOffset = new Vector3f(-0.25f, -0.15f, 0.1f);
		Matrix4f swordLocalTranslation = (new Matrix4f()).translation(swordOffset);
		sword.setLocalTranslation(swordLocalTranslation);

		Matrix4f swordLocalRotation = (new Matrix4f()).rotationY((float) Math.toRadians(-90.0f));
		sword.setLocalRotation(swordLocalRotation);

		Matrix4f swordScale = (new Matrix4f()).scaling(2.75f);
		sword.setLocalScale(swordScale);

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

		// crates
		crate1 = new GameObject(GameObject.root(), crateS, crateTx);
		crate1.setLocalTranslation(new Matrix4f().translation(quadrantSize / 2, 0.5f, -quadrantSize / 2));
		crate1.setLocalScale(new Matrix4f().scaling(0.5f));

		crate2 = new GameObject(GameObject.root(), crateS, crateTx);
		crate2.setLocalTranslation(new Matrix4f().translation(-quadrantSize / 2, 0.5f, -quadrantSize / 2));
		crate2.setLocalScale(new Matrix4f().scaling(0.5f));

		crate3 = new GameObject(GameObject.root(), crateS, crateTx);
		crate3.setLocalTranslation(new Matrix4f().translation(-quadrantSize / 2, 0.5f, quadrantSize / 2));
		crate3.setLocalScale(new Matrix4f().scaling(0.5f));

		crate4 = new GameObject(GameObject.root(), crateS, crateTx);
		crate4.setLocalTranslation(new Matrix4f().translation(quadrantSize / 2, 0.5f, quadrantSize / 2));
		crate4.setLocalScale(new Matrix4f().scaling(0.5f));
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);

		redLight = new Light();
		redLight.setType(Light.LightType.SPOTLIGHT);
		redLight.setAmbient(1.0f, 0.0f, 0.0f);
		redLight.setDiffuse(1.0f, 0.0f, 0.0f);
		redLight.setSpecular(1.0f, 0.0f, 0.0f);
		redLight.setLocation(crate1.getWorldLocation().add(crate1.getLocalUpVector().mul(1.5f)));
		(engine.getSceneGraph()).addLight(redLight);

		redLight1 = new Light();
		redLight1.setType(Light.LightType.SPOTLIGHT);
		redLight1.setAmbient(1.0f, 0.0f, 0.0f);
		redLight1.setDiffuse(1.0f, 0.0f, 0.0f);
		redLight1.setSpecular(1.0f, 0.0f, 0.0f);
		redLight1.setLocation(crate2.getWorldLocation().add(crate2.getLocalUpVector().mul(1.5f)));
		(engine.getSceneGraph()).addLight(redLight1);

		redLight2 = new Light();
		redLight2.setType(Light.LightType.SPOTLIGHT);
		redLight2.setAmbient(1.0f, 0.0f, 0.0f);
		redLight2.setDiffuse(1.0f, 0.0f, 0.0f);
		redLight2.setSpecular(1.0f, 0.0f, 0.0f);
		redLight2.setLocation(crate3.getWorldLocation().add(crate3.getLocalUpVector().mul(1.5f)));
		(engine.getSceneGraph()).addLight(redLight2);

		redLight3 = new Light();
		redLight3.setType(Light.LightType.SPOTLIGHT);
		redLight3.setAmbient(1.0f, 0.0f, 0.0f);
		redLight3.setDiffuse(1.0f, 0.0f, 0.0f);
		redLight3.setSpecular(1.0f, 0.0f, 0.0f);
		redLight3.setLocation(crate4.getWorldLocation().add(crate4.getLocalUpVector().mul(1.5f)));
		(engine.getSceneGraph()).addLight(redLight3);
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

		// ----------------- Positioning ----------------
		Vector3f initialPosition;
		Matrix4f initialRotation = new Matrix4f();

		if (playerTexture.equals("frog.png")) {
			initialPosition = new Vector3f(8.0f, frogHeightAdjust, 0.0f);
			initialRotation.identity().rotationY((float) java.lang.Math.toRadians(270.0f));
		} else {
			initialPosition = new Vector3f(-8.0f, frogHeightAdjust, 0.0f);
			initialRotation.identity().rotationY((float) java.lang.Math.toRadians(90.0f));
		}
		avatar.setLocalTranslation(new Matrix4f().translation(initialPosition));
		avatar.setLocalRotation(initialRotation);

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

		// build some action objects for doing things in response to user input
		FwdAction fwdAction = new FwdAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);
		BackwardAction backwardAction = new BackwardAction(this, protClient);
		StrafeAction strafeAction = new StrafeAction(this, protClient);
		DashAction dashAction = new DashAction(this);
		ShieldAction shieldAction = new ShieldAction(this);
		AttackAction attackAction = new AttackAction(this);
		ThrowAction throwAction = new ThrowAction(this);
		ReloadAction reloadAction = new ReloadAction(this);

		String gpName = im.getFirstGamepadName();

		// attach the action objects to keyboard
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.W,
				fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.S,
				fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.A,
				strafeAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.D,
				strafeAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.LEFT,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.RIGHT,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// im.associateActionWithAllKeyboards(
		// net.java.games.input.Component.Identifier.Key.C,
		// dashAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		// im.associateActionWithAllKeyboards(
		// net.java.games.input.Component.Identifier.Key.Q,
		// shieldAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// im.associateActionWithAllKeyboards(
		// net.java.games.input.Component.Identifier.Key.E,
		// attackAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		// im.associateActionWithAllKeyboards(
		// net.java.games.input.Component.Identifier.Key.R,
		// throwAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		// im.associateActionWithAllKeyboards(
		// net.java.games.input.Component.Identifier.Key.F,
		// reloadAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// attach the action objects to keyboard
		if (gpName != null) {
			System.out.println("Gamepad found: " + gpName);

			// Movement
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Axis.Y,
					fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Axis.X,
					strafeAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Axis.Z,
					turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			// Combat/abilities
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Button._0,
					attackAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Button._3,
					throwAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Button._2,
					dashAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Axis.RZ,
					shieldAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gpName,
					net.java.games.input.Component.Identifier.Button._1,
					reloadAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		} else {
			System.out.println("No gamepad found. Using keyboard controls only.");
		}
	}

	public GameObject getAvatar() {
		return avatar;
	}

	@Override
	public void update() {
		updateHUDDisplays();
		updateProjectileAmmo();
		updateDash((float) elapsedTime / 1000.0f);

		if (!cratesPositioned) {
			try {
				if (terr != null && engine.getRenderSystem().getViewport("MAIN") != null) {
					positionCratesOnTerrain();
					cratesPositioned = true;
				}
			} catch (Exception e) {
			}
		}

		// ^ =============== animations ===============
		frogS.updateAnimation();

		// ^ =============== time setup ===============
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();

		// ^ =============== Shutdown / Gameover Checks ===============
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

		// ^ =============== Player Statuses ===============

		// * invulnerability
		if (isInvulnerable && System.currentTimeMillis() - invulnerabilityStartTime > INVULNERABILITY_DURATION) {
			isInvulnerable = false;
		}

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

		// ^ =============== Inputs and Abilities ===============
		// Calculate cooldown percentage for ball throws
		// long currentTime = System.currentTimeMillis();
		// float cooldownRemaining = 0;

		// if (currentTime - lastBallThrowTime < BALL_THROW_COOLDOWN) {
		// cooldownRemaining = (BALL_THROW_COOLDOWN - (currentTime - lastBallThrowTime))
		// / 1000.0f;
		// }

		// * input manager
		im.update((float) elapsedTime);

		// * hitbox
		if (hitboxActive) {
			checkHitboxStatus();
		}
		checkPlayerHits();

		// * shield
		if (qKeyHeld) {
			activateShield();
		}
		checkShieldStatus();

		if (shieldActive && shieldHitTime > 0) {
			long timeSinceHit = System.currentTimeMillis() - shieldHitTime;
			if (timeSinceHit > SHIELD_HIT_FLASH_DURATION) {
				shield.getRenderStates().setColor(new Vector3f(1.0f, 1.0f, 1.0f)); // Reset color
				shieldHitTime = 0;
			}
		}

		updateSwordAnimation((float) elapsedTime / 1000.0f);

		// ^ =============== Physics / Projectiles ===============
		// * Projectile
		checkBallBoundary();

		if (sphereCreated) {
			checkSphereLifetime();

			if (sphere != null && protClient != null && isClientConnected && sphereId != null) {
				protClient.sendUpdateBallMessage(sphereId, sphere.getWorldLocation());
			}

		}

		if (gm != null) {
			gm.cleanupExpiredBalls(SPHERE_LIFETIME);
			gm.update((float) elapsedTime);
		}

		// * Physics
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

		// ^ =============== Audio ===============
		setEarParameters();
		
		if (isWalking) {
			footstepSound.setLocation(avatar.getWorldLocation());
		}
		// ^ =============== Position Updates ===============
		Vector3f loc = avatar.getWorldLocation();
		float height = terr.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height + frogHeightAdjust, loc.z()));

		positionCameraBehindAvatar();
		processNetworking((float) elapsedTime);
	}

	public void setEarParameters() {
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(avatar.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}

	private void updateHUDDisplays() {
		long currentTime = System.currentTimeMillis();

		// HUD1 - Health display
		String healthStr = "Health: " + playerHealth;
		Vector3f healthColor = new Vector3f(1, 0, 0);
		(engine.getHUDmanager()).setHUD1(healthStr, healthColor, 15, 15);

		// HUD2 - Ball throw cooldown
		float throwCooldownRemaining = 0;
		if (currentTime - lastBallThrowTime < BALL_THROW_COOLDOWN) {
			throwCooldownRemaining = (BALL_THROW_COOLDOWN - (currentTime - lastBallThrowTime)) / 1000.0f;
		}

		String throwStatus = throwCooldownRemaining > 0
				? "Throw COOLDOWN: " + String.format("%.1f", throwCooldownRemaining) + "s"
				: "Throw Ready";
		Vector3f throwColor = throwCooldownRemaining > 0
				? new Vector3f(1.0f, 0.5f, 0.0f) // Orange for cooldown
				: new Vector3f(0.0f, 1.0f, 0.0f); // Green for ready
		(engine.getHUDmanager()).setHUD2(throwStatus, throwColor, 15, 40);

		// HUD3 - Shield status with clear visual indicators
		String shieldStatus;
		Vector3f shieldColor;

		if (shieldActive) {
			long remainingTime = SHIELD_DURATION - (currentTime - shieldActivationTime);
			float secondsRemaining = Math.max(0, remainingTime / 1000.0f);
			shieldStatus = "Shield: ACTIVE [" + String.format("%.1f", secondsRemaining) + "s]";
			shieldColor = new Vector3f(0, 0.7f, 1); // Blue for active shield
		} else if (shieldOnCooldown) {
			long cooldownRemaining = SHIELD_COOLDOWN - (currentTime - shieldDeactivationTime);
			float secondsRemaining = Math.max(0, cooldownRemaining / 1000.0f);
			shieldStatus = "Shield: COOLDOWN [" + String.format("%.1f", secondsRemaining) + "s]";
			shieldColor = new Vector3f(0.7f, 0.7f, 0.7f); // Gray for cooldown
		} else {
			shieldStatus = "Shield: READY [Hold Q]";
			shieldColor = new Vector3f(0.0f, 1.0f, 0.0f); // Green for ready
		}
		(engine.getHUDmanager()).setHUD3(shieldStatus, shieldColor, 15, 65);

		// HUD4 - Attacks
		StringBuilder ammoStr = new StringBuilder("Ammo: ");
		for (int i = 0; i < MAX_AMMO; i++) {
			if (i < projectileAmmo) {
				ammoStr.append("O ");
			} else {
				ammoStr.append("X ");
			}
		}

		String attackStatus;
		Vector3f attackColor;

		if (hitboxActive) {
			attackStatus = "Attack: ACTIVE";
			attackColor = new Vector3f(1.0f, 0.0f, 0.0f); // Red for active attack
		} else if (currentTime - lastAttackTime < ATTACK_COOLDOWN) {
			float attackCooldownRemaining = (ATTACK_COOLDOWN - (currentTime - lastAttackTime)) / 1000.0f;
			attackStatus = "Attack: COOLDOWN [" + String.format("%.1f", attackCooldownRemaining) + "s]";
			attackColor = new Vector3f(1.0f, 0.5f, 0.0f); // Orange for cooldown
		} else {
			attackStatus = "Attack: READY [Press E]";
			attackColor = new Vector3f(0.0f, 1.0f, 0.0f); // Green for ready
		}
		(engine.getHUDmanager()).setHUD4(attackStatus, attackColor, 15, 90);

		String hud4Text = attackStatus + " | " + ammoStr.toString();
		Vector3f hud4Color = new Vector3f(0.2f, 0.7f, 1.0f);
		(engine.getHUDmanager()).setHUD4(hud4Text, hud4Color, 15, 90);

		if (isReloading) {
			long timeRemaining = RELOAD_TIME - (System.currentTimeMillis() - reloadStartTime);
			float percentComplete = 1.0f - (timeRemaining / (float) RELOAD_TIME);

			StringBuilder reloadBar = new StringBuilder("RELOADING: [");
			int barLength = 20;
			int completedSegments = (int) (barLength * percentComplete);

			for (int i = 0; i < barLength; i++) {
				if (i < completedSegments) {
					reloadBar.append("O");
				} else {
					reloadBar.append("o");
				}
			}
			reloadBar.append("]");

			// Position in center of screen
			int centerX = screenWidth / 2 - 150;
			int centerY = screenHeight / 2;

			Vector3f reloadColor = new Vector3f(1.0f, 0.8f, 0.2f);
			(engine.getHUDmanager()).setHUD4(reloadBar.toString(), reloadColor, centerX, centerY);
		}
	}

	private void positionCameraBehindAvatar() {
		Vector4f u = new Vector4f(-1.5f, 0f, 0f, 1f);
		Vector4f v = new Vector4f(0f, 1.5f, 0f, 1f);
		Vector4f n = new Vector4f(0f, 0f, 1.5f, 1f);
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
			case KeyEvent.VK_Q: {
				if (!isDashing) {
					qKeyHeld = true;
				}
				break;
			}
			case KeyEvent.VK_ALT: {
				if (!isDashing) {
					startDash();
				}
				break;
			}
			case KeyEvent.VK_W: {
				wKeyHeld = true;
				frogS.stopAnimation();
				frogS.playAnimation("RUN", 0.5f, AnimatedShape.EndType.LOOP, 0);

				if (!isWalking) {
					footstepSound.play();
					isWalking = true;
				}
				break;
			}
			case KeyEvent.VK_A: {
				aKeyHeld = true;
				break;
			}
			case KeyEvent.VK_S: {
				sKeyHeld = true;
				break;
			}
			case KeyEvent.VK_D: {
				dKeyHeld = true;
				break;
			}
			case KeyEvent.VK_F: {
				fKeyHeld = true;
				break;
			}
			case KeyEvent.VK_H: {
				frogS.stopAnimation();
				frogS.playAnimation("HELLO", 0.25f, AnimatedShape.EndType.NONE, 0);
				break;
			}
			case KeyEvent.VK_R: {
				// Don't allow throwing balls while shield is up
				if (!isDashing && !qKeyHeld && !shieldActive) {
					long currentTime = System.currentTimeMillis();
					// Check if cooldown has passed
					if (currentTime - lastBallThrowTime > BALL_THROW_COOLDOWN) {
						createThrowableSphere();
						lastBallThrowTime = currentTime;

						if (!running) {
							running = true;
							System.out.println("starting physics");
						}
					} else {
						// Calculate remaining cooldown time in seconds
						float remainingCooldown = (BALL_THROW_COOLDOWN - (currentTime - lastBallThrowTime)) / 1000.0f;
						System.out.println("Throw on cooldown! Ready in " + remainingCooldown + " seconds");
					}
				} else {
					System.out.println("Cannot throw while shield is active!");
				}
				break;
			}
			case KeyEvent.VK_E: {
				// Don't allow hitbox activation while shield is up
				if (!isDashing && !qKeyHeld && !shieldActive) {
					activateHitbox();
				} else {
					System.out.println("Cannot use hitbox while shield is active!");
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

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_W: {
				wKeyHeld = false;
				frogS.stopAnimation();

				if (isWalking) {
					footstepSound.stop();
					isWalking = false;
				}
				break;
			}
			case KeyEvent.VK_A: {
				aKeyHeld = false;
				break;
			}
			case KeyEvent.VK_S: {
				sKeyHeld = false;
				break;
			}
			case KeyEvent.VK_D: {
				dKeyHeld = false;
				break;
			}
			case KeyEvent.VK_Q: {
				qKeyHeld = false;
				break;
			}
			case KeyEvent.VK_F: {
				fKeyHeld = false;
				break;
			}
		}
		super.keyReleased(e);
	}

	// * ---------- DASH SECTION ----------------
	public void startDash() {
		if (isDashing) {
			return;
		}

		long currentTime = System.currentTimeMillis();
		if (currentTime - lastDashTime < DASH_COOLDOWN) {
			float cooldownRemaining = (DASH_COOLDOWN - (currentTime - lastDashTime)) / 1000.0f;
			System.out.println("Dash on cooldown! Ready in " + String.format("%.1f", cooldownRemaining) + " seconds");
			return;
		}

		// Default dash direction
		Vector4f forwardDir = new Vector4f(0f, 0f, 1f, 1f);
		forwardDir.mul(avatar.getWorldRotation());
		Vector3f forward = new Vector3f(forwardDir.x(), 0, forwardDir.z()).normalize();

		Vector3f right = new Vector3f(forward.z(), 0, forward.x()).normalize();

		dashDirection = new Vector3f(0, 0, 0);
		boolean keyPressed = false;

		if (wKeyHeld) {
			dashDirection.add(forward);
			keyPressed = true;
		}

		if (sKeyHeld) {
			dashDirection.add(new Vector3f(forward).negate());
			keyPressed = true;
		}

		if (aKeyHeld) {
			dashDirection.add(new Vector3f(right).negate());
			keyPressed = true;
		}

		if (dKeyHeld) {
			dashDirection.add(right);
			keyPressed = true;
		}

		if (!keyPressed) {
			dashDirection = new Vector3f(forward).negate();
		} else {
			dashDirection.normalize();
		}

		dashStartPosition = new Vector3f(avatar.getWorldLocation());

		dashEndPosition = new Vector3f(
				dashStartPosition.x() + dashDirection.x() * DASH_DISTANCE,
				dashStartPosition.y(),
				dashStartPosition.z() + dashDirection.z() * DASH_DISTANCE);

		isDashing = true;
		dashStartTime = currentTime;
		lastDashTime = currentTime;

		// Put attacks on cooldown
		lastAttackTime = currentTime;
		lastBallThrowTime = currentTime;

		if (shieldActive) {
			deactivateShield();
		}
	}

	private void updateDash(float elapsedTime) {
		if (!isDashing)
			return;

		long currentTime = System.currentTimeMillis();
		long dashElapsedTime = currentTime - dashStartTime;

		if (dashElapsedTime >= DASH_DURATION) {
			isDashing = false;
			return;
		}

		float dashProgress = (float) dashElapsedTime / DASH_DURATION;

		Vector3f newPosition = new Vector3f();
		newPosition.x = dashStartPosition.x() + (dashEndPosition.x() - dashStartPosition.x()) * dashProgress;
		newPosition.y = dashStartPosition.y(); // Keep y-coordinate the same
		newPosition.z = dashStartPosition.z() + (dashEndPosition.z() - dashStartPosition.z()) * dashProgress;

		float height = terr.getHeight(newPosition.x(), newPosition.z());
		newPosition.y = height + frogHeightAdjust;

		avatar.setLocalLocation(newPosition);

		// Make player invulnerable during dash
		isInvulnerable = true;
		invulnerabilityStartTime = currentTime;
	}

	private void faceDirection(Vector3f direction) {
		if (direction.length() < 0.1f)
			return;

		float angle = (float) Math.atan2(direction.x(), direction.z());

		Matrix4f newRotation = new Matrix4f().identity().rotationY(angle);

		avatar.setLocalRotation(newRotation);
	}

	// * ---------- SWORD SECTION ----------------
	private void updateSwordAnimation(float elapsedTimeSeconds) {
		if (swordAnimating) {
			// Slower speed for wind-up, faster for swing-down
			float animationSpeed;

			if (swordAnimationProgress < 0.3f) {
				animationSpeed = SWORD_ANIMATION_SPEED * 0.5f;
			} else if (swordAnimationProgress < 0.6f) {
				animationSpeed = SWORD_ANIMATION_SPEED * 1.0f;

				if (!hitboxActive && swordAnimationProgress >= 0.4f && swordAnimationProgress <= 0.55f) {
					hitboxActive = true;
					hitboxActivationTime = System.currentTimeMillis();
				}
			} else {
				animationSpeed = SWORD_ANIMATION_SPEED;
			}

			// Update animation progress
			swordAnimationProgress += SWORD_ANIMATION_SPEED * elapsedTimeSeconds;

			if (swordAnimationProgress >= 1.0f) {
				// Animation complete
				swordAnimating = false;
				swordAnimationProgress = 0.0f;

				// Reset to original position
				Vector3f swordOffset = new Vector3f(-0.25f, -0.2f, 0.1f);
				sword.setLocalTranslation(new Matrix4f().translation(swordOffset));

				// Reset to original rotation
				sword.setLocalRotation(new Matrix4f().rotationY((float) Math.toRadians(-90.0f)));
			} else {
				// For the first half, swing down
				// For the second half, swing back up
				float angle;

				if (swordAnimationProgress < 0.5f) {
					// First half - swing down (0 to 60 degrees)
					angle = 90.0f * (swordAnimationProgress * 2);
				} else {
					// Second half - swing back up (60 to 0 degrees)
					angle = 90.0f * (1 - (swordAnimationProgress - 0.5f) * 2);
				}

				// Apply rotation around X axis (swing down/up)
				Matrix4f rotation = new Matrix4f()
						.rotationY((float) Math.toRadians(-90.0f))
						.rotateZ((float) Math.toRadians(-angle));

				sword.setLocalRotation(rotation);
			}
		}
	}

	private void activateShield() {
		if (!shieldActive && !shieldOnCooldown) {
			Vector3f shieldActiveOffset = new Vector3f(0.0f, 0f, 0.25f); // Original active position
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldActiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);
			Matrix4f shieldLocalRotation = (new Matrix4f()).rotationY((float) Math.toRadians(0.0f));
			shield.setLocalRotation(shieldLocalRotation);

			// Start tracking activation time
			shieldActivationTime = System.currentTimeMillis();
			shieldActive = true;

			if (protClient != null && isClientConnected) {
				protClient.sendShieldActivateMessage();
			}

			System.out.println("Shield activated");
		} else if (shieldOnCooldown) {
			// Calculate remaining cooldown time in seconds
			float remainingCooldown = (SHIELD_COOLDOWN - (System.currentTimeMillis() - shieldDeactivationTime))
					/ 1000.0f;
			System.out.println("Shield on cooldown! Ready in " + remainingCooldown + " seconds");
		}
	}

	private void deactivateShield() {
		if (shieldActive) {
			Vector3f shieldInactiveOffset = new Vector3f(0.3f, -0.25f, 0.1f);
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldInactiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);
			Matrix4f shieldLocalRotation = (new Matrix4f()).rotateY((float) Math.toRadians(30.0f));
			shield.setLocalRotation(shieldLocalRotation);
			shieldActive = false;

			shieldDeactivationTime = System.currentTimeMillis();
			shieldOnCooldown = true;

			if (protClient != null && isClientConnected) {
				protClient.sendShieldDeactivateMessage();
			}

			System.out.println("Shield deactivated and on cooldown");
		}
	}

	private void checkShieldStatus() {
		// Check if shield cooldown is over
		if (shieldOnCooldown && System.currentTimeMillis() - shieldDeactivationTime > SHIELD_COOLDOWN) {
			shieldOnCooldown = false;
			System.out.println("Shield cooldown ended");
		}

		// If Q is not held down anymore or shield duration exceeded, deactivate shield
		if (shieldActive && (!qKeyHeld || System.currentTimeMillis() - shieldActivationTime > SHIELD_DURATION)) {
			deactivateShield();
		}
	}

	public void activateHitbox() {
		long currentTime = System.currentTimeMillis();

		// Check if attack is on cooldown
		if (currentTime - lastAttackTime < ATTACK_COOLDOWN) {
			float remainingCooldown = (ATTACK_COOLDOWN - (currentTime - lastAttackTime)) / 1000.0f;
			System.out.println("Attack on cooldown! Ready in " + String.format("%.1f", remainingCooldown) + " seconds");
			return;
		}

		// Enable rendering to make it visible
		// hitbox.getRenderStates().enableRendering();

		swordAnimating = true;
		swordAnimationProgress = 0.0f;

		// Start tracking activation time
		hitboxActivationTime = System.currentTimeMillis();
		hitboxActive = true;

		lastAttackTime = currentTime;

		if (protClient != null && isClientConnected) {
			protClient.sendSwordAnimationMessage();
		}

		System.out.println("Hitbox activated");
	}

	private void checkHitboxStatus() {
		if (hitboxActive && System.currentTimeMillis() - hitboxActivationTime > HITBOX_DURATION) {
			// Deactivate the hitbox
			// hitbox.getRenderStates().disableRendering();
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
		if (shieldActive) {
			System.out.println("Hit blocked by shield!");

			if (blockSound != null) {
				blockSound.setLocation(avatar.getWorldLocation());
				blockSound.play();
			}

			shield.getRenderStates().setColor(new Vector3f(1.0f, 0.0f, 0.0f));
			shieldHitTime = System.currentTimeMillis();

			if (protClient != null && isClientConnected) {
				protClient.sendShieldHitMessage();
			}

			return; // Exit the method without taking damage
		}

		// Only apply damage if not already invulnerable
		if (!isInvulnerable) {
			playerHealth--;
			System.out.println("Player hit! Health reduced to: " + playerHealth);

			if (oofSound != null && slapSound != null) {
				slapSound.setLocation(avatar.getWorldLocation());
				oofSound.setLocation(avatar.getWorldLocation());
				oofSound.play();
				slapSound.play();
			}

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
	private final long BALL_THROW_COOLDOWN = 3000; // 3 seconds

	public void createThrowableSphere() {
		if (projectileAmmo <= 0) {
			System.out.println("Out of ammo! Find a crate to reload (F key).");
			return;
		}

		projectileAmmo--;

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

	private void updateProjectileAmmo() {
		nearestCrate = findNearestCrate();

		// Handle reloading
		if (fKeyHeld && nearestCrate != null && projectileAmmo < MAX_AMMO) {
			if (!isReloading) {
				isReloading = true;
				reloadStartTime = System.currentTimeMillis();
			} else {
				// Check if reload time has passed
				if (System.currentTimeMillis() - reloadStartTime >= RELOAD_TIME) {
					// Reload complete
					projectileAmmo++;
					isReloading = false;
					System.out.println("Reloaded! Ammo: " + projectileAmmo);
				}
			}
		} else {
			isReloading = false;
		}
	}

	private GameObject findNearestCrate() {
		Vector3f playerPos = avatar.getWorldLocation();
		GameObject[] crates = { crate1, crate2, crate3, crate4 };
		GameObject nearest = null;
		float closestDistance = crateInteractionDistance;

		for (GameObject crate : crates) {
			if (crate != null) {
				float distance = playerPos.distance(crate.getWorldLocation());
				if (distance < closestDistance) {
					closestDistance = distance;
					nearest = crate;
				}
			}
		}

		return nearest;
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
		if (shieldActive) {
			System.out.println("Ball hit blocked by shield!");

			if (blockSound != null) {
				blockSound.setLocation(avatar.getWorldLocation());
				blockSound.play();
			}

			shield.getRenderStates().setColor(new Vector3f(1.0f, 0.0f, 0.0f));
			shieldHitTime = System.currentTimeMillis();

			if (protClient != null && isClientConnected) {
				protClient.sendShieldHitMessage();
			}

			// Remove the ghost ball even though no damage was taken
			gm.removeGhostBall(ballId);
			return; // Exit without taking damage
		}

		// Reduce health
		playerHealth--;
		System.out.println("Player hit! Health reduced to: " + playerHealth);

		if (oofSound != null && slapSound != null) {
			slapSound.setLocation(avatar.getWorldLocation());
			oofSound.setLocation(avatar.getWorldLocation());
			oofSound.play();
			slapSound.play();
		}

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

	private void positionCratesOnTerrain() {
		float quadrantSize = 15.0f;

		Vector3f crate1Pos = crate1.getWorldLocation();
		float crate1Height = terr.getHeight(crate1Pos.x(), crate1Pos.z());
		crate1.setLocalTranslation(new Matrix4f().translation(crate1Pos.x(), crate1Height, crate1Pos.z()));

		Vector3f crate2Pos = crate2.getWorldLocation();
		float crate2Height = terr.getHeight(crate2Pos.x(), crate2Pos.z());
		crate2.setLocalTranslation(new Matrix4f().translation(crate2Pos.x(), crate2Height, crate2Pos.z()));

		Vector3f crate3Pos = crate3.getWorldLocation();
		float crate3Height = terr.getHeight(crate3Pos.x(), crate3Pos.z());
		crate3.setLocalTranslation(new Matrix4f().translation(crate3Pos.x(), crate3Height, crate3Pos.z()));

		Vector3f crate4Pos = crate4.getWorldLocation();
		float crate4Height = terr.getHeight(crate4Pos.x(), crate4Pos.z());
		crate4.setLocalTranslation(new Matrix4f().translation(crate4Pos.x(), crate4Height, crate4Pos.z()));

		redLight.setLocation(crate1.getWorldLocation().add(crate1.getLocalUpVector().mul(1.5f)));
		redLight1.setLocation(crate2.getWorldLocation().add(crate2.getLocalUpVector().mul(1.5f)));
		redLight2.setLocation(crate3.getWorldLocation().add(crate3.getLocalUpVector().mul(1.5f)));
		redLight3.setLocation(crate4.getWorldLocation().add(crate4.getLocalUpVector().mul(1.5f)));

		System.out.println("Crates positioned on terrain successfully");
	}

	// needed for actions
	public void setQKeyHeld(boolean held) {
		qKeyHeld = held;
	}

	public void setFKeyHeld(boolean held) {
		fKeyHeld = held;
	}
}