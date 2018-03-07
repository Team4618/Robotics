package team4618.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;

import team4618.robot.CommandSequence.CommandState;
import team4618.robot.Subsystem;

import static team4618.robot.subsystems.DriveSubsystem.Parameters.*;
import static team4618.robot.Subsystem.Units.*;

public class DriveSubsystem extends Subsystem {
    public static double getFeetPerPulse(double wheelDiameterInInches, double ticksPerRevolution) { return (2 * Math.PI * (wheelDiameterInInches / 2) / 12) / ticksPerRevolution; }

    public static final double feet_per_pulse = getFeetPerPulse(6, 4096);

    public class DriveSide {
        public WPI_TalonSRX shepherd;
        public WPI_VictorSPX sheep;
        public boolean flipDirection;

        public DriveSide(int shepherd_can_id, int sheep_can_id, boolean flipDirection) {
            shepherd = new WPI_TalonSRX(shepherd_can_id);
            sheep = new WPI_VictorSPX(sheep_can_id);
            sheep.follow(shepherd);
            shepherd.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 0);
            this.flipDirection = flipDirection;
        }

        public void setSetpoint(double setpoint) { shepherd.set(ControlMode.Velocity, setpoint * (1.0 / 10.0) * (1 / DriveSubsystem.feet_per_pulse)); }
        public double getDistance() { return (flipDirection ? -1 : 1) * feet_per_pulse * shepherd.getSensorCollection().getQuadraturePosition(); }
        //NOTE: multiply by 10 because it provides in ticks/100ms and we want ticks/sec
        public double getRate() { return (flipDirection ? -1 : 1) * feet_per_pulse * 10 * shepherd.getSensorCollection().getQuadratureVelocity(); }

