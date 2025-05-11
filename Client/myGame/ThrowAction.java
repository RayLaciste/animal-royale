package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ThrowAction extends AbstractInputAction {
    private MyGame game;
    
    public ThrowAction(MyGame g) {
        game = g;
    }
    
    @Override
    public void performAction(float time, Event e) {
        game.createThrowableSphere();
    }
}