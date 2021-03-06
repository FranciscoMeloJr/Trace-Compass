package org.eclipse.tracecompass.analysis.os.linux.core.tests.profile;

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
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.Node;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal;
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
        public void print(String name, Mode mode) throws Exception {
            System.out.println("Print tree:");
            String content = new String("digraph G { \n");
            if (mode != Mode.COLOR) {
                if (mode != Mode.ID) {
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
                    for (Node<TestData> n : result) {
                        content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                    }
                } else {
                    // Edges and nodes:
                    for (Node<TestData> n : result) {
                        if (n.getParent() != null) {
                            System.out.print(n.getNodeId() + " -> " + n.getParent().getNodeId() + "; \n");
                            content = content.concat(n.getParent().getNodeId() + " -> " + n.getNodeId() + "; \n");
                        } else {
                            System.out.print(n.getNodeId() + "; \n");
                            content = content.concat(n.getNodeId() + "; \n");
                        }
                    }
                    for (Node<TestData> n : result) {
                        content = content.concat(n.getNodeId() + " " + "[label = \"" + n.getNodeId() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                    }
                }
            } else {
                for (Node<TestData> n : result) {
                    if (n.getParent() != null) {
                        System.out.print(n.getNodeLabel() + " -> " + n.getParent().getNodeLabel() + "; \n");
                        content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                    } else {
                        System.out.print(n.getNodeLabel() + "; \n");
                        content = content.concat(n.getNodeLabel() + "; \n");
                    }
                }
                for (Node<TestData> n : result) {
                    content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData().getColor() + "]\" ]; \n");
                }
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
                String fileName = new String("/tmp/");
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
                        System.out.println("Done printing");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Color for the graph plot
    public enum Color {
        Grey, Green, Red;
    }

    // Mode for print
    public enum Mode {
        ID, LABEL, COLOR;
    }

    // Which tree:
    public enum Tree {
        ECCT, DCT, CG;
        // Calling context tree, dynamic call tree, call graph
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
        Mode mode = Mode.ID;
        System.out.println("Comparison test x");
        ProfileTraversal.levelOrderTraversal(fRoot);

        ProfileTraversal.levelOrderTraversal(fRoot2);

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b, visitor);
        visitor.print("treeOutput.gv", mode);

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

        Mode mode = Mode.LABEL;
        System.out.println("Comparison test x");
        ProfileTraversal.levelOrderTraversal(fRoot, visitorInput1);
        visitorInput1.print("treeInput1.gv", mode);
        // visitor.reset();

        ProfileTraversal.levelOrderTraversal(fRoot2, visitorInput2);
        visitorInput2.print("treeInput2.gv", mode);
        // visitor.reset();

        Node<TestData> b = ProfileTraversal.levelOrderTraversalComparator2(fRoot, fRoot2);
        ProfileTraversal.levelOrderTraversal(b, visitorOutput);
        visitorOutput.print("treeOutput.gv", mode);

    }

    /**
     * This is a Junit test is the algorithm to create trees
     *
     * @throws Exception
     */
    Integer fCount = 0;
    ArrayList<String> fAct = new ArrayList<>();

    /**
     * @throws Exception
     */
    @Test
    public void testCreateTree() throws Exception {
        GraphvizVisitor dot = new GraphvizVisitor();
        Node<TestData> root = Node.create(new TestData(0, "root"));
        Mode mode = Mode.LABEL;
        Tree typeTree = Tree.ECCT;
        // Create the events:
        String trace[][] = {
                { "F", "B", "A" },
                { "F", "B", "D", "C" },
                { "F", "B", "D", "E" },
                { "F", "G", "I", "H" },
        };

        for (String[] event : trace) {
            addSample(root, event, 1, typeTree);
        }

        ProfileTraversal.levelOrderTraversal(root, dot);

        fAct.clear();
        ProfileTraversal.levelOrderTraversal(root, new IProfileVisitor<TestData>() {
            @Override
            public void visit(Node<TestData> node) {
                fAct.add(node.getNodeLabel());
                fCount++;
            }
        });

        assertEquals("wrong number of nodes in the tree", 10, (long) fCount);

        String exp[] = { "root", "F", "B", "G", "A", "D", "I", "C", "E", "H" };
        assertArrayEquals(exp, fAct.toArray());

        dot.print("samples1.dot", mode);
    }

    /**
     * This is a Junit test is the algorithm to compare trees
     *
     * @throws Exception
     */

    @Test
    public void testCompare() throws Exception {
        GraphvizVisitor dot = new GraphvizVisitor();
        Node<TestData> root = Node.create(new TestData(0, "root"));
        Node<TestData> root2 = Node.create(new TestData(0, "root"));
        Mode mode = Mode.LABEL;
        Tree tree = Tree.ECCT;

        // Create the events:
        String trace1[][] = {
                { "xis", "foo", "xis" },
                { "xis" },
                { "xis" },
                { "baz" },
        };

        for (String[] event : trace1) {
            addSample(root, event, 10, tree);
        }

        // Create the events:
        String trace2[][] = {
                { "xis", "foo" },
        };

        for (String[] event : trace2) {
            addSample(root2, event, 5, tree);
        }

        ProfileTraversal.levelOrderTraversal(root, dot);
        dot.print("input1.dot", mode);
        dot.reset();
        ProfileTraversal.levelOrderTraversal(root2, dot);
        dot.print("input2.dot", mode);
        dot.reset();
        ProfileTraversal.levelOrderTraversalComparatorHash(root, root2);
        ProfileTraversal.levelOrderTraversal(root2, dot);
        // Change the print mode
        mode = Mode.COLOR;
        dot.print("samples1.dot", mode);
    }

    /**
     * This is a Junit test is to generate a call graph
     *
     * @throws Exception
     */

    @Test
    public void testCallGraph() throws Exception {
        GraphvizVisitor dot = new GraphvizVisitor();
        Node<TestData> root = Node.create(new TestData(0, "root"));
        Mode mode = Mode.ID;
        Tree tree = Tree.DCT;
        // Create the events:
        String trace1[][] = {
                { "xis", "foo", "xis" },
                { "xis" },
                { "xis" },
                { "baz" },
        };

        for (String[] event : trace1) {
            // Creating a DCT:
            addSample(root, event, 10, tree);
        }

        ProfileTraversal.levelOrderTraversal(root, dot);
        dot.print("input1.dot", mode);
        dot.reset();
    }

    /**
     * This function add a sample in the calling context tree
     */
    public void addSample(Node<TestData> root, String[] event, int value, Tree t) {
        // for each stack level

        Node<TestData> current = root;
        for (String label : event) {

            System.out.println(label + " " + value);
            Node<TestData> match = null;
            if (t.equals(Tree.ECCT)) {
                for (Node<TestData> child : current.getChildren()) {
                    // Since it is a calling context tree, the same labels get
                    // merged, otherwise it would be a call tree:
                    if (label.equals(child.getNodeLabel())) {
                        match = child;
                        break;
                    }
                }
            }
            // if the node does not exist, create it and set its parent
            if (match == null) {
                match = Node.create(new TestData(value, label));
                current.addChild(match);
            }

            // increase the weight
            match.getProfileData().addWeight(value);

            // update current node
            current = match;
        }
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
