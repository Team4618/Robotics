package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PIDController;
import team4618.robot.Subsystem;

import static team4618.robot.Subsystem.Units.Percent;
import static team4618.robot.Subsystem.Units.Unitless;
import static team4618.robot.subsystems.IntakeSubsystem.Parameters.*;

public class IntakeSubsystem extends Subsystem {

    public DoubleSolenoid arms = new DoubleSolenoid(2, 3);

    public AnalogInput liftPot = new AnalogInput(0);
    public WPI_VictorSPX leftLift = new WPI_VictorSPX(33);
    public WPI_VictorSPX rightLift = new WPI_VictorSPX(24);
    public PIDController liftController = new PIDController(0, 0, 0, liftPot, value -> {
        leftLift.set(-value);
        rightLift.set(value);
    });

    public WPI_VictorSPX leftIntake = new WPI_VictorSPX(15);
    public WPI_VictorSPX rightIntake = new WPI_VictorSPX(25);

    @Subsystem.ParameterEnum
    public enum Parameters { LiftP, LiftI, LiftD, LiftPotDown, LiftPotUp }
    public void updateParameters() { liftController.setPID(value(LiftP), value(LiftI), value(LiftD)); }

    public void init() {

    }

    public void postState() {
        PostState("Lift Pot", Unitless, liftPot.getVoltage());
        PostState("Lift Setpoint", Unitless, liftController.getSetpoint());
        PostState("Lift Power", Percent, leftLift.getMotorOutputPercent());
    }

    public String name() { return "Intake"; }
}
