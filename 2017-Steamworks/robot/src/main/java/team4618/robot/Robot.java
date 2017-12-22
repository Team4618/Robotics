package team4618.robot;

import edu.wpi.first.wpilibj.*;

public class Robot extends SampleRobot {

    public RobotDrive drivetrain;
    public Joystick driver;
    public DoubleSolenoid shifter;

    public void robotInit() {
        drivetrain = new RobotDrive(1, 2, 0, 3);

        driver = new Joystick(0);
        shifter = new DoubleSolenoid(0, 1);
    }

    public void operatorControl() {
        while (isOperatorControl() && isEnabled()) {
            drivetrain.arcadeDrive(-driver.getRawAxis(1), driver.getRawAxis(4));

            if(driver.getRawButton(5)) {
                shifter.set(DoubleSolenoid.Value.kForward);
            } else {
                shifter.set(DoubleSolenoid.Value.kReverse);
            }

            Timer.delay(0.05);
        }
    }
}