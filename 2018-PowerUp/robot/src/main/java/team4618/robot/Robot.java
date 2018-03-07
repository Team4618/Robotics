package team4618.robot;

import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;
import team4618.robot.subsystems.IntakeSubsystem;

import static team4618.robot.CommandSequence.autoTable;
import static team4618.robot.CommandSequence.teleopTable;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.LiftPotLow;

public class Robot extends TimedRobot {
    public static Joystick driver = new Joystick(0);
    public static Joystick op = new Joystick(1);

    public static DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
    public static IntakeSubsystem intakeSubsystem = new IntakeSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        CommandSequence.init(this, "Shopping Cart");
        autoProgram = new CommandSequence(CommandSequence.table.getSubTable("Executing"));
        Subsystems.init();
    }

    public void robotPeriodic() {
        Subsystems.postState();

        if(op.getRawButton(8)) {
            driveSubsystem.navx.reset();
            elevatorSubsystem.shepherd.setSelectedSensorPosition(0, 0, 0);
            elevatorSubsystem.shepherd.set(0);
        }
    }

    public void autonomousInit() {
        CommandSequence.resetLogic(this);
        autoProgram.reset();
        autoProgram.loadCommandsFromTable(autoTable);

        /*
        autoProgram.addCommand("Intake", "openIntake");
        autoProgram.addCommand("Drive", "driveDistance", 2, 4, 2, 2);
        autoProgram.addCommand("Intake", "closeIntake");
        autoProgram.addCommand("Drive", "driveDistance", -8, 4, 2, 2);
        autoProgram.addCommand("Drive", "turnToAngle", 180, 4, 2, 20);
        autoProgram.addCommand("Elevator", "goToHeight", 13000);
        autoProgram.addCommand("Intake", "shoot");
        autoProgram.addCommand("Elevator", "goToHeight", 0);
        */

        /*
        autoProgram.addCommand("Drive", "driveDistance", 12, 8, 1, 4);
        autoProgram.addCommand("Elevator", "setHeight", 16000);
        autoProgram.addCommand("Drive", "turnToAngle", 270, 4, 2, 20);
        autoProgram.addCommand("Drive", "driveDistance", 1.6, 6, 2, 0.5);
        autoProgram.addCommand("Elevator", "waitForSetpoint");
        autoProgram.addCommand("Intake", "shoot", 0.35);
        autoProgram.addCommand("Drive", "turnToAngle", 0, 3, 1.5, 40);
        autoProgram.addCommand("Elevator", "goToHeight", 0);
        */

        autoProgram.addCommand("Drive", "driveDistance", 108 / 12, 8, 1, 4);
        autoProgram.addCommand("Elevator", "setHeight", 16000);
        autoProgram.addCommand("Drive", "turnToAngle", 270 + 45, 2, 2, 20);
        autoProgram.addCommand("Intake", "shoot", 0.35);
        autoProgram.addCommand("Drive", "turnToAngle", 0, 2, 2, 20);
        autoProgram.addCommand("Elevator", "goToHeight", 0);
        autoProgram.addCommand("Drive", "driveDistance", 4, 8, 1, 4);

        /*
        autoProgram.addCommand("Elevator", "goToHeight", 29000, 2, 4000);
        autoProgram.addCommand("Drive", "driveDistance", 18, 4, 2, 2);
        autoProgram.addCommand("Drive", "turnToAngle", 270, 4, 2, 20);
        autoProgram.addCommand("Drive", "driveDistance", 20, 4, 2, 2);
        autoProgram.addCommand("Drive", "turnToAngle", 180, 4, 2, 20);
        autoProgram.addCommand("Drive", "driveDistance", 14, 4, 2, 2);
        autoProgram.addCommand("Drive", "turnToAngle", 90, 4, 2, 20);
        autoProgram.addCommand("Drive", "driveDistance", 14, 4, 2, 2);
        */

        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);
        intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
    }

    public void autonomousPeriodic() {
        autoProgram.run();
        Subsystems.periodic();
    }

    enum TeleopState { ClimbUp, ClimbDown, Intake, Shoot, Default }
    TeleopState state;
    CommandSequence teleopSequence;

    public Button pullInButton = new Button(driver, 3);
    public Button shiftButton = new Button(driver, 1);
    public Button shootFastButton = new Button(driver, 4);
    public Button shootSlowButton = new Button(driver, 2);
    public Button upElevatorButton = new Button(driver, 5);
    public Button downElevatorButton = new Button(driver, 6);
    public double intakeAnalog() { return driver.getRawAxis(2); }

    public TeleopState getState() {
        /*
        if(climbButton.released && (state == TeleopState.ClimbUp)) {
            return TeleopState.ClimbDown;
        } else if(climbButton.released) {
            return TeleopState.ClimbUp;
        } else if(intakeAnalog() > 0.1) {
            return TeleopState.Intake;
        } else if(shootFastButton.) {
            return TeleopState.Shoot;
        } else if((state == TeleopState.ClimbUp) || (state == TeleopState.ClimbDown)) {
            return state;
        }
        */

        return TeleopState.Default;
    }

    public void teleopInit() {
        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);

        state = TeleopState.Default;
        teleopSequence = new CommandSequence(teleopTable.getSubTable("Executing"));

        elevatorSetpoint = 0;
        elevatorSubsystem.heightSetpoint = 0;
        lowGear = false;
        intakeSubsystem.liftUp = false;
    }

    int[] elevatorSetpoints = new int[]{ 0, 5000, 13000, 26000, 29500 };
    int elevatorSetpoint = 0;
    boolean lowGear = false;

    public void teleopPeriodic() {
        Button.tickAll();
        TeleopState oldState = state;
        state = getState();
        boolean init = state != oldState;

        if(init)
            teleopSequence.reset();

        teleopTable.getEntry("State").setString(state.toString());

        /*
        if(state == TeleopState.Intake) {
            if(init) {
                teleopSequence.addCommand("Elevator", "goToHeight", 0, 2, 1000);
                teleopSequence.addCommand("Intake", "down");
            }

            if(teleopSequence.isDone()) {
                intakeSubsystem.leftIntake.set(0.75);
                intakeSubsystem.rightIntake.set(0.75);
                intakeSubsystem.arms.set(DoubleSolenoid.Value.kReverse);
            }
        } else if(state == TeleopState.Shoot) {

            if(intakeSubsystem.getLiftPosition() < intakeSubsystem.value(LiftPotLow)) {
                intakeSubsystem.leftIntake.set(-5 * shootAnalog());
                intakeSubsystem.rightIntake.set(-5 * shootAnalog());
            } else {
                intakeSubsystem.leftIntake.set(0);
                intakeSubsystem.rightIntake.set(0);
            }

            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            intakeSubsystem.liftUp = false;
        }
        */

        elevatorSubsystem.elevatorBrake.set(DoubleSolenoid.Value.kReverse);

        int oldElevatorSetpoint = elevatorSetpoint;
        if (upElevatorButton.released && (elevatorSetpoint < (elevatorSetpoints.length - 1))) {
            elevatorSetpoint++;
        } else if (downElevatorButton.released && (elevatorSetpoint > 0)) {
            elevatorSetpoint--;
        }

        if (shootSlowButton.isDown()) {
            intakeSubsystem.leftIntake.set(-0.35);
            intakeSubsystem.rightIntake.set(-0.35);
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            intakeSubsystem.liftUp = false;
        } else if (shootFastButton.isDown()) {
            intakeSubsystem.leftIntake.set(-0.55);
            intakeSubsystem.rightIntake.set(-0.55);
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            intakeSubsystem.liftUp = false;
        } else if(pullInButton.isDown()) {
            intakeSubsystem.leftIntake.set(0.75);
            intakeSubsystem.rightIntake.set(0.75);
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            intakeSubsystem.liftUp = true;
        } else if (intakeAnalog() > 0.1) {
            elevatorSetpoint = 0;
            intakeSubsystem.leftIntake.set(0.75);
            intakeSubsystem.rightIntake.set(0.75);
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kReverse);
            intakeSubsystem.liftUp = false;
        } else {
            intakeSubsystem.leftIntake.set(0);
            intakeSubsystem.rightIntake.set(0);
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            intakeSubsystem.liftUp = true;
        }

        if (shiftButton.released) { lowGear = !lowGear; }
        driveSubsystem.shifter.set(lowGear ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive((lowGear ? 1.0 : 0.85) * driver.getRawAxis(0), -(lowGear ? 0.85 : 0.85) * driver.getRawAxis(1));

        if(oldElevatorSetpoint != elevatorSetpoint) {
            System.out.println("Set " + elevatorSetpoints[elevatorSetpoint]);
            elevatorSubsystem.heightSetpoint = elevatorSetpoints[elevatorSetpoint];
        }

        if(elevatorSubsystem.getHeight() > 8000) {
            intakeSubsystem.liftUp = false;
        }

        Subsystems.periodic();
        teleopSequence.run();
    }

    public void disabledInit() { }
    public void disabledPeriodic() { }

    @CommandSequence.Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @CommandSequence.Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @CommandSequence.Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @CommandSequence.Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
}