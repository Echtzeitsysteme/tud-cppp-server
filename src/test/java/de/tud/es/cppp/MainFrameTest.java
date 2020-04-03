package de.tud.es.cppp;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class MainFrameTest {
    NetworkNode n1 = new NetworkNode("N1");
    static NetworkNode n2 = new NetworkNode("N2");
    static NetworkNode n3 = new NetworkNode("N3");
    static NetworkNode n4 = new NetworkNode("N4");
    MainFrame mf = MainFrame.getInstance();
    ArrayList<Integer> nodesInLayer=new ArrayList<Integer>();


    {
        n1.addStationNode(n2);
        n1.addStationNode(n3);
        n3.addStationNode(n4);
    }


}