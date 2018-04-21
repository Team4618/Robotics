package team4618.robot;

import org.json.simple.JSONObject;

import java.net.DatagramSocket;
import java.net.ServerSocket;

public class Network {
    public static DatagramSocket statePort; //UDP 5801 for robot and eventual controller state
    public static ServerSocket commPort; //TCP 5082 for general communications
    public static Thread netThread;

    public static void init() {
        try {
            statePort = new DatagramSocket(5801);
            commPort = new ServerSocket(5082);
            netThread = new Thread(Network::tick);
        } catch(Exception e) { e.printStackTrace(); }
    }

    public static void tick() {
        try {
            //https://www.javaworld.com/article/2853780/core-java/socket-programming-for-scalable-systems.html?page=2
            //listen for accepts & add them to the connected things list
            //check things on the connected things list to see if theyre still connected, if they arent remove them
            //handle all incoming communications on commPort

            //send things on commPort


            //send state on statePort
            /*
            JSONObject states = new JSONObject();
            Subsystems.subsystems.values().forEach(s -> {
                s.postState();
                states.put(s.name(), s.getState());
            });
            */

            Thread.sleep(10);
        } catch(Exception e) { e.printStackTrace(); }
    }
}
