package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import team4618.robot.CommandSequence.CommandState;
import team4618.robot.Subsystem;

import static team4618.robot.Subsystem.Units.Percent;
import static team4618.robot.Subsystem.Units.Unitless;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.*;

public class IntakeSubsystem extends Subsystem {

    public DoubleSolenoid arms = new DoubleSolenoid(2, 3);
    public DoubleSolenoid latch = new DoubleSolenoid(4, 5);

    public Encoder liftEncoder = new Encoder(0, 1);
    public WPI_VictorSPX leftLift = new WPI_VictorSPX(33);
    public WPI_VictorSPX rightLift = new WPI_VictorSPX(24);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);
    public DigitalInput cubeSensor = new DigitalInput(2);

    public boolean cubeSensorEnabled = true;
    public boolean liftUp = false;

    @Subsystem.ParameterEnum
    public enum Parameters { LiftDown, LiftLow, LiftHigh, LiftUp,
                             LiftUpPower, LiftDescentPower, LiftReleaseLatchPower, LiftReleaseLatchTime,
                             ShootTime }

    public void init() {

    }

    public void postState() {
        PostState("Lift Encoder", Unitless, liftEncoder.get());
        PostState("Lift Power", Percent, leftLift.getMotorOutputPercent());
        PostState("Cube Sensor", Percent, cubeSensor.get() ? 1 : 0);
    }

    public double getLiftPosition() {
        return liftEncoder.get();
    }

    public void setLiftPower(double value) {
        leftLift.set(-value);
        rightLift.set(value);
    }

    //NOTE: intake is positive, shoot is negative
    public void setIntakePower(double value) {
        leftIntake.set(value);
        rightIntake.set(value);
    }

    public boolean hasCube() {
        return !cubeSensor.get() && cubeSensorEnabled;
    }

    public boolean isLiftDown() {
        return getLiftPosition() > value(LiftLow);
    }

    public boolean isLiftUp() {
        return getLiftPosition() < value(LiftHigh);
    }

    @Command
    public void openIntake(CommandState state) {
        setIntakePower(0.75);
        arms.set(DoubleSolenoid.Value.kReverse);
    }

    @Command
    public void closeIntake(CommandState state) {
        setIntakePower(0);
        arms.set(DoubleSolenoid.Value.kForward);
    }

    @Command
    public boolean shoot(CommandState state, @Unit(Unitless) double speed) {
        boolean isDone = state.elapsedTime >= value(ShootTime);
        setIntakePower(isDone ? 0 : -speed);
        arms.set(DoubleSolenoid.Value.kForward);
        return isDone;
    }

    boolean wasUp = false;
    double startTime = 0;

    public void periodic() {
        double liftPosition = getLiftPosition();
        if(liftUp) {
            if(liftPosition < value(LiftHigh)) {
                setLiftPower(0);
                latch.set(DoubleSolenoid.Value.kForward);
            } else {
                setLiftPower(value(LiftUpPower));
                latch.set(DoubleSolenoid.Value.kForward);
            }
        } else {
            if(liftPosition > value(LiftLow)) {
                setLiftPower(0);
                latch.set(DoubleSolenoid.Value.kForward);
            } else {
                latch.set(DoubleSolenoid.Value.kReverse);
                setLiftPower((Timer.getFPGATimestamp() - startTime < value(LiftReleaseLatchTime)) ? value(LiftReleaseLatchPower) : value(LiftDescentPower));
            }
        }

        if(wasUp != liftUp) {
            startTime = Timer.getFPGATimestamp();
            System.out.println("Changing state");
        }
        wasUp = liftUp;

        //System.out.println("Elapsed: " + (Timer.getFPGATimestamp() - startTime));
    }

    public String name() { return "Intake"; }
}
