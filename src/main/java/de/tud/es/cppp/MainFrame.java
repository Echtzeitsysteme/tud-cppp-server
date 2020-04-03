package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graphstream.ui.graphicGraph.stylesheet.StyleSheet;
import scala.util.parsing.combinator.testing.Str;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class MainFrame extends JFrame {

    private static MainFrame instance;
    private StyleConsoleFrame consoleWindow = StyleConsoleFrame.getInstance();
    private NodeInfoFrame nodeInfoFrame = NodeInfoFrame.getInstance();
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

    private JPanel makeSouthPanel() {
        // Construct
        JPanel southPanel = new JPanel(new BorderLayout());
        JPanel southWestPanel = new JPanel();
        styleTextInputField = new JTextField(30);
        JLabel lastCommandLabel = new JLabel();
        missingNodesLabel = new JLabel();

        // Setup
        styleTextInputField.addActionListener(a -> {
            String input = styleTextInputField.getText();
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
        AbstractAction showConsole = new AbstractAction("Show Styling Console") {
            @Override
            public void actionPerformed(ActionEvent e) {
                consoleWindow.setVisible(true);
            }
        };
        AbstractAction showDetails = new AbstractAction("Show Node Details") {
            @Override
            public void actionPerformed(ActionEvent e) {
                nodeInfoFrame.setVisible(true);
            }
        };

        JMenuItem tableMenuItem = createMenuItem(showNodeTableAction, 't', "shift T");
        JMenuItem quitMenuItem = createMenuItem(quitAction, 'q', "shift Q");
        JMenuItem showStyleConsoleMenuItem = createMenuItem(showConsole, 'c', "shift C");
        JMenuItem showDetailsMenuItem = createMenuItem(showDetails, 'd', "shift D");

        menu.add(tableMenuItem);
        menu.add(showStyleConsoleMenuItem);
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

            Iterator<String> it = nodes.iterator();
            while (it.hasNext()) {
                stringBuilder.append(it.next() + ", ");
            }
            int length = stringBuilder.length();
            stringBuilder.replace(length - 2, length - 1, ";");
        }else{
            stringBuilder.append("None / all connected");
        }
        missingNodesLabel.setText(stringBuilder.toString());
    }
}