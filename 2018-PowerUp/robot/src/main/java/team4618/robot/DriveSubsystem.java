package team4618.robot;

import com.ctre.phoenix.ParamEnum;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;

import static team4618.robot.DriveSubsystem.Parameters.*;
import static team4618.robot.Subsystem.Units.*;

public class DriveSubsystem extends Subsystem {
    public static final double feet_per_pulse = ((2 * Math.PI * 4 / 12) / 1228) / 2;

    public class DriveSide {
        public WPI_TalonSRX shepherd;
        public WPI_VictorSPX sheep;
        //public Encoder encoder;

        public DriveSide(int shepherd_can_id, int sheep_can_id, int enc_A, int enc_B) {
            shepherd = new WPI_TalonSRX(shepherd_can_id);
            sheep = new WPI_VictorSPX(sheep_can_id);
            sheep.follow(shepherd);

            //encoder = new Encoder(enc_A, enc_B);
            //encoder.setDistancePerPulse(DriveSubsystem.feet_per_pulse);
            //encoder.setPIDSourceType(PIDSourceType.kRate);
        }

        public void postState(String prefix) {
            //PostState(prefix + " Speed", FeetPerSecond, encoder.getRate());
            //PostState(prefix + " Setpoint", FeetPerSecond, controller.getSetpoint());
            //PostState(prefix + " Power", Unitless, controller.get());
            //PostState(prefix + " Distance", Feet, encoder.getDistance());
            //PostState(prefix + " Value", Feet, shepherd.get());
        }
    }

    public DriveSide left = new DriveSide(4, 57, 0, 1);
    public DriveSide right = new DriveSide(62, 59, 2, 3);
    public DoubleSolenoid shifter = new DoubleSolenoid(0, 1);

    public DifferentialDrive teleopDrive = new DifferentialDrive(left.shepherd, right.shepherd);

    public void init() {
        //left.encoder.setReverseDirection(true);

        right.shepherd.setInverted(true);
        right.sheep.setInverted(true);

        teleopDrive.setSafetyEnabled(false);
    }

    @Subsystem.ParameterEnum
    public enum Parameters { LeftP, LeftI, LeftD, RightP, RightI, RightD,
                             TurnSlop, TurnSpeed, DistanceSlop }

    public void updateParameters() { }

    public void enable() { }

    public void disable() { }

    public void postState() {
        left.postState("Left");
        right.postState("Right");
    }

    public String name() { return "Drive"; }
}