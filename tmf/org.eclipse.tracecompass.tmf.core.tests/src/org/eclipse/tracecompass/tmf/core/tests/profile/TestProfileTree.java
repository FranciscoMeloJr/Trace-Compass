package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.tmf.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.tmf.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.tmf.core.profile.Node;
import org.eclipse.tracecompass.internal.tmf.core.profile.ProfileTraversal;
import org.junit.Before;
import org.junit.Test;

/**
 * @author frank
 *
 */
public class TestProfileTree {

    /**
     * @author frank
     *
     */
    public class Visitor implements IProfileVisitor<TestData> {
        /**
         * result ArrayList of Nodes, which are TestData
         */
        public ArrayList<Node<TestData>> result = new ArrayList<>();

        @Override
        public void visit(Node<TestData> node) {
            result.add(node);
        }
    }

    /**
     * @author frank
     *
     */
    public class GraphvizVisitor implements IProfileVisitor<TestData> {
        /**
         * result ArrayList of Nodes, which are TestData
         */
        public ArrayList<Node<TestData>> result = new ArrayList<>();

        @Override
        public void visit(Node<TestData> node) {
            result.add(node);
        }

        /**
         * This function reset the visit
         */
        public void reset() {
            result = new ArrayList<>();
        }

        /**
         * This function print on the console the tree
         *
         * @throws Exception
         */
        public void print(String name) throws Exception {
            System.out.println("Print tree:");
            String content = new String("digraph G { \n");
            // Edges and nodes:
            for (Node<TestData> n : result) {
                if (n.getParent() != null) {
                    System.out.print(n.getNodeLabel() + " -> " + n.getParent().getNodeLabel() + "; \n");
                    content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                } else {
                    System.out.print(n.getNodeLabel() + "; \n");
                    content = content.concat(n.getNodeLabel() + "; \n");
                }
            }
            // Content:
            // B [label = "B [-1]"]; C [label = "C [-1]"]; D [label = "D [-1]"];
            // I [label = "I [-1]"];
            for (Node<TestData> n : result) {
                content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");

            }
            content = content.concat("\n }\n");
            writeToFile(name, content);
        }

