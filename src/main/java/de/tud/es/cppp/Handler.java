package de.tud.es.cppp;

import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.abego.treelayout.util.FixedNodeExtentProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public class Handler {

    // Either Hardcode Router-Mac or Identify Router by MAC-Prefix
    private static final boolean HARDCODE_ROUTER_MAC = false;
    private static final String ROUTER_MAC = "00";

    private static final String MAC_PREFIX = "24:24";
    private static Handler instance;
    private HashMap<String, NetworkNode> nodesById = new HashMap<>();
    private HashMap<String, NetworkNode> unknownApMacs = new HashMap<>();
    private HashMap<String, NetworkNode> unknownStaMacs = new HashMap<>();
    private Logger logger = LogManager.getLogger(Handler.class.getSimpleName());
    private MainFrame mainFrame = MainFrame.getInstance();
    private NodesTableFrame nodesTableFrame = NodesTableFrame.getInstance();
    private NetworkNode networkRoot = null;
    private Graph graph = new SingleGraph("NetworkGraph");

    private DefaultConfiguration<NetworkNode> config = new DefaultConfiguration<>(1, 1);
    private FixedNodeExtentProvider<NetworkNode> extentProvider = new FixedNodeExtentProvider<>(1, 1);
    private boolean routerIsFound = false;
    private SpriteManager spriteManager = new SpriteManager(graph);

    private Handler() {
        logger.debug("Instantiate Handler");
        if (HARDCODE_ROUTER_MAC) {
            networkRoot = new NetworkNode("Router");
            networkRoot.setApMac(ROUTER_MAC);
            unknownApMacs.put(ROUTER_MAC, networkRoot);
            routerIsFound = true;
        }
        initGraph();
        visualizeGraph();
    }

    public static synchronized Handler getInstance() {
        if (Handler.instance == null) {
            Handler.instance = new Handler();
        }
        return Handler.instance;
    }

    public synchronized void handleincomingTopologyMessage(TopologyMessage msg) {
        boolean topologyChanged = true;
        String nodeId = msg.getId();
        String apMac = msg.getAp_mac();
        String staMac = msg.getSta_mac();
        NetworkNode node;
        logger.debug("Handling message from Node {}", nodeId);

        // Check if Node is already known
        if (nodesById.containsKey(nodeId)) {

            node = nodesById.get(nodeId);
            topologyChanged = node.handleMessage(msg);
            if (topologyChanged) {
                // Remove node as Child of Uplink
                NetworkNode oldUplink = node.getUplinkNode();
                if (oldUplink != null) {
                    oldUplink.removeStationNode(node);

                }

                // Remove Node as Uplink of Children
                for (NetworkNode staNode : node.getStaNodes()) {
                    staNode.setUplinkNode(null);
                    nodesById.remove(staNode.getId());
                    unknownApMacs.put(staNode.getApMac(), staNode);
                }
            }

            logger.debug("Node is known already, Topology has {}changed", topologyChanged ? "" : "not ");
        } else if (unknownStaMacs.containsKey(staMac)) {
            logger.debug("Node is known as Station already");
            node = unknownStaMacs.get(staMac);
            node.handleMessage(msg);
            nodesById.put(nodeId, node);
            unknownStaMacs.remove(staMac);
            // Node is known by its staMac and its apMac, work with one, and remove other representation
            unknownApMacs.remove(apMac);
        } else if (unknownApMacs.containsKey(apMac)) {
            logger.debug("Node is known as AP already");
            node = unknownApMacs.get(apMac);
            node.handleMessage(msg);
            nodesById.put(nodeId, node);
            unknownApMacs.remove(apMac);
        } else {
            logger.debug("Node is unknown yet");
            node = new NetworkNode(msg);
            nodesById.put(nodeId, node);
        }



        if (topologyChanged) {
            createLinksForNode(node);
            visualizeGraph();
        } else {
            // only update rssi
            for (Edge edge : graph.getEdgeSet()) {
                if (edge.getNode1().getId().equals(nodeId)) {
                    edge.setAttribute("ui.label", node.getRssi() + " dBm");
                }
            }

        }
        //nodesTableFrame.update(nodesById.values());
        mainFrame.pack();
        if (node.getUplinkNode() == null){
            logger.error("SOMETHING WENT WRONG, UPLINK IS NULL");
        }

        logger.debug("Handling Done\n");
    }

    protected void visualizeGraph() {
        if (routerIsFound && networkRoot != null) {
            logger.debug("Create visualization");


            graph.getNodeSet().clear();
            graph.getEdgeSet().clear();

            addNodesToGraph(networkRoot, getNodePositions());
            addEdgesToGraph(networkRoot);
        }

        findUnconnectedNodes();
    }

    private Map<NetworkNode, Rectangle2D.Double> getNodePositions() {
        // abego TreeLayout, define new Tree, create Layout, DO NEW AT EVERY MESSAGE
        NetworkTree tree = new NetworkTree(networkRoot);
        TreeLayout<NetworkNode> treeLayout = new TreeLayout<>(tree, extentProvider, config);
        return treeLayout.getNodeBounds();
    }

    private void findUnconnectedNodes() {
        HashMap<String, NetworkNode> allNodesMap = getAllNodesMap();
        Iterator<Node> it = graph.getNodeIterator();
        while (it.hasNext()) {
            Node itNode = it.next();
            String id = itNode.getId();
            allNodesMap.remove(id);
        }
        mainFrame.updateUnconnectedNodes(allNodesMap.keySet());
        logger.debug("Unconnected Nodes: {}", printCollection(allNodesMap.keySet()));
    }

    private String printCollection(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        for (Object o : collection) {
            sb.append(o.toString()).append(",");
        }
        return sb.toString();
    }

    private void initGraph() {
        // setup graph
        graph.setStrict(false);
        graph.setAttribute("ui.stylesheet", "url('file:" + new File("graphStyle.css").getAbsolutePath() + "')");
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        // setup view for mainFrame
        Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        mainFrame.add(viewer.addDefaultView(false), BorderLayout.CENTER);
        mainFrame.pack();

        // Setup Listener
        ViewerPipe viewerPipe = viewer.newViewerPipe();
        GraphViewerListener graphViewerListener = new GraphViewerListener(viewerPipe, this);
        Thread t = new Thread(graphViewerListener);
        viewerPipe.addViewerListener(graphViewerListener);
        viewerPipe.addSink(graph);

        // start listener, show frame
        t.start();
        mainFrame.setVisible(true);
    }


    private void addNodesToGraph(NetworkNode node, Map<NetworkNode, Rectangle2D.Double> nodeBounds) {
        String id = node.getId();
        Rectangle2D.Double bounds = nodeBounds.get(node);
        Node newGraphNode = graph.addNode(id);
        double x = bounds.x;
        double y = -bounds.y;
        logger.debug("Add node {} to Graphstream at ({},{})", id, x, y);
        newGraphNode.setAttribute("ui.label", id);
        newGraphNode.setAttribute("xyz", x, y, 0);

        // recursive call to all stas
        for (NetworkNode staNode : node.getStaNodes()) {
            addNodesToGraph(staNode, nodeBounds);
        }
    }

    private void addEdgesToGraph(NetworkNode node) {
        for (NetworkNode staNode : node.getStaNodes()) {
            logger.debug("Add edge from {} to {} to Graphstream", node.getId(), staNode.getId());
            Edge edge = graph.addEdge(node.getId() + "->" + staNode.getId(), node.getId(), staNode.getId());
            edge.setAttribute("ui.label", staNode.getRssi() + " dBm");
            addEdgesToGraph(staNode);
        }
    }

    protected NetworkNode getNodeWhere(String value, Function<NetworkNode, String> func) {
        HashSet<NetworkNode> allNodes = new HashSet<>();
        allNodes.addAll(nodesById.values());
        allNodes.addAll(unknownApMacs.values());
        allNodes.addAll(unknownStaMacs.values());
        for (NetworkNode other : allNodes) {
            if (func.apply(other).equals(value)) {
                return other;
            }
        }
        return null;
    }

    private HashMap<String, NetworkNode> getAllNodesMap() {
        HashMap<String, NetworkNode> allNodesMap = new HashMap<>(nodesById);
        unknownApMacs.values().forEach(node -> allNodesMap.put(node.getId(), node));
        unknownStaMacs.values().forEach(node -> allNodesMap.put(node.getId(), node));


        return allNodesMap;
    }

    private void createLinksForNode(NetworkNode node) {
        logger.debug("Try to find Uplink");

        // Handle Uplink
        String uplinkBSSID = node.getUplink_bssid();
        NetworkNode myUplinkNode = getNodeWhere(uplinkBSSID, NetworkNode::getApMac);

        // Uplink is either router without NetworkNode yet, or in some set, or unknown Node.
        // All Nodes except Router have fixed MAC-Prefix
        logger.info("Router {}found yet! Uplink Mac: {}; Mac-Prefix: {}", routerIsFound ? "" : "not ", uplinkBSSID, MAC_PREFIX);
        if (!routerIsFound && !uplinkBSSID.substring(0, 5).equals(MAC_PREFIX)) {
            logger.debug("My Uplink is Router");
            networkRoot = new NetworkNode("Router");
            nodesById.put(networkRoot.getId(), networkRoot);
            routerIsFound = true;
            myUplinkNode = networkRoot;
            myUplinkNode.setApMac(uplinkBSSID);
        } else if (myUplinkNode == null) {
            logger.debug("My Uplink is yet unknown, create new Node");
            // Uplink not found, create new Node as Uplink
            myUplinkNode = new NetworkNode();
            myUplinkNode.setApMac(uplinkBSSID);
            unknownApMacs.put(uplinkBSSID, myUplinkNode);
        } else {
            logger.debug("Uplink was known already");
        }
        // Connect both Nodes
        node.setUplinkNode(myUplinkNode);
        myUplinkNode.addStationNode(node);


        // Handle Stations
        logger.debug("Create Stations Link");
        TopologyMessage.Station[] stations = node.getStations();
        if (stations != null) {
            for (TopologyMessage.Station station : stations) {
                String staMac = station.getMac();
                NetworkNode thisStationNode = getNodeWhere(staMac, NetworkNode::getStaMac);
                if (thisStationNode == null) {
                    // Sta node not known yet
                    thisStationNode = new NetworkNode();
                    thisStationNode.setStaMac(staMac);
                    unknownStaMacs.put(staMac, thisStationNode);
                }
                node.addStationNode(thisStationNode);
                thisStationNode.setUplinkNode(node);
            }
        }

    }


    public Graph getGraph() {
        return graph;
    }

    public Component getNodesTableFrame() {
        return nodesTableFrame;
    }

    public SpriteManager getSpriteManager() {
        return spriteManager;
    }

    public void nodeTimedOut(NetworkNode node) {
        nodesById.remove(node.getId());
        unknownApMacs.remove(node.getApMac());
        unknownStaMacs.remove(node.getStaMac());
        NetworkNode uplinkNode = node.getUplinkNode();
        logger.debug(node.toString());
        if (uplinkNode != null) {
            uplinkNode.removeStationNode(node);
        }
        spriteManager.removeSprite(node.getId());
        visualizeGraph();
    }

    public void handleWsnMessage(MqttMessage mqttMessage, String nodeId) {
        if (graph.getNode(nodeId) != null) {
            String value = new String(mqttMessage.getPayload());
            Sprite s = spriteManager.getSprite(nodeId);

            if (s == null) {
                s = spriteManager.addSprite(nodeId);
            }

            s.attachToNode(nodeId);
            s.setPosition(0.7, 0, 0);
            s.setAttribute("ui.label", "Sensor: " + value);
        }
            //logger.warn("Node [{}] is not know yet", nodeId);


    }
}
