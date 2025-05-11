package myGame;

import java.lang.Math;
import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class BackwardAction extends AbstractInputAction {
    private MyGame game;
    private GameObject av;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    private ProtocolClient protClient;
    private final float DEADZONE = 0.2f;

    public BackwardAction(MyGame g, ProtocolClient p) {
        game = g;
        protClient = p;
    }
    
    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();
        
        // Deadzone
        if (Math.abs(keyValue) < DEADZONE) {
            return;
        }

        float inputScale = Math.abs(keyValue);
        float directionMultiplier = (keyValue < 0) ? -1.0f : 1.0f;
        
        float normalizedValue = (Math.abs(keyValue) - DEADZONE) / (1.0f - DEADZONE);
        normalizedValue = Math.max(0.0f, Math.min(1.0f, normalizedValue));
        
        av = game.getAvatar();
        oldPosition = av.getWorldLocation();
        fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
        fwdDirection.mul(av.getWorldRotation());
        
        fwdDirection.mul(0.0035f * time * normalizedValue * directionMultiplier);
        
        newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
        av.setLocalLocation(newPosition);
        
        if (protClient != null) {
            protClient.sendMoveMessage(av.getWorldLocation());
        }
    }
}