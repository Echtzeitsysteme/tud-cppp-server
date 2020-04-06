package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graphstream.ui.graphicGraph.stylesheet.StyleSheet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class MainFrame extends JFrame {

    private static MainFrame instance;
    private NodeInfoFrame nodeInfoFrame = NodeInfoFrame.getInstance();
    private MQTTLogFrame  mqttLogFrame = MQTTLogFrame.getInstance();


    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private JTextField styleTextInputField;
    private JLabel missingNodesLabel;

    public static synchronized MainFrame getInstance(){
        if (MainFrame.instance == null){
            MainFrame.instance = new MainFrame();
        }
        return MainFrame.instance;
    }

    JCheckBox showDetailsCheckbox;

    private Logger logger = LogManager.getLogger(MainFrame.class.getSimpleName());
    private MainFrame(){
        super("CPPP WSN Desktop App");

        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        // NORTH
        showDetailsCheckbox = new JCheckBox("Automatically Show Detailed Information about Node");
        showDetailsCheckbox.setSelected(true);
        this.add(showDetailsCheckbox, BorderLayout.NORTH);

        // SOUTH
        JPanel southPanel = makeSouthPanel();
        this.add(southPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = makeMenuBar();
        this.setJMenuBar(menuBar);
    }

    @Override
    public void setVisible(boolean b) {
        styleTextInputField.requestFocus();
        super.setVisible(b);
    }


    private ArrayList<String> inputHistory = new ArrayList<>();
    private int historyPointer = -1;
    private int safeIncrease(){
        if (historyPointer < inputHistory.size()-1){
            historyPointer++;
        }
        return historyPointer;
    }
    private int safeDecrease(){
        if (historyPointer > -1){
            historyPointer--;
        }
        return historyPointer;
    }

    private JPanel makeSouthPanel() {
        // Construct
        JPanel southPanel = new JPanel(new BorderLayout());
        JPanel southWestPanel = new JPanel();
        styleTextInputField = new JTextField(30);
        JLabel lastCommandLabel = new JLabel();
        missingNodesLabel = new JLabel();

        // Setup
        // listen on up/down key for browsing in input history, as in other consoles
        styleTextInputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getExtendedKeyCode()) {
                    case KeyEvent.VK_UP:
                        safeIncrease();
                        if (historyPointer != -1) {
                            styleTextInputField.setText(inputHistory.get(historyPointer));
                        }else{
                            styleTextInputField.setText("");
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        safeDecrease();
                        if (historyPointer != -1) {
                            styleTextInputField.setText(inputHistory.get(historyPointer));
                        }else{
                            styleTextInputField.setText("");
                        }
                        break;
                    default:
                        //No Action
                        break;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        // handle hitting enter
        styleTextInputField.addActionListener(a -> {
            logger.debug("{}", a.getActionCommand().getBytes());
            String input = styleTextInputField.getText();
            inputHistory.add(0, input);
            StringBuilder stringBuilder = new StringBuilder("Last Command");
            StyleSheet styleSheet = new StyleSheet();

            lastCommandLabel.setForeground(Color.BLACK);

            boolean parseable = true;
            try {
                styleSheet.parseFromString(input);
            } catch (IOException e) {
                e.printStackTrace();
                parseable = false;
            }

            logger.debug("Input {} is {}parseable", input, parseable ? "" : "not ");

            if (parseable){
                // If parseable, add style
                Handler.getInstance().getGraph().addAttribute("ui.stylesheet", input);
            } else {
                // If not Parseable, append Errormessage
                lastCommandLabel.setForeground(Color.RED);
                stringBuilder.append(" has Syntax Error");
            }

            stringBuilder.append(": ").append(input);

            lastCommandLabel.setText(stringBuilder.toString());
            historyPointer = -1;
            styleTextInputField.setText("");
        });

        // Connect
        southWestPanel.add(styleTextInputField);
        southWestPanel.add(lastCommandLabel);
        southPanel.add(southWestPanel, BorderLayout.WEST);
        southPanel.add(missingNodesLabel, BorderLayout.EAST);

        return southPanel;
    }

    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");

        AbstractAction showNodeTableAction = new AbstractAction("Show Nodes Table") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Handler.getInstance().getNodesTableFrame().setVisible(true);
            }
        };
        AbstractAction quitAction = new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        AbstractAction showDetails = new AbstractAction("Show Node Details") {
            @Override
            public void actionPerformed(ActionEvent e) {
                nodeInfoFrame.setVisible(true);
            }
        };
        AbstractAction showMQTTLog = new AbstractAction("Show MQTT Log") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mqttLogFrame.setVisible(true);
            }
        };

        JMenuItem tableMenuItem = createMenuItem(showNodeTableAction, 't', "shift T");
        JMenuItem quitMenuItem = createMenuItem(quitAction, 'q', "shift Q");
        JMenuItem showDetailsMenuItem = createMenuItem(showDetails, 'd', "shift D");
        JMenuItem showMQTTMenuItem = createMenuItem(showMQTTLog, 'm', "shift M");

        menu.add(showMQTTMenuItem);
        menu.add(tableMenuItem);
        menu.add(showDetailsMenuItem);

        menu.add(quitMenuItem);



        menuBar.add(menu);

        return menuBar;
    }

    private JMenuItem createMenuItem(AbstractAction action, char mnemonic, String keystroke){
        JMenuItem menuItem = new JMenuItem();
        menuItem.setAction(action);
        menuItem.setMnemonic(mnemonic);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(keystroke));
        return menuItem;
    }

    public void updateUnconnectedNodes(Set<String> nodes) {
        StringBuilder stringBuilder = new StringBuilder("Unconnected Nodes: ");
        if (nodes.size() > 0) {

            for (String node : nodes) {
                stringBuilder.append(node).append(", ");
            }
            int length = stringBuilder.length();
            stringBuilder.replace(length - 2, length - 1, ";");
        }else{
            stringBuilder.append("None / all connected");
        }
        missingNodesLabel.setText(stringBuilder.toString());
    }
}