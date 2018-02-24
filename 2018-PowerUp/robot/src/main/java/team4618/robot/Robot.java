package team4618.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;
import team4618.robot.subsystems.IntakeSubsystem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static team4618.robot.subsystems.DriveSubsystem.Parameters.SpeedLimit;
import static team4618.robot.subsystems.DriveSubsystem.Parameters.TiltCorrectAngle;
import static team4618.robot.subsystems.DriveSubsystem.Parameters.TurnLimit;
import static team4618.robot.subsystems.ElevatorSubsystem.Parameters.*;

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
    IntakeSubsystem intakeSubsystem = new IntakeSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue("Shopping Cart");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        logicTable = network.getTable("Custom Dashboard/Logic");
        autoProgram = new CommandSequence(network.getTable("Custom Dashboard/Executing"));
        Subsystems.init();

        for(Method logicProvider : this.getClass().getDeclaredMethods()) {
            if(logicProvider.isAnnotationPresent(Logic.class)) {
                logicTable.getEntry(logicProvider.getName()).setString("Unknown");
            }
        }
    }

    public void robotPeriodic() {
        Subsystems.postState();

        if(driver.getRawButton(4)) {
            driveSubsystem.navx.reset();
            elevatorSubsystem.elevatorShepherd.setSelectedSensorPosition(0, 0, 0);
            elevatorSubsystem.elevatorShepherd.set(0);
        }
    }

    public void loadCommandsFromTable(NetworkTable table) {
        String[] ordered = table.getSubTables().toArray(new String[0]);
        Arrays.sort(ordered);
        boolean choseConditional = false;
        for (String i : ordered) {
            NetworkTable currCommandTable = table.getSubTable(i);

            if(currCommandTable.containsKey("Subsystem Name") && currCommandTable.containsKey("Command Name") && currCommandTable.containsKey("Params")) {
                autoProgram.addCommand(currCommandTable.getEntry("Subsystem Name").getString(""),
                        currCommandTable.getEntry("Command Name").getString(""),
                        currCommandTable.getEntry("Params").getDoubleArray(new double[0]));
            } else if(currCommandTable.containsKey("Conditional") && currCommandTable.containsSubTable("Commands") && !choseConditional) {
                boolean hasCondition = !currCommandTable.getEntry("Conditional").getString("").equals("null");
                boolean condition = logicTable.getEntry(currCommandTable.getEntry("Conditional").getString("")).getString("").equals("True");
                if(!hasCondition || condition) {
                    choseConditional = true;
                    loadCommandsFromTable(currCommandTable.getSubTable("Commands"));
                }
            }
        }
    }

    public void autonomousInit() {
        for(Method logicProvider : this.getClass().getDeclaredMethods()) {
            if(logicProvider.isAnnotationPresent(Logic.class)) {
                try {
                    logicTable.getEntry(logicProvider.getName()).setString((Boolean) logicProvider.invoke(this) ? "True" : "False");
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        autoProgram.reset();

        /*
        String[] ordered = autoTable.getSubTables().toArray(new String[0]);
        Arrays.sort(ordered);
        for (String i : ordered) {
            NetworkTable table = autoTable.getSubTable(i);

            if(table.containsKey("Subsystem Name") && table.containsKey("Command Name") && table.containsKey("Params")) {
                autoProgram.addCommand(table.getEntry("Subsystem Name").getString(""),
                                       table.getEntry("Command Name").getString(""),
                                       table.getEntry("Params").getDoubleArray(new double[0]));
            } else if(table.containsKey("Conditional") && table.containsSubTable("Commands")) {
                boolean hasCondition = !table.getEntry("Conditional").getString("").equals("null");
                boolean condition = logicTable.getEntry(table.getEntry("Conditional").getString("")).getString("").equals("True");
                if(!hasCondition || condition) {
                    loadCommandsFromTable();
                }
            }
        }
        */
        loadCommandsFromTable(autoTable);

        autoProgram.addCommand("Elevator", "goToHeight", new double[] {ElevatorSubsystem.ElevatorHeight.ScaleLow.setpoint});

        /*
        autoProgram.addCommand("Drive", "driveDistance", new double[] {18, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {270, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {20, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {180, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {14, 4, 2, 2});
        autoProgram.addCommand("Drive", "turnToAngle", new double[] {90, 4, 2, 20});
        autoProgram.addCommand("Drive", "driveDistance", new double[] {14, 4, 2, 2});
        */
    }

    public void autonomousPeriodic() { autoProgram.run(); }

    enum TeleopState { ClimbUp, ClimbDown, Intake, Shoot, Default }
    TeleopState state;

    public Button climbButton = new Button(driver, 3);
    public Button shiftButton = new Button(driver, 9);
    public double intakeAnalog() { return driver.getRawAxis(2); }
    public double shootAnalog() { return driver.getRawAxis(3); }

    public TeleopState getState() {
        if(climbButton.released && (state == TeleopState.ClimbUp)) {
            return TeleopState.ClimbDown;
        } else if(climbButton.released) {
            return TeleopState.ClimbUp;
        } else if(intakeAnalog() > 0.1) {
            return TeleopState.Intake;
        } else if(shootAnalog() > 0.1) {
            return TeleopState.Shoot;
        } else if((state == TeleopState.ClimbUp) || (state == TeleopState.ClimbDown)) {
            return state;
        }

        return TeleopState.Default;
    }

    public void teleopInit() {
        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);

        state = TeleopState.Default;

        elevatorSubsystem.elevatorShepherd.setSelectedSensorPosition(0, 0, 0);
        elevatorSubsystem.elevatorShepherd.set(0);
    }

    boolean lowGear = false;

    public double lerp(double a, double t, double b) { return (1 - t) * a + t * b; }

    public void teleopPeriodic() {
        Button.tickAll();
        state = getState();

        if(shiftButton.released) {
            lowGear = !lowGear;
        }

        intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
        driveSubsystem.shifter.set(lowGear ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive((lowGear ? 1.0 : 0.85) * driver.getRawAxis(0), -(lowGear ? 0.85 : 0.85) * driver.getRawAxis(1));
//traps are gay
        if(op.getRawButton(1)) {
            //elevatorSubsystem.elevatorShepherd.set(ControlMode.Velocity, 20000 * (1.0 / 10.0));
            elevatorSubsystem.setElevatorSetpoint(elevatorSubsystem.value(ElevatorUpSpeed));
        } else if(op.getRawButton(3)) {
            //elevatorSubsystem.elevatorShepherd.set(ControlMode.Velocity, 0);
            elevatorSubsystem.setElevatorSetpoint(0);
        } else if(op.getRawButton(2)) {
            //elevatorSubsystem.elevatorShepherd.set(ControlMode.Velocity, -2000 * (1.0 / 10.0));
            elevatorSubsystem.setElevatorSetpoint(-elevatorSubsystem.value(ElevatorDownSpeed));
        } else {
            elevatorSubsystem.elevatorShepherd.set(op.getRawAxis(1));
        }
        elevatorSubsystem.elevatorBrake.set(driver.getRawButton(1) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);

        /*
        if(op.getRawButton(1)) {
            if(elevatorSubsystem.liftController.isEnabled())
                elevatorSubsystem.liftController.disable();

            System.out.println(elevatorSubsystem.liftController.isEnabled());
            elevatorSubsystem.leftLift.set(-op.getRawAxis(1));
            elevatorSubsystem.rightLift.set(op.getRawAxis(1));
        } else {
            if(!elevatorSubsystem.liftController.isEnabled()) {
                elevatorSubsystem.liftController.reset();
                elevatorSubsystem.liftController.enable();
            }

            elevatorSubsystem.liftController.setSetpoint(elevatorSubsystem.value(LiftPotUp)); //lerp(elevatorSubsystem.value(LiftPotDown), (1 + driver.getRawAxis(1)) / 2, elevatorSubsystem.value(LiftPotUp)));
        }

        if(climbButton.released) {
            elevatorSubsystem.liftController.reset();
            elevatorSubsystem.liftController.enable();
        }
        */


        if(driver.getRawAxis(2) > 0.1) {
            intakeSubsystem.leftIntake.set(-5 * driver.getRawAxis(2));
            intakeSubsystem.rightIntake.set(-5 * driver.getRawAxis(2));
        } else if(driver.getRawAxis(3) > 0.1) {
            intakeSubsystem.leftIntake.set(0.75);
            intakeSubsystem.rightIntake.set(0.75);
        } else {
            intakeSubsystem.leftIntake.set(0);
            intakeSubsystem.rightIntake.set(0);
        }
        /*

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
        */
    }

    public void disabledInit() { }
    public void disabledPeriodic() { }

    @Retention(RetentionPolicy.RUNTIME) public @interface Logic {}

    @Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
}