        /**
         * This function print on a file the output of the tree:
         */
        public void writeToFile(String name, String content) throws Exception {
            try {

                // String content = "This is the content to write into file";
                String fileName = new String("/home/frank/Desktop/");
                fileName = fileName.concat(name); //
                File file = new File(fileName); // "/home/frank/Desktop/tree.gv");

                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }

                try (FileWriter fw = new FileWriter(file.getAbsoluteFile())) {
                    try (BufferedWriter bw = new BufferedWriter(fw)) {
                        bw.write(content);
                        bw.close();
                        System.out.println("Done");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class TestData implements IProfileData {

        private int fWeight;
        private String fLabel;

        public TestData(int weight, String label) {
            fWeight = weight;
            fLabel = label;
        }

        @Override
        public int getWeight() {
            return fWeight;
        }

        @Override
        public String getLabel() {
            return fLabel;
        }

        public void setWeight(int newfWeight) {
            fWeight = newfWeight;
        }

        public void setLabel(String newfLabel) {
            fLabel = newfLabel;
        }

        @Override
        public void merge(IProfileData other) {
            if (!(other instanceof TestData)) {
                throw new IllegalArgumentException("wrong type for minus operation");
            }
            TestData data = (TestData) other;
            if (fLabel.equals(data.getLabel())) {
                fWeight += data.getWeight();
            }
        }

        @Override
        public IProfileData minus(IProfileData other) {
            if (!(other instanceof TestData)) {
                throw new IllegalArgumentException("wrong type for minus operation");
            }
            TestData data = (TestData) other;
            fWeight = fWeight - data.getWeight();
            return new TestData(fWeight - data.getWeight(), fLabel);
        }

        @Override
        public String toString() {
            return fLabel + "," + fWeight;
        }

        @Override
        public boolean equals(IProfileData other) {
            if (!(other instanceof TestData)) {
                throw new IllegalArgumentException("wrong type for minus operation");
            }
            TestData data = (TestData) other;
            return fLabel.equals(data.getLabel());
        }

        public void addWeight(int value) {
            fWeight = fWeight + value;
        }

    }

    private Node<TestData> fRoot, fRoot2;

    String[] fLabels1 = { "A", "B", "C", "D", "E", "F", "G", "H", "I" };
    String[][] fTreeDef1 = {
            { "F", "B" },
            { "F", "G" },
            { "B", "A" },
            { "B", "D" },
            { "D", "C" },
            { "D", "E" },
            { "G", "I" },
            { "I", "H" },
    };
    String[] fLabels2 = { "A", "B", "C", "D", "E", "F", "G", "H", "I" };
    String[][] fTreeDef2 = {
            { "F", "B" },
            { "F", "G" },
            { "B", "A" },
            { "B", "D" },
            { "D", "C" },
            { "D", "E" },
            { "G", "I" },
            { "I", "H" },
    };
    int[] values = { 12, 13, 15, 16, 17, 29, 16, 17, 29 };
    String[] fExpectedPreorder = { "F", "B", "A", "D", "C", "E", "G", "I", "H" };
    String[] fExpectedLevelorder = { "F", "B", "G", "A", "D", "I", "C", "E", "H" };

    private Node<TestData> makeTree(String rootLabel, String[] labels, String[][] defs, int increment) {
        int i = 0;
        Map<String, Node<TestData>> map = new HashMap<>();
        for (String label : labels) {
            Node<TestData> node = Node.create(new TestData(values[i] + increment, label));
            map.put(label, node);
            i++;
        }
        Node<TestData> root = map.get(rootLabel);
        for (String[] item : defs) {
            NonNullUtils.checkNotNull(map.get(item[0])).addChild(NonNullUtils.checkNotNull(map.get(item[1])));
        }
        return root;
    }

    /**
     * This is the setup for the experiments, which are basically the tree
     * creation
     */
    @Before
    public void setup() {
        fRoot = makeTree("F", fLabels1, fTreeDef1, 10);
        fRoot2 = makeTree("F", fLabels2, fTreeDef2, 1); // the tree used for
                                                        // comparison
    }

    /**
     * This is a Junit test for Printing the tree
     */
    @Test
    public void testPrintTree() {

        Visitor visitor2 = new Visitor();
        // Create the first tree:
        System.out.println(ProfileTraversal.levelOrderTraversal(fRoot));
        // Create the second tree:
        ProfileTraversal.levelOrderTraversal(fRoot2, visitor2);

        // Asset for Equals
        assertEquals(fExpectedLevelorder.length, visitor2.result.size());
        for (int i = 0; i < fExpectedLevelorder.length; i++) {
            assertEquals(fExpectedLevelorder[i], visitor2.result.get(i).getProfileData().getLabel());
        }

        // Testing copy
        Node<TestData> a = ProfileTraversal.Copy2(fRoot);
        System.out.println(ProfileTraversal.levelOrderTraversal(a));
    }

    /**
     * This is a Junit test for Comparing equal trees
     *
     * @throws Exception
     */
    @Test
    public void testComparison() throws Exception {

        GraphvizVisitor visitor = new GraphvizVisitor();
        System.out.println("Comparison test x");
        ProfileTraversal.levelOrderTraversal(fRoot);

        ProfileTraversal.levelOrderTraversal(fRoot2);

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b, visitor);
        visitor.print("treeOutput.gv");

    }

    /**
     * This is a Junit test for Comparing equal and print the three trees on the
     * file
     *
     * @throws Exception
     */
    @Test
    public void testComparisonPrint() throws Exception {

        // Question: how to use only one visitor to print?

        GraphvizVisitor visitorInput1 = new GraphvizVisitor();
        GraphvizVisitor visitorInput2 = new GraphvizVisitor();
        GraphvizVisitor visitorOutput = new GraphvizVisitor();

        System.out.println("Comparison test x");
        ProfileTraversal.levelOrderTraversal(fRoot, visitorInput1);
        visitorInput1.print("treeInput1.gv");
        // visitor.reset();

        ProfileTraversal.levelOrderTraversal(fRoot2, visitorInput2);
        visitorInput2.print("treeInput2.gv");
        // visitor.reset();

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b, visitorOutput);
        visitorOutput.print("treeOutput.gv");

    }

    /**
     * This is a Junit test is the algorithm to create trees
     *
     * @throws Exception
     */
    @Test
    public void testCreateTree() throws Exception {

        GraphvizVisitor visitor1 = new GraphvizVisitor();
        GraphvizVisitor visitor2 = new GraphvizVisitor();
        GraphvizVisitor visitor3 = new GraphvizVisitor();

        // Create the events:
        String event1[] = { "20", "A", "B","D" };
        String event2[] = { "10", "C" };
        String event3[] = { "10", "A", "B", "D" };
        Node<TestData> root1 = createTree(event1, event2);
        Node<TestData> root2 = createTree(event3, null);
        // merge(pointer.getParent(), event4);

        ProfileTraversal.levelOrderTraversal(root1, visitor1);
        visitor1.print("treeInput1.gv");

        ProfileTraversal.levelOrderTraversal(root2, visitor2);
        visitor2.print("treeInput2.gv");

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparatorHash(root2, root1);
        ProfileTraversal.levelOrderTraversal(b, visitor3);
        visitor3.print("treeOutput.gv");
    }

    // This function creates a tree from a basic and then add samples
    public Node<TestData> createTree(String[] stringCreating, String[] event2) throws Exception {

        // Create the events:
        String event1[] = stringCreating;
        // String event2[] = { "10", "baz" };
        // String event3[] = { "10", "F", "B", "D", "E" };
        // String event4[] = { "10", "F", "G", "I", "H" };

        String creation[] = null;
        creation = event1;
        // Put it on the tree
        int info = Integer.parseInt(creation[0]);
        Node<TestData> pointer = null;
        Node<TestData> temp = null;
        Node<TestData> n = Node.create(new TestData(info, creation[creation.length - 1])); // create_the_last_one
        // Create the tree backwards
        temp = n;
        pointer = n;

        for (int i = 2; i <= creation.length - 1; i++) {
            pointer = temp;
            temp = Node.create(new TestData(info, creation[creation.length - i])); // createEachNode
            pointer.setParent(temp);
            temp.addChild(pointer);
        }

        // Put the root:
        Node<TestData> root = Node.create(new TestData(0, "root"));
        root.addChild(pointer.getParent());
        pointer.getParent().setParent(root);

        if (event2 != null) {
            addSample(pointer.getParent(), event2);
            // merge(pointer.getParent(), event2);
            // merge(pointer.getParent(), event4);
        }

        return root;
    }

    /**
     * This function merges a tree with a String["10", "A","B","C"]
     */
    public Node<TestData> addSample(Node<TestData> root, String[] event2) {
        // String [] = {"10","F","B","A"}
        // First node addition
        Node<TestData> temp = null;
        Node<TestData> parent = null;
        Node<TestData> pointer = null;

        parent = root;
        boolean created = false;
        String aux[] = null;
        aux = event2;
        String label = aux[1]; // "F"
        int info = Integer.parseInt(aux[0]);
        System.out.println(parent);

        if (label.equals(parent.getNodeLabel())) {
            parent.getProfileData().addWeight(info);
            pointer = parent;
        } else {
            temp = Node.create(new TestData(info, label));
            temp.setParent(parent.getParent());
            parent.getParent().addChild(temp);
            pointer = temp;
        }

        // n node:
        info = Integer.parseInt(aux[0]);
        for (int i = 2; i < aux.length; i++) {
            label = aux[i]; // "A"
            created = false;
            for (Node<TestData> node : pointer.getChildren()) {
                System.out.println(node);
                if (label.equals(node.getNodeLabel())) {
                    node.getProfileData().addWeight(info);
                    created = true;
                    pointer = node;
                }
            }
            if (!created) {
                temp = Node.create(new TestData(info, label));
                temp.setParent(pointer);
                pointer.addChild(temp);
                pointer = temp;
            }
        }
        return pointer;
    }

    /**
     * This is a Junit test for Comparing trees A > B
     */
    @Test
    public void testComparison2() {
        System.out.println("Comparison test");
        ProfileTraversal.levelOrderTraversal(fRoot);
        ProfileTraversal.levelOrderTraversal(fRoot2);

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b);
    }

