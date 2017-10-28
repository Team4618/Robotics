package team4618;

import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;

public class Robot extends SampleRobot {

    Victor motorController;

    public void robotInit() {
        motorController = new Victor(2);
    }

    public void autonomous() {
        while (isAutonomous() && isEnabled()) {
            Timer.delay(0.05);
        }
    }

    public void operatorControl() {
        while (isOperatorControl() && isEnabled()) {
            motorController.set(0.5);
            Timer.delay(0.05);
        }
    }
}
