package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.junit.Assert.*;

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

public class TestProfileTree {
    /**
     * @author frank
     *
     */
    public class Visitor implements IProfileVisitor<TestData> {
        /**
         * result ArrayList of Nodes, which TestData
         */
        public ArrayList<Node<TestData>> result = new ArrayList<>();

        @Override
        public void visit(Node<TestData> node) {
            result.add(node);
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
    String[] fExpectedPreorder = { "F", "B", "A", "D", "C", "E", "G", "I", "H" };
    String[] fExpectedLevelorder = { "F", "B", "G", "A", "D", "I", "C", "E", "H" };

    private Node<TestData> makeTree(String rootLabel, String[] labels, String[][] defs) {
        Map<String, Node<TestData>> map = new HashMap<>();
        for (String label : labels) {
            Node<TestData> node = Node.create(new TestData(0, label));
            map.put(label, node);
        }
        Node<TestData> root = map.get(rootLabel);
        for (String[] item : defs) {
            NonNullUtils.checkNotNull(map.get(item[0])).addChild(NonNullUtils.checkNotNull(map.get(item[1])));
        }
        return root;
    }

    @Before
    public void setup() {
        fRoot = makeTree("F", fLabels1, fTreeDef1);
        fRoot2 = makeTree("F", fLabels2, fTreeDef2);
    }

    /**
     * This is a Junit test for Printing the tree
     */
    @Test
    public void testPrintTree() {

        Visitor visitor = new Visitor();
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

        Node<TestData> a = ProfileTraversal.Copy2(fRoot);
        System.out.println(a);
        System.out.println(ProfileTraversal.levelOrderTraversal(a));
    }
    /**
     * This is a JUnit test for list comparison
     */
    @Test
    public void testComparisonOfLists() {

        Comparator<Node<TestData>> cmp = new Comparator<Node<TestData>>() {
            @Override
            public int compare(Node<TestData> arg0, Node<TestData> arg1) {
                return arg0.getProfileData().getLabel().compareTo(arg1.getProfileData().getLabel());
            }
        };

        ArrayList<Node<TestData>> lst1 = new ArrayList<>();
        ArrayList<Node<TestData>> lst2 = new ArrayList<>();

        //Add note:
        lst1.add(Node.create(new TestData(10, "C")));
        lst1.add(Node.create(new TestData(10, "B")));
        lst1.add(Node.create(new TestData(10, "A")));

        lst2.add(Node.create(new TestData(20, "C")));
        lst2.add(Node.create(new TestData(20, "B")));
        lst2.add(Node.create(new TestData(20, "D")));
        lst2.add(Node.create(new TestData(20, "E")));
        lst2.add(Node.create(new TestData(20, "F")));
        lst2.add(Node.create(new TestData(20, "G")));

        //Sort first:
        lst1.sort(cmp);
        lst2.sort(cmp);

        int i1 = 0, i2 = 0;

        ArrayList<Node<TestData>> result = new ArrayList<>();

        //Comparison algorithm:
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
