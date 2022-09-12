package com.patiun.comportcommunicator.window;

import com.fazecast.jSerialComm.SerialPort;
import com.patiun.comportcommunicator.config.PortIndices;
import com.patiun.comportcommunicator.factory.ComponentFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class StatsWindow extends JFrame {

    private static final String WINDOW_NAME = "Stats";

    private static final StatsWindow INSTANCE = new StatsWindow();

    private final LinkedHashMap<String, Integer> portNamesToBytes = new LinkedHashMap<>();

    private final JTextArea textArea;

    public StatsWindow() {
        super();
        ComponentFactory.getInstance().setUpFrame(this, WINDOW_NAME);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        for (SerialPort serialPort : Arrays.copyOfRange(SerialPort.getCommPorts(), PortIndices.START_PORT_INDEX, PortIndices.END_PORT_INDEX)) {
            portNamesToBytes.put(serialPort.getDescriptivePortName(), 0);
        }

        textArea = ComponentFactory.getInstance().buildTextArea(false);
        updateTextArea();

        add(textArea);

        pack();
        setVisible(true);
    }

    public void incrementBytesTransferred(String portName, int value) {
        int currentValue = portNamesToBytes.get(portName);
        portNamesToBytes.put(portName, currentValue + value);
        updateTextArea();
    }

    private void updateTextArea() {
        StringBuilder newText = new StringBuilder();
        boolean outputPort = false;
        int previousBytes = 0;
        for (String portName : portNamesToBytes.keySet()) {
            newText.append(portName);
            if (outputPort) {
                newText.append(" - Bytes transferred: ").append(portNamesToBytes.get(portName) + previousBytes).append("\n");
            } else {
                newText.append(" -> ");
                previousBytes = portNamesToBytes.get(portName);
            }
            outputPort = !outputPort;
        }
        textArea.setText(newText.toString());
        pack();
    }

    public static StatsWindow getInstance() {
        return INSTANCE;
    }

}
