package myGame;

import tage.*;
import org.joml.*;
import java.util.UUID;

public class GhostBall extends GameObject {
    private UUID id;
    private UUID ownerId; // The ID of the client who threw the ball
    private long creationTime;
    
    public GhostBall(UUID id, UUID ownerId, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        this.id = id;
        this.ownerId = ownerId;
        this.creationTime = System.currentTimeMillis();
        setLocalLocation(p);
        
        // Set scale similar to the local ball
        Matrix4f initialScale = (new Matrix4f()).scaling(0.07f);
        setLocalScale(initialScale);
    }
    
    public UUID getID() { return id; }
    public UUID getOwnerID() { return ownerId; }
    public long getCreationTime() { return creationTime; }
    
    public void setPosition(Vector3f p) { setLocalLocation(p); }
}