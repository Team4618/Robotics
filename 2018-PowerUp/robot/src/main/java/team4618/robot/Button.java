package team4618.robot;

import edu.wpi.first.wpilibj.Joystick;

import java.util.ArrayList;
import java.util.function.Supplier;

public class Button {
    public static ArrayList<Button> buttons = new ArrayList<>();

    boolean wasDown = false;
    public boolean released = false;
    boolean pressBegin = false;
    Supplier<Boolean> down;

    public Button(Joystick joystick, int buttonIndex) {
        this(() -> joystick.getRawButton(buttonIndex));
    }

    public Button(Supplier<Boolean> isDown) {
        this.down = isDown;
        buttons.add(this);
    }

    public boolean isDown() {
        return down.get();
    }

    public void tick() {
        released = wasDown && !isDown();
        pressBegin = !wasDown && isDown();
        wasDown = isDown();

    }

    public void reset() {}

    public static void tickAll() { buttons.forEach(Button::tick); }
    public static void resetAll() { buttons.forEach(Button::reset); }
}
