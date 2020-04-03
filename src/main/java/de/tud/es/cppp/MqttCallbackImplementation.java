package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.graphstream.ui.spriteManager.Sprite;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Arrays;

public class MqttCallbackImplementation implements MqttCallback {

    private Logger logger = LogManager.getLogger(MqttCallbackImplementation.class.getSimpleName());
    private JSONParser parser = new JSONParser();
    private Handler handler = Handler.getInstance();

    public MqttCallbackImplementation() {
        logger.debug("Instantiate Callback");
    }

    @Override
    public void connectionLost(Throwable throwable) {
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage){
        logger.debug("MQTT Message arrived on topic {}", topic);
        logger.debug("Message: {}", new String(mqttMessage.getPayload()));
        try {
            String[] parts = topic.split("/");
            String topicEnd = parts[3];
            switch (topicEnd) {
                case "Topology":
                    String jsonString = new String(mqttMessage.getPayload());
                    handleTopologyMessage(jsonString);
                    break;
                case "wsn":
                    String nodeId = parts[2];
                    handleWsnMessage(mqttMessage, nodeId);
                    break;
                case "Composed":
                    handleComposedMessage(mqttMessage);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleComposedMessage(MqttMessage msg){
        byte[] payload = msg.getPayload();

        int msgStartIndex = 0, paraOpenCount = 0;

        for (int i = 0; i < payload.length; i++) {
            // Increase openCount when para is found
            if (payload[i] == '{'){
                // if open Count is still 0, this is start of msg
                if(paraOpenCount == 0){
                    msgStartIndex = i;
                }
                paraOpenCount++;

            }else if(payload[i] == '}'){
                // decrease count
                paraOpenCount--;

                if(paraOpenCount == 0){
                    // is end of message
                    byte[] messageArray = Arrays.copyOfRange(payload, msgStartIndex, i+1);
                    String msgString = new String(messageArray);
                    handleTopologyMessage(msgString);
                }
            }
        }

    }

    private void handleWsnMessage(MqttMessage mqttMessage, String nodeId) {
        String value = new String(mqttMessage.getPayload());
        Sprite s = handler.getSpriteManager().getSprite(nodeId);
        if (s != null){
            s.setAttribute("ui.label", "Sensor: " + value);
        }else{
            logger.warn("Node is not know yet");
        }
    }

    private void handleTopologyMessage(String mqttMessagePayload){
        logger.debug("BLA{}", mqttMessagePayload);
        try {
            JSONObject json = (JSONObject) parser.parse(mqttMessagePayload);
            TopologyMessage tm = new TopologyMessage(json);
            handler.handleincomingTopologyMessage(tm);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }
}
