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

    public class TestData implements IProfileData {

        private final int fWeight;
        private final String fLabel;

        public TestData(int weight, String label) {
            fWeight = weight;
            fLabel = label;
        }

        public int getWeight() {
            return fWeight;
        }

        public String getLabel() {
            return fLabel;
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
    }

    private Node<TestData> fRoot;

    String[] fLabels = { "A", "B", "C", "D", "E", "F", "G", "H", "I" };
    String[][] fTreeDef = {
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
        for (String label : fLabels) {
            Node<TestData> node = Node.create(new TestData(0, label));
            map.put(label, node);
        }
        fRoot = map.get("F");
        for (String[] item : fTreeDef) {
            NonNullUtils.checkNotNull(map.get(item[0])).addChild(NonNullUtils.checkNotNull(map.get(item[1])));
        }
    }

    @Test
    public void testPrintTree() {
        ArrayList<Node<TestData>> result = new ArrayList<>();
        IProfileVisitor<TestData> visitor = new IProfileVisitor<TestData>() {
            @Override
            public void visit(Node<TestData> node) {
                result.add(node);
            }
        };
        ProfileTraversal.levelOrderTraversal(fRoot, visitor);
        assertEquals(fExpectedLevelorder.length, result.size());
        for (int i = 0; i < fExpectedLevelorder.length; i++) {
            assertEquals(fExpectedLevelorder[i], result.get(i).getProfileData().getLabel());
        }

    }

}
