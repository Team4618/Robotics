package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PIDController;
import team4618.robot.Subsystem;

import team4618.robot.CommandSequence.CommandState;
import static team4618.robot.Subsystem.Units.*;
import static team4618.robot.subsystems.ElevatorSubsystem.Parameters.*;

public class ElevatorSubsystem extends Subsystem {

    //NOTE: shepherd is a 775, sheep are CIMs
    public WPI_TalonSRX elevatorShepherd = new WPI_TalonSRX(13);
    public WPI_VictorSPX elevatorSheep1 = new WPI_VictorSPX(14);
    public WPI_VictorSPX elevatorSheep2 = new WPI_VictorSPX(23);
    public DoubleSolenoid elevatorBrake = new DoubleSolenoid(6, 7);

    public void init() {
        elevatorShepherd.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 0);
        elevatorShepherd.configPeakOutputForward(1, 0);
        elevatorShepherd.configPeakOutputReverse(-1 /*0.35*/, 0);

        elevatorSheep1.follow(elevatorShepherd);
        elevatorSheep2.follow(elevatorShepherd);

        elevatorShepherd.setSensorPhase(true);
        elevatorShepherd.setInverted(true);
        elevatorSheep1.setInverted(true);
        elevatorSheep2.setInverted(true);
    }

    public void postState() {
        PostState("775 Current", Unitless, elevatorShepherd.getOutputCurrent());
        PostState("Elevator Setpoint", Unitless, (elevatorShepherd.getControlMode() == ControlMode.Velocity) ? (10 * elevatorShepherd.getClosedLoopTarget(0)) : 0);
        PostState("Elevator Position", Unitless, elevatorShepherd.getSensorCollection().getQuadraturePosition());
        PostState("Elevator Speed", Unitless, elevatorShepherd.getSensorCollection().getQuadratureVelocity() * 10);
        PostState("Elevator Shepherd Power", Percent, elevatorShepherd.getMotorOutputPercent());
        PostState("Elevator Sheep 1 Power", Percent, elevatorSheep1.getMotorOutputPercent());
        PostState("Elevator Sheep 2 Power", Percent, elevatorSheep2.getMotorOutputPercent());
    }

    @Subsystem.ParameterEnum
    public enum Parameters {
        UpElevatorP, UpElevatorI, UpElevatorD, UpElevatorF,
        DownElevatorP, DownElevatorI, DownElevatorD, DownElevatorF,
        ElevatorUpSpeed, ElevatorDownSpeed
    }

    @Command
    public boolean goToHeight(CommandState state, @Unit(Unitless) double height) {
        double currHeight = elevatorShepherd.getSensorCollection().getQuadraturePosition();
        double speed = DriveSubsystem.trapazoidalProfile(state.elapsedTime, 2, value(ElevatorUpSpeed), currHeight, height, 4000, 0);
        setElevatorSetpoint(speed);

        state.postState("Speed", Unitless, speed);
        state.postState("Height Travelled", Unitless, currHeight);
        state.postState("Height Remaining", Unitless, height - currHeight);

        boolean done = Math.abs(height - currHeight) < 1000;

        if(done)
            setElevatorSetpoint(0);

        return done;
    }

    public enum ElevatorHeight {
        Bottom(100), Switch(9900), ScaleLow(21500), ScaleHigh(29000), Climb(28000);

        public double setpoint;
        ElevatorHeight(double s) { this.setpoint = s; }
    }

    public void setElevatorSetpoint(double setpoint) {
        boolean goingUp = setpoint >= 0;
        elevatorShepherd.config_kP(0, value(goingUp ? UpElevatorP : DownElevatorP), 0);
        elevatorShepherd.config_kI(0, value(goingUp ? UpElevatorI : DownElevatorI), 0);
        elevatorShepherd.config_kD(0, value(goingUp ? UpElevatorD : DownElevatorD), 0);
        elevatorShepherd.config_kF(0, value(goingUp ? UpElevatorF : DownElevatorF), 0);
        elevatorShepherd.set(ControlMode.Velocity, setpoint * (1.0 / 10.0));
    }

    public String name() { return "Elevator"; }
}
