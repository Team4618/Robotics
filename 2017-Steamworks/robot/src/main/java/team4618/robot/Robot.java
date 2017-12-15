package team4618.robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Timer;

public class Robot extends SampleRobot {

    RobotDrive drivetrain;
    Joystick driver;

    public void robotInit() {
        drivetrain = new RobotDrive(1, 2, 3, 4);
        driver = new Joystick(0);
    }

    public void operatorControl() {
        while (isOperatorControl() && isEnabled()) {
            drivetrain.arcadeDrive(driver.getRawAxis(0) /*power*/, driver.getRawAxis(1) /*rotate*/);
            Timer.delay(0.05);
        }
    }
}
