package team4618.robot;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static team4618.robot.subsystems.DriveSubsystem.Parameters.SpeedLimit;
import static team4618.robot.subsystems.DriveSubsystem.Parameters.TiltCorrectAngle;
import static team4618.robot.subsystems.DriveSubsystem.Parameters.TurnLimit;

public class Robot extends TimedRobot {
    public Joystick driver = new Joystick(0);
    public Joystick op = new Joystick(1);

    //TODO: move all this into a seperate class
    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable autoTable;
    public static NetworkTable logicTable;

    DriveSubsystem driveSubsystem = new DriveSubsystem();
    ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue("Shopping Cart");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        logicTable = network.getTable("Custom Dashboard/Logic");
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
            } else if(table.containsKey("Conditional") && table.containsSubTable("Commands")) {
                //TODO: add conditional branch command
            }
        }

        autoProgram.addCommand("Drive", "driveDistance", new double[] {18, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {270, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {20, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {180, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {14, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {90, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {14, 4, 2, 2});
    }

    public void autonomousPeriodic() { autoProgram.run(); }

    /*
    public void autonomousPeriodic() {
        driveSubsystem.left.setSetpoint(4);
        driveSubsystem.right.setSetpoint(4);
    }
    */

    public void teleopInit() {
        //elevatorSubsystem.intakeUp = true;
        //elevatorSubsystem.intakeOpen = false;
    }

    boolean was9Down = false;
    boolean was5Down = false;
    boolean was6Down = false;

    boolean lowGear = false;

    public void teleopPeriodic() {
        //driveSubsystem.doTeleop(driver);
        //elevatorSubsystem.doTeleop(op);

        //driveSubsystem.teleopDrive.arcadeDrive(op.getRawAxis(0), -0.9 * op.getRawAxis(1));

        boolean is9Down = driver.getRawButton(9);
        if(was9Down && !is9Down) {
            lowGear = !lowGear;
        }
        was9Down = is9Down;

        boolean is5Down = driver.getRawButton(5);
        if(was5Down && !is5Down) {

        }
        was5Down = is5Down;

        boolean is6Down = driver.getRawButton(6);
        if(was6Down && !is6Down) {

        }
        was6Down = is6Down;

        driveSubsystem.shifter.set(lowGear ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive((lowGear ? 1.0 : 0.85) * driver.getRawAxis(0), -(lowGear ? 0.85 : 0.85) * driver.getRawAxis(1));

        if(driver.getRawAxis(2) > 0.1) {
            elevatorSubsystem.leftIntake.set(-5 * driver.getRawAxis(2));
            elevatorSubsystem.rightIntake.set(-5 * driver.getRawAxis(2));
        } else if(driver.getRawAxis(3) > 0.1) {
            elevatorSubsystem.leftIntake.set(0.75);
            elevatorSubsystem.rightIntake.set(0.75);
        } else {
            elevatorSubsystem.leftIntake.set(0);
            elevatorSubsystem.rightIntake.set(0);
        }

        elevatorSubsystem.intakeHorizontal.set(op.getRawButton(3) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        if(op.getRawButton(1)) {
            elevatorSubsystem.elevatorShepherd.set(op.getRawAxis(1));
            elevatorSubsystem.leftLift.set(0);
            elevatorSubsystem.rightLift.set(0);
        } else {
            elevatorSubsystem.elevatorShepherd.set(0);
            elevatorSubsystem.leftLift.set(op.getRawAxis(1));
            elevatorSubsystem.rightLift.set(-op.getRawAxis(1));
        }
    }

    public void disabledInit() { }

    public void disabledPeriodic() { }

    //TODO: logic loader
    public @interface Logic {}

    @Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
}