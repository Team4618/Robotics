package team4618.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.wpilibj.*;
import team4618.robot.subsystems.DriveSubsystem;
import team4618.robot.subsystems.ElevatorSubsystem;
import team4618.robot.subsystems.IntakeSubsystem;

import static team4618.robot.CommandSequence.autoTable;
import static team4618.robot.CommandSequence.teleopTable;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.WristDown;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.WristElevatorSafe;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.WristSlop;

public class Robot extends TimedRobot {
    public static Joystick driver = new Joystick(0);
    public static Joystick op = new Joystick(1);

    public static DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
    public static IntakeSubsystem intakeSubsystem = new IntakeSubsystem();

    CommandSequence autoProgram;

    public void robotInit() {
        //TODO: switch name for different bots
        CommandSequence.init(this, "Shopping Cart");
        autoProgram = new CommandSequence(CommandSequence.table.getSubTable("Executing"));
        Subsystems.init();

        ledOutput = new DigitalOutput(3);
        lastTime = Timer.getFPGATimestamp();
    }

    WPI_VictorSPX ledController = new WPI_VictorSPX(33);
    DigitalOutput ledOutput;

    public double lastTime = 0;
    public RobotPosition pos = new RobotPosition(0, 0, 0);

    public void robotPeriodic() {
        NetworkTable mainTable = CommandSequence.table;

        double time = Timer.getFPGATimestamp();
        pos = driveSubsystem.getPosition(pos, time - lastTime);
        lastTime = time;
        mainTable.getEntry("x").setValue(pos.x);
        mainTable.getEntry("y").setValue(pos.y);
        mainTable.getEntry("angle").setValue(pos.angle);

        Subsystems.postState();
        mainTable.getEntry("mode").setString(isEnabled() ? (isAutonomous() ? "Autonomous" : "Teleop") : "Disabled");

        if(op.getRawButton(8)) {
            driveSubsystem.navx.reset();
            elevatorSubsystem.shepherd.setSelectedSensorPosition(0, 0, 0);
            elevatorSubsystem.shepherd.set(0);
            elevatorSubsystem.heightSetpoint = 0;
        }

        if(op.getRawButton(9)) {
            intakeSubsystem.wristEncoder.reset();
        }

        ledController.set(intakeSubsystem.hasCube() ? 1 : 0);
        ledOutput.set(intakeSubsystem.hasCube());
    }

    public void autonomousInit() {
        CommandSequence.resetLogic(this);
        autoProgram.reset();
        autoProgram.loadCommandsFromTable(autoTable);

        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);
        intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
        intakeSubsystem.disableWhenSetpointReached = false;
        intakeSubsystem.setWristDown();
        intakeSubsystem.setIntakePower(0);
        intakeSubsystem.slowWrist = true;
        elevatorSubsystem.heightSetpoint = 0;

        driveSubsystem.navx.reset();
        elevatorSubsystem.shepherd.setSelectedSensorPosition(0, 0, 0);
        elevatorSubsystem.shepherd.set(0);

