package team4618.robot;

import edu.wpi.first.wpilibj.Joystick;

public class ToggleButton extends Button {
    boolean defaultState;
    boolean state;

    public ToggleButton(Joystick joystick, int buttonIndex, boolean defaultState) {
        super(joystick, buttonIndex);
        this.defaultState = defaultState;
        state = defaultState;
    }

    @Override
    public void tick() {
        super.tick();
        if(released) { state = !state; }
    }

    @Override
    public void reset() {
        state = defaultState;
    }
}
