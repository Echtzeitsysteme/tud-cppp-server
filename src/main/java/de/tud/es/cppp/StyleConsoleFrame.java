package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graphstream.ui.graphicGraph.stylesheet.StyleSheet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.text.ParseException;


public class StyleConsoleFrame extends JFrame {
    private Logger logger = LogManager.getLogger(StyleConsoleFrame.class.getSimpleName());
    private static StyleConsoleFrame instance;
    public static synchronized StyleConsoleFrame getInstance(){
        if (StyleConsoleFrame.instance == null){
            StyleConsoleFrame.instance = new StyleConsoleFrame();
        }
        return StyleConsoleFrame.instance;
    }

    Handler handler;

    private StyleConsoleFrame(){
        super("StyleConsole");
        JTextPane historyTextPane = new JTextPane();
        JPanel panel = new JPanel();
        JTextField styleTextInputField = new JTextField(30);
        //JScrollPane scrollPane = new JScrollPane();
        historyTextPane.setText("");
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();

        Document historyDoc = historyTextPane.getStyledDocument();

        styleTextInputField.addActionListener(a -> {
            String input = styleTextInputField.getText();
            StyleSheet styleSheet = new StyleSheet();
            boolean parseable = true;
            try {
                styleSheet.parseFromString(input);
            } catch (IOException e) {
                e.printStackTrace();
                parseable = false;
            }
            logger.debug("Input {} is {}parseable", input, parseable ? "" : "not ");

            // Put Input to History
            try {
                historyDoc.insertString(historyDoc.getLength(), input, attributeSet);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            if (parseable){
                // If parseable, parse
                Handler.getInstance().getGraph().addAttribute("ui.stylesheet", input);
            } else {
                // If not Parseable, append Errormessage
                StyleConstants.setForeground(attributeSet, Color.red);
                try {
                    historyDoc.insertString(historyDoc.getLength(), " Syntax Error, could not be parsed!", attributeSet);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
                StyleConstants.setForeground(attributeSet, Color.black);
            }

            // Append newline
            try {
                historyDoc.insertString(historyDoc.getLength(), "\n", attributeSet);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }

            styleTextInputField.setText("");

        });

        panel.setLayout(new BorderLayout(20,20));
        panel.add(styleTextInputField, BorderLayout.NORTH);
        //scrollPane.add(textPane);
        panel.add(historyTextPane, BorderLayout.CENTER);
        panel.add(new JPanel(), BorderLayout.WEST);
        panel.add(new JPanel(), BorderLayout.EAST);
        this.setMinimumSize(new Dimension(800, 600));
        this.add(panel);
        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.pack();
    }
}
