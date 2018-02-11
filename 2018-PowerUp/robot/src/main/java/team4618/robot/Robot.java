package team4618.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class Robot extends TimedRobot {
    public Joystick driver = new Joystick(0);
    public Joystick op = new Joystick(1);

    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable autoTable;

    DriveSubsystem driveSubsystem = new DriveSubsystem();
    ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue("Crumb Tray");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        autoProgram = new CommandSequence(network.getTable("Custom Dashboard/Executing"));
        Subsystems.init();
    }

    public void robotPeriodic() {
        Subsystems.postState();

        if(driver.getRawButton(4)) {
            driveSubsystem.navx.reset();
        }
    }

    public void autonomousInit() {
        autoProgram.reset();

        String[] ordered = autoTable.getSubTables().toArray(new String[0]);
        Arrays.sort(ordered);
        for (String i : ordered) {
            NetworkTable table = autoTable.getSubTable(i);

            if(table.containsKey("Subsystem Name") && table.containsKey("Command Name") && table.containsKey("Params")) {
                autoProgram.addCommand(table.getEntry("Subsystem Name").getString(""),
                                       table.getEntry("Command Name").getString(""),
                                       table.getEntry("Params").getDoubleArray(new double[0]));
            }
        }
    }

    public void autonomousPeriodic() {
        autoProgram.run();
    }

    /*
    public void autonomousPeriodic() {
        driveSubsystem.left.setSetpoint(4);
        driveSubsystem.right.setSetpoint(4);
    }
    */

    public void teleopInit() {
        elevatorSubsystem.intakeUp = true;
        elevatorSubsystem.intakeOpen = false;
    }

    public void teleopPeriodic() {
        driveSubsystem.doTeleop(driver);
        elevatorSubsystem.doTeleop(op);
    }

    public void disabledInit() { }

    public void disabledPeriodic() { }
}