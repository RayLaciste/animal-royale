package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ReloadAction extends AbstractInputAction {
    private MyGame game;
    
    public ReloadAction(MyGame g) {
        game = g;
    }
    
    @Override
    public void performAction(float time, Event e) {
        if (e.getValue() > 0.5f) {
            game.setFKeyHeld(true);
        } else {
            game.setFKeyHeld(false);
        }
    }
}