package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import team4618.robot.Subsystem;

import team4618.robot.CommandSequence.CommandState;

import static team4618.robot.Subsystem.Units.*;
import static team4618.robot.subsystems.ElevatorSubsystem.Parameters.*;

public class ElevatorSubsystem extends Subsystem {

    public WPI_TalonSRX shepherd = new WPI_TalonSRX(13);
    public WPI_VictorSPX sheep = new WPI_VictorSPX(23);
    public BaseMotorController auxiliary = new WPI_TalonSRX(14); //TODO: change this to a victor on the comp bot
    public DoubleSolenoid elevatorBrake = new DoubleSolenoid(7, 6);
    public double heightSetpoint = 0;

    public void init() {
        shepherd.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 0);
        shepherd.configPeakOutputForward(1, 0);
        shepherd.configPeakOutputReverse(-1 /*0.35*/, 0);

        shepherd.setSensorPhase(true);
        shepherd.setInverted(true);
        auxiliary.setInverted(true);
        sheep.setInverted(true);
    }

    public double getSpeed() {
        return shepherd.getSensorCollection().getQuadratureVelocity() * 10;
    }

    public void postState() {
        PostState("775 Current", Unitless, auxiliary.getOutputCurrent());
        PostState("Cim Current", Unitless, shepherd.getOutputCurrent());
        PostState("Velocity Setpoint", Unitless, (shepherd.getControlMode() == ControlMode.Velocity) ? (10 * shepherd.getClosedLoopTarget(0)) : 0);
        PostState("Position", Unitless, shepherd.getSensorCollection().getQuadraturePosition());
        PostState("Speed", Unitless, getSpeed());
        PostState("Shepherd Power", Percent, shepherd.getMotorOutputPercent());
        PostState("Sheep Power", Percent, sheep.getMotorOutputPercent());
        PostState("Auxiliary Power", Percent, auxiliary.getMotorOutputPercent());
        PostState("Height Setpoint", Unitless, heightSetpoint);
    }

    @Subsystem.ParameterEnum
    public enum Parameters {
        UpP, UpI, UpD, UpF,
        DownP, DownI, DownD, DownF,
        UpSpeed, DownSpeed, Slop, DistanceToSlowdown,
        SpeedSlop, AuxiliaryDeadzone
    }

    @Command
    public boolean goToHeight(CommandState state, double height) {
        heightSetpoint = height;
        return isAt(height) && (Math.abs(getSpeed()) < value(SpeedSlop));
    }

    @Command
    public void setHeight(CommandState state, double height) {
        heightSetpoint = height;
    }

    @Command
    public boolean waitForSetpoint(CommandState state) {
        return isAt(heightSetpoint) && (Math.abs(getSpeed()) < value(SpeedSlop));
    }

    public boolean isAt(double height) {
        return Math.abs(getHeight() - height) < value(Slop);
    }

    public double getHeight() {
        return shepherd.getSensorCollection().getQuadraturePosition();
    }

    public void setSpeedSetpoint(double setpoint) {
        boolean goingUp = setpoint >= 0;
        shepherd.config_kP(0, value(goingUp ? UpP : DownP), 0);
        shepherd.config_kI(0, value(goingUp ? UpI : DownI), 0);
        shepherd.config_kD(0, value(goingUp ? UpD : DownD), 0);
        shepherd.config_kF(0, value(goingUp ? UpF : DownF), 0);
        shepherd.set(ControlMode.Velocity, setpoint * (1.0 / 10.0));
    }

    public static double lerp(double a, double t, double b) { return (1 - t) * a + t * b; }

    public void periodic() {
        double elevatorHeight = getHeight();
        boolean isAtSetpoint = isAt(heightSetpoint);

        if(isAtSetpoint) {
            shepherd.configContinuousCurrentLimit(10, 0);
            setSpeedSetpoint(0);
        } else {
            shepherd.configContinuousCurrentLimit(700, 0);
            double speed = (elevatorHeight > heightSetpoint) ? -value(DownSpeed) : value(UpSpeed);

            if(Math.abs(elevatorHeight - heightSetpoint) <= value(DistanceToSlowdown)) {
                speed *= lerp(0.1, Math.abs(elevatorHeight - heightSetpoint) / value(DistanceToSlowdown), 1);
            }

            setSpeedSetpoint(speed);
        }

        sheep.set(shepherd.getMotorOutputPercent());
    }

    public String name() { return "Elevator"; }
}
