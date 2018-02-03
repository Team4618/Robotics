package team4618.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class Robot extends TimedRobot {
    public Joystick driver = new Joystick(0);
    public DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable currentlyExecutingTable;
    public static NetworkTable autoTable;

    public WPI_TalonSRX elevatorShepherd = new WPI_TalonSRX(58);
    public WPI_VictorSPX elevatorSheep = new WPI_VictorSPX(56);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(50);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(3);

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue("Crumb Tray");
        currentlyExecutingTable = network.getTable("Custom Dashboard/Executing");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        Subsystems.init();

        elevatorSheep.follow(elevatorShepherd);
    }

    public void robotPeriodic() {
        Subsystems.postState();
    }

    public void teleopInit() { }

    public void teleopPeriodic() {
        double multiplier = (driver.getRawButton(5) || driver.getRawButton(6)) ? 1.0 : 0.80;
        driveSubsystem.shifter.set(driver.getRawButton(5) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive(0.80 * driver.getRawAxis(4), -multiplier * driver.getRawAxis(1));
    }

    public void disabledInit() { }

    public void disabledPeriodic() { }
}