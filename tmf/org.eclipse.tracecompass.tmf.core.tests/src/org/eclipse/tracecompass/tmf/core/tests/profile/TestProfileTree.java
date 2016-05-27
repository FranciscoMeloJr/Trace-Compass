package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.junit.Assert.*;

import java.util.ArrayList;
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
            return fLabel;
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

    @Before
    public void setup() {
        Map<String, Node<TestData>> map = new HashMap<>();
        for (String label : fLabels1) {
            Node<TestData> node = Node.create(new TestData(0, label));
            map.put(label, node);
        }
        fRoot = map.get("F");
        for (String[] item : fTreeDef1) {
            NonNullUtils.checkNotNull(map.get(item[0])).addChild(NonNullUtils.checkNotNull(map.get(item[1])));
        }

        Map<String, Node<TestData>> map2 = new HashMap<>();
        for (String label : fLabels2) {
            Node<TestData> node = Node.create(new TestData(0, label));
            map2.put(label, node);
        }
        fRoot2 = map2.get("F");
        for (String[] item : fTreeDef1) {
            NonNullUtils.checkNotNull(map2.get(item[0])).addChild(NonNullUtils.checkNotNull(map2.get(item[1])));
        }
    }

    /**
     *
     */
    @Test
    public void testPrintTree() {

        Visitor visitor = new Visitor();
        Visitor visitor2 = new Visitor();
        //Create the first tree:
        ProfileTraversal.levelOrderTraversal(fRoot, visitor);
        //Create the second tree:
        ProfileTraversal.levelOrderTraversal(fRoot2, visitor2);

        //Asset for Equals
        assertEquals(fExpectedLevelorder.length, visitor.result.size());
        for (int i = 0; i < fExpectedLevelorder.length; i++) {
            assertEquals(fExpectedLevelorder[i], visitor.result.get(i).getProfileData().getLabel());
        }

    }

}
