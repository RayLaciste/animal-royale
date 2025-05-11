package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class StrafeAction extends AbstractInputAction 
{
    private MyGame game;
    private GameObject av;
    private Vector4f rightDirection;
    private ProtocolClient protClient;
    private float speed = 0.01f;
    
    public StrafeAction(MyGame g, ProtocolClient p) 
    {
        game = g;
        protClient = p;
    }
    
    @Override
    public void performAction(float time, Event e) 
    {
        float keyValue = e.getValue();
        
        if (keyValue > -.2 && keyValue < .2)
            return;
            
        float direction = (e.getComponent().getIdentifier().getName().equals("D")) ? -1.0f : 1.0f;
        
        av = game.getAvatar();
        Vector3f oldPosition = av.getWorldLocation();
        
        Matrix4f rotation = new Matrix4f(av.getWorldRotation());
        Vector4f fwd = new Vector4f(0f, 0f, 1f, 1f).mul(rotation).normalize();
        rightDirection = new Vector4f(fwd.z(), 0f, -fwd.x(), 1f).normalize();
        
        Vector3f newPosition = new Vector3f(
            oldPosition.x() + rightDirection.x() * direction * speed * time,
            oldPosition.y(),
            oldPosition.z() + rightDirection.z() * direction * speed * time
        );
        
        av.setLocalLocation(newPosition);
        
        if (protClient != null) {
            protClient.sendMoveMessage(av.getWorldLocation());
        }
    }
}