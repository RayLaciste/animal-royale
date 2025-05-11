package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class DashAction extends AbstractInputAction {
    private MyGame game;
    
    public DashAction(MyGame g) {
        game = g;
    }
    
    @Override
    public void performAction(float time, Event e) {
        game.startDash();
    }
}