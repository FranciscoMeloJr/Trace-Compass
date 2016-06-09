package org.eclipse.tracecompass.internal.tmf.core.profile;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Node<T extends IProfileData> {

    private static AtomicInteger fCounter = new AtomicInteger();
    private ArrayList<Node> fChildren;
    private Node fParent;
    private final int fId;
    T fProfileData;

    public Node() {
        fChildren = new ArrayList<>();
        fId = fCounter.getAndIncrement();
    }

    public static <T extends IProfileData> Node<T> create(T data) {
        Node<T> node = new Node<>();
        node.setProfileData(data);
        return node;
    }
    public static <T extends IProfileData> Node<T> create(Node<T> node) {
        Node<T> newNode = new Node<>();
        newNode.setProfileData(node.fProfileData);
        newNode.setParent(node.getParent());
        return newNode;
    }
    public void addChild(Node<T> node) {
        fChildren.add(node);
        node.setParent(this);
    }

    public Iterable<Node> getChildren() {
        return fChildren;
    }

    public ArrayList<Node> getAllChildren() {
        return fChildren;
    }
    public Node getParent() {
        return fParent;
    }

    public void setParent(Node<T> parent) {
        fParent = parent;
    }

    public T getProfileData() {
        return fProfileData;
    }

    public void setProfileData(T data) {
        fProfileData = data;
    }

    public int getNodeId() {
        return fId;
    }
    public String getNodeLabel() {
        return fProfileData.getLabel();
    }
    public void mergeNode(Node<T> node) {
        fProfileData.merge(node.fProfileData);
    }

    @Override
    public String toString() {
        return "(" + fId + "," + fProfileData + ")";
    }

    public boolean equals(Node<T> node) {
        if ((fId == node.fId) && (fProfileData.equals(node.fProfileData))) {
            return true;
        }
        return false;
    }
    public Node copy() {
        Node<T> newNode = new Node<>();
        newNode.setProfileData(fProfileData);
        newNode.setParent(getParent());
        return newNode;
    }
}
