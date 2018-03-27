package team4618.robot;

import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;
import team4618.robot.subsystems.IntakeSubsystem;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static team4618.robot.CommandSequence.autoTable;
import static team4618.robot.CommandSequence.teleopTable;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.LiftLow;

public class Robot extends TimedRobot {
    public static Joystick driver = new Joystick(0);
    public static Joystick op = new Joystick(1);

    public static DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
    public static IntakeSubsystem intakeSubsystem = new IntakeSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        //TODO: switch name for different bots
        CommandSequence.init(this, "Felix");
        autoProgram = new CommandSequence(CommandSequence.table.getSubTable("Executing"));
        Subsystems.init();
    }

    public void robotPeriodic() {
        Subsystems.postState();
        CommandSequence.table.getEntry("mode").setString(isEnabled() ? (isAutonomous() ? "Autonomous" : "Teleop") : "Disabled");

        if(op.getRawButton(8)) {
            driveSubsystem.navx.reset();
            elevatorSubsystem.shepherd.setSelectedSensorPosition(0, 0, 0);
            elevatorSubsystem.shepherd.set(0);
            elevatorSubsystem.heightSetpoint = 0;
        }

        if(op.getRawButton(9)) {
            intakeSubsystem.liftEncoder.reset();
        }
    }

    public void autonomousInit() {
        CommandSequence.resetLogic(this);
        autoProgram.reset();
        autoProgram.loadCommandsFromTable(autoTable);

        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);
        intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
        intakeSubsystem.liftUp = false;
        intakeSubsystem.setIntakePower(0);
        elevatorSubsystem.heightSetpoint = 0;

        driveSubsystem.navx.reset();
        elevatorSubsystem.shepherd.setSelectedSensorPosition(0, 0, 0);
        elevatorSubsystem.shepherd.set(0);

        intakeSubsystem.liftEncoder.reset();
    }

    public void autonomousPeriodic() {
        autoProgram.run();
        Subsystems.periodic();
    }

    public void setIntakeState(double intakePower, boolean armsOpen, boolean liftUp) {
        intakeSubsystem.setIntakePower(intakeSubsystem.liftSittingDown || liftUp ? intakePower : 0);
        intakeSubsystem.arms.set(armsOpen ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);
        intakeSubsystem.liftUp = liftUp;
    }

    ToggleButton elevatorOverride = new ToggleButton(op, 10, false);
    ToggleButton intakeOverride = new ToggleButton(op, 7, false);
    ToggleButton cubeSensorOverride = new ToggleButton(op, 6, false);
    Button openLatch = new Button(op, 2);
    Button engageAuxiliry = new Button(op, 4);
    double elevatorPowerAnalog() { return op.getRawAxis(1); }
    double intakeLiftAnalog() { return op.getRawAxis(0); }

    Button pullInButton = new Button(driver, 3);
    Button shootFastButton = new Button(driver, 2);
    Button shootSlowButton = new Button(driver, 4);
    Button upElevatorButton = new Button(driver, 5);
    Button downElevatorButton = new Button(driver, 6);
    Button backForClimbButton = new Button(driver, 7);
    ToggleButton shiftToggle = new ToggleButton(driver, 1, false);
    ToggleButton climbToggle = new ToggleButton(driver, 8, false);
    ToggleButton elevatorBreakToggle = new ToggleButton(driver, 10, false);
    double intakeAnalog() { return driver.getRawAxis(2); }
    double liftDownAnalog() { return driver.getRawAxis(3); }
    double elevatorAnalog() { return driver.getRawAxis(5); }
    boolean climbDisableHold() { return driver.getPOV() == 180; }

    //int[] elevatorSetpoints = new int[]{ 0, 5000, 13000, 26000, 29000 };
    //TODO: otis
    int[] elevatorSetpoints = new int[]{ 0, 5000, 13000, 26000, 29000/*, 30000*/ };

    int elevatorSetpoint = 0;
    boolean driveForClimb = false;
    boolean elevatorUpForClimb = false;
    double climbStartTime;
    boolean intakeHasCube = false;
    boolean holdFromAuto = true;
    CommandSequence.CommandState driveForClimbCommandState;

    public void teleopInit() {
        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);

        intakeSubsystem.liftUp = false;
        driveForClimb = false;
        Button.resetAll();

        elevatorSetpoint = 0; //TODO: set this to the setpoint immediately below heightSetpoint
        holdFromAuto = true;
    }

    public void teleopPeriodic() {
        //TODO: clean up the safety logic
        Button.tickAll();

        //elevator setpoint up & down buttons
        if (upElevatorButton.released && (elevatorSetpoint < (elevatorSetpoints.length - 1))) {
            elevatorSetpoint++;
        } else if (downElevatorButton.released && (elevatorSetpoint > 0)) {
            elevatorSetpoint--;
        }

        if(upElevatorButton.released || downElevatorButton.released) {
            holdFromAuto = false;
        }

        boolean disableLatch = false;
        boolean holdLiftDown = false;

        boolean bottomSafeZone = (elevatorSetpoint == 0) && (elevatorSubsystem.getHeight() < 10000);
        boolean topSafeZone = (elevatorSetpoint >= elevatorSetpoints.length - 1) && (elevatorSubsystem.getHeight() > 28000);

        //intake states
        if(!climbToggle.state) {
            //TODO: put this on the dpad
            if(elevatorBreakToggle.isDown()) {
                intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
                intakeSubsystem.liftUp = true;
                disableLatch = true;
                intakeSubsystem.setIntakePower((intakeSubsystem.getLiftPosition() < -50) ? -0.7 : 0);
            } else if (shootSlowButton.isDown()) {
                setIntakeState(-0.35, false, false);
            } else if (shootFastButton.isDown()) {
                setIntakeState(-0.55, false, false);
            } else if (pullInButton.isDown()) {
                setIntakeState(0.75, false, false);
            } else if (intakeAnalog() > 0.1) {
                elevatorSetpoint = 0;
                holdFromAuto = false;
                holdLiftDown = true;
                if(intakeSubsystem.hasCube()) {
                    intakeHasCube = true;
                }
                setIntakeState(intakeHasCube ? 0 : 0.75, !intakeHasCube, false);
            } else if(liftDownAnalog() > 0.1){
                setIntakeState(0, false, true);
            } else {
                intakeHasCube = false;
                setIntakeState(0, false, false);
            }

            intakeSubsystem.liftUp &= bottomSafeZone || topSafeZone;
        }

        intakeSubsystem.disableLatch = disableLatch;
        intakeSubsystem.holdLiftDown = holdLiftDown;

        //Automated drive back for climb
        if(backForClimbButton.released) {
            driveForClimbCommandState = new CommandSequence.CommandState(null);
            driveForClimb = true;
        }

        //Set height setpoint to setpoint from the array
        if(!holdFromAuto) {
            elevatorSubsystem.heightSetpoint = (intakeSubsystem.isLiftDown() || topSafeZone) ? elevatorSetpoints[elevatorSetpoint] : 0;
        }

        //Climb controls
        double driveMultiplier = 1.0;
        if(climbToggle.state) {
            if(climbToggle.released) {
                elevatorBreakToggle.reset();
                elevatorUpForClimb = true;
                climbStartTime = Timer.getFPGATimestamp();
            }

            double timeElapsed = Timer.getFPGATimestamp() - climbStartTime;
            double elevatorPower = Math.min(0, elevatorAnalog());

            climbToggle.state = true;
            driveMultiplier = 0.45;
            intakeSubsystem.setIntakePower(timeElapsed < 0.5 ? -1 : 0);
            intakeSubsystem.liftUp = true;
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            double climbSetpoint = 29000;
            elevatorSubsystem.heightSetpoint = intakeSubsystem.isLiftUp() ? climbSetpoint : 0;

            if(elevatorSubsystem.isAt(climbSetpoint) && ((elevatorPower < -0.15) || climbDisableHold())) {
                elevatorUpForClimb = false;
            }

            if(!elevatorUpForClimb) {
                boolean elevatorStalling = false;
                //(Math.abs(elevatorSubsystem.shepherd.getMotorOutputPercent()) > 0.8) && (Math.abs(elevatorSubsystem.getSpeed()) < 4000);
                elevatorSubsystem.shepherd.set(elevatorPower);
                elevatorSubsystem.auxiliary.set(elevatorStalling ? 0 : elevatorPower);
            }
        }

        elevatorSubsystem.periodicEnabled = !climbToggle.state || (climbToggle.state && elevatorUpForClimb);
        elevatorSubsystem.elevatorBrake.set((climbToggle.state && elevatorBreakToggle.state) ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);

        //drive controls
        if(driveForClimb) {
            driveForClimbCommandState.update();
            if(driveSubsystem.driveDistance(driveForClimbCommandState, -0.9, 1.5, 1, 0.25)) {
                driveForClimb = false;
            }
            driveForClimbCommandState.init = false;
        } else {
            driveSubsystem.shifter.set(shiftToggle.state ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
            driveSubsystem.teleopDrive.arcadeDrive((shiftToggle.state ? 0.85 : 0.75) * driveMultiplier * driver.getRawAxis(0), -driveMultiplier * driver.getRawAxis(1));
        }

        //OP override controls (these go last so they overwrite any values above)
        if(elevatorOverride.state) {
            elevatorSubsystem.shepherd.set(elevatorPowerAnalog());
            elevatorSubsystem.auxiliary.set(engageAuxiliry.isDown() ? elevatorPowerAnalog() : 0);
            elevatorSubsystem.periodicEnabled = false;
        }

        //TODO: otis - testing
        /*
        if((elevatorSubsystem.getHeight() < elevatorSetpoints[2]) && (elevatorSubsystem.heightSetpoint < 2000)) {
            elevatorSubsystem.periodicEnabled = false;
            elevatorSubsystem.shepherd.set(0);
        }
        */

        if(intakeOverride.state) {
            intakeSubsystem.setLiftPower(intakeLiftAnalog());
            intakeSubsystem.latch.set(openLatch.isDown() ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);
            intakeSubsystem.periodicEnabled = false;
        } else {
            intakeSubsystem.periodicEnabled = true;
        }

        intakeSubsystem.cubeSensorEnabled = !cubeSensorOverride.state;

        //Teleop status, displayed on the dashboard
        teleopTable.getEntry("Gear").setString(shiftToggle.state ? "Low" : "High");
        teleopTable.getEntry("Elevator Setpoint").setString(String.valueOf(elevatorSetpoint));
        teleopTable.getEntry("Elevator").setString(elevatorOverride.state ? "Manual" : "Automatic");
        teleopTable.getEntry("Intake").setString(intakeOverride.state ? "Manual" : "Automatic");
        teleopTable.getEntry("Cube Sensor").setString(intakeSubsystem.cubeSensorEnabled ? "Enabled" : "Disabled");
        teleopTable.getEntry("Climb Mode").setString(climbToggle.state ? "Enabled" : "Disabled");
        teleopTable.getEntry("Intake Spinning").setBoolean(Math.abs(intakeSubsystem.leftIntake.getMotorOutputPercent()) > 0.1);

        teleopTable.getEntry("Wrist Safe").setBoolean(bottomSafeZone || topSafeZone);

        //teleopTable.getEntry("Driver DPad").setNumber(driver.getPOV());
        //teleopTable.getEntry("Climb Disable PID DPad").setBoolean(climbDisableHold());

        Subsystems.periodic();
    }

    public void disabledInit() { }
    public void disabledPeriodic() { }
//hi jon
    @CommandSequence.Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @CommandSequence.Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @CommandSequence.Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @CommandSequence.Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
    @CommandSequence.Logic public boolean alwaysTrue() { return true; } //TODO: this is really stupid
}