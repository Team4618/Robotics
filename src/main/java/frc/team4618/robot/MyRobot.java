package frc.team4618.robot;

import edu.wpi.first.wpilibj.IterativeRobot;

public class MyRobot extends SampleRobot {

    Victor motorController;

    public void robotInit() {
        motorController = new Victor(2);
    }

    public void autonomous() {
        while (isAutonomous() && isEnable()) {
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
