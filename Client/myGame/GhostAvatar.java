package myGame;

import java.util.UUID;

import tage.*;
import org.joml.*;
import tage.shapes.Cube;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject {
	private UUID uuid;
	private GameObject shield;
	private boolean shieldActive = false;
	private long shieldHitTime = 0;
	private final long SHIELD_HIT_FLASH_DURATION = 200;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {
		super(GameObject.root(), s, t);
		uuid = id;
		setPosition(p);
		setLocalRotation(new Matrix4f().identity());
	}

	public UUID getID() {
		return uuid;
	}

	public void setPosition(Vector3f m) {
		setLocalLocation(m);
	}

	public Vector3f getPosition() {
		return getWorldLocation();
	}

	public void setRotation(Matrix4f rotation) {
		setLocalRotation(rotation);

	}

	public Matrix4f getRotation() {
		return getWorldRotation();
	}

	// ^ ========================= Shield Stuff ========================= ^ //
	public void createShield(ObjShape shieldShape, TextureImage shieldTexture) {
		shield = new GameObject(this, shieldShape, shieldTexture);
		shield.propagateTranslation(true);
		shield.propagateRotation(true);
		shield.applyParentRotationToPosition(true);

		Vector3f shieldLocalOffset = new Vector3f(0.0f, 0.15f, 0.25f);
		Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldLocalOffset);
		shield.setLocalTranslation(shieldLocalTranslation);

		Matrix4f shieldScale = (new Matrix4f()).scaling(0.25f);
		shield.setLocalScale(shieldScale);

		shield.getRenderStates().disableRendering();
	}

	public void activateShield() {
		if (shield != null) {
			shield.getRenderStates().enableRendering();
			shieldActive = true;
		}
	}

	public void deactivateShield() {
		if (shield != null) {
			shield.getRenderStates().disableRendering();
			shieldActive = false;
		}
	}

	public boolean isShieldActive() {
		return shieldActive;
	}

	public void triggerShieldHitEffect() {
		if (shield != null && shieldActive) {
			shield.getRenderStates().setColor(new Vector3f(1.0f, 0.0f, 0.0f)); // Red flash
			shieldHitTime = System.currentTimeMillis();
		}
	}

	public void update(float elapsedTime) {
		// Update shield hit flash effect
		if (shield != null && shieldActive && shieldHitTime > 0) {
			long timeSinceHit = System.currentTimeMillis() - shieldHitTime;
			if (timeSinceHit > SHIELD_HIT_FLASH_DURATION) {
				shield.getRenderStates().setColor(new Vector3f(1.0f, 1.0f, 1.0f)); // Reset color
				shieldHitTime = 0;
			}
		}
	}

}
