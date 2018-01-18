package team4618.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.*;

import static team4618.robot.Subsystem.*;
import static team4618.robot.DriveSubsystem.Parameters.*;
import static team4618.robot.Subsystem.Units.*;

public class DriveSubsystem extends Subsystem {
    public static final double feet_per_pulse = (2 * Math.PI * 4 / 12) / 1228;

    public class DriveSide implements PIDOutput {
        public TalonSRX frontDrive;
        public TalonSRX rearDrive;
        public Encoder encoder;
        public PIDController controller;

        public DriveSide(int front_can_id, int rear_can_id, int enc_A, int enc_B) {
            frontDrive = new TalonSRX(front_can_id);
            rearDrive = new TalonSRX(rear_can_id);
            encoder = new Encoder(enc_A, enc_B);
            encoder.setDistancePerPulse(DriveSubsystem.feet_per_pulse);
            encoder.setPIDSourceType(PIDSourceType.kRate);
            controller = new PIDController(0, 0, 0, encoder, this);
        }

        public void postState(String prefix) {
            PostState(prefix + " Speed", FeetPerSecond, encoder.getRate());
            PostState(prefix + " Setpoint", FeetPerSecond, controller.getSetpoint());
            PostState(prefix + " Power", Unitless, controller.get());
        }

        @Override
        public void pidWrite(double output) {
            frontDrive.set(ControlMode.PercentOutput, output);
            rearDrive.set(ControlMode.PercentOutput, output);
        }
    }

    public DriveSide left = new DriveSide(1, 2, 0, 1);
    public DriveSide right = new DriveSide(3, 4, 2, 3);
    public DoubleSolenoid shifter = new DoubleSolenoid(0, 1);
    public ADXRS450_Gyro gyro = new ADXRS450_Gyro();

    public void init() {
        left.encoder.setReverseDirection(true);

        right.frontDrive.setInverted(true);
        right.rearDrive.setInverted(true);

        gyro.calibrate();
    }

    @ParameterEnum
    public enum Parameters { P, I, D }

    public void updateParameters() {
        left.controller.setPID(value(P), value(I), value(D));
        right.controller.setPID(value(P), value(I), value(D));
    }

    public void enable() {
        left.controller.enable();
        right.controller.enable();
    }

    public void disable() {
        left.controller.disable();
        right.controller.disable();
    }

    @Command
    public void driveDistance(@Unit(Feet) double distance) {
        left.encoder.reset();
        left.encoder.setPIDSourceType(PIDSourceType.kDisplacement);
        left.controller.setSetpoint(distance);

        right.encoder.reset();
        right.encoder.setPIDSourceType(PIDSourceType.kDisplacement);
        right.controller.setSetpoint(distance);

        //WaitFor((left.encoder.getDistance() == distance) && (right.encoder.getDistance() == distance));
    }

    @Command(/*blocking = false*/)
    public void drive(@Unit(FeetPerSecond) double speed, @Unit(DegreesPerSecond) double turn) {

    }

    public void postState() {
        left.postState("Left");
        right.postState("Right");
        PostState("Angle", Degrees, gyro.getAngle());
    }

    public String name() { return "Drive"; }
}