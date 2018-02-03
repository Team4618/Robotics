package team4618.robot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class FileIO {

    public static HashMap<String, Double> MapFromFile(Subsystem subsystem) {
        try {
            FileInputStream fis = new FileInputStream("/home/lvuser/" + subsystem.name() + "_parameters");
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<String, Double> result = (HashMap<String, Double>) ois.readObject();
            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void MapToFile(Subsystem subsystem, HashMap<String, Double> map) {
        try {
            FileOutputStream fos = new FileOutputStream("/home/lvuser/" + subsystem.name() + "_parameters");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
