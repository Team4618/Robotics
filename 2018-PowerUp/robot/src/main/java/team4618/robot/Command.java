package team4618.robot;

import java.lang.reflect.Method;

public class Command {
    public Method command;
    public Method initializer;
    public Subsystem subsystem;

    public Command(Method command, Method initializer, Subsystem subsystem) {
        this.command = command;
        this.initializer = initializer;
        this.subsystem = subsystem;

        if(initializer != null) {
            Class<?>[] commandParams = command.getParameterTypes();
            Class<?>[] initializerParams = initializer.getParameterTypes();
            //TODO: make sure they're compatible
        }
    }
}
