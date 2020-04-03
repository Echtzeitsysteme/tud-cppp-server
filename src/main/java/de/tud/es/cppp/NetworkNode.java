package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class NetworkNode {


    private Logger logger = LogManager.getLogger(NetworkNode.class.getSimpleName());

    private NetworkNode uplinkNode;
    private HashSet<NetworkNode> staNodes = new HashSet<>();

    private String id, apIp, apMac, uplink_bssid, staMac, staIp, meshLevel, rssi, sensorValue = "";
    private int noStas;

    private TopologyMessage.Station[] stations;
    private JSONObject jsonObject;

    public Date getTimestamp() {
        return timestamp;
    }

    private Date timestamp;

    public NetworkNode() {
        this.id = String.valueOf(hashCode());
    }


    public NetworkNode(String id) {
        this.id = id;
    }

    public NetworkNode(TopologyMessage msg) {
        timestamp = new Date();
        getParamsFromMessage(msg);
    }

    public void setStationNodes(HashSet<NetworkNode> stations) {
        this.staNodes = stations;
    }

    public void setSensorValue(String sensorValue) {
        this.sensorValue = sensorValue;
    }

    private String checkIfSet(String s) {
        return (s == null) ? "unknown" : s;
    }

    public void addStationNode(NetworkNode station) {
        if (!staNodes.contains(station))
            this.staNodes.add(station);
    }

    public String getId() {
        return (id == null) ? "NoID_" + Integer.toHexString(this.hashCode()) : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NetworkNode getUplinkNode() {
        return uplinkNode;
    }

    public void setUplinkNode(NetworkNode uplinkNode) {
        this.uplinkNode = uplinkNode;
    }

    public HashSet<NetworkNode> getStaNodes() {
        return staNodes;
    }

    public String getMeshLevel() {
        return meshLevel;
    }

    public String getApIp() {
        return apIp;
    }

    public String getApMac() {
        return checkIfSet(apMac);
    }

    public void setApMac(String apMac) {
        this.apMac = apMac;
    }

    public String getUplink_bssid() {
        return checkIfSet(uplink_bssid);
    }

    public int getNoStas() {
        return noStas;
    }

    public String getRssi() {
        return rssi;
    }

    public String getStaMac() {
        return checkIfSet(staMac);
    }

    public void setStaMac(String staMac) {
        this.staMac = staMac;
    }

    public String getStaIp() {
        return staIp;
    }

    public TopologyMessage.Station[] getStations() {
        return stations;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void handleMessage(TopologyMessage newMsg) {
        // reset all info
        uplinkNode = null;

        staNodes.clear();

        getParamsFromMessage(newMsg);
    }

    private void getParamsFromMessage(TopologyMessage msg) {
        this.id = msg.getId();
        this.apMac = msg.getAp_mac();
        this.apIp = msg.getAp_ip();

        this.staMac = msg.getSta_mac();
        this.staIp = msg.getSta_ip();
        this.uplink_bssid = msg.getUplink_bssid();

        this.meshLevel = msg.getMesh_level();
        this.noStas = msg.getNo_stas();
        this.rssi = msg.getRssi();
        this.stations = msg.getStations();

        this.timestamp = msg.getTimestamp();
        this.jsonObject = msg.getJsonObject();
    }

    public String printNeighbors() {
        return "NetworkNode{" +
                "id='" + id + '\'' +
                ", uplinkNode=" + uplinkNode +
                ", staNodes=" + staNodes +
                '}';
    }

    @Override
    public String toString() {
        return id;
    }

    static String[] fieldTexts = {"UplinkNode:",
            "Uplinks MAC:",
            "RSSI to Uplink:",
            "MAC:",
            "IP:",
            "Node ID:",
            "meshLevel:",
            "AP MAC:",
            "AP IP:",
            "#Stations:",
            "stations:",
            "Info Time:",
            "Sensor Value:"
    };

    public HashMap<String, String> getInfoMap() {
        HashMap<String, String> NameValueMap = new HashMap<>();
        String[] values;
        if (id.equals("Router")) {
            values = new String[]{"n/a",
                    "n/a",
                    "n/a",
                    "n/a",
                    "n/a",
                    id,
                    "n/a",
                    apMac,
                    "n/a",
                    "n/a",
                    "n/a",
                    "n/a",
                    "n/a"};
        } else {

            String uplinkNodeId = uplinkNode == null ? "null" : uplinkNode.getId();
            values = new String[]{uplinkNodeId,
                    uplink_bssid,
                    String.valueOf(rssi),
                    staMac,
                    staIp,
                    this.id,
                    String.valueOf(meshLevel),
                    apMac,
                    apIp,
                    String.valueOf(noStas),
                    stationsAsString(),
                    sdf.format(timestamp),
                    sensorValue};
        }
        for (int i = 0; i < values.length; i++) {
            NameValueMap.put(fieldTexts[i], values[i]);
        }
        return NameValueMap;

    }

    private String stationsAsString() {
        StringBuilder sb = new StringBuilder();
        for (NetworkNode staNode : staNodes) {
            sb.append(staNode.getId());
            sb.append('\n');
        }
        return sb.toString();
    }


    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public String getNodeInfo() {
        return staIp + " | " + staMac + '\n' +
                "---\n" +
                id + '\n' +
                "---\n" +
                apIp + " | " + apMac + '\n';
    }

    public void removeStationNode(NetworkNode node) {
        logger.debug("STAS BEFORE: {}", staNodes);
        staNodes.removeIf(nodeInSet -> nodeInSet.getId().equals(node.getId()));
        logger.debug("STAS AFTER: {}", staNodes);
    }

    public void decreaseStations() {
        noStas--;
    }
}