    /**
     * This is a Junit test for Comparing trees B > A
     */
    @Test
    public void testComparison3() {
        System.out.println("Comparison test");
        ProfileTraversal.levelOrderTraversal(fRoot);
        ProfileTraversal.levelOrderTraversal(fRoot2);

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b);
    }

    /**
     * This is a JUnit test for list comparison
     */
    @Test
    public void testComparisonOfLists() {
        System.out.println("Result");
        Comparator<Node<TestData>> cmp = new Comparator<Node<TestData>>() {
            @Override
            public int compare(Node<TestData> arg0, Node<TestData> arg1) {
                return arg0.getProfileData().getLabel().compareTo(arg1.getProfileData().getLabel());
            }
        };

        ArrayList<Node<TestData>> lst1 = new ArrayList<>();
        ArrayList<Node<TestData>> lst2 = new ArrayList<>();

        // Add note:
        lst1.add(Node.create(new TestData(10, "C")));
        lst1.add(Node.create(new TestData(10, "B")));
        lst1.add(Node.create(new TestData(10, "A")));

        lst2.add(Node.create(new TestData(20, "C")));
        lst2.add(Node.create(new TestData(20, "B")));
        lst2.add(Node.create(new TestData(20, "D")));
        lst2.add(Node.create(new TestData(20, "E")));
        lst2.add(Node.create(new TestData(20, "F")));
        lst2.add(Node.create(new TestData(20, "G")));

        // Sort first:
        lst1.sort(cmp);
        lst2.sort(cmp);

        int i1 = 0, i2 = 0;

        ArrayList<Node<TestData>> result = new ArrayList<>();

        // Comparison algorithm:
        while (i1 < lst1.size() || i2 < lst2.size()) {
            System.out.println(i1 + " " + i2);

            Node<TestData> node1, node2;

            if (i1 < lst1.size()) {
                node1 = lst1.get(i1);
            } else {
                node1 = Node.create(new TestData(0, lst2.get(i2).getProfileData().getLabel()));
            }

            if (i2 < lst2.size()) {
                node2 = lst2.get(i2);
            } else {
                node2 = Node.create(new TestData(0, lst1.get(i1).getProfileData().getLabel()));
            }

            int res = cmp.compare(node1, node2);
            if (res == 0) {
                System.out.println("equal nodes: " + node1 + " " + node2);

                IProfileData data = node1.getProfileData().minus(node2.getProfileData());
                result.add(Node.create((TestData) data));
                i1++;
                i2++;
            } else if (res > 0) {
                System.out.println("node1 greater than node2: " + node1 + " " + node2);

                Node<TestData> defNode = Node.create(new TestData(0, node2.getProfileData().getLabel()));
                IProfileData data = defNode.getProfileData().minus(node2.getProfileData());
                result.add(Node.create((TestData) data));
                i2++;
            } else {
                System.out.println("node1 less than node2: " + node1 + " " + node2);

                Node<TestData> defNode = Node.create(new TestData(0, node1.getProfileData().getLabel()));
                IProfileData data = node1.getProfileData().minus(defNode.getProfileData());
                result.add(Node.create((TestData) data));
                i1++;
            }

        }
        System.out.println("Result");
        System.out.println(result);
    }

}
