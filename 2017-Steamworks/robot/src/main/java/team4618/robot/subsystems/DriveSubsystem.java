package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import org.opencv.core.Mat;
import team4618.robot.Robot;
import team4618.robot.Subsystem;

import static team4618.robot.subsystems.DriveSubsystem.Parameters.*;
import static team4618.robot.Subsystem.Units.*;

public class DriveSubsystem extends Subsystem {
    public static final double feet_per_pulse = ((2 * Math.PI * 4 / 12) / 1228) / 2;

    public class DriveSide {
        public WPI_TalonSRX master;
        public WPI_TalonSRX slave;
        public Encoder encoder;
        public PIDController controller;

        public DriveSide(int master_can_id, int slave_can_id, int enc_A, int enc_B) {
            master = new WPI_TalonSRX(master_can_id);
            slave = new WPI_TalonSRX(slave_can_id);
            slave.follow(master);

            encoder = new Encoder(enc_A, enc_B);
            encoder.setDistancePerPulse(DriveSubsystem.feet_per_pulse);
            encoder.setPIDSourceType(PIDSourceType.kRate);
            controller = new PIDController(0, 0, 0, encoder,
                                           output -> master.set(ControlMode.PercentOutput, output));
        }

        public void postState(String prefix) {
            PostState(prefix + " Speed", FeetPerSecond, encoder.getRate());
            PostState(prefix + " Setpoint", FeetPerSecond, controller.getSetpoint());
            PostState(prefix + " Power", Unitless, controller.get());
            PostState(prefix + " Distance", Feet, encoder.getDistance());
        }
    }

    public DriveSide left = new DriveSide(1, 2, 0, 1);
    public DriveSide right = new DriveSide(3, 4, 2, 3);
    public DoubleSolenoid shifter = new DoubleSolenoid(0, 1);
    public AHRS navx = new AHRS(SPI.Port.kMXP);

    public DifferentialDrive teleopDrive = new DifferentialDrive(left.master, right.master);

    public void init() {
        left.encoder.setReverseDirection(true);

        right.master.setInverted(true);
        right.slave.setInverted(true);

        navx.reset();

        teleopDrive.setSafetyEnabled(false);
    }

    @Subsystem.ParameterEnum
    public enum Parameters { LeftP, LeftI, LeftD, RightP, RightI, RightD,
                             TurnSlop, TurnRateSlop, TurnSpeed, TurnOvershootSpeed,
                             DistanceSlop, DistanceRateSlop, OvershootSpeed}

    public void updateParameters() {
        left.controller.setPID(value(LeftP), value(LeftI), value(LeftD));
        right.controller.setPID(value(RightP), value(RightI), value(RightD));
    }

    public double Lerp(double a, double t, double b) {
        return (1 - t) * a + t * b;
    }

    public void initPID() {
        left.controller.reset();
        right.controller.reset();

        left.controller.enable();
        right.controller.enable();

        left.encoder.reset();
        right.encoder.reset();
    }

    public void stopPID() {
        left.controller.reset();
        right.controller.reset();

        left.master.set(0);
        right.master.set(0);
    }

    public double sign(double x) { return x / Math.abs(x); }

    @Command
    public boolean driveDistance(Robot.CommandState commandState, @Unit(Feet) double distance, @Unit(FeetPerSecond) double maxSpeed, @Unit(Seconds) double timeUntilMaxSpeed) {
        if(commandState.init)
            initPID();

        double curr_distance = (left.encoder.getDistance() + right.encoder.getDistance()) / 2.0;
        boolean overshot;
        if(distance > 0) {
            overshot = curr_distance > distance;
        } else {
            overshot = curr_distance < distance;
        }

        double speed = sign(distance) * (overshot ? -value(OvershootSpeed) : Lerp(0, Math.min(commandState.elapsedTime / timeUntilMaxSpeed, 1), maxSpeed));
        left.controller.setSetpoint(speed);
        right.controller.setSetpoint(speed);

        commandState.postState("Speed", FeetPerSecond, speed);
        commandState.postState("Left Remaining", Feet, left.encoder.getDistance() - distance);
        commandState.postState("Right Remaining", Feet, right.encoder.getDistance() - distance);

        boolean left_done = (Math.abs(left.encoder.getDistance() - distance) < value(DistanceSlop)) &&
                            (Math.abs(left.encoder.getRate()) < value(DistanceRateSlop));
        boolean right_done = (Math.abs(right.encoder.getDistance() - distance) < value(DistanceSlop)) &&
                            (Math.abs(right.encoder.getRate()) < value(DistanceRateSlop));

        if(left_done && right_done)
            stopPID();

        return left_done && right_done;
    }

    public double canonicalizeAngle(double rawAngle) {
        double angle = rawAngle;
        int revolutions = (int) (angle / 360);
        double mod360 = (angle - revolutions * 360);
        return mod360 < 0 ? 360 + mod360 : mod360;
    }

    public double getAngle() { return canonicalizeAngle(navx.getAngle()); }

    @Command
    public boolean turnToAngle(Robot.CommandState commandState, @Unit(Degrees) double angle) {
        if(commandState.init)
            initPID();

        double curr_angle = getAngle();
        double remaining_angle = curr_angle - angle;
        double left_direction;
        if (curr_angle > angle) {
            left_direction = (360 - curr_angle + angle) < (curr_angle - angle) ? 1 : -1;
        } else {
            left_direction = (360 - angle + curr_angle) > (angle - curr_angle) ? 1 : -1;
        }
        left.controller.setSetpoint(left_direction * value(TurnSpeed));
        right.controller.setSetpoint(-left_direction * value(TurnSpeed));

        boolean turn_done = (Math.abs(remaining_angle) < value(TurnSlop)) &&
                            (Math.abs(navx.getRate()) < value(TurnRateSlop));
        commandState.postState("Remaining", Degrees, remaining_angle);

        if(turn_done)
            stopPID();

        return turn_done;
    }

    //TODO: pathfinder
    @Command
    public boolean goTo() {
        return false;
    }

    public void postState() {
        left.postState("Left");
        right.postState("Right");
        PostState("Raw Angle", Degrees, navx.getAngle());
        PostState("Angle", Degrees, getAngle());
        PostState("Speed", FeetPerSecond, (left.encoder.getRate() + right.encoder.getRate()) / 2);
    }

    public String name() { return "Drive"; }
}