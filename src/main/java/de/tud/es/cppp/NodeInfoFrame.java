package de.tud.es.cppp;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NodeInfoFrame extends JFrame {
    private static NodeInfoFrame instance;
    public static synchronized NodeInfoFrame getInstance(){
        if (NodeInfoFrame.instance == null){
            NodeInfoFrame.instance = new NodeInfoFrame();
        }
        return NodeInfoFrame.instance;
    }

    private static HashMap<String, JLabel> textFields = new HashMap<>();

    private NodeInfoFrame() {
        super("Node Information");
        JPanel nodeInfoPanel = new JPanel();

        nodeInfoPanel.setLayout(new GridBagLayout());
            /*
            nodeInfoPanel.setMinimumSize(new Dimension((int) (0.33 * WINDOW_WIDTH), WINDOW_HEIGHT));
            nodeInfoPanel.setPreferredSize(new Dimension((int) (0.33 * WINDOW_WIDTH), WINDOW_HEIGHT));
            nodeInfoPanel.setMaximumSize(new Dimension((int) (0.33 * WINDOW_WIDTH), WINDOW_HEIGHT));
             */
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.gridwidth = 5;
        labelConstraints.insets = new Insets(2, 20, 2, 20);
        GridBagConstraints textFieldConstraints = (GridBagConstraints) labelConstraints.clone();
        textFieldConstraints.gridx = 5;

        for (String labelText : NetworkNode.fieldTexts) {
            // Create label + tf
            JLabel label = new JLabel(labelText);
            JLabel tf = new JLabel("");
            // put in Map for Reference
            textFields.put(labelText, tf);
            // add to panel
            nodeInfoPanel.add(label, labelConstraints);
            nodeInfoPanel.add(tf, textFieldConstraints);

            labelConstraints.gridy++;
            textFieldConstraints.gridy++;
        }

        JPanel panel = new JPanel();
        panel.add(nodeInfoPanel);
        this.setPreferredSize(new Dimension(260,310));
        this.pack();
        this.add(panel);
    }

    public static void setNodeInfo(NetworkNode node){
        HashMap<String, String> infoMap = node.getInfoMap();
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            textFields.get(entry.getKey()).setText(entry.getValue());
        }
    }
}