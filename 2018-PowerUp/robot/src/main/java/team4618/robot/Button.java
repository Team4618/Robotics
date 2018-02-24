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
        released = false;
        if(wasDown && !isDown()) {
            released = true;
        }
        wasDown = isDown();
    }

    public static void tickAll() {
        for(Button b : buttons) {
            b.tick();
        }
    }
}
