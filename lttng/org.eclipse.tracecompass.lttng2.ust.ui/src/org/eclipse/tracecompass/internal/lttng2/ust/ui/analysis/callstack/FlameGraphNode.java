/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Class defining a node used to build an aggregation tree (Caller and depth
 * aggregation)
 *
 * @author Sonia Farrah
 *
 */
public class FlameGraphNode {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private Long address;
    private FlameGraphNode parent;
    private HashMap<Long, FlameGraphNode> children = new HashMap<>();
    private Long duration;
    private int depth;
    private Long timeStampe;
    private int maxDepth;
    private int nbreCalls = 0;
    private String name;

    // mods 0:
    private int fN = -1;

    /**
     * Constructor mod 1
     */
    FlameGraphNode(Long Address, Long Duration, int Depth) {
        this(Address, Duration, Depth, 0);
    }

    /**
     * Constructor mod 2
     */
    FlameGraphNode(Long Address, Long Duration, int Depth, int n) {
        address = Address;
        duration = Duration;
        depth = Depth;
        fN = n;
        System.out.println("Address:" + Long.toHexString(address) + " " + " duration " + duration + " depth " + depth + " fN " + fN);
    }

    /**
     * @return The function's address
     */
    public Long getAddress() {
        return address;
    }

    /**
     * @param Address
     *            The function's address
     */
    public void setAddress(Long Address) {
        address = Address;
    }

    /**
     * @return The function's callees
     */
    public HashMap<Long, FlameGraphNode> getChildren() {
        return children;
    }

    /**
     * @param Children
     *            The callees of a function
     */
    public void setChildren(HashMap<Long, FlameGraphNode> Children) {
        children = Children;
    }

    /**
     * @return The caller of a function
     */
    public FlameGraphNode getParent() {
        return parent;
    }

    /**
     * @param Parent
     *            The caller of a function
     */
    public void setParent(FlameGraphNode Parent) {
        parent = Parent;
    }

    /**
     * @param child
     *            The callees of a function
     */
    public void addChild(FlameGraphNode child) {
        if (!children.containsKey(child.getAddress())) {
            child.setParent(this);
            child.setNbreCalls(child.nbreCalls + 1);
            children.put(child.getAddress(), child);
        } else {
            FlameGraphNode node = NonNullUtils.checkNotNull(children.get(child.getAddress()));
            node.setDuration(node.getDuration() + child.getDuration());
            node.setNbreCalls(node.nbreCalls + 1);
            mergeChildren(node, child);
            children.replace(node.getAddress(), node);
        }
    }

    /**
     * Merge the callees of two callers
     *
     * @param firstNode
     *            The first parent secondNode The second parent
     */
    private void mergeChildren(FlameGraphNode FirstNode, FlameGraphNode SecondNode) {
        for (Map.Entry<Long, FlameGraphNode> FunctionEntry : SecondNode.children.entrySet()) {
            if (FirstNode.children.containsKey(FunctionEntry.getKey())) {
                FlameGraphNode node = NonNullUtils.checkNotNull(FirstNode.children.get(FunctionEntry.getKey()));
                node.setDuration(node.getDuration() + FunctionEntry.getValue().getDuration());
                node.setNbreCalls(node.getNbreCalls() + 1);
                if (FunctionEntry.getValue().children.size() > 0) {
                    mergeChildren(node, FunctionEntry.getValue());
                }
                FirstNode.children.replace(node.getAddress(), node);
            } else {
                FlameGraphNode node = FunctionEntry.getValue();
                FirstNode.children.put(node.getAddress(), node);
            }
        }
    }

    /**
     * @return The duration of the function
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * @param Duration
     *            The total time of a function
     */
    public void setDuration(Long Duration) {
        duration = Duration;
    }

    /**
     * @return The depth of the function
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param Depth
     *            The depth of a function
     */
    public void setDepth(int Depth) {
        depth = Depth;
    }

    /**
     * @return The timeStamp of a function
     */
    public Long getTimestamp() {
        return timeStampe;
    }

    /**
     * @param TimeStampe
     *            The time stamp of a function
     */
    public void setTimeStamp(Long TimeStampe) {
        timeStampe = TimeStampe;
    }

