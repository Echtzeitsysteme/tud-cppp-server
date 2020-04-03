package de.tud.es.cppp;

import org.abego.treelayout.util.AbstractTreeForTreeLayout;
import java.util.LinkedList;
import java.util.List;

public class NetworkTree extends AbstractTreeForTreeLayout<NetworkNode> {

    public NetworkTree(NetworkNode root) {
        super(root);
    }

    @Override
    public NetworkNode getParent(NetworkNode node) {
        return node.getUplinkNode();
    }

    @Override
    public List<NetworkNode> getChildrenList(NetworkNode node) {
        return new LinkedList<>(node.getStaNodes());
    }
}
