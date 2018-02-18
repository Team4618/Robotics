package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import team4618.robot.Subsystem;

import team4618.robot.CommandSequence.CommandState;
import static team4618.robot.Subsystem.Units.*;

public class ElevatorSubsystem extends Subsystem {

    //NOTE: shepherd is a 775, sheep are CIMs
    public WPI_TalonSRX elevatorShepherd = new WPI_TalonSRX(13);
    public WPI_VictorSPX elevatorSheep1 = new WPI_VictorSPX(14);
    public WPI_VictorSPX elevatorSheep2 = new WPI_VictorSPX(23);
    public DoubleSolenoid elevatorBrake = new DoubleSolenoid(4, 5);

    public DoubleSolenoid intakeHorizontal = new DoubleSolenoid(2, 3);

    public AnalogInput liftPot = new AnalogInput(0);
    public WPI_VictorSPX leftLift = new WPI_VictorSPX(33);
    public WPI_VictorSPX rightLift = new WPI_VictorSPX(24);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);

    public void init() {
        elevatorSheep1.follow(elevatorShepherd);
        elevatorSheep2.follow(elevatorShepherd);
    }

    public void postState() {
        PostState("Shepherd Current", Unitless, elevatorShepherd.getOutputCurrent());
        PostState("Lift Pot", Unitless, liftPot.getVoltage());
        PostState("Raw Position", Unitless, elevatorShepherd.getSensorCollection().getQuadraturePosition());
    }

    boolean was3Down = false;
    public boolean intakeUp = true;

    boolean was4Down = false;
    public boolean intakeOpen = false;

    public void doTeleop(Joystick op) {
        boolean is3Down = op.getRawButton(3);
        if(was3Down && !is3Down) {
            intakeUp = !intakeUp;
        }
        was3Down = is3Down;

        boolean is4Down = op.getRawButton(4);
        if(was4Down && !is4Down) {
            intakeOpen = !intakeOpen;
        }
        was4Down = is4Down;

        if(op.getRawButton(5)) {
            leftIntake.set(-0.75);
            rightIntake.set(0.75);
        } else if(op.getRawButton(1)) {
            double outSpeed = 1.0; //((-op.getRawAxis(1) + 1) / 2);
            leftIntake.set(outSpeed);
            rightIntake.set(-outSpeed);
        } else {
            leftIntake.set(0);
            rightIntake.set(0);
        }

        intakeHorizontal.set(intakeOpen ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);

        elevatorBrake.set(op.getRawButton(8) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        if(op.getRawButton(6)) {
            elevatorShepherd.set(0.60);
        } else if(op.getRawButton(7)) {
            elevatorShepherd.set(-0.15);
        } else {
            elevatorShepherd.set(0);
        }

        elevatorShepherd.set(op.getRawAxis(1));
    }

    @Command
    public boolean goToHeight(CommandState state, @Unit(Feet) double height) {
        return false;
    }

    public String name() { return "Elevator"; }
}
