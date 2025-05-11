package myGame;

import java.lang.Math;
import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class FwdAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    private ProtocolClient protClient;
    private final float DEADZONE = 0.2f;

    public FwdAction(MyGame g, ProtocolClient p) {
        game = g;
        protClient = p;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();

        // Deadzone
        if (keyValue > -.2 && keyValue < .2)
            return;

        String componentName = e.getComponent().getIdentifier().getName();
        float direction;

        if (componentName.equals("W")) {
            direction = 1.0f;
        } else if (componentName.equals("S")) {
            direction = -1.0f;
        } else {
            direction = -Math.signum(keyValue);
        }

        av = game.getAvatar();
        oldPosition = av.getWorldLocation();
        fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
        fwdDirection.mul(av.getWorldRotation());

        fwdDirection.mul(0.0035f * time * direction);

        newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
        av.setLocalLocation(newPosition);

        if (protClient != null) {
            protClient.sendMoveMessage(av.getWorldLocation());
        }
    }
}