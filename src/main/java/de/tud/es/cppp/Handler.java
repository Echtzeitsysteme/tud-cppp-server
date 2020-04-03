package de.tud.es.cppp;

import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.abego.treelayout.util.FixedNodeExtentProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public class Handler {

    // TODO: Maybe get MAC from Wireless Interface's default gateway
    private static final String ROUTER_MAC = "00";

    private HashMap<String, NetworkNode> nodesById = new HashMap<>();
    private HashMap<String, NetworkNode> unknownApMacs = new HashMap<>();
    private HashMap<String, NetworkNode> unknownStaMacs = new HashMap<>();
    private HashSet<NetworkNode> allNodes = new HashSet<>();
    private Logger logger = LogManager.getLogger(Handler.class.getSimpleName());

    private MainFrame mainFrame = MainFrame.getInstance();
    private NodesTableFrame nodesTableFrame = NodesTableFrame.getInstance();


    private NetworkNode networkRoot = null;


    private Graph graph = new SingleGraph("NetworkGraph");
    private SpriteManager spriteManager = new SpriteManager(graph);

    private DefaultConfiguration<NetworkNode> config = new DefaultConfiguration<>(40, 40);
    private FixedNodeExtentProvider<NetworkNode> extentProvider = new FixedNodeExtentProvider<>(200, 100);

    private static Handler instance;
    public static synchronized Handler getInstance() {
        if (Handler.instance == null) {
            Handler.instance = new Handler();
        }
        return Handler.instance;
    }

    private Handler() {
        logger.debug("Instantiate Handler");
        initGraph();
    }

    // TODO: Check for disconnecting nodes

    public void handleincomingTopologyMessage(TopologyMessage msg) {
        String nodeId = msg.getId();
        String apMac = msg.getAp_mac();
        String staMac = msg.getSta_mac();
        NetworkNode node;
        logger.debug("Handling message from Node {}", nodeId);

        // Check if Node is already known
        if (nodesById.containsKey(nodeId)) {
            logger.debug("Node is known already");
            node = nodesById.get(nodeId);
            // Remove node as Child of Uplink
            NetworkNode oldUplink = node.getUplinkNode();
            if (oldUplink!=null){
                oldUplink.removeStationNode(node);
                oldUplink.decreaseStations();
            }

            // Remove Node as Uplink of Children
            for (NetworkNode staNode : node.getStaNodes()) {
                staNode.setUplinkNode(null);
                nodesById.remove(staNode.getId());
                unknownApMacs.put(staNode.getApMac(), staNode);
            }
            node.handleMessage(msg);
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

        updateAllNodesSet();
        createLinksForNode(node);

        nodesTableFrame.update(nodesById.values());



        if (networkRoot != null) {
            logger.debug("Create visualization");



            graph.getNodeSet().clear();
            graph.getEdgeSet().clear();

            addNodesToGraph(networkRoot, getNodePositions());
            addEdgesToGraph(networkRoot);

            findUnconnectedNodes();
        }

        mainFrame.pack();
        logger.debug("Handling Done");
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
        while(it.hasNext()){
            Node itNode = it.next();
            String id = itNode.getId();

            allNodesMap.remove(id);

        }
        mainFrame.updateUnconnectedNodes(allNodesMap.keySet());
    }

    private String printCollection(Collection<?> collection){
        StringBuilder sb = new StringBuilder("Set: ");
        for (Object o : collection) {
            sb.append(o.toString()).append(",");
        }
        return sb.toString();
    };

    private void initGraph(){
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


    private void addNodesToGraph(NetworkNode node, Map<NetworkNode, Rectangle2D.Double> nodeBounds){
        String id = node.getId();
        //logger.debug("Add node {} to Graphstream", id);
        Rectangle2D.Double bounds = nodeBounds.get(node);
        Node newGraphNode = graph.addNode(id);
        double x = bounds.x;
        double y = -bounds.y;
        logger.debug("Add node {} to Graphstream at ({},{})", id, x, y);
        newGraphNode.setAttribute("ui.label", id);
        newGraphNode.setAttribute("xyz", x, y, 0);
        /*
        Sprite s = spriteManager.addSprite(id);
        s.attachToNode(id);
        // TODO: Optimize Sprite Position
        s.setPosition(50,0,0);
        s.addAttribute("ui.label", "Sensor: n/a");
        */
        // recursive call to all stas
        for (NetworkNode staNode : node.getStaNodes()) {
            addNodesToGraph(staNode, nodeBounds);
        }


    };

    private void addEdgesToGraph(NetworkNode node) {

        for (NetworkNode staNode : node.getStaNodes()) {
            logger.debug("Add edge from {} to {} to Graphstream", node.getId(), staNode.getId());
            Edge edge = graph.addEdge(node.getId() + "->" + staNode.getId(), node.getId(), staNode.getId());
            edge.setAttribute("ui.label", staNode.getRssi() + " dBm");
            addEdgesToGraph(staNode);
        }
    }

    protected NetworkNode getNodeWhere(String value, Function<NetworkNode, String> func) {
        for (NetworkNode other : allNodes) {
            if (func.apply(other).equals(value)) {
                return other;
            }
        }
        return null;
    }

    private void updateAllNodesSet() {
        allNodes.clear();

        allNodes.addAll(nodesById.values());
        allNodes.addAll(unknownApMacs.values());
        allNodes.addAll(unknownStaMacs.values());

        logger.debug("Merged all Nodes into {}", printCollection(allNodes));
    }

    private HashMap<String, NetworkNode> getAllNodesMap() {
        HashMap<String, NetworkNode> allNodesMap = new HashMap<>(nodesById);
        unknownApMacs.values().forEach(node -> allNodesMap.put(node.getId(), node));
        unknownStaMacs.values().forEach(node -> allNodesMap.put(node.getId(), node));

        logger.debug("Merged all Nodes, All Nodes: ");
        logger.debug(printCollection(allNodesMap.values()));

        return allNodesMap;
    }

    private void createLinksForNode(NetworkNode node) {
        logger.debug("Create Uplink Link");

        // Handle Uplink
        String uplinkBSSID = node.getUplink_bssid();
        NetworkNode myUplinkNode = getNodeWhere(uplinkBSSID, NetworkNode::getApMac);

        if (myUplinkNode == null) {
            logger.debug("My Uplink is yet unknown, create new Node");
            // Uplink not found, create new Node as Uplink
            myUplinkNode = new NetworkNode();
            myUplinkNode.setApMac(uplinkBSSID);
            if (uplinkBSSID.equals(ROUTER_MAC)){
                logger.debug("My Uplink is Router");
                networkRoot = myUplinkNode;
                networkRoot.setId("Router");
            }
            unknownApMacs.put(uplinkBSSID, myUplinkNode);
        }else{
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

    public void setNodesTableFrame(NodesTableFrame nodesTableFrame) {
        this.nodesTableFrame = nodesTableFrame;
    }

    public  Graph getGraph() {
        return graph;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public  HashMap<String, NetworkNode> getNodesById() {
        return nodesById;
    }

    public Component getNodesTableFrame() {
        return nodesTableFrame;
    }

    public SpriteManager getSpriteManager() {
        return spriteManager;
    }
}