        public void postState(String prefix) {
            PostState(prefix + " Speed", FeetPerSecond, getRate());
            PostState(prefix + " Position", Feet, getDistance());
            PostState(prefix + " Raw Position", Unitless, shepherd.getSensorCollection().getQuadraturePosition());
            PostState(prefix + " Setpoint", FeetPerSecond, (shepherd.getControlMode() == ControlMode.Velocity) ? (feet_per_pulse * 10 * shepherd.getClosedLoopTarget(0)) : 0);
            PostState(prefix + " Power", Percent, shepherd.getMotorOutputPercent());
        }
    }

    public DriveSide left = new DriveSide(11, 21, true);
    public DriveSide right = new DriveSide(12, 22, false);
    public DoubleSolenoid shifter = new DoubleSolenoid(0, 1);
    public AHRS navx = new AHRS(SPI.Port.kMXP);

    public DifferentialDrive teleopDrive = new DifferentialDrive(left.shepherd, right.shepherd);

    public void init() {
        left.shepherd.setSafetyEnabled(false);
        left.shepherd.setSensorPhase(true);

        right.shepherd.setSafetyEnabled(false);
        right.shepherd.setSensorPhase(true); //NOTE: we have to set this here but not while using software PID because the talon ignores setInverted
        right.shepherd.setInverted(true);
        right.sheep.setInverted(true);

        navx.reset();

        teleopDrive.setSafetyEnabled(false);
    }

    @Subsystem.ParameterEnum
    public enum Parameters { LeftP, LeftI, LeftD, LeftF,
                             RightP, RightI, RightD, RightF,
                             TurnSlop, TurnRateSlop, TurnOvershootSpeed,
                             DistanceSlop, DistanceRateSlop, DistanceOvershootSpeed }

    public void updateParameters() {
        left.shepherd.config_kP(0, value(LeftP), 0);
        left.shepherd.config_kI(0, value(LeftI), 0);
        left.shepherd.config_kD(0, value(LeftD), 0);
        left.shepherd.config_kF(0, value(LeftF), 0);

        right.shepherd.config_kP(0, value(RightP), 0);
        right.shepherd.config_kI(0, value(RightI), 0);
        right.shepherd.config_kD(0, value(RightD), 0);
        right.shepherd.config_kF(0, value(RightF), 0);
    }

    public static double sign(double x) { return x / Math.abs(x); }
    public static double lerp(double a, double t, double b) { return (1 - t) * a + t * b; }

    public void postState() {
        left.postState("Left");
        right.postState("Right");
        PostState("Speed", FeetPerSecond, (left.getRate() + right.getRate()) / 2.0);
        PostState("Angle", Degrees, navx.getAngle());
        PostState("Roll", Degrees, navx.getRoll());
    }

    public void resetPID() {
        left.shepherd.setSelectedSensorPosition(0, 0, 0);
        right.shepherd.setSelectedSensorPosition(0, 0, 0);

        left.shepherd.set(0);
        right.shepherd.set(0);
    }

    //TODO: fix this, slowdown to distance part breaks with negatives
    public static double trapazoidalProfile(double elapsedTime, double timeUntilMaxSpeed, double maxSpeed,
                                            double distanceTravelled, double distanceSetpoint, double distanceToSlowdown,
                                            double overshootSpeed) {
        double distanceRemaining = distanceSetpoint - distanceTravelled;

        if(distanceRemaining <= distanceToSlowdown) {
            return lerp(0, distanceRemaining / distanceToSlowdown, maxSpeed);
        } else if(distanceTravelled > distanceSetpoint) {
            return overshootSpeed;
        } else if(elapsedTime <= timeUntilMaxSpeed) {
            return lerp(0, elapsedTime / timeUntilMaxSpeed, maxSpeed);
        } else if(elapsedTime > timeUntilMaxSpeed) {
            return maxSpeed;
        }

        return 0;
    }

    @Command
    public boolean driveDistance(CommandState commandState, @Unit(Feet) double distance, @Unit(FeetPerSecond) double maxSpeed,
                                                            @Unit(Seconds) double timeUntilMaxSpeed, @Unit(Feet) double distanceToSlowdown) {
        if(commandState.init)
            resetPID();

        double leftDistance = sign(distance) * left.getDistance();
        double rightDistance = sign(distance) * right.getDistance();
        double distanceTravelled = (leftDistance + rightDistance) / 2.0;
        double speed = sign(distance) * trapazoidalProfile(commandState.elapsedTime, timeUntilMaxSpeed, maxSpeed, distanceTravelled, Math.abs(distance), distanceToSlowdown, value(DistanceOvershootSpeed));
        left.setSetpoint(speed);
        right.setSetpoint(speed);

        commandState.postState("Speed", FeetPerSecond, speed);
        commandState.postState("Left Travelled", Feet, leftDistance);
        commandState.postState("Right Travelled", Feet, rightDistance);
        commandState.postState("Left Remaining", Feet, Math.abs(distance) - leftDistance);
        commandState.postState("Right Remaining", Feet, Math.abs(distance) - rightDistance);

        boolean left_done = ((Math.abs(distance) - leftDistance) < value(DistanceSlop)) && (Math.abs(left.getRate()) < value(DistanceRateSlop));
        boolean right_done = ((Math.abs(distance) - rightDistance) < value(DistanceSlop)) && (Math.abs(right.getRate()) < value(DistanceRateSlop));

        if(left_done && right_done)
            resetPID();

        return left_done && right_done;
    }

    public double canonicalizeAngle(double rawAngle) {
        int revolutions = (int) (rawAngle / 360);
        double mod360 = (rawAngle - revolutions * 360);
        return mod360 < 0 ? 360 + mod360 : mod360;
    }

    public double getAngle() { return canonicalizeAngle(navx.getAngle()); }

    @Command("Turn to %")
    public boolean turnToAngle(CommandState commandState, @Unit(Degrees) double angle, @Unit(FeetPerSecond) double maxSpeed,
                                                          @Unit(Seconds) double timeUntilMaxSpeed, @Unit(Degrees) double angleToSlowdown) {
        if(commandState.init)
            resetPID();

        double canonicalized = canonicalizeAngle(angle);
        double curr_angle = getAngle();
        double remaining_angle = curr_angle - canonicalized;
        double left_direction;
        if (curr_angle > canonicalized) {
            left_direction = (360 - curr_angle + canonicalized) < (curr_angle - canonicalized) ? 1 : -1;
        } else {
            left_direction = (360 - canonicalized + curr_angle) > (canonicalized - curr_angle) ? 1 : -1;
        }
        double speed = lerp(0, Math.min(commandState.elapsedTime / timeUntilMaxSpeed, 1), maxSpeed);
        left.setSetpoint(left_direction * speed);
        right.setSetpoint(-left_direction * speed);

        boolean turn_done = (Math.abs(remaining_angle) < value(TurnSlop)) &&
                            (Math.abs(navx.getRate()) < value(TurnRateSlop));
        commandState.postState("Remaining", Degrees, remaining_angle);

        if(turn_done)
            resetPID();

        return turn_done;
    }

    //TODO: this
    @Command
    public boolean driveCurve(CommandState commandState) {
        return false;
    }

    public String name() { return "Drive"; }
}