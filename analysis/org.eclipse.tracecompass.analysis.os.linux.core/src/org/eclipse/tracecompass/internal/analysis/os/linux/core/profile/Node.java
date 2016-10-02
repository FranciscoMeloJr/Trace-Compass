package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Node<T extends IProfileData> {

    private static AtomicInteger fCounter = new AtomicInteger();
    private ArrayList<Node> fChildren;
    private Node fParent;
    private final int fId;
    T fProfileData;
    private int fColor = 0;
    private long fPointer = 0;
    private long fDur = 0;
    private String fGroup = new String(Integer.toString(0));

    public Node() {
        fChildren = new ArrayList<>();
        fId = fCounter.getAndIncrement();
    }

    //This function veryfies if the node is empty
    public boolean isEmpty() {
        if(fProfileData != null) {
            return false;
        }
        return true;
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

    public int diff(Node<T> compare, int threshold) {
        getProfileData().minus(compare.getProfileData());
        //zero is the same:
        fColor = getProfileData().minus(compare.getProfileData(), threshold);
        System.out.println("Color " + fColor);
        return fColor;
    }

    //Color properties:
    public int getColor() {
        return fColor;
    }

    public void setColor(int newColor)
    {
        fColor = newColor;
    }

    //Displaying children:
    public void setPointer(long l) {
        fPointer += l;

    }

    public long getPointer() {
        return fPointer;

    }

    //Displaying self:
    public void setDur(long l) {
        fDur += l;

    }

    public long getDur() {
        return fDur;

    }

    //Set and get for group:
    public void setGroup(String newGr) {
        fGroup = newGr;
    }
    public String getGroup() {
        return fGroup;

    }
}
