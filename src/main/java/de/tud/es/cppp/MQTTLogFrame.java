package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Enumeration;

public class MQTTLogFrame extends JFrame {
    DefaultTableModel tableModel;
    private final String[] columnNames = {"Time", "Topic", "Message"};
    private Logger logger = LogManager.getLogger(MQTTLogFrame.class.getSimpleName());

    private static MQTTLogFrame instance;
    public static synchronized MQTTLogFrame getInstance(){
        if (MQTTLogFrame.instance == null){
            MQTTLogFrame.instance = new MQTTLogFrame();
        }
        return MQTTLogFrame.instance;
    }

    public MQTTLogFrame() {
        super("MQTT Log");
        logger.debug("Instantiate MQTTLog");


        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));
        //this.getContentPane().setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(columnNames, 0);

        JTable table = new JTable(tableModel);

        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
            }
        });
        // Set Table Editor with Textfield
        JTextField textField = new JTextField();
        textField.setEditable(false);
        table.setDefaultEditor(Object.class, new DefaultCellEditor(textField));

        table.getTableHeader().setReorderingAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);

        // Set width for all Columns
        TableColumn col = table.getColumnModel().getColumn(0);
        col.setPreferredWidth(50);
        col = table.getColumnModel().getColumn(1);
        col.setPreferredWidth(250);
        col = table.getColumnModel().getColumn(2);
        col.setPreferredWidth(350);
        //col.setMinWidth(50);
        //col.setMaxWidth(150);
        //col.setResizable(true);

        final JScrollPane scrollPane = new JScrollPane(table);
        this.getContentPane().add(scrollPane);
        this.setLocationRelativeTo(null);
        this.pack();
    }


    private class AddRowJob implements Runnable {
        String time, topic, payload;

        public AddRowJob(String time, String topic, String payload) {
            this.time = time;
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public void run() {
            tableModel.addRow(new String[]{time, topic, payload});
        }
    }

    public void addMessage(String time, String topic, String payload) {
        SwingUtilities.invokeLater(new AddRowJob(time, topic, payload));
    }
}
