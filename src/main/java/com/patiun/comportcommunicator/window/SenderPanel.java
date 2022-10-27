package com.patiun.comportcommunicator.window;

import com.fazecast.jSerialComm.SerialPort;
import com.patiun.comportcommunicator.bytestuffing.ByteStuffer;
import com.patiun.comportcommunicator.entity.Packet;
import com.patiun.comportcommunicator.factory.ComponentFactory;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SenderPanel extends JPanel {

    private static final String ACCEPTED_CHARACTERS_REGEX = "[ -~]";
    private static final List<Character> forbiddenCharacters = Arrays.asList((char) KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_DELETE);

    private final List<Byte> bufferedBytes = new ArrayList<>();
    private final SerialPort inputPort;

    private final ByteStuffer byteStuffer;

    private final StatsPanel statsPanel;

    private final JLabel name;

    public SenderPanel(SerialPort inputPort, ByteStuffer byteStuffer, StatsPanel statsPanel) throws HeadlessException {
        super();
        ComponentFactory.setUpPanel(this);
        setLayout(new BorderLayout());

        this.inputPort = inputPort;
        ControlPanel.getInstance().registerPort(inputPort);

        this.byteStuffer = byteStuffer;

        this.statsPanel = statsPanel;

        name = ComponentFactory.buildLabel(inputPort.getSystemPortName() + " - Sender");
        add(name, BorderLayout.PAGE_START);

        add(setUpInput(), BorderLayout.CENTER);
    }

    private JScrollPane setUpInput() {
        JTextArea inputTextArea = ComponentFactory.buildTextArea(true);
        inputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control V"), "none");
        inputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "none");
        inputTextArea.getInputMap().put(KeyStroke.getKeyStroke("control X"), "none");
        inputTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (forbiddenCharacters.contains(e.getKeyChar())) {
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!Pattern.matches(ACCEPTED_CHARACTERS_REGEX, String.valueOf(e.getKeyChar()))) {
                    e.consume();
                    return;
                }
                inputTextArea.setCaretPosition(inputTextArea.getDocument().getLength());
                byte keyByte = (byte) e.getKeyChar();
                bufferedBytes.add(keyByte);
                if (bufferedBytes.size() == Packet.DATA_BYTES_NUMBER) {
                    boolean stuffedFrame;
                    Packet packet;
                    if (bufferedBytes.contains(Packet.FLAG_BYTE)) {
                        List<Byte> stuffedBytes = byteStuffer.stuffBytes(bufferedBytes);
                        packet = new Packet(getPortNumberByte(), stuffedBytes);
                        stuffedFrame = true;
                    } else {
                        packet = new Packet(getPortNumberByte(), bufferedBytes);
                        stuffedFrame = false;
                    }
                    byte[] packetBytes = packet.toBytes();
                    List<Byte> packetBytesList = Arrays.asList(ArrayUtils.toObject(packetBytes));
                    if (stuffedFrame) {
                        statsPanel.updateStuffedFrame(packetBytesList, 3, 3 + packet.getData().size(), packetBytesList.size() - 1);
                    } else {
                        statsPanel.updateNonStuffedFrame(packetBytesList, packetBytesList.size() - 1);
                    }
                    int bytesWritten = inputPort.writeBytes(packetBytes, packetBytes.length);
                    DebugPanel.getInstance().sendMessage(name.getText(), "Sent " + bytesWritten + " bytes");
                    bufferedBytes.clear();
                }
            }
        });

        return ComponentFactory.buildScrollPane(inputTextArea);
    }

    private byte getPortNumberByte() {
        String portName = inputPort.getSystemPortName();
        String portNumber = portName.substring(3);
        return Byte.parseByte(portNumber);
    }

}
