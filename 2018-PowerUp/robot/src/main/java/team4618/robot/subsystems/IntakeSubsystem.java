package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import team4618.robot.CommandSequence.CommandState;
import team4618.robot.Robot;
import team4618.robot.Subsystem;

import static team4618.robot.Subsystem.Units.Percent;
import static team4618.robot.Subsystem.Units.Unitless;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.*;

public class IntakeSubsystem extends Subsystem {

    public DoubleSolenoid arms = new DoubleSolenoid(2, 3);

    public Encoder wristEncoder = new Encoder(0, 1);
    public WPI_VictorSPX wrist = new WPI_VictorSPX(24);
    public PIDController wristPID = new PIDController(0, 0, 0, wristEncoder, wrist);
    public DoubleSolenoid latch = new DoubleSolenoid(4, 5);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);
    public DigitalInput cubeSensor = new DigitalInput(2);

    public boolean cubeSensorEnabled = true;
    public double wristSetpoint = 0;
    public boolean slowWrist = true;

    @Subsystem.ParameterEnum
    public enum Parameters { WristDown, WristUp, WristShoot,
                             WristUpPower, WristDownPower, WristHoldPower, WristUpSlowPower, WristDownSlowPower,
                             WristElevatorSafe, WristHalf, WristCalibrate, WristSlop,
                             WristP, WristI, WristD,
                             ShootTime }

    public void updateParameters() {
        wristPID.setPID(value(WristP), value(WristI), value(WristD));
    }

    public void init() {
        wristEncoder.setPIDSourceType(PIDSourceType.kRate);
    }

    public void postState() {
        PostState("Wrist Raw Encoder", Unitless, wristEncoder.get());
        PostState("Wrist Position", Unitless, getWristPosition());
        PostState("Wrist Power", Percent, wrist.getMotorOutputPercent());
        PostState("Cube Sensor", Percent, cubeSensor.get() ? 1 : 0);
    }

    public void setWristUp() { wristSetpoint = value(WristUp); }
    public void setWristDown() { wristSetpoint = value(WristDown); }
    public void setWristShoot() { wristSetpoint = value(WristShoot); }

    public double getWristPosition() {
        return wristEncoder.get() + value(WristCalibrate);
    }

    //NOTE: intake is positive, shoot is negative
    public void setIntakePower(double value) {
        leftIntake.set(value);
        rightIntake.set(value);
    }

    public boolean hasCube() {
        return !cubeSensor.get() && cubeSensorEnabled;
    }

    public boolean isElevatorSafe() {
        return getWristPosition() > value(WristElevatorSafe);
    }
    public boolean isWristDown() {
        return Math.abs(getWristPosition() - value(WristDown)) < value(WristSlop);
    }
    public boolean wristAtSetpoint() { return Math.abs(getWristPosition() - wristSetpoint) < value(WristSlop); }

    @Command
    public void setWristShootPosition(CommandState state) {
        setWristShoot();
    }

    @Command
    public void setWristDownPosition(CommandState state) {
        setWristDown();
    }

    @Command
    public boolean waitForWristSetpoint(CommandState state) {
        return wristAtSetpoint();
    }

    @Command
    public boolean openIntake(CommandState state) {
        arms.set(DoubleSolenoid.Value.kReverse);
        setIntakePower((state.elapsedTime < 0.5) || hasCube() ? 0 : 0.75);
        return state.elapsedTime > 0.5;
    }

    public double automaticIntakeTimer = 0;
    @Command
    public boolean closeIntake(CommandState state) {
        if(hasCube() && (automaticIntakeTimer == 0)) {
            automaticIntakeTimer = state.elapsedTime;
        }

        boolean done = (state.elapsedTime - automaticIntakeTimer > 0.35) || (state.elapsedTime > 20.0);

        if(done) {
            setIntakePower(0);
            arms.set(DoubleSolenoid.Value.kForward);
        }

        return done;
    }

    @Command
    public boolean shoot(CommandState state, @Unit(Unitless) double speed) {
        boolean isDone = state.elapsedTime >= value(ShootTime);
        setIntakePower(isDone ? 0 : -speed);
        arms.set(DoubleSolenoid.Value.kForward);
        return isDone;
    }

    public double lastWristPosition = 0;
    public int timeoutCounter = 0;
    public boolean wasLatchUp = false;
    public double latchTimer = 0;

    public void periodic() {
        double wristPosition = getWristPosition();

        double wristPower = 0;
        if(Math.abs(wristSetpoint - wristPosition) < value(WristSlop)) {
            boolean topOrBottom = (wristPosition > -20) || (wristPosition < -165);
            wristPower = topOrBottom ? 0 : value(WristHoldPower);
            //wristPID.setSetpoint(0);
            //wristPID.enable();
        } else if(wristPosition < wristSetpoint) {
            //wristPID.disable();
            wristPower = slowWrist ? value(WristDownSlowPower) : value(WristDownPower);
        } else if(wristPosition > wristSetpoint) {
            //wristPID.disable();
            wristPower = slowWrist ? value(WristUpSlowPower) : value(WristUpPower);
        }

        boolean latchUp = true;
        if(wristSetpoint <= value(WristUp)) {
            latchUp = false;
        }
        latch.set(latchUp ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);

        if(latchUp && !wasLatchUp) {
            latchTimer = Timer.getFPGATimestamp();
            System.out.print("Set timer ");
        }
        wasLatchUp = latchUp;

        if(latchUp && (Timer.getFPGATimestamp() - latchTimer < 0.5)) {
            wristPower = -0.2; //value(WristHoldPower);
            System.out.println(Timer.getFPGATimestamp() - latchTimer);
        }

        wrist.set(wristPower);

        if((lastWristPosition == wristPosition) && (Math.abs(wrist.getMotorOutputPercent()) > 0.15)) {
            timeoutCounter++;
        } else {
            timeoutCounter = 0;
        }
        lastWristPosition = wristPosition;

        if(timeoutCounter >= 100) {
            timeoutCounter = 0;
            //TODO: fix this so I dont have to set the override toggle directly, itd be nice if i could just say periodicEnabled = false
            Robot.intakeOverride.state = true;
        }

        /*
        if(wristSetpoint < value(WristHalf)) {
            wrist.set(liftPosition > wristSetpoint ? value(WristUpPower) : 0);
        } else {
            wrist.set(liftPosition < wristSetpoint ? value(WristDownPower) : 0);
        }
        */
    }

    public String name() { return "Intake"; }
}
