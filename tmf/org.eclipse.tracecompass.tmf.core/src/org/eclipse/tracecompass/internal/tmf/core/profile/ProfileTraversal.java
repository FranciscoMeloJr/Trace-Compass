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
    /**
     * @param root
     * @param visitor
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

}
