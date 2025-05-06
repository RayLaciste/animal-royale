package myGame;
import tage.*;
import tage.input.action.*;
import net.java.games.input.Event;
import org.joml.*;

public class JumpAction extends AbstractInputAction {
    private MyGame game;
    private ProtocolClient protClient;
    
    // Track if we're currently in a jump
    private boolean isJumping = false;
    
    // Track the time since the jump started
    private long jumpStartTime = 0;
    
    // How long to stay in the air (in milliseconds)
    private final long JUMP_DURATION = 500; // 0.5 seconds
    
    // Original position to return to
    private Vector3f originalPos = null;
   
    public JumpAction(MyGame g, ProtocolClient p) {
        game = g;
        protClient = p;
    }
   
    @Override
    public void performAction(float time, Event e) {
        // Only trigger a jump if we're not already jumping
        if (!isJumping) {
            // Get avatar
            GameObject avatar = game.getAvatar();
           
            // Get and store current position as the original
            originalPos = new Vector3f(avatar.getWorldLocation());
           
            // Create new position 1 unit higher
            Vector3f newPos = new Vector3f(
                originalPos.x(),
                originalPos.y() + 1.0f,  // Just move up by 1 unit immediately
                originalPos.z()
            );
           
            // Apply the new position
            avatar.setLocalLocation(newPos);
            System.out.println("Jump executed! New Y position: " + newPos.y());
           
            // Send to server if needed
            if (protClient != null) {
                protClient.sendMoveMessage(newPos);
            }
            
            // Start tracking the jump
            isJumping = true;
            jumpStartTime = System.currentTimeMillis();
        }
    }
    
    // Call this method from your game's update loop
    public void update() {
        // If we're in a jump and the duration has elapsed
        if (isJumping && (System.currentTimeMillis() - jumpStartTime > JUMP_DURATION)) {
            // Get avatar
            GameObject avatar = game.getAvatar();
            
            // Return to original position
            avatar.setLocalLocation(originalPos);
            System.out.println("Jump ended! Returned to Y position: " + originalPos.y());
            
            // Send to server if needed
            if (protClient != null) {
                protClient.sendMoveMessage(originalPos);
            }
            
            // Reset jump state
            isJumping = false;
            originalPos = null;
        }
    }
    
    // Check if currently jumping
    public boolean isJumping() {
        return isJumping;
    }
}