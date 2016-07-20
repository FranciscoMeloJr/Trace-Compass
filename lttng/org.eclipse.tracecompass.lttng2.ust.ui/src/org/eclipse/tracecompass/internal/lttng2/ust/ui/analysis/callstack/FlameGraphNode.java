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

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Constructor
     */
    FlameGraphNode(Long Address, Long Duration, int Depth) {
        address = Address;
        duration = Duration;
        depth = Depth;
        System.out.println("Address:" + address + " "+ " duration " + duration + " depth " + depth);
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
            mergeChildren(node,child);
            children.replace(node.getAddress(), node);
        }
    }

    /**
     * Merge the callees of two callers
     * @param firstNode
     *            The first parent
     *        secondNode
     *            The second parent
     */
private void mergeChildren(FlameGraphNode FirstNode,FlameGraphNode SecondNode)
{
    for (Map.Entry<Long, FlameGraphNode> FunctionEntry : SecondNode.children.entrySet()) {
        if(FirstNode.children.containsKey(FunctionEntry.getKey()))
        {
            FlameGraphNode node = NonNullUtils.checkNotNull(FirstNode.children.get(FunctionEntry.getKey()));
            node.setDuration(node.getDuration() + FunctionEntry.getValue().getDuration());
            node.setNbreCalls(node.getNbreCalls()+1);
            if(FunctionEntry.getValue().children.size()>0){
                mergeChildren(node,FunctionEntry.getValue());
            }
            FirstNode.children.replace(node.getAddress(), node);
        }
        else{
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
     * @return
     *      The function's name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *      The function's name
     */
    public void setName(String Name) {
        name = Name;
    }

}
