package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class AttackAction extends AbstractInputAction {
    private MyGame game;
    
    public AttackAction(MyGame g) {
        game = g;
    }
    
    @Override
    public void performAction(float time, Event e) {
        game.activateHitbox();
    }
}