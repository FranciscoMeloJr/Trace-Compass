package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * @author francisco
 * This class is related with the Profile traversal of the Enhanced Calling Context tree
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
            System.out.print("\n");
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            visitor.visit(current);
            System.out.print(current);
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
        System.out.print("levelOrderTraversal ");
        LinkedList<Node<T>> queue = new LinkedList<>();
        LinkedList<Node<T>> result = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            System.out.print(current.toString());
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
                System.out.println(child.getProfileData());
            }
            result.add(current);
        }

        System.out.println("\n");
        return result;
    }

    // This function makes a copy of a node
    public static <T extends IProfileData> Node<T> Copy2(Node<T> root) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        LinkedList<Node<T>> result = new LinkedList<>();
        Node<T>[] arrNodes = new Node[100];
        int arrJ = 0;

        // Add the root to the queue
        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            arrNodes[arrJ] = current.copy();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            result.add(current);
            arrJ++;
        }
        // Update the parents
        for (int i = 0; i < arrJ; i++) {
            if (arrNodes[i].getParent() != null) {
                Node<T> aux = findLabel(arrNodes, arrNodes[i].getParent().fProfileData.getLabel());
                arrNodes[i].setParent(aux);
            }
        }

        // Making the new tree:
        for (int i = 0; i < arrJ; i++) {
            for (int j = 1; j < arrJ; j++) {
                // run through the array:
                if (arrNodes[i].getNodeLabel() == arrNodes[j].getParent().getNodeLabel()) {
                    arrNodes[i].addChild(arrNodes[j]);
                }
            }
        }
        return arrNodes[0];
    }

    /**
     * This function finds a node with a specific label given
     *
     * @param arrNodes
     *            it is the Array to be used
     * @param label
     *            it is the label to be found
     */
    public static <T extends IProfileData> Node<T> findLabel(Node<T>[] arrNodes, String label) {

        for (int i = 0; i < arrNodes.length - 1; i++) {
            if (arrNodes[i].getProfileData().getLabel() == label) {
                return arrNodes[i];
            }
        }
        return null;
    }

    /**
     * This does a level order comparison of trees
     *
     * @param root1
     *            it is the first tree to iterate
     * @param root2
     *            it is the second tree to iterate
     */
    public static <T extends IProfileData> Node<T> levelOrderTraversalComparator(Node<T> root1, Node<T> root2) {
        System.out.println("Level Order comparator");
        // create a new tree as copy of the second tree - root2 > implement the
        // copy operation
        // create a temp node with the information of the two nodes
        // do a hashmap between the node source and the node result
        // add the temp node on the new tree
        LinkedList<Node<T>> queue1 = new LinkedList<>();
        LinkedList<Node<T>> queue2 = new LinkedList<>();
        Node<T>[] arrNodes = new Node[10]; // (input_node, output_node)
        int arrJ = 0;

        // copy
        Node<T> rootCopy = ProfileTraversal.Copy2(root2);

        queue1.add(root1);
        queue2.add(rootCopy);

        while (!queue1.isEmpty() && !queue2.isEmpty()) {
            Node<T> current1 = queue1.poll();
            Node<T> current2 = queue2.poll();

            if (current2.getNodeLabel() == current1.getNodeLabel()) {
                T data = current2.getProfileData();
                data.minus(current1.getProfileData());
                System.out.println(data);
                current2.setProfileData(data); // put on the tree
                arrNodes[arrJ] = current2;
                arrJ++;
            }

            for (Node<T> child : current1.getChildren()) {
                queue1.add(child);
            }
            for (Node<T> child : current2.getChildren()) {
                queue2.add(child);
            }
        }
        /*
         * add more loop because otherwise we will not distinguish the levels to
         * the comparison so do level by level of comparison
         */
        return rootCopy;
    }

    public static <T extends IProfileData> Node<T> levelOrderTraversalComparator2(Node<T> root1, Node<T> root2) {
        System.out.println("Level Order comparator " + "\n");
        LinkedList<Node<T>> queue1 = new LinkedList<>();
        LinkedList<Node<T>> queue2 = new LinkedList<>();
        System.out.print("rootCopy:");
        // copy
        Node<T> rootCopy = ProfileTraversal.Copy2(root2);
        System.out.print("queue");
        queue1.add(root1);
        queue2.add(rootCopy);
        ProfileTraversal.levelOrderTraversal(root1);
        ProfileTraversal.levelOrderTraversal(root2);
        System.out.print("rootCopy:");
        while (!queue1.isEmpty() && !queue2.isEmpty()) {

            Node<T> current1 = queue1.poll();
            Node<T> current2 = queue2.poll();
            System.out.print("Arvore 1" + current1);
            System.out.print("Arvore 2" + current2);
            if (current2.getNodeLabel() == current1.getNodeLabel()) {
                System.out.print(current1.getProfileData().getWeight() + " " + current1.getProfileData().getWeight() + " ");
                T data = current2.getProfileData();
                data.minus(current1.getProfileData());
                System.out.print("after:" + data + "\n");
                current2.setProfileData(data); // put on the tree

                for (Node<T> child : current1.getChildren()) {
                    queue1.add(child);
                }
                for (Node<T> child : current2.getChildren()) {
                    queue2.add(child);
                }
            }
        }
        /*
         * add more loop because otherwise we will not distinguish the levels to
         * the comparison so do level by level of comparison
         */
        return rootCopy;
    }

    /**
     * This function does the traversal Level Order Comparator
     *
     * @param node
     *            it is the tree
     * @param label
     *            it is the string
     */
    public static <T extends IProfileData> Node<T> levelOrderTraversalComparator3(Node<T> root1, Node<T> root2) {
        System.out.println("Level Order comparator " + "\n");
        LinkedList<Node<T>> queue1 = new LinkedList<>();
        LinkedList<Node<T>> queue2 = new LinkedList<>();
        System.out.print("rootCopy:");
        // copy
        Node<T> rootCopy = ProfileTraversal.Copy2(root2);
        System.out.print("Arvore 1 e 2c:");
        queue1.add(root1);
        queue2.add(rootCopy);
        ProfileTraversal.levelOrderTraversal(root1);
        ProfileTraversal.levelOrderTraversal(root2);
        System.out.print("comparation:");

        Node<T> current1 = null;
        Node<T> current2 = null;
        boolean sameFather = false;
        while (!queue1.isEmpty()) {
            current1 = queue1.poll();
            if (sameFather == false) {
                current2 = queue2.poll();
            }
            System.out.println("Comparando: " + current1 + " " + current2);
            if (current2 != null) {
                if (current1.getNodeLabel() == current2.getNodeLabel()) {
                    sameFather = true;
                    T data = current2.getProfileData();
                    data.minus(current1.getProfileData());
                    System.out.print("after:" + data + "\n");
                    current2.setProfileData(data); // put on the tree

                    // If parents are the same accumulate
                    for (Node<T> child : current1.getChildren()) {
                        System.out.print("fila 1");
                        queue1.add(child);
                    }
                    // If parents are the same accumulate
                    for (Node<T> child : current2.getChildren()) {
                        System.out.print("fila 2");
                        queue2.add(child);
                    }

                    // How to synchronize the input?
                    current2 = queue2.poll();
                    System.out.println("novo current2: " + current2);
                }
            }
        }
        return rootCopy;
    }

    /**
     * This function does a hash map for the comparison
     *
     * @param root1
     *            it is the first tree
     * @param root2
     *            it is the second tree
     */
    public static <T extends IProfileData> Node<T> levelOrderTraversalComparatorHash(Node<T> root1, Node<T> root2) {
        System.out.println("Level Order comparator using hash x" + "\n");
        LinkedList<Node<T>> queue1 = new LinkedList<>();
        LinkedList<Node<T>> queue2 = new LinkedList<>();
        Map<KeyTree, Node<T>> hmap1 = new HashMap<>();
        Map<KeyTree, Node<T>> hmap2 = new HashMap<>();
        System.out.print("rootCopy:");
        // copy
        Node<T> rootCopy = ProfileTraversal.Copy2(root2);

        // Tree 1:
        queue1.add(root1);
        Node<T> pointerParent = root1.getParent();
        Node<T> current = null;
        int level;
        while (!queue1.isEmpty()) {
            current = queue1.poll();
            level = 0;
            pointerParent = current.getParent();
            if (pointerParent != null) {
                while (pointerParent != null) {
                    pointerParent = pointerParent.getParent();
                    level++;
                }
            }
            String label = current.getNodeLabel();
            KeyTree aux = new KeyTree(label, level);
            hmap1.put(aux, current);
            for (Node<T> child : current.getChildren()) {
                queue1.add(child);
            }
        }
        /*
         * System.out.println("hmap 1 "); for (KeyTree key : hmap1.keySet()) {
         * System.out.println(key.getLabel() + " " + key.getLevel() + " " +
         * hmap1.get(key)); }
         */
        queue2.add(root2);
        pointerParent = root2.getParent();
        current = null;
        level = 0;
        while (!queue2.isEmpty()) {
            current = queue2.poll();
            level = 0;
            pointerParent = current.getParent();
            if (pointerParent != null) {
                while (pointerParent != null) {
                    pointerParent = pointerParent.getParent();
                    level++;
                }
            }
            String label = current.getNodeLabel();
            KeyTree aux = new KeyTree(label, level);
            hmap2.put(aux, current);
            for (Node<T> child : current.getChildren()) {
                queue2.add(child);
            }
        }

        /*
         * System.out.println("hmap 2 "); for (KeyTree key : hmap2.keySet()) {
         * System.out.println(key.getLabel() + " " + key.getLevel() + " " +
         * hmap2.get(key)); }
         */
        // Combining:

        Node<T> current1, current2;
        System.out.println("Combining");
        for (KeyTree key : hmap1.keySet()) {
            if (hmap1.get(key) != null) {
                current1 = hmap1.get(key);
                current2 = hmap2.get(key);
                hmap2.remove(key);
                if ((current2 != null) && (current1 != null)) {
                    T data = current2.getProfileData();
                    data.minus(current1.getProfileData());
                    current2.setProfileData(data); // put on the tree
                    System.out.println(current2);
                    if (data.getWeight() < current2.getProfileData().getWeight()) {
                        System.out.println("Green");
                    } else {
                        System.out.println("Red");
                    }
                    hmap2.put(key, current2);
                }
            }
        }
        System.out.println("Final ");
        for (KeyTree key : hmap2.keySet()) {
            System.out.println(key.getLabel() + " " + key.getLevel() + " " + hmap2.get(key));
            System.out.println(key.hashCode());
        }
        return rootCopy;
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

    /**
     * This function finds a node on a tree from a label
     *
     * @param node
     *            it is the tree
     * @param label
     *            it is the string
     */
    public static <T extends IProfileData> Node<T> findNode(Node<T> root, String label) {

        System.out.println("findNode");
        LinkedList<Node<T>> queue = new LinkedList<>();
        Node<T> result = null;

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            if (current.getNodeLabel() == label) {
                result = current;
            }
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
        }

        System.out.println(result);
        return result;
    }

    /**
     * This function updates a node in an array list
     *
     * @param arrNodes
     *            it is the Array to be used
     * @param node
     *            it is the node that will be updated
     */
    public static <T extends IProfileData> Node<T> updateNode(Node<T>[] arrNodes, Node<T> node) {

        for (int i = 0; i < arrNodes.length - 1; i++) {
            if (arrNodes[i].getProfileData().getLabel() == node.getNodeLabel()) {
                return arrNodes[i] = node;
            }
        }
        return null;
    }

    /**
     * This function creates a copy of a tree
     *
     * @param tree
     *            is the tree to be used
     */
    public static <T extends IProfileData> Node<T> Copy(Node<T> root1) {
        LinkedList<Node<T>> queue = new LinkedList<>();
        Node<T> result = new Node<>();
        LinkedList<Node<T>> acc = new LinkedList<>();
        HashMap<Node<T>, Node<T>> hmap = new HashMap<>(); // node between the
                                                          // src and the result
        // copy
        queue.add(root1);
        result.setParent(root1.getParent());
        result.setProfileData(root1.getProfileData());

        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();

            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            if ((current.getParent() != null)) {
                hmap.put(current.getParent(), current);
            } else {
                hmap.put(new Node(), current);
            }
            acc.add(current);
        }

        System.out.println(hmap.size());
        for (Node<T> key : hmap.keySet()) {
            if (key != null) {
                System.out.println("Parent " + key + " value" + hmap.get(key));
            }
        }
        for (Node<T> key : hmap.keySet()) {
            if (key != null) {
                NonNullUtils.checkNotNull(hmap.get(key)).addChild(hmap.get(key));
            }
        }

        return result;
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

        // Collections.sort(AL2);
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
    public static <T extends IProfileData> ArrayList<Node<T>> convertQueue(Queue<Node<T>> tree) {

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
     * @return Node<T>
     */
    public static <T extends IProfileData> Node<T> minus(Node<T> node1, Node<T> node2) {

        Node<T> temp = new Node();

        /*
         * if (node1.fProfileData.equals(node2.fProfileData)) {
         * temp.fProfileData.setLabel(node1.fProfileData.getLabel());
         * IProfileData data = node1.fProfileData.minus(node2.fProfileData);
         * //System.out.println(a + " " + b + " " + total);
         * temp.setProfileData(data); }
         */
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