    /**
     * @return The depth of the aggregation tree
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @param MaxDepth
     *            The depth of the aggregation tree
     */
    public void setMaxDepth(int MaxDepth) {
        maxDepth = MaxDepth;
    }

    /**
     * @return The number of calls of a function
     */
    public int getNbreCalls() {
        return nbreCalls;
    }

    /**
     * @param NbreCalls
     *            The number of calls of a function
     */
    public void setNbreCalls(int NbreCalls) {
        nbreCalls = NbreCalls;
    }

    /**
     * @return The function's name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The function's name
     */
    public void setName(String Name) {
        name = Name;
    }

    // Mod 5:
    public void setN(int N) {
        fN = N;
    }

    // mod 4
    @Override
    public String toString() {
        String aux = null;
        if (name != null) {
            aux = new String(name);
        }
        return aux;
    }

    // Mod 0
    public FlameGraphNode difference(FlameGraphNode e) {
        FlameGraphNode result = new FlameGraphNode(address, duration - e.duration, depth, fN - e.fN );

        return result;
    }

    // Mod 1
    @Override
    public boolean equals(Object other) {
        if (other instanceof FlameGraphNode) {
            FlameGraphNode node = (FlameGraphNode) other;

            if ((address == node.address) && (node.depth == node.depth)) {
                return true;
            }
        }
        return false;
    }

    // Mod 2
    @Override
    public int hashCode() {
        return address.hashCode() * depth;
    }

    // Mod 3:
    /*
     * This function takes two nodes and make the difference of them (and their children)
     * */
    public ArrayList<FlameGraphNode> levelOrderComparison(FlameGraphNode root1, FlameGraphNode root2) {

        ArrayList<FlameGraphNode> auxRoot1 = listNodes(root1);
        ArrayList<FlameGraphNode> auxRoot2 = listNodes(root2);

        ArrayList<FlameGraphNode> auxRootResult = new ArrayList<>();
        //Hash:
        HashMap<KeyTree, FlameGraphNode> map = new HashMap();

        //Populating the HashMap:
        for (FlameGraphNode node : auxRoot1) {
            System.out.println("Address 0x" +  Long.toHexString(node.getAddress()));
            KeyTree k = new KeyTree(Long.toHexString(node.getAddress()),node.depth);
            map.put(k,node);
        }

        //Iterating over the hashMap and taking the difference
        for (KeyTree key : map.keySet()) {
            FlameGraphNode aux = map.get(key);
            for (FlameGraphNode node : auxRoot2) {
                if(aux.equals(node))
                {
                    FlameGraphNode dif = aux.difference(node);
                    auxRootResult.add(dif);
                }
            }
        }

        return auxRootResult;
    }

    //ListNodes:
    public ArrayList<FlameGraphNode> listNodes(FlameGraphNode firstNode) {
        System.out.println("FlameGraph");
        System.out.println("Node:" + Long.toHexString(firstNode.getAddress()) + " Depth " + firstNode.getDepth());

        HashMap<Long, FlameGraphNode> aux = firstNode.getChildren();
        FlameGraphNode temp = firstNode;

        // ArrayList:
        ArrayList<FlameGraphNode> al = new ArrayList();
        al.add(temp);

        boolean cont = true;
        while (cont) {
            if (aux != null) {
                aux = temp.getChildren();
            }
            for (Long key : aux.keySet()) {

                FlameGraphNode node = aux.get(key);
                if (node != null) {
                    al.add(node);
                }
                // taking the FlameNode:
                temp = aux.get(key);
            }
            if (aux.size() == 0) {
                cont = false;
            }
        }

        return al;

    }
    // Class used for hashMap
    public static class KeyTree {
        final String label;
        final int level;

        public KeyTree(String label, int level) {
            this.label = label;
            this.level = level;
        }

        public String getLabel() {
            return this.label;
        }

        public int getLevel() {
            return this.level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, label);
        }

        @Override
        public boolean equals(Object k) {
            if (k == null) {
                return false;
            }
            if (k instanceof KeyTree) {
                KeyTree other = (KeyTree) k;
                return this.getLevel() == other.getLevel() &&
                        this.getLabel().equals(other.getLabel());
            }
            return false;
        }

        @Override
        public String toString() {
            return (this.label + " " + this.level);
        }

    }
}
