package myGame;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ShieldAction extends AbstractInputAction {
    private MyGame game;

    public ShieldAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();

        if (keyValue > -.2 && keyValue < .2)
            return;

        if (e.getValue() < -0.75f) {
            game.setQKeyHeld(true);
        } else {
            game.setQKeyHeld(false);
        }
    }
}