        intakeSubsystem.wristEncoder.reset();
        Subsystems.subsystems.values().forEach(s -> s.periodicEnabled = true);
    }

    public void autonomousPeriodic() {
        Button.tickAll();
        autoProgram.run();
        Subsystems.periodic();
    }

    ToggleButton elevatorOverride = new ToggleButton(op, 10, false);
    public static ToggleButton intakeOverride = new ToggleButton(op, 7, false);
    ToggleButton cubeSensorOverride = new ToggleButton(op, 6, false);
    Button openLatch = new Button(op, 2);
    Button wristUp = new Button(op, 3);
    Button placeCube = new Button(op, 5);
    Button engageAuxiliary = new Button(op, 4);
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
    //Button intakeButton = new Button(() -> driver.getRawAxis(2) > 0.1);
    //Button wristUpButton = new Button(() -> driver.getRawAxis(3) > 0.1);
    boolean intakeAnalogPressed() { return driver.getRawAxis(2) > 0.1; }
    boolean wristUpAnalogPressed() { return driver.getRawAxis(3) > 0.1; }
    double elevatorAnalog() { return driver.getRawAxis(5); }

    int[] elevatorSetpoints = new int[]{ 0, 5000, 13000, 26000, 29000, 30000/*31000*/};

    int elevatorSetpoint = 0;
    boolean driveForClimb = false;
    boolean elevatorUpForClimb = false;
    boolean wristDisabledForClimb = false;
    double climbStartTime;
    boolean intakeHasCube = false;
    boolean holdFromAuto = true;
    boolean wasIntaking = false;
    double intakeStartTime = 0;
    CommandSequence.CommandState driveForClimbCommandState;

    public void teleopInit() {
        driveSubsystem.shifter.set(DoubleSolenoid.Value.kReverse);
        driveSubsystem.left.shepherd.set(0);
        driveSubsystem.right.shepherd.set(0);

        intakeSubsystem.setWristDown();
        intakeSubsystem.slowWrist = false;
        intakeSubsystem.disableWhenSetpointReached = true;
        driveForClimb = false;
        Button.resetAll();

        elevatorSetpoint = 0; //TODO: set this to the setpoint immediately below heightSetpoint
        holdFromAuto = true;
        Subsystems.subsystems.values().forEach(s -> s.periodicEnabled = true);
    }

    public void setIntakeState(double intakePower, boolean armsOpen) {
        intakeSubsystem.setIntakePower(intakePower);
        intakeSubsystem.arms.set(armsOpen ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);
    }

    public double clamp(double min, double x, double max) {
        return Math.min(max, Math.max(x, min));
    }

    public void teleopPeriodic() {
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

        boolean bottomSafeZone = (elevatorSetpoint == 0) && (elevatorSubsystem.getHeight() < 10000);
        boolean topSafeZone = (elevatorSetpoint >= elevatorSetpoints.length - 1) && (elevatorSubsystem.getHeight() > 28000);
        boolean wristSafe = intakeSubsystem.isElevatorSafe() || topSafeZone || bottomSafeZone;

        //intake states
        if(!climbToggle.state) {
            if(intakeAnalogPressed()) {
                if(!wasIntaking) {
                    intakeStartTime = Timer.getFPGATimestamp();
                }

                elevatorSetpoint = 0;
                holdFromAuto = false;
                wasIntaking = true;
                if(intakeSubsystem.hasCube()) {
                    intakeHasCube = true;
                }

                double intakePower = intakeHasCube || ((Timer.getFPGATimestamp() - intakeStartTime) < 0.5) ? 0 : 0.75;
                setIntakeState(intakePower, !intakeHasCube);
            } else if(placeCube.isDown()) {
                setIntakeState(0, true);
            } else if(pullInButton.isDown()) {
                setIntakeState(0.75, false);
            } else if(elevatorBreakToggle.isDown() /*TODO: Put this on the DPad*/) {
                setIntakeState(/*intakeSubsystem.isWristShoot() ?  -1 :*/ 0, false);
            } else if(shootSlowButton.isDown()) {
                setIntakeState(-0.35, false);
            } else if(shootFastButton.isDown()) {
                setIntakeState(-1, false);
            } else {
                setIntakeState(0, false);

                intakeHasCube = false;
                wasIntaking = false;
            }

            if((wristUpAnalogPressed() || wristUp.isDown()) && (elevatorSetpoint == 0)) {
                intakeSubsystem.hitSetpoint = false;
                intakeSubsystem.setWristUp();
            } else if(intakeAnalogPressed()) {
                intakeSubsystem.hitSetpoint = false;
                intakeSubsystem.setWristDown();
            } else if(elevatorBreakToggle.isDown() /*TODO: Put this on the DPad*/) {
                if(elevatorBreakToggle.pressBegin)
                    intakeSubsystem.hitSetpoint = false;

                intakeSubsystem.setWristShoot();
            } else {
                if(elevatorSetpoint == 0) {
                    intakeSubsystem.hitSetpoint = false;
                    intakeSubsystem.setWristDown();
                } else {
                    double maxSetpoint = intakeSubsystem.value(WristDown);
                    double minSetpoint = intakeSubsystem.value(WristElevatorSafe) + 2 * intakeSubsystem.value(WristSlop);
                    double newSetpoint = intakeSubsystem.wristSetpoint + 3 * elevatorAnalog();
                    intakeSubsystem.wristSetpoint = clamp(minSetpoint, newSetpoint, maxSetpoint);
                }
            }

            if(!wristSafe && !elevatorBreakToggle.isDown()) {
                intakeSubsystem.hitSetpoint = false;
                intakeSubsystem.setWristDown();
            }
        }

        //Automated drive back for climb
        if(backForClimbButton.released) {
            driveForClimbCommandState = new CommandSequence.CommandState(null);
            driveForClimb = true;
        }

        //Set height setpoint to setpoint from the array
        if(!holdFromAuto) {
            elevatorSubsystem.heightSetpoint = wristSafe ? elevatorSetpoints[elevatorSetpoint] : elevatorSubsystem.getHeight();
        }

        //Climb controls
        double driveMultiplier = 1.0;
        if(climbToggle.state) {
            if(climbToggle.released) {
                elevatorBreakToggle.reset();
                elevatorUpForClimb = true;
                wristDisabledForClimb = false;
                climbStartTime = Timer.getFPGATimestamp();
            }

            double timeElapsed = Timer.getFPGATimestamp() - climbStartTime;
            double elevatorPower = Math.min(0, elevatorAnalog());

            climbToggle.state = true;
            driveMultiplier = 0.45;
            intakeSubsystem.setIntakePower(timeElapsed < 0.5 ? -1 : 0);
            intakeSubsystem.setWristUp();
            intakeSubsystem.hitSetpoint = false; //TODO: this does what wristDisabledForClimb does, eliminate the latter
            intakeSubsystem.arms.set(DoubleSolenoid.Value.kForward);
            double climbSetpoint = 29300;
            //TODO: as far as I can tell this only works because its below the regular setpoint setting code, thats how it avoids the wrist safety
            elevatorSubsystem.heightSetpoint = intakeSubsystem.wristAtSetpoint() ? climbSetpoint : 0;

            if(intakeSubsystem.wristAtSetpoint()) {
                wristDisabledForClimb = true;
            }

            if(elevatorSubsystem.isAt(climbSetpoint) && (elevatorPower < -0.30)) {
                elevatorUpForClimb = false;
            }

            if(!elevatorUpForClimb) {
                elevatorSubsystem.shepherd.set(elevatorPower);
                elevatorSubsystem.sheep.set(elevatorPower);
                elevatorSubsystem.auxiliary.set(ControlMode.PercentOutput, elevatorPower);
            }

            if(wristDisabledForClimb) {
                //TODO: we may need to send power to hold the wrist up
                intakeSubsystem.wrist.set(0);
            }
        }

        elevatorSubsystem.periodicEnabled = !climbToggle.state || (climbToggle.state && elevatorUpForClimb);
        elevatorSubsystem.elevatorBrake.set((climbToggle.state && elevatorBreakToggle.state) ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);

        intakeSubsystem.periodicEnabled = !climbToggle.state || (climbToggle.state && !wristDisabledForClimb);

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
            elevatorSubsystem.sheep.set(elevatorPowerAnalog());
            elevatorSubsystem.auxiliary.set(ControlMode.PercentOutput, engageAuxiliary.isDown() ? elevatorPowerAnalog() : 0);
            elevatorSubsystem.periodicEnabled = false;
        }

        if(intakeOverride.state) {
            if(intakeSubsystem.wristPID.isEnabled())
                intakeSubsystem.wristPID.disable();

            double wristPower = Math.abs(intakeLiftAnalog()) < 0.1 ? 0 : intakeLiftAnalog();
            intakeSubsystem.wrist.set(wristPower);
            intakeSubsystem.periodicEnabled = false;
        }

        intakeSubsystem.cubeSensorEnabled = !cubeSensorOverride.state;

        //Teleop status, displayed on the dashboard
        teleopTable.getEntry("Gear").setString(shiftToggle.state ? "Low" : "High");
        teleopTable.getEntry("Elevator Setpoint").setString(String.valueOf(elevatorSetpoint));
        teleopTable.getEntry("Elevator").setString(elevatorSubsystem.periodicEnabled ? "Automatic" : "Manual");
        teleopTable.getEntry("Intake").setString(intakeSubsystem.periodicEnabled ? "Automatic" : "Manual");
        teleopTable.getEntry("Cube Sensor").setString(intakeSubsystem.cubeSensorEnabled ? "Enabled" : "Disabled");
        teleopTable.getEntry("Climb Mode").setString(climbToggle.state ? "Enabled" : "Disabled");
        teleopTable.getEntry("Intake Spinning").setBoolean(Math.abs(intakeSubsystem.leftIntake.getMotorOutputPercent()) > 0.1);
        teleopTable.getEntry("Wrist Safe").setBoolean(wristSafe);

        Subsystems.periodic();
    }

    public void testInit() {
        //teleopInit();
        autonomousInit();
        elevatorOverride.state = true;
        intakeOverride.state = true;
    }

    public void testPeriodic() {
        //teleopPeriodic();
        autonomousPeriodic();
    }

    public void disabledInit() {
        intakeSubsystem.wristPID.disable();
    }

//hi jon
    @CommandSequence.Logic public boolean leftSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'L'; }
    @CommandSequence.Logic public boolean rightSwitchOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(0) == 'R'; }
    @CommandSequence.Logic public boolean leftScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L'; }
    @CommandSequence.Logic public boolean rightScaleOurs() { return DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'R'; }
    @CommandSequence.Logic public boolean alwaysTrue() { return true; } //TODO: this is really stupid
}