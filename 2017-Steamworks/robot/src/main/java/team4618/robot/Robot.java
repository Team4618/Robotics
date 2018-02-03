package team4618.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import team4618.robot.subsystems.DriveSubsystem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class Robot extends TimedRobot {
    public Joystick driver = new Joystick(0);
    public DriveSubsystem driveSubsystem = new DriveSubsystem();
    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable currentlyExecutingTable;
    public static NetworkTable autoTable;

    public void robotInit() {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue("Crumb Tray");
        currentlyExecutingTable = network.getTable("Custom Dashboard/Executing");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        Subsystems.init();
    }

    public void robotPeriodic() {
        Subsystems.postState();
    }

    public static class CommandState {
        public double startTime;
        public double elapsedTime;
        public boolean init = true;

        public CommandState() {
            startTime = Timer.getFPGATimestamp();
        }

        public void update() {
            elapsedTime = Timer.getFPGATimestamp() - startTime;
        }

        public void postState(String name, Subsystem.Units unit, double value) {
            currentlyExecutingTable.getEntry(name + "_Value").setValue(value);
            currentlyExecutingTable.getEntry(name + "_Unit").setValue(unit.toString());
        }
    }

    public static class AutonomousCommand {
        public Subsystem subsystem;
        public Method command;
        public double[] parameters;
        public CommandState state;

        public AutonomousCommand(String subsystemName, String commandName, double[] params) {
            try {
                String paramPrintout = "";
                for(double p : params) { paramPrintout += (p + ", "); }
                System.out.println(subsystemName + " -> " + commandName + "(" + paramPrintout + ")");

                subsystem = Subsystems.subsystems.get(subsystemName);
                for(Method m : subsystem.getClass().getDeclaredMethods()) {
                    if(m.getName().equals(commandName))
                        command = m;
                }
                parameters = params;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    ArrayList<AutonomousCommand> commands = new ArrayList<>();
    int currentlyExecuting = 0;

    public void autonomousInit() {
        commands.clear();
        currentlyExecuting = 0;

        String[] ordered = autoTable.getSubTables().toArray(new String[0]);
        Arrays.sort(ordered);
        for (String i : ordered) {
            NetworkTable table = autoTable.getSubTable(i);

            if(table.containsKey("Subsystem Name") && table.containsKey("Command Name") && table.containsKey("Params")) {
                commands.add(new AutonomousCommand(table.getEntry("Subsystem Name").getString(""),
                        table.getEntry("Command Name").getString(""),
                        table.getEntry("Params").getDoubleArray(new double[0])));
            }
        }
    }

    public void autonomousPeriodic() {
        if(currentlyExecuting < commands.size()) {
            try {
                AutonomousCommand currentCommand = commands.get(currentlyExecuting);

                if (currentCommand.state == null) {
                    currentCommand.state = new CommandState();

                    currentlyExecutingTable.getEntry("Subsystem Name").setString(currentCommand.subsystem.name());
                    currentlyExecutingTable.getEntry("Command Name").setString(currentCommand.command.getName());
                }
                currentCommand.state.update();

                Object[] params = new Object[1 + currentCommand.parameters.length];
                params[0] = currentCommand.state;
                for (int i = 0; i < currentCommand.parameters.length; i++) {
                    params[i + 1] = currentCommand.parameters[i];
                }

                Object ret = currentCommand.command.invoke(currentCommand.subsystem, params);
                if (ret instanceof Boolean) {
                    if ((Boolean) ret) {
                        for (String key : currentlyExecutingTable.getKeys()) {
                            currentlyExecutingTable.getEntry(key).delete();
                        }
                        currentlyExecuting++;
                    }
                } else {
                    for (String key : currentlyExecutingTable.getKeys()) {
                        currentlyExecutingTable.getEntry(key).delete();
                    }
                    currentlyExecuting++;
                }

                currentCommand.state.init = false;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void teleopInit() {
        driveSubsystem.left.controller.disable();
        driveSubsystem.right.controller.disable();
    }

    public void teleopPeriodic() {
        //TODO: state based teleop controls
        //OnHoldSet(driver.getRawButton(5), driveSubsystem, DriveSubsystem.State.Shifted);
        //OnClickSet(op.getButton(2), elevatorSubsystem, Box2);
        //Subsystems.doTeleop();

        driveSubsystem.shifter.set(driver.getRawButton(5) ? DoubleSolenoid.Value.kForward : DoubleSolenoid.Value.kReverse);
        driveSubsystem.teleopDrive.arcadeDrive(driver.getRawAxis(4), -driver.getRawAxis(1));

        if(driver.getRawButton(4)) {
            driveSubsystem.navx.reset();
        }
    }

    public void disabledInit() {

    }

    public void disabledPeriodic() {
        if(driver.getRawButton(4)) {
            driveSubsystem.navx.reset();
        }
    }
}