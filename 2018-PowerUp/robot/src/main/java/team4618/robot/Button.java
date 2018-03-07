package team4618.robot;

import edu.wpi.first.wpilibj.Joystick;

import java.util.ArrayList;

public class Button {
    public static ArrayList<Button> buttons = new ArrayList<>();

    Joystick joystick;
    int buttonIndex;
    boolean wasDown = false;
    boolean released = false;

    public Button(Joystick joystick, int buttonIndex) {
        this.joystick = joystick;
        this.buttonIndex = buttonIndex;
        buttons.add(this);
    }

    public boolean isDown() {
        return joystick.getRawButton(buttonIndex);
    }

    public void tick() {
        released = wasDown && !isDown();
        wasDown = isDown();
    }

    public void reset() {}

    public static void tickAll() { buttons.forEach(Button::tick); }
    public static void resetAll() { buttons.forEach(Button::reset); }
}
