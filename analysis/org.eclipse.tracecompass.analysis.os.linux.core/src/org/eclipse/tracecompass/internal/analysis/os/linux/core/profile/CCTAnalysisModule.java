package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Iterables;

/**
 * @author frank
 *
 */
public class CCTAnalysisModule extends TmfAbstractAnalysisModule {
    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.os.linux.core.profile.cctanalysis.module"; //$NON-NLS-1$

    private volatile Node<ProfileData> fRoot;
    private Node<ProfileData> treeTest = Node.create(new ProfileData(0, "root"));
    private Node<ProfileData> parent = treeTest;

    /**
     * Default constructor
     */
    public CCTAnalysisModule() {
        super();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        System.out.println("Execute");
        ITmfTrace trace = checkNotNull(getTrace());

        // Node<ProfileData> root = Node.create(new ProfileData(0, "root"));
        RequestTest request = new RequestTest(); // with the active
                                                 // trace
        trace.sendRequest(request); // the method handleData is called for
                                    // each event
        try {
            request.waitForCompletion();
            fRoot = request.getTree();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Node<ProfileData> getTree() {
        return fRoot;
    }

    /**
     * Abstract event request to fill a tree
     */

    private class RequestTest extends TmfEventRequest {

        Node<ProfileData> fNode, fCurrent;
        Stack<Node<ProfileData>> temp;
        GraphvizVisitor dot;

        private ITmfEventField first;

        public RequestTest() {
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);

            temp = new Stack<>();
            fNode = Node.create(new ProfileData(0, "root"));
            fCurrent = fNode;
            dot = new GraphvizVisitor();
        }

        /*
         * the First handleData() test:
         *
         * @Override public void handleData(final ITmfEvent event) { // Just for
         * test, print on the console and add the children System.out.println(
         * "Name: " + event.getName()); System.out.println("Content: " +
         * event.getContent()); //it is empty when is exit }
         *
         */
        @Override
        public void handleData(final ITmfEvent event) {
            // Just for test, print on the console and add to the stack:
            System.out.println(event.getName());

            final String eventName = event.getType().getName();
            long endTime;

            Node<ProfileData> aux;

            ProfileData data;

            Random rand = new Random();

            if (eventName.equals("lttng_ust_cyg_profile:func_entry")) {
                System.out.println(event.getType().getFieldNames());
                first = Iterables.get(event.getContent().getFields(), 0);
                String label = first.toString();

                aux = Node.create(new ProfileData(0, label));

                // put on the tree:
                parent.addChild(aux);
                parent = aux;

                //but if the node is already there, what should we do?
                parent = newaddSample(parent, aux, 1);

                System.out.println("Pushing" + aux);

            } else {
                if (eventName.contains("lttng_ust_cyg_profile:func_exit")) {
                    System.out.println("Creating");
                    endTime = event.getTimestamp().getValue();
                    System.out.println(endTime);

                    //Timestamp:
                    aux = parent;
                    data = aux.fProfileData;
                    data.fWeight += endTime;
                    data.setEndTime(endTime);
                    data.setX(rand.nextInt(100));
                    aux.fProfileData = data;

                    //Update Reference pointer
                    parent = parent.getParent();

                }
            }
            System.out.println("Current " + fCurrent.toString());
        }

        @Override
        public void handleCompleted() {
            Node<ProfileData> aux;
            // Add to the tree:
            while (!temp.isEmpty()) {
                aux = temp.pop();
                fCurrent.addChild(aux);
                fCurrent.fProfileData.fendTime += aux.fProfileData.getEndTime();
                fCurrent = aux;
            }

            System.out.println("Sucess");
            ProfileTraversal.levelOrderTraversal(fNode, dot);
            try {
                dot.print("XMLTest.dot", Mode.LABEL_);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("end");
        }

        public Node<ProfileData> getTree() {
            return fNode;
        }
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        System.out.println("CCTAnalysisModule.canExecute()");
        return true;
    }

    @Override
    protected void canceling() {
    }

    /**
     * This function add a sample in the calling context tree
     */
    public Node<ProfileData> newaddSample(Node<ProfileData> root, Node<ProfileData> sample, int value) //, Tree t)
    {
        Tree t = Tree.ECCT;

        Node<ProfileData> current = root;
        Node<ProfileData> match = null;
        if (t.equals(Tree.ECCT)) {
            for (Node<ProfileData> child : current.getChildren()) {
                // Since it is a calling context tree, the same labels get
                // merged, otherwise it would be a call tree:
                String label = sample.fProfileData.getLabel();
                if (label.equals(child.getNodeLabel())) {
                    match = child;
                    break;
                }
                // if the node does not exist, create it and set its parent
                if (match == null) {
                    match = sample;
                    current.addChild(match);
                }
                // increase the weight
                match.getProfileData().addWeight(value);

                // update current node
                current = match;
            }
        }
        return current;
    }
    /**
     * This function add a sample in the tree, which can be CCT
     */
    public void addSample(Node<ProfileData> root, String[] event, int value, Tree t) {
        // for each stack level

        Node<ProfileData> current = root;
        for (String label : event) {

            System.out.println(label + " " + value);
            Node<ProfileData> match = null;
            if (t.equals(Tree.ECCT)) {
                for (Node<ProfileData> child : current.getChildren()) {
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
                match = Node.create(new ProfileData(value, label));
                current.addChild(match);
            }

            // increase the weight
            match.getProfileData().addWeight(value);

            // update current node
            current = match;
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
            System.out.println(current);
        }

    }

    // Which kind of tree:
    public enum Tree {
        ECCT, DCT, CG;
        // Calling context tree, dynamic call tree, call graph
    }

    // Mode for print
    public enum Mode {
        ID_, LABEL_, COLOR_;
    }

    @Override
    public String toString() {
        return "CCT Analysis Module";
    }

    /**
     * @author frank This class implements IProfileData to be implemented on the
     *         tests related with Profiling and ECCT
     */

    public class GraphvizVisitor implements IProfileVisitor<ProfileData> {
        /**
         * result ArrayList of Nodes, which are ProfileData
         */
        public ArrayList<Node<ProfileData>> result = new ArrayList<>();

        @Override
        public void visit(Node<ProfileData> node) {
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
            if (mode != Mode.COLOR_) {
                if (mode != Mode.ID_) {
                    // Edges and nodes:
                    for (Node<ProfileData> n : result) {
                        if (n.getParent() != null) {
                            System.out.print(n.getNodeLabel() + " -> " + n.getParent().getNodeLabel() + "; \n");
                            content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                        } else {
                            System.out.print(n.getNodeLabel() + "; \n");
                            content = content.concat(n.getNodeLabel() + "; \n");
                        }
                    }
                    for (Node<ProfileData> n : result) {
                        content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                    }
                } else {
                    // Edges and nodes:
                    for (Node<ProfileData> n : result) {
                        if (n.getParent() != null) {
                            System.out.print(n.getNodeId() + " -> " + n.getParent().getNodeId() + "; \n");
                            content = content.concat(n.getParent().getNodeId() + " -> " + n.getNodeId() + "; \n");
                        } else {
                            System.out.print(n.getNodeId() + "; \n");
                            content = content.concat(n.getNodeId() + "; \n");
                        }
                    }
                    for (Node<ProfileData> n : result) {
                        content = content.concat(n.getNodeId() + " " + "[label = \"" + n.getNodeId() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                    }
                }
            } else {
                for (Node<ProfileData> n : result) {
                    if (n.getParent() != null) {
                        System.out.print(n.getNodeLabel() + " -> " + n.getParent().getNodeLabel() + "; \n");
                        content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                    } else {
                        System.out.print(n.getNodeLabel() + "; \n");
                        content = content.concat(n.getNodeLabel() + "; \n");
                    }
                }
                for (Node<ProfileData> n : result) {
                    content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData() + "]\" ]; \n"); // tirei
                                                                                                                                                  // o
                                                                                                                                                  // color
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
                String fileName = new String("/tmp/"); //$NON-NLS-1$
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

    protected class TestData implements IProfileData {

        private String fLabel;
        private int fWeight;

        // Constructor:
        public TestData(int weight, String label) {
            if (weight == 0) {
                fWeight = 0;
            } else {
                fWeight = weight;
            }
            fLabel = label;
        }

        // Constructor:
        public TestData(long weight, String label) {
            fWeight = (int) weight;
            fLabel = label;
        }

        // Add to the weight:
        public void addWeight(int value) {
            fWeight += value;
        }

        @Override
        public void merge(IProfileData other) {
            // TODO Auto-generated method stub

        }

        @Override
        public IProfileData minus(IProfileData other) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean equals(IProfileData other) {
            if (!(other instanceof TestData)) {
                throw new IllegalArgumentException("wrong type for minus operation");
            }
            TestData data = (TestData) other;
            if (fLabel.equals(data.getLabel())) {
                if (fWeight == data.getWeight()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getLabel() {
            return fLabel;
        }

        @Override
        public int getWeight() {
            return fWeight;
        }

        @Override
        public String toString() {
            return new String(fWeight + " " + fLabel);
        }
    }

}
