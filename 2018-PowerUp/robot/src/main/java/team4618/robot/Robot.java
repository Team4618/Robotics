package team4618.robot;

import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;
import team4618.robot.subsystems.IntakeSubsystem;

import static team4618.robot.CommandSequence.autoTable;

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

        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);
        intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
    }

    public void autonomousPeriodic() {
        autoProgram.run();
        Subsystems.periodic();
    }

    public void setIntakeState(double intakePower, boolean armsOpen, boolean liftUp) {
        intakeSubsystem.setIntakePower(intakePower);
        intakeSubsystem.arms.set(armsOpen ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);
        intakeSubsystem.liftUp = liftUp;
    }

    Button pullInButton = new Button(driver, 3);
    Button shootFastButton = new Button(driver, 2);
    Button shootSlowButton = new Button(driver, 4);
    Button upElevatorButton = new Button(driver, 5);
    Button downElevatorButton = new Button(driver, 6);
    ToggleButton shiftToggle = new ToggleButton(driver, 1, false);
    ToggleButton climbToggle = new ToggleButton(driver, 10, false);  //TODO: wrong index
    ToggleButton elevatorBreakToggle = new ToggleButton(driver, 11, false);  //TODO: wrong index
    double intakeAnalog() { return driver.getRawAxis(2); }
    double elevatorAnalog() { return driver.getRawAxis(4); } //TODO: wrong index

    public void teleopInit() {
        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);

        elevatorSetpoint = 0;
        elevatorSubsystem.heightSetpoint = 0;
        intakeSubsystem.liftUp = false;
        Button.resetAll();
    }

    int[] elevatorSetpoints = new int[]{ 0, 5000, 13000, 26000, 29500 };
    int elevatorSetpoint = 0;

    public void teleopPeriodic() {
        Button.tickAll();

        //elevator setpoint up & down buttons
        if (upElevatorButton.released && (elevatorSetpoint < (elevatorSetpoints.length - 1))) {
            elevatorSetpoint++;
        } else if (downElevatorButton.released && (elevatorSetpoint > 0)) {
            elevatorSetpoint--;
        }

        //intake states
        if (shootSlowButton.isDown()) {
            setIntakeState(-0.35, false, false);
        } else if (shootFastButton.isDown()) {
            setIntakeState(-0.55, false, false);
        } else if(pullInButton.isDown()) {
            setIntakeState(0.75, false, true);
        } else if (intakeAnalog() > 0.1) {
            elevatorSetpoint = 0;
            setIntakeState(0.75, true, false);
        } else {
            setIntakeState(0, false, true);
        }

        //drive controls
        driveSubsystem.shifter.set(shiftToggle.state ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive((shiftToggle.state ? 1.0 : 0.85) * driver.getRawAxis(0), -driver.getRawAxis(1));

        //Climb controls
        if(climbToggle.state) {
            if(climbToggle.released) {
                elevatorSetpoint = elevatorSetpoints.length - 1;
                elevatorBreakToggle.reset();
            }

            boolean elevatorStalling = (Math.abs(elevatorSubsystem.shepherd.getMotorOutputPercent()) > 0.8) &&
                                       (Math.abs(elevatorSubsystem.getSpeed()) < 4000);
            elevatorSubsystem.shepherd.set(elevatorAnalog());
            elevatorSubsystem.auxiliary.set(elevatorStalling ? 0 : elevatorAnalog());
        }

        elevatorSubsystem.periodicEnabled = !climbToggle.state;
        elevatorSubsystem.elevatorBrake.set((climbToggle.state && elevatorBreakToggle.state) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);

        //Set height setpoint to setpoint from the array
        elevatorSubsystem.heightSetpoint = elevatorSetpoints[elevatorSetpoint];

        //OP override controls (these go last so they overwrite any values above)
        if(op.getRawButton(1)) {
            elevatorSubsystem.shepherd.set(op.getRawAxis(1));
            elevatorSubsystem.auxiliary.set(op.getRawButton(4) ? op.getRawAxis(1) : 0);
            elevatorSubsystem.periodicEnabled = false;
        }

        if(op.getRawButton(3)) {
            intakeSubsystem.setLiftPower(op.getRawAxis(1));
            intakeSubsystem.latch.set(op.getRawButton(2) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
            intakeSubsystem.periodicEnabled = false;
        } else {
            intakeSubsystem.periodicEnabled = true;
        }

        Subsystems.periodic();
    }

    public void disabledInit() { }
    public void disabledPeriodic() { }

    @CommandSequence.Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @CommandSequence.Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @CommandSequence.Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @CommandSequence.Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
}