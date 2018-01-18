package team4618.robot;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Timer;

//TODO: Maybe change to timed robot, investigate command based
public class Robot extends SampleRobot {
    public Joystick driver = new Joystick(0);
    public DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static NetworkTableInstance network;

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        //Subsystems.init();
        driveSubsystem.initSystem();
    }

    public void operatorControl() {
        //Subsystems.enable();
        driveSubsystem.enable();

        while (isOperatorControl() && isEnabled()) {
            driveSubsystem.shifter.set(driver.getRawButton(5) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);

            //I don't like multiplying by a negative to flip the direction
            double left_speed = -10 * driver.getRawAxis(1);
            double right_speed = -10 * driver.getRawAxis(5);
            driveSubsystem.left.controller.setSetpoint(left_speed);
            driveSubsystem.right.controller.setSetpoint(right_speed);

            driveSubsystem.postState();

            Timer.delay(0.05);
        }

        driveSubsystem.disable();
    }
}