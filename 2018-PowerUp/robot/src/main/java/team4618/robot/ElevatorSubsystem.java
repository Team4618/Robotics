package team4618.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import static team4618.robot.Subsystem.Units.*;

public class ElevatorSubsystem extends Subsystem {
    public WPI_TalonSRX motor = new WPI_TalonSRX(10);

    public void init() { }

    public void enable() { }

    public void disable() { }

    public void postState() { }

    @Command
    public boolean goToHeight(@Unit(Feet) double height) {
        return false;
    }

    @Command
    public void ejectBlock() {

    }

    //@States(default = Analog)
    enum States { Box1, Box2, Box3, Analog }

    public String name() { return "Elevator"; }
}
