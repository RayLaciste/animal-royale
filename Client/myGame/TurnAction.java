package myGame;

import java.lang.Math;
import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction {
	private MyGame game;
	private GameObject av;
	private Vector4f oldUp;
	private Matrix4f rotAroundAvatarUp, oldRotation, newRotation;
	private ProtocolClient protClient;

	public TurnAction(MyGame g, ProtocolClient p) {
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e) {
		float keyValue = e.getValue();

		if (keyValue > -.2 && keyValue < .2)
			return;

		String componentName = e.getComponent().getIdentifier().getName();
		float direction;

		if (componentName.equals("Left")) {
			direction = -1.0f;
		} else if (componentName.equals("Right")) {
			direction = 1.0f;
		} else {
			direction = Math.signum(keyValue);
		}

		av = game.getAvatar();
		oldRotation = new Matrix4f(av.getWorldRotation());
		oldUp = new Vector4f(0f, 1f, 0f, 1f).mul(oldRotation);

		rotAroundAvatarUp = new Matrix4f().rotation(
				-0.0035f * time * direction * Math.abs(keyValue),
				new Vector3f(oldUp.x(), oldUp.y(), oldUp.z()));

		newRotation = oldRotation;
		newRotation.mul(rotAroundAvatarUp);
		av.setLocalRotation(newRotation);

		if (protClient != null) {
			protClient.sendRotateMessage(av.getWorldRotation());
		}
	}
}