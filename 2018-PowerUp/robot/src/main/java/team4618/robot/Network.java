package team4618.robot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Network {
    public static DatagramChannel channel;
    public static Thread netThread;
    public static AtomicBoolean netThreadRunning = new AtomicBoolean(true);
    public static HashMap<SocketAddress, ConnectedClient> connections = new HashMap<>();

    public static class ConnectedClient {
        public boolean wantsState;
        public long lastPing;
    }

    public static void init() {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(5801));
            netThread = new Thread(Network::tick);
            netThread.start();
            System.out.println("Network Thread: " + netThread.isAlive());
        } catch(Exception e) { e.printStackTrace(); }
    }

    public static void sendTo(JSONObject packet, SocketAddress dest) {
        try {
            channel.send(ByteBuffer.wrap(packet.toString().getBytes(Charset.forName("UTF-8"))), dest);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void HandlePacket(SocketAddress sender, byte[] data) {
        try {
            String message = new String(data, Charset.forName("UTF-8"));
            JSONObject json = (JSONObject) JSONValue.parseWithException(message);

            if(!json.get("Type").equals("Ping"))
                System.out.println(json.get("Type"));

            ConnectedClient client = connections.get(sender);
            if(client != null) {
                client.lastPing = System.currentTimeMillis();
            }

            switch ((String) json.get("Type")) {
                case "Connect": {
                    if (client == null) {
                        ConnectedClient newClient = new ConnectedClient();
                        newClient.wantsState = (boolean) json.get("State");
                        newClient.lastPing = System.currentTimeMillis();
                        connections.put(sender, client);

                        JSONObject welcomeMessage = new JSONObject();
                        welcomeMessage.put("Type", "Welcome");
                        welcomeMessage.put("Name", Robot.name);
                        welcomeMessage.put("Bounds", new JSONObject());
                        JSONArray commands = new JSONArray();
                        //TODO: make each command its own json object
                        Subsystems.subsystems.values().forEach(s -> s.commands.keySet().forEach(c -> commands.add(s.name() + ":" + c)));
                        welcomeMessage.put("Commands", commands);
                        JSONArray conditionals = new JSONArray();
                        conditionals.addAll(CommandSequence.conditions.keySet());
                        welcomeMessage.put("Conditionals", conditionals);
                        sendTo(welcomeMessage, sender);
                    }
                } break;

                case "GetAuto": {

                } break;

                case "SetAuto": {
                    JSONArray commands = (JSONArray) json.get("Commands");
                    Robot.autoProgram.loadedCommands = CommandSequence.loadCommandsFromJSON(commands);

                    JSONObject autoIsPacket = new JSONObject();
                    autoIsPacket.put("Type", "AutoIs");
                    autoIsPacket.put("Commands", commands);
                    connections.entrySet().forEach(e -> sendTo(autoIsPacket, e.getKey()));
                } break;

                case "GetParameters": {

                } break;

                case "SetParameters": {

                } break;

                case "SetLocation": {

                } break;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static ByteBuffer buffer = ByteBuffer.allocate(16384);
    public static void tick() {
        while(netThreadRunning.get()) {
            try {
                //Handle & respond to incoming packets
                boolean hasPackets = true;

                while (hasPackets) {
                    buffer.clear();
                    SocketAddress sender = channel.receive(buffer);
                    if (sender == null) {
                        hasPackets = false;
                    } else {
                        byte[] data = new byte[buffer.position()];
                        buffer.position(0);
                        buffer.get(data);
                        HandlePacket(sender, data);
                    }
                }

                //check last ping times of connected things, remove from list if timed out

                //send state
            /*
            JSONObject states = new JSONObject();
            Subsystems.subsystems.values().forEach(s -> {
                s.postState();
                states.put(s.name(), s.getState());
            });
            */

                Thread.sleep(100);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
