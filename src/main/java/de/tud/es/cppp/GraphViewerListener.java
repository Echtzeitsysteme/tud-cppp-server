package de.tud.es.cppp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.*;

public class GraphViewerListener implements ViewerListener,Runnable{

    boolean loop = true;
    Handler handler;
    Graph graph;
    ViewerPipe viewerPipe;
    private static Logger logger = LogManager.getLogger(GraphViewerListener.class.getSimpleName());

    GraphViewerListener(ViewerPipe viewerPipe, Handler handler){
        logger.debug("Create viewListener");
        this.viewerPipe = viewerPipe;
        this.handler = handler;
        this.graph = handler.getGraph();
    }

    @Override
    public void viewClosed(String s) {
        loop=false;
    }

    @Override
    public void buttonPushed(String nodeId) {
        logger.debug("Button pushed on node " + nodeId);
        graph.getEachNode().forEach(n -> n.removeAttribute("ui.selected"));
        graph.getNode(nodeId).setAttribute("ui.selected");
        NetworkNode node = handler.getNodeWhere(nodeId, NetworkNode::getId);
        NodeInfoFrame.setNodeInfo(node);
        if (MainFrame.getInstance().showDetailsCheckbox.isSelected()){
            NodeInfoFrame.getInstance().setVisible(true);
        }
    }

    @Override
    public void buttonReleased(String nodeId) {
        logger.debug("Button rlsd on node " + nodeId);
        //Camera camera = viewer.getDefaultView().getCamera();
        //viewer.getDefaultView().getCamera().resetView();
    }

    @Override
    public void run() {
        while(loop) {
            viewerPipe.pump();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}