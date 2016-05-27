package org.eclipse.tracecompass.internal.tmf.core.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author frank
 *
 */
public class ProfileTraversal {

    /**
     * @param root
     * @param visitor
     */
    public static <T extends IProfileData> void preOrderTraversal(Node<T> root, IProfileVisitor<T> visitor) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        LinkedList<Node<T>> temp = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            temp.clear();
            Node<T> current = queue.poll();
            // reverse order
            for (Node<T> child : current.getChildren()) {
                temp.addFirst(child);
            }
            for (Node<T> child : temp) {
                queue.addFirst(child);
            }
            visitor.visit(current);
        }
    }

    /**
     * This function makes the levelOrderTraversal of a tree, which contains a
     * generic node
     *
     * @param root
     *            a tree first node to be traversed
     * @param visitor
     *            a visitor pattern implementation
     * @return the queue with the level order traversal
     */
    public static <T extends IProfileData> void levelOrderTraversal(Node<T> root, IProfileVisitor<T> visitor) {
        LinkedList<Node<T>> queue = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            visitor.visit(current);
        }

    }

    /**
     * This function generates a queue from the levelOrderTraversal of a tree,
     * which contains a generic node
     *
     * @param root
     *            a tree first node to be traversed
     */
    public static <T extends IProfileData> Queue<Node<T>> levelOrderTraversal(Node<T> root) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        LinkedList<Node<T>> result = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            result.add(current);
        }
        return result;
    }

    /**
     * This function makes the levelOrderTraversal of a two trees that contains
     * a generic node
     *
     * @param root1
     *            and root2 the first two nodes to be traversed
     * @param visitor
     *            a visitor pattern implementation
     */
    public static <T extends IProfileData> boolean levelOrderTraversalComparator(Node<T> root1, Node<T> root2, IProfileVisitor<T> visitor) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        LinkedList<Node<T>> levelOrderFirst = new LinkedList<>();
        LinkedList<Node<T>> levelOrderSecond = new LinkedList<>();

        int i = 0;
        queue.add(root1);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            visitor.visit(current);
            levelOrderFirst.add(current);
        }
        queue.add(root2);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            visitor.visit(current);
            levelOrderSecond.add(current);
        }

        // Do the level order traversal
        // Sort
        // Merge
        // Compare
        for (Node<T> node : levelOrderFirst) {
            if (!node.equals(levelOrderSecond.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * This function creates a sorted queue for comparison
     *
     * @param tree
     *            is the tree to be used
     */
    public static <T extends IProfileData> Queue<Node<T>> Sort(Node<T> tree) {

        Queue<Node<T>> queue = levelOrderTraversal(tree);

        // System.out.print("Entrance:" + Q1.size());
        Queue<Node<T>> q1 = new LinkedList<>();
        ArrayList<Node<T>> convertedArrayList = new ArrayList<>();
        ArrayList<Node<T>> mergedArrayList = new ArrayList<>();

        convertedArrayList = convertQueue(queue);

        // System.out.print("AL size:" + AL3.size());

        //Collections.sort((List<T>) convertedArrayList);
        mergedArrayList = mergeArrayList(convertedArrayList);

        q1 = convertArrayList(mergedArrayList);
        // System.out.print("Q1 size:" + q1.size());
        return q1;

    }

    /**
     * This function convert a queue in a ArrayList
     *
     * @param tree
     *            is the tree to be used
     */
    public static <T extends IProfileData> ArrayList<Node<T>> convertQueue(Queue tree) {

        ArrayList<Node<T>> Q2 = new ArrayList<>();

        Node<T> temp;
        Queue<Node<T>> queue = new LinkedList<>();
        queue = tree;

        while (!queue.isEmpty()) {
            temp = queue.poll();
            Q2.add(temp);
        }
        return Q2;
    }

    /**
     * This function merges nodes with the same label
     *
     * @param tree
     *            is the tree to be used
     */
    public static <T extends IProfileData> ArrayList<Node<T>> mergeArrayList(ArrayList<Node<T>> AL) {
        ArrayList<Node<T>> temp = new ArrayList<>();
        ArrayList<Node<T>> merged = new ArrayList<>();
        Node<T> newNode;
        temp = AL;

        for (int i = 1; i < temp.size(); i++) {
            Node<T> obj = temp.get(i);
            for (int j = 0; j < temp.size(); j++) {
                if (obj.equals(temp.get(j)) && (i != j)) {
                    newNode = temp.get(j);
                    obj.fProfileData.merge(newNode.fProfileData); // merge
                    temp.remove(j);
                }

            }
            merged.add(obj);
        }
        return merged;
    }

    /**
     * This function merges nodes with the same label
     *
     * @param tree
     *            is the tree to be used
     */
    public static <T extends IProfileData> Node<T> mergeTree(Node<T> root, Node<T> node) {
        LinkedList<Node<T>> queue = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            if (current.fProfileData.equals(node.fProfileData)) {
                current.fProfileData.merge(node.fProfileData);
            }
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
        }
        return root;
    }

    /**
     * This function converts ArrayList into Queues
     *
     * @param tree
     *            is the tree to be used
     * @return returns a queue from the queue
     */
    public static <T extends IProfileData> Queue<Node<T>> convertArrayList(ArrayList<Node<T>> tree) {

        Queue<Node<T>> queue = new LinkedList<>();
        // System.out.println("tree size: " + tree.size());
        for (Node n : tree) {
            queue.add(n);
        }
        return queue;
    }
    // Do the minus operation and return a new Queue

    /**
     * @param N1
     *            queue of the first tree
     * @param N2
     *            queue of the second tree
     * @return
     */
    public static <T extends IProfileData> Queue<Node<T>> doMinus(Queue<Node<T>> N1, Queue<Node<T>> N2) {
        System.out.println("\n" + "Minus Operation ");
        Node<T> temp, temp2, temp1;
        Queue<Node<T>> result = new LinkedList<>();

        // Print(N1);
        // Print(N2);
        System.out.println("size" + N1.size() + " " + N2.size());
        while (N1.size() > 0 && N2.size() > 0) {
            // System.out.print("while");
            temp1 = N1.poll();
            temp2 = N2.poll();
            // System.out.print(temp1.fInformation +" " + temp2.fInformation);
            temp = minus(temp1, temp2);
            result.add(temp);
        }
        // System.out.print("while 2");
        Queue<Node<T>> newResult = eliminateNull(result);
        // Queue<Node<T>> newResult2 = Print(newResult);

        // System.out.print("print");
        return newResult;
    }

    /**
     * @param node1
     * @param node2
     * @return
     * @return Node
     */
    public static <T extends IProfileData> Node<T> minus(Node<T> node1, Node<T> node2) {

        Node<T> temp = new Node();

        if (node1.fProfileData.equals(node2.fProfileData)) {
            temp.fProfileData.setLabel(node1.fProfileData.getLabel());
            IProfileData data = node1.fProfileData.minus(node2.fProfileData);
            // System.out.println(a + " " + b + " " + total);
            temp.setProfileData(data);
        }
        return temp;
    }

    /**
     * This function eliminates the null:
     *
     * @param queue
     * @return
     */
    public static <T extends IProfileData> Queue<Node<T>> eliminateNull(Queue<Node<T>> queue) {
        Queue<Node<T>> aux = new LinkedList<>();
        Queue<Node<T>> result = new LinkedList<>();
        Node<T> temp;
        aux = queue;
        while (!aux.isEmpty()) {
            temp = aux.poll();
            if (temp.fProfileData != null) {
                result.add(temp);
            }
        }
        return result;
    }
}
