package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

public class Main {
    private static Logger logger;

    private static final String TOPIC = "/ManualTesting/#";
    //private static final String TOPIC = "/Wifii/#";

    // Default Topic format is: "/Wifi/<ESP_ID>/subtopic", where subtopic is "Wsn" or "Topology"
    //private static final String TOPIC = "WiFi/#";
    private static final String BROKER = "tcp://192.168.178.201:1883";
    //private static final String BROKER = "tcp://127.0.0.1:1883";
    private static final String CLIENT_ID = "DesktopApp";


    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        logger = LogManager.getLogger(Main.class.getSimpleName());

        Handler.getInstance();
        StyleConsoleFrame.getInstance();
        MainFrame.getInstance();
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            logger.info("Connecting to broker: " + BROKER);
            client.connect(connOpts);
            logger.info("Connected");
            client.setCallback(new MqttCallbackImplementation());
            client.subscribe(TOPIC);

            logger.info("Program Init Done");

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private static void publish(MqttClient client) {
        String payload1 = "{" +
                "  \"nodeinfo\" : {" +
                "    \"id\" : \"ESP_cb01fe\"," +
                "    \"ap_mac\" : \"24:24:01:28:0c:ac\"," +
                "    \"sta_mac\" : \"84:f3:eb:cb:01:fe\"," +
                "    \"uplink_bssid\" : \"38:10:d5:d2:72:b1\"," +
                "    \"ap_ip\" : \"10.24.1.1\"," +
                "    \"sta_ip\" : \"192.168.178.154\"," +
                "    \"rssi\" : \"-49\"," +
                "    \"mesh_level\" : \"1\"," +
                "    \"no_stas\" : \"1\"" +
                "  }," +
                "  \"stas\" : [ {" +
                "    \"mac\" : \"2c:f4:32:17:38:2e\"," +
                "    \"ip\" : \"10.24.1.2\"" +
                "  } ]" +
                "}";
        MqttMessage msg1 = new MqttMessage(payload1.getBytes());
        String payload2 = "{" +
                "  \"nodeinfo\" : {" +
                "    \"id\" : \"ESP_173059\"," +
                "    \"ap_mac\" : \"24:24:01:8f:1f:fe\"," +
                "    \"sta_mac\" : \"2c:f4:32:17:30:59\"," +
                "    \"uplink_bssid\" : \"38:10:d5:d2:72:b1\"," +
                "    \"ap_ip\" : \"10.24.1.1\"," +
                "    \"sta_ip\" : \"192.168.178.125\"," +
                "    \"rssi\" : \"-78\"," +
                "    \"mesh_level\" : \"1\"," +
                "    \"no_stas\" : \"0\"" +
                "  }," +
                "  \"stas\" : [ ]" +
                "}";
        MqttMessage msg2 = new MqttMessage(payload2.getBytes());
        String payload3 = "{" +
                "  \"nodeinfo\" : {" +
                "    \"id\" : \"ESP_17382e\"," +
                "    \"ap_mac\" : \"24:24:02:01:df:eb\"," +
                "    \"sta_mac\" : \"2c:f4:32:17:38:2e\"," +
                "    \"uplink_bssid\" : \"24:24:01:28:0c:ac\"," +
                "    \"ap_ip\" : \"10.24.2.1\"," +
                "    \"sta_ip\" : \"10.24.1.2\"," +
                "    \"rssi\" : \"-80\"," +
                "    \"mesh_level\" : \"2\"," +
                "    \"no_stas\" : \"0\"" +
                "  }," +
                "  \"stas\" : [ ]" +
                "}";
        MqttMessage msg3 = new MqttMessage(payload3.getBytes());
        try {
            client.publish("/WiFii/1", msg1);
            client.publish("/WiFii/1", msg2);
            client.publish("/WiFii/1", msg3);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}