package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import org.opencv.core.Mat;
import team4618.robot.CommandSequence.CommandState;
import team4618.robot.Subsystem;

import static team4618.robot.Subsystem.Units.Percent;
import static team4618.robot.Subsystem.Units.Unitless;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.*;

public class IntakeSubsystem extends Subsystem {

    public DoubleSolenoid arms = new DoubleSolenoid(2, 3);

    public Encoder wristEncoder = new Encoder(0, 1);
    public WPI_VictorSPX wrist = new WPI_VictorSPX(33);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);
    public DigitalInput cubeSensor = new DigitalInput(2);

    public boolean cubeSensorEnabled = true;
    public double wristSetpoint = 0;

    @Subsystem.ParameterEnum
    public enum Parameters { WristDown, WristUp, WristShoot,
                             WristUpPower, WristDownPower,
                             WristElevatorSafe, WristHalf, WristCalibrate, WristSlop,
                             ShootTime }

    public void init() { }

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
    public void openIntake(CommandState state) {
        arms.set(DoubleSolenoid.Value.kReverse);
        setIntakePower(hasCube() ? 0 : 0.75);
    }

    public double automaticIntakeTimer = 0;
    @Command
    public boolean closeIntake(CommandState state) {
        if(hasCube() && (automaticIntakeTimer == 0)) {
            automaticIntakeTimer = state.elapsedTime;
        }

        boolean done = (state.elapsedTime - automaticIntakeTimer > 0.35) || (state.elapsedTime > 4.0);

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

    public void periodic() {
        double wristPosition = getWristPosition();
        double power = 0;
        if(Math.abs(wristSetpoint - wristPosition) < value(WristSlop)) {
            power = 0;
        } else if(wristPosition < wristSetpoint) {
            power = value(WristDownPower);
        } else if(wristPosition > wristSetpoint) {
            power = value(WristUpPower);
        }
        wrist.set(power);

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
