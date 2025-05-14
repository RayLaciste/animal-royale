package myGame;

import tage.*;
import tage.input.action.*;
import net.java.games.input.Event;
import org.joml.*;

public class ShieldToggleAction extends AbstractInputAction {
    private MyGame game;
    private ProtocolClient protClient;

    public ShieldToggleAction(MyGame g, ProtocolClient p) {
        game = g;
        protClient = p;
    }

    @Override
    public void performAction(float time, Event e) {
        game.toggleShield();
    }
}
