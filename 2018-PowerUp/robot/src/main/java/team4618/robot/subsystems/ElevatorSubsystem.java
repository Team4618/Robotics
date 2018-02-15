package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import team4618.robot.Subsystem;

import team4618.robot.CommandSequence.CommandState;
import static team4618.robot.Subsystem.Units.*;

public class ElevatorSubsystem extends Subsystem {

    public WPI_TalonSRX elevatorShepherd = new WPI_TalonSRX(58);
    public WPI_VictorSPX elevatorSheep = new WPI_VictorSPX(56);
    public DoubleSolenoid elevatorBrake = new DoubleSolenoid(2, 3);

    DoubleSolenoid intakeVertical = new DoubleSolenoid(6, 7);
    DoubleSolenoid intakeHorizontal = new DoubleSolenoid(4, 5);

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(50);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(3);

    public void init() {
        elevatorSheep.follow(elevatorShepherd);
    }

    public void postState() {
        PostState("Shepherd Current", Unitless, elevatorShepherd.getOutputCurrent());
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
        intakeVertical.set(intakeUp ? DoubleSolenoid.Value.kReverse : DoubleSolenoid.Value.kForward);

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
