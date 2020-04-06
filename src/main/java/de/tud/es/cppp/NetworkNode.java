package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class NetworkNode {


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
            "stations:"
    };
    private static int idNo = 1;
    private Logger logger = LogManager.getLogger(NetworkNode.class.getSimpleName());
    private NetworkNode uplinkNode;
    private HashSet<NetworkNode> staNodes = new HashSet<>();
    private String id;
    private String apIp;
    private String apMac;
    private String uplink_bssid;
    private String staMac;
    private String staIp;
    private String meshLevel;
    private String rssi;
    private int noStas;
    private TopologyMessage.Station[] stations;
    private TopologyMessage oldMsg;
    private Timer timer;

    public NetworkNode() {
        this.id = "UnknownNode_" + idNo++;
        timer = new Timer(this);
        timer.start();
    }

    public NetworkNode(String id) {
        this.id = id;
        timer = new Timer(this);
        //timer.start();
    }


    public NetworkNode(TopologyMessage newMsg) {
        this.id = newMsg.getId();
        timer = new Timer(this);
        timer.start();
        handleMessage(newMsg);
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

    public NetworkNode getUplinkNode() {
        return uplinkNode;
    }

    public void setUplinkNode(NetworkNode uplinkNode) {
        this.uplinkNode = uplinkNode;
    }

    public HashSet<NetworkNode> getStaNodes() {
        return staNodes;
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

    public String getRssi() {
        return rssi;
    }

    public String getStaMac() {
        return checkIfSet(staMac);
    }

    public void setStaMac(String staMac) {
        this.staMac = staMac;
    }

    public TopologyMessage.Station[] getStations() {
        return stations;
    }

    public boolean handleMessage(TopologyMessage newMsg) {
        boolean topologyChanged = true;
        if (oldMsg != null) {
             topologyChanged = topologyChanged(newMsg);
        }

        // reset all info
        if (topologyChanged) {
            uplinkNode = null;
            staNodes.clear();
        }

        getParamsFromMessage(newMsg);

        // update Node
        oldMsg = newMsg;
        timer.updateLastMsgReceived();

        return topologyChanged;
    }

    private boolean topologyChanged(TopologyMessage newMsg) {

        boolean topologyUnchanged = oldMsg.isTopologyUnchanged(newMsg);
        boolean topologyChanged = !topologyUnchanged;

        if (topologyChanged){
            logger.debug("OldMsg: {}", oldMsg.toString());
            logger.debug("NewMsg: {}", newMsg.toString());
        }
        return topologyChanged;
    }

    private void getParamsFromMessage(TopologyMessage msg) {
        this.id = msg.getId();
        this.apMac = msg.getAp_mac();
        this.apIp = msg.getAp_ip();
        this.staMac = msg.getSta_mac();
        this.staIp = msg.getSta_ip();
        this.rssi = msg.getRssi();

        this.noStas = msg.getNo_stas();
        this.uplink_bssid = msg.getUplink_bssid();
        this.meshLevel = msg.getMesh_level();
        this.stations = msg.getStations();
    }

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
                    stationsAsString()};
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

    public void removeStationNode(NetworkNode station) {
        logger.debug("This is Node {}. Remove {} from {}", id, station.getId(), staNodes);
        staNodes.removeIf(nodeInSet -> nodeInSet.getId().equals(station.getId()));
        noStas--;
        logger.debug("After Removal: {}", staNodes);

    }

    private class Timer extends Thread {
        long lastMsgReceived;
        NetworkNode myNode;
        private volatile boolean interrupted = false;

        public Timer(NetworkNode node) {
            logger.debug("Timer created for node [{}]", id);
            this.lastMsgReceived = System.currentTimeMillis();
            this.myNode = node;
        }

        public void updateLastMsgReceived() {
            this.lastMsgReceived = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (!Thread.interrupted() && !interrupted) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long now = System.currentTimeMillis();
                long timediff = now - lastMsgReceived;
                //logger.debug("Node [{}]; Timediff: [{}]", id, timediff);
                if (timediff > 10000) {
                    logger.warn("Node [{}] Timed Out", id);
                    Handler.getInstance().nodeTimedOut(myNode);
                    interrupted = true;
                }
            }

        }
    }

    @Override
    public String toString() {
        return "NetworkNode{" +
                //"logger=" + logger +
                ", uplinkNode=" + uplinkNode.getId() +
                //", staNodes=" + staNodes +
                ", id='" + id + '\'' +
                ", apIp='" + apIp + '\'' +
                ", apMac='" + apMac + '\'' +
                ", uplink_bssid='" + uplink_bssid + '\'' +
                ", staMac='" + staMac + '\'' +
                ", staIp='" + staIp + '\'' +
                ", meshLevel='" + meshLevel + '\'' +
                ", rssi='" + rssi + '\'' +
                ", noStas=" + noStas +
                //", stations=" + Arrays.toString(stations) +
                ", oldMsg=" + oldMsg +
                //", timer=" + timer +
                '}';
    }
}
