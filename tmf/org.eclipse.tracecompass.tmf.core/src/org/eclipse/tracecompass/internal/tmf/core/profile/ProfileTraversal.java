package org.eclipse.tracecompass.internal.tmf.core.profile;

import java.util.LinkedList;

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
    /** This function makes the levelOrderTraversal of a tree, which contains a generic node
     * @param root a tree first node to be traversed
     * @param visitor a visitor pattern implementation
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
    /** This function makes the levelOrderTraversal of a two trees that contains a generic node
     * @param root1 and root2 the first two nodes to be traversed
     * @param visitor a visitor pattern implementation
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


        for(Node<T> node: levelOrderFirst)
        {
            if(!node.equals(levelOrderSecond.get(i))) {
                return false;
            }
        }
        return true;
    }
}
