package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Date;

public class TopologyMessage{
    private String id, ap_mac, sta_mac, uplink_bssid, ap_ip, sta_ip, rssi, mesh_level;
    private int no_stas;
    Station[] stations;

    private static Logger logger = LogManager.getLogger(TopologyMessage.class.getSimpleName());

    public String getId() {
        return id;
    }
    public String getAp_mac() {
        return ap_mac;
    }
    public String getSta_mac() {
        return sta_mac;
    }
    public String getUplink_bssid() {
        return uplink_bssid;
    }
    public String getAp_ip() {
        return ap_ip;
    }
    public String getSta_ip() {
        return sta_ip;
    }
    public String getRssi() {
        return rssi;
    }
    public String getMesh_level() {
        return mesh_level;
    }
    public int getNo_stas() {
        return no_stas;
    }

    public Station[] getStations() {
        return stations;
    }


    class Station {
        private String mac;

        public Station(JSONObject station) {
            this.mac = (String) station.get("mac");
        }

        public String getMac() {
            return mac;
        }

        @Override
        public String toString() {
            return "Station{" +
                    "mac='" + mac + '\'' +
                    '}';
        }
    }

    public TopologyMessage(JSONObject json) {
        //logger.debug("Instantiate new Message");
        /*
         Message is
          JSONObj
            |- "nodeinfo"   : JSONObj
                                    |- 9*key:value pairs
            |- "stas        :   JSONArray
                                    |- x*JSONObj
                                            |- mac  :   value
                                            |- ip   :   value

        */
        // Handle nodeinfo-JSONObj
        JSONObject nodeInfo = (JSONObject) json.get("nodeinfo");

        // Important Fields for Tree Building
        id = (String) nodeInfo.get("id");
        ap_mac = (String) nodeInfo.get("ap_mac");
        sta_mac = (String) nodeInfo.get("sta_mac");
        uplink_bssid = (String) nodeInfo.get("uplink_bssid");
        no_stas = Integer.parseInt((String) nodeInfo.get("no_stas"));
        if (no_stas > 0) {
            JSONArray stas = (JSONArray) json.get("stas");
            stations = new Station[no_stas];
            for (int i = 0; i<no_stas; i++) {
                stations[i]= new Station((JSONObject) stas.get(i));
            }
        }

        // Unimportant Fields, can be left away
        ap_ip = catchUnassignedField(nodeInfo.get("ap_ip"));
        sta_ip = catchUnassignedField( nodeInfo.get("sta_ip"));
        rssi = catchUnassignedField( nodeInfo.get("rssi"));
        mesh_level = catchUnassignedField( nodeInfo.get("mesh_level"));
    }

    private String catchUnassignedField(Object o){
        if (o == null){
            return "Not Defined";
        } else
            return (String) o;
    }


    public boolean isTopologyUnchanged(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopologyMessage that = (TopologyMessage) o;

        if (no_stas != that.no_stas){
            return false;
        }
        if (!uplink_bssid.equals(that.uplink_bssid)){
            return false;
        }
        if (!mesh_level.equals(that.mesh_level)){
            return false;
        }

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return true;//Arrays.equals(stations, that.stations);
    }

    @Override
    public int hashCode() {
        int result = uplink_bssid.hashCode();
        result = 31 * result + mesh_level.hashCode();
        result = 31 * result + no_stas;
        result = 31 * result + Arrays.hashCode(stations);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyMessage{" +
                "id='" + id + '\'' +
                ", ap_mac='" + ap_mac + '\'' +
                ", sta_mac='" + sta_mac + '\'' +
                ", uplink_bssid='" + uplink_bssid + '\'' +
                ", ap_ip='" + ap_ip + '\'' +
                ", sta_ip='" + sta_ip + '\'' +
                ", rssi='" + rssi + '\'' +
                ", mesh_level='" + mesh_level + '\'' +
                ", no_stas=" + no_stas +
                ", stations=" + Arrays.toString(stations) +
                '}';
    }
}
