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

	private GameObject sword;
	private float swordAnimationProgress = 0.0f;
	private boolean swordAnimating = false;
	private final float SWORD_ANIMATION_SPEED = 3.0f;

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

		Vector3f shieldLocalOffset = new Vector3f(0.2f, 0.15f, 0.1f);
		Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldLocalOffset);
		shield.setLocalTranslation(shieldLocalTranslation);

		Matrix4f shieldScale = (new Matrix4f()).scaling(0.25f);
		shield.setLocalScale(shieldScale);

	}

	public void activateShield() {
		if (shield != null) {
			Vector3f shieldActiveOffset = new Vector3f(0.0f, 0.15f, 0.25f);
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldActiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);

			shieldActive = true;
		}
	}

	public void deactivateShield() {
		if (shield != null) {
			Vector3f shieldInactiveOffset = new Vector3f(0.2f, 0.15f, 0.1f);
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldInactiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);

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

	// ^ ========================= Sword Stuff ========================= ^ //
	public void createSword(ObjShape swordShape, TextureImage swordTexture) {
		sword = new GameObject(this, swordShape, swordTexture);
		sword.propagateTranslation(true);
		sword.propagateRotation(true);
		sword.applyParentRotationToPosition(true);

		Vector3f swordOffset = new Vector3f(-0.2f, 0.15f, 0.1f);
		Matrix4f swordLocalTranslation = (new Matrix4f()).translation(swordOffset);
		sword.setLocalTranslation(swordLocalTranslation);

		Matrix4f swordLocalRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90.0f));
		sword.setLocalRotation(swordLocalRotation);

		Matrix4f swordScale = (new Matrix4f()).scaling(0.75f);
		sword.setLocalScale(swordScale);
	}

	public void startSwordAnimation() {
		if (sword != null) {
			swordAnimating = true;
			swordAnimationProgress = 0.0f;
		}
	}

	private void updateSwordAnimation(float elapsedTimeSeconds) {
		if (sword != null && swordAnimating) {
			// Update animation progress
			swordAnimationProgress += SWORD_ANIMATION_SPEED * elapsedTimeSeconds;

			if (swordAnimationProgress >= 1.0f) {
				// Animation complete - return to original position
				swordAnimating = false;
				swordAnimationProgress = 0.0f;

				// Reset sword to original position (pointing up)
				Vector3f swordOffset = new Vector3f(-0.2f, 0.15f, 0.1f);
				Matrix4f swordLocalTranslation = (new Matrix4f()).translation(swordOffset);
				sword.setLocalTranslation(swordLocalTranslation);

				// Reset to original rotation (pointing up)
				Matrix4f swordLocalRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90.0f));
				sword.setLocalRotation(swordLocalRotation);
			} else {
				// Simple diagonal downward slash
				// Using a sine wave to create a smooth up-down motion
				float verticalOffset = (float) java.lang.Math.sin(swordAnimationProgress * java.lang.Math.PI);

				// Calculate positions:
				// Start from original position, go down diagonally to the left, then back up
				float xOffset = -0.2f - (verticalOffset * -0.2f); // Move left as goes down
				float yOffset = 0.15f - (verticalOffset * 0.2f); // Move down then up
				float zOffset = 0.1f + (swordAnimationProgress * 0.1f); // Move slightly forward throughout

				Vector3f swordOffset = new Vector3f(xOffset, yOffset, zOffset);
				Matrix4f swordLocalTranslation = (new Matrix4f()).translation(swordOffset);
				sword.setLocalTranslation(swordLocalTranslation);

				// Rotate the sword to follow the diagonal path
				float tiltAngle = (float) java.lang.Math.toRadians(-30.0f * verticalOffset); // Tilt as it moves down
				float twistAngle = (float) java.lang.Math.toRadians(-45.0f * swordAnimationProgress); // Twist
																										// throughout

				Matrix4f swordLocalRotation = new Matrix4f();
				swordLocalRotation.rotationY((float) java.lang.Math.toRadians(-90.0f)); // Start vertical
				swordLocalRotation.rotateX(tiltAngle); // Tilt forward as it moves down
				swordLocalRotation.rotateZ(twistAngle); // Twist slightly for diagonal motion

				sword.setLocalRotation(swordLocalRotation);
			}
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

		updateSwordAnimation(elapsedTime / 1000.0f);
	}

}
