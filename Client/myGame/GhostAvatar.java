package myGame;

import java.util.UUID;

import tage.*;
import org.joml.*;
import tage.shapes.AnimatedShape;
import tage.shapes.Cube;
import java.lang.Math;

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

		Vector3f shieldLocalOffset = new Vector3f(0.3f, -0.25f, 0.1f);
		Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldLocalOffset);
		shield.setLocalTranslation(shieldLocalTranslation);

		Matrix4f shieldLocalRotation = (new Matrix4f()).rotateY((float) java.lang.Math.toRadians(30.0f));
		shield.setLocalRotation(shieldLocalRotation);

		Matrix4f shieldScale = (new Matrix4f()).scaling(0.75f);
		shield.setLocalScale(shieldScale);

	}

	public void activateShield() {
		if (shield != null) {
			Vector3f shieldActiveOffset = new Vector3f(0.0f, 0.0f, 0.25f);
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldActiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);
			Matrix4f shieldLocalRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(0.0f));
			shield.setLocalRotation(shieldLocalRotation);

			shieldActive = true;
		}
	}

	public void deactivateShield() {
		if (shield != null) {
			Vector3f shieldInactiveOffset = new Vector3f(0.3f, -0.25f, 0.1f);
			Matrix4f shieldLocalTranslation = (new Matrix4f()).translation(shieldInactiveOffset);
			shield.setLocalTranslation(shieldLocalTranslation);
			Matrix4f shieldLocalRotation = (new Matrix4f()).rotateY((float) java.lang.Math.toRadians(30.0f));
			shield.setLocalRotation(shieldLocalRotation);

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

		Vector3f swordOffset = new Vector3f(-0.25f, -0.15f, 0.1f);
		Matrix4f swordLocalTranslation = (new Matrix4f()).translation(swordOffset);
		sword.setLocalTranslation(swordLocalTranslation);

		Matrix4f swordLocalRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90.0f));
		sword.setLocalRotation(swordLocalRotation);

		Matrix4f swordScale = (new Matrix4f()).scaling(2.75f);
		sword.setLocalScale(swordScale);
	}

	public void startSwordAnimation() {
		if (sword != null) {
			swordAnimating = true;
			swordAnimationProgress = 0.0f;
		}
	}

	private void updateSwordAnimation(float elapsedTimeSeconds) {
		if (swordAnimating) {
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
				// Simple swing down and up animation
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

	// ^ ========================= Animation Stuff ========================= ^ //
	private boolean isAnimating = false;

	public void startRunAnimation() {
		if (this.getShape() instanceof AnimatedShape) {
			AnimatedShape animShape = (AnimatedShape) this.getShape();
			animShape.stopAnimation();
			animShape.playAnimation("RUN", 0.5f, AnimatedShape.EndType.LOOP, 0);
			isAnimating = true;
		}
	}

	public void stopAnimation() {
		if (this.getShape() instanceof AnimatedShape) {
			AnimatedShape animShape = (AnimatedShape) this.getShape();
			animShape.stopAnimation();
			isAnimating = false;
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

		if (this.getShape() instanceof AnimatedShape) {
			((AnimatedShape) this.getShape()).updateAnimation();
		}
	}

}
