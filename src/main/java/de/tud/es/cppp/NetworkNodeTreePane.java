/*
 * [The "BSD license"]
 * Copyright (c) 2011, abego Software GmbH, Germany (http://www.abego.org)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the abego Software GmbH nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.tud.es.cppp;

import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class NetworkNodeTreePane extends JComponent {
    private final TreeLayout<NetworkNode> treeLayout;
    private Logger logger;

    private TreeForTreeLayout<NetworkNode> getTree() {
        return treeLayout.getTree();
    }

    private Iterable<NetworkNode> getChildren(NetworkNode parent) {
        return getTree().getChildren(parent);
    }

    private Rectangle2D.Double getBoundsOfNode(NetworkNode node) {
        return treeLayout.getNodeBounds().get(node);
    }

    /**
     * Specifies the tree to be displayed by passing in a {@link TreeLayout} for
     * that tree.
     *
     * @param treeLayout the {@link TreeLayout} to be displayed
     */
    public NetworkNodeTreePane(TreeLayout<NetworkNode> treeLayout) {
        this.treeLayout = treeLayout;
        logger = LogManager.getLogger();
        Dimension size = treeLayout.getBounds().getBounds().getSize();
        setPreferredSize(size);
    }

    // -------------------------------------------------------------------
    // painting

    private final static int ARC_SIZE = 20;
    private final static Color BOX_COLOR = Color.orange;
    private final static Color BORDER_COLOR = Color.darkGray;
    private final static Color TEXT_COLOR = Color.black;
    // TODO: visualize rssi at edge
    private void paintEdges(Graphics g, NetworkNode parent) {
        if (!getTree().isLeaf(parent)) {
            Rectangle2D.Double b1 = getBoundsOfNode(parent);
            double x1 = b1.getCenterX();
            double y1 = b1.getCenterY();
            for (NetworkNode child : getChildren(parent)) {
                Rectangle2D.Double b2 = getBoundsOfNode(child);
                g.drawLine((int) x1,
                        (int) y1,
                        (int) b2.getCenterX(),
                        (int) b2.getCenterY());

                paintEdges(g, child);
            }
        }
    }

    private void paintBox(Graphics g, NetworkNode NetworkNode) {
        String[] lines = NetworkNode.getNodeInfo().split("\n");
        FontMetrics m = getFontMetrics(getFont());



        // draw the box in the background
        g.setColor(BOX_COLOR);
        Rectangle2D.Double box = getBoundsOfNode(NetworkNode);
        g.fillRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
                (int) box.height-1 , ARC_SIZE, ARC_SIZE);
        g.setColor(BORDER_COLOR);
        g.drawRoundRect((int) box.x, (int) box.y, (int) box.width - 1,
                (int) box.height-1, ARC_SIZE, ARC_SIZE);

        // draw the text on top of the box (possibly multiple lines)
        g.setColor(TEXT_COLOR);


        int textHeight = lines.length * m.getHeight();

        int x = (int) box.x + ARC_SIZE / 2;
        int y = (int) box.y + m.getAscent() + m.getLeading() + 1
                //+ (int)(box.height - (textHeight/2))
        ;
        for (String line : lines) {

            g.drawString(line, x, y);
            y += m.getHeight();
        }

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        paintEdges(g, getTree().getRoot());

        // paint the boxes
        for (NetworkNode NetworkNode : treeLayout.getNodeBounds().keySet()) {
            paintBox(g, NetworkNode);
        }
    }
}