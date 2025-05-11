package myGame;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager {
	private MyGame game;
	Matrix4f ghostRotation;

	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private Vector<GhostBall> ghostBalls = new Vector<GhostBall>();

	public GhostManager(VariableFrameRateGame vfrg) {
		game = (MyGame) vfrg;
	}

	public Vector<GhostBall> getGhostBalls() {
		return ghostBalls;
	}

	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	public void createGhostBall(UUID ballId, UUID ownerId, Vector3f position) throws IOException {
		ObjShape s = game.getSphereShape();
		TextureImage t = game.getSphereTexture();

		GhostBall newBall = new GhostBall(ballId, ownerId, s, t, position);
		ghostBalls.add(newBall);
	}

	public void removeGhostBall(UUID ballId) {
		GhostBall ghostBall = findBall(ballId);
		if (ghostBall != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostBall);
			ghostBalls.remove(ghostBall);
		}
	}

	public void updateGhostBall(UUID ballId, Vector3f position) {
		GhostBall ghostBall = findBall(ballId);
		if (ghostBall != null) {
			ghostBall.setPosition(position);
		}
	}

	private GhostBall findBall(UUID ballId) {
		Iterator<GhostBall> it = ghostBalls.iterator();
		while (it.hasNext()) {
			GhostBall ghostBall = it.next();
			if (ghostBall.getID().compareTo(ballId) == 0) {
				return ghostBall;
			}
		}
		return null;
	}

	public void cleanupExpiredBalls(long maxLifetime) {
		long currentTime = System.currentTimeMillis();
		Iterator<GhostBall> it = ghostBalls.iterator();

		while (it.hasNext()) {
			GhostBall ball = it.next();
			if (currentTime - ball.getCreationTime() > maxLifetime) {
				game.getEngine().getSceneGraph().removeGameObject(ball);
				it.remove();
			}
		}
	}

	public void createGhostAvatar(UUID id, Vector3f position, String textureName) throws IOException {
		GhostAvatar existingAvatar = findAvatar(id);
		Matrix4f ghostRotation = new Matrix4f();

		if (existingAvatar != null) {
			System.out
					.println("Avatar with ID " + id + " already exists, updating position instead of creating new one");
			existingAvatar.setPosition(position);
			return;
		}

		System.out.println("adding ghost with ID --> " + id + " using texture --> " + textureName);
		ObjShape s = game.getGhostShape();

		TextureImage t;
		if (textureName.equals("bear.png")) {
			t = game.getBearTexture(); // bear.png
		} else {
			t = game.getFrogTexture(); // frog.png
		}

		if (textureName.equals("bear.png")) {
			float zPos = position.z();
			position = new Vector3f(-8.0f, position.y(), zPos);
			ghostRotation.identity().rotationY((float) java.lang.Math.toRadians(90.0f));
		} else {
			float zPos = position.z();
			position = new Vector3f(8.0f, position.y(), zPos);
			ghostRotation.identity().rotationY((float) java.lang.Math.toRadians(270.0f));
		}

		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position);

		Matrix4f initialScale = (new Matrix4f()).scaling(0.20f);
		newAvatar.setLocalScale(initialScale);

		newAvatar.getRenderStates().setModelOrientationCorrection(
				(new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90.0f)));

		Matrix4f initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(135.0f));
		newAvatar.setLocalRotation(initialRotation);

		newAvatar.createShield(game.getShieldShape(), game.getShieldTexture());
		newAvatar.createSword(game.getSwordShape(), game.getSwordTexture());

		newAvatar.setLocalRotation(ghostRotation);
		newAvatar.setPosition(position);
		ghostAvatars.add(newAvatar);
	}

	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		} else {
			System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id) {
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while (it.hasNext()) {
			ghostAvatar = it.next();
			if (ghostAvatar.getID().compareTo(id) == 0) {
				return ghostAvatar;
			}
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.setPosition(position);
		} else {
			System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void updateGhostRotation(UUID id, Matrix4f rotation) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.setRotation(rotation);
		} else {
			System.out.println("tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}

	// ^ ========================= Shield Stuff ========================= ^ //
	public void activateGhostShield(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.activateShield();
		} else {
			System.out.println("tried to activate ghost shield, but unable to find ghost in list");
		}
	}

	public void deactivateGhostShield(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.deactivateShield();
		} else {
			System.out.println("tried to deactivate ghost shield, but unable to find ghost in list");
		}
	}

	public void triggerGhostShieldHitEffect(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.triggerShieldHitEffect();
		} else {
			System.out.println("tried to trigger ghost shield hit effect, but unable to find ghost in list");
		}
	}

	public void update(float elapsedTime) {
		for (GhostAvatar avatar : ghostAvatars) {
			avatar.update(elapsedTime);
		}
	}

	// ^ ========================= Sword Stuff ========================= ^ //
	public void triggerGhostSwordAnimation(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.startSwordAnimation();
		} else {
			System.out.println("tried to trigger ghost sword animation, but unable to find ghost in list");
		}
	}
}
