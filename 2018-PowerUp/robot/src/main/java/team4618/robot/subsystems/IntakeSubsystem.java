package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Timer;
import team4618.robot.CommandSequence.CommandState;
import team4618.robot.Subsystem;

import static team4618.robot.Robot.op;
import static team4618.robot.Subsystem.Units.Percent;
import static team4618.robot.Subsystem.Units.Unitless;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.*;

public class IntakeSubsystem extends Subsystem {

    public DoubleSolenoid arms = new DoubleSolenoid(2, 3);
    public DoubleSolenoid latch = new DoubleSolenoid(4, 5);

    public AnalogInput liftPot = new AnalogInput(0);
    public WPI_VictorSPX leftLift = new WPI_VictorSPX(33);
    public WPI_VictorSPX rightLift = new WPI_VictorSPX(24);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);

    public boolean liftUp = false;

    @Subsystem.ParameterEnum
    public enum Parameters { LiftPotDown, LiftPotLow, LiftPotHigh, LiftPotUp,
                             LiftUpPower, LiftSlowDescentPower, LiftReleaseLatchPower, LiftReleaseLatchTime,
                             ShootTime }

    public void init() {

    }

    public void postState() {
        PostState("Lift Pot", Unitless, liftPot.getVoltage());
        PostState("Lift Power", Percent, leftLift.getMotorOutputPercent());
    }

    public double getLiftPosition() {
        return liftPot.getVoltage();
    }

    public void setLiftPower(double value) {
        leftLift.set(-value);
        rightLift.set(value);
    }

    @Command
    public void openIntake(CommandState state) {
        leftIntake.set(0.75);
        rightIntake.set(0.75);
        arms.set(DoubleSolenoid.Value.kReverse);
    }

    @Command
    public void closeIntake(CommandState state) {
        leftIntake.set(0);
        rightIntake.set(0);
        arms.set(DoubleSolenoid.Value.kForward);
    }

    @Command
    public boolean shoot(CommandState state, @Unit(Unitless) double speed) {
        boolean isDone = state.elapsedTime >= value(ShootTime);
        leftIntake.set(isDone ? 0 : -speed);
        rightIntake.set(isDone ? 0 : -speed);
        arms.set(DoubleSolenoid.Value.kForward);
        return isDone;
    }

    boolean wasUp = false;
    double startTime = 0;

    public void periodic() {
        if(op.getRawButton(3)) {
            setLiftPower(op.getRawAxis(1));
            latch.set(op.getRawButton(2) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        } else if(false) {
            double liftPosition = getLiftPosition();
            if(liftUp) {
                if(liftPosition > value(LiftPotHigh)) {
                    setLiftPower(0);
                    latch.set(DoubleSolenoid.Value.kReverse);
                } else {
                    setLiftPower(value(LiftUpPower));
                    latch.set(DoubleSolenoid.Value.kReverse);
                }
            } else {
                if(liftPosition < value(LiftPotLow)) {
                    setLiftPower(0);
                    latch.set(DoubleSolenoid.Value.kReverse);
                } else {
                    latch.set(DoubleSolenoid.Value.kForward);
                    setLiftPower((Timer.getFPGATimestamp() - startTime < value(LiftReleaseLatchTime)) ? value(LiftReleaseLatchPower) : value(LiftSlowDescentPower));
                }
            }

            if(wasUp != liftUp) {
                startTime = Timer.getFPGATimestamp();
                System.out.println("Changing state");
            }
            wasUp = liftUp;

            //System.out.println("Elapsed: " + (Timer.getFPGATimestamp() - startTime));
        }
    }

    public String name() { return "Intake"; }
}
