package team4618.robot;

public interface PositionProvider {
    RobotPosition getPosition(RobotPosition prevPos, double dt);
}
