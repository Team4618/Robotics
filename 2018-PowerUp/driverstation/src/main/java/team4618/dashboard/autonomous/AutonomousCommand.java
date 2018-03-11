package team4618.dashboard.autonomous;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import team4618.dashboard.pages.AutonomousPage;

import java.util.Arrays;

public class AutonomousCommand {
    public String templateName;
    public double[] parameterValues;

    public String conditional;
    public AutonomousCommand[] commands;

    public AutonomousCommand(String conditional, AutonomousCommand[] commands) {
        this.conditional = conditional;
        this.commands = commands;
    }

    public AutonomousCommand(AutonomousCommandTemplate t) {
        templateName = t.hashName();
        parameterValues = new double[t.parameterNames.length];
    }

    public AutonomousCommand(JSONObject json) {
        if(json.containsKey("Conditional") && json.containsKey("Commands")) {
            JSONArray jsonCommands = (JSONArray) json.get("Commands");
            conditional = (String) json.get("Conditional");
            commands = new AutonomousCommand[jsonCommands.size()];
            for(int i = 0; i < commands.length; i++)
                commands[i] = new AutonomousCommand((JSONObject) jsonCommands.get(i));
        } else if(json.containsKey("Subsystem Name") && json.containsKey("Command Name") && json.containsKey("Params")) {
            JSONArray params = (JSONArray) json.get("Params");
            templateName = json.get("Subsystem Name") + ":" + json.get("Command Name");
            parameterValues = new double[params.size()];
            for(int i = 0; i < parameterValues.length; i++)
                parameterValues[i] = (double) params.get(i);
        }
    }

    public boolean isBranchedCommand() { return commands != null; }
    public AutonomousCommandTemplate getTemplate() { return AutonomousCommandTemplate.templates.get(templateName); }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        if(isBranchedCommand()) {
            result.put("Conditional", conditional);
            JSONArray branchCommands = new JSONArray();
            result.put("Commands", branchCommands);
            Arrays.asList(commands).forEach(c -> branchCommands.add(c.toJSON()));
        } else {
            result.put("Subsystem Name", getTemplate().subsystemName);
            result.put("Command Name", getTemplate().commandName);
            result.put("Params", parameterValues);
        }
        return result;
    }

    public VBox editorBlock(VBox editor, PathNode pathNode) {
        AutonomousCommandTemplate template = getTemplate();

        HBox titleRow = new HBox();
        Button deleteBlock = new Button("Delete");
        deleteBlock.setOnAction(evt -> {
            pathNode.commands.remove(this);
            AutonomousPage.rebuildEditor();
        });

        titleRow.getChildren().addAll(new Label(templateName), deleteBlock);
        VBox result = AutonomousPage.commandBlock(editor, titleRow);

        for(int i = 0; i < template.parameterNames.length; i++) {
            final int index = i;
            HBox parameterRow = new HBox();
            TextField parameterField = new TextField(Double.toString(parameterValues[i]));
            parameterField.setOnAction(event -> {
                try {
                    parameterValues[index] = Double.valueOf(parameterField.getText());
                } catch (Exception e) { }
            });
            parameterRow.getChildren().addAll(new Label(template.parameterNames[i]), parameterField, new Label(template.parameterUnits[i]));
            result.getChildren().add(parameterRow);
        }

        return result;
    }

    public VBox commandBlock(VBox parent) {
        VBox result;
        if(isBranchedCommand()) {
            result = AutonomousPage.commandBlock(parent, "branch: " + conditional);
            Arrays.asList(commands).forEach(c -> result.getChildren().add(c.commandBlock(result)));
        } else {
            AutonomousCommandTemplate template = getTemplate();
            result = AutonomousPage.commandBlock(parent, templateName);

            for(int i = 0; i < template.parameterNames.length; i++) {
                String paramText = template.parameterNames[i] + " " + parameterValues[i] + " " + template.parameterUnits[i];
                result.getChildren().addAll(new Label(paramText));
            }
        }
        return result;
    }
}