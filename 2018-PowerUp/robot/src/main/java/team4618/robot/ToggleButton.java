package team4618.robot;

import edu.wpi.first.wpilibj.Joystick;
import java.util.function.Supplier;

public class ToggleButton extends Button {
    public boolean defaultState;
    public boolean state;

    public ToggleButton(Joystick joystick, int buttonIndex, boolean defaultState) {
        super(joystick, buttonIndex);
        this.defaultState = defaultState;
        state = defaultState;
    }

    public ToggleButton(Supplier<Boolean> isdown, boolean defaultState) {
        super(isdown);
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
