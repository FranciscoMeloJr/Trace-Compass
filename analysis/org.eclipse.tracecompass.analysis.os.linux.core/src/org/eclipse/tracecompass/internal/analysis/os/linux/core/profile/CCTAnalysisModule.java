package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal.KeyTree;
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
 * @author francisco
 *
 */
public class CCTAnalysisModule extends TmfAbstractAnalysisModule {
    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.os.linux.core.profile.cctanalysis.module"; //$NON-NLS-1$

    // ArrayList of ECCTs, which are delimited by static implementation
    private static ArrayList<Node<ProfileData>> ArrayECCTs = new ArrayList<>();
    private static LinkedHashMap<KeyTree, Node<ProfileData>> hashECCTs[] = null;

    Node<ProfileData> aux = null;
    Node<ProfileData> fRoot = Node.create(new ProfileData(0, "root"));
    ArrayList<Node<ProfileData>> fRoots = new ArrayList<>();
    Node<ProfileData> parent = fRoot;
    String Sdelimiter = new String("interval:tracepoint");
    String fEntry = new String("lttng_ust_cyg_profile:func_entry");
    String fExit = new String("lttng_ust_cyg_profile:func_exit");
    long fGap; // 11578599;
    static boolean diff = false;

    // This tree is the differential part:
    static LinkedHashMap<KeyTree, Node<ProfileData>> treeDif;

    static ArrayList<Integer> numberLevels = new ArrayList<>();

    /**
     * Default constructor
     */
    public CCTAnalysisModule() {
        super();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = checkNotNull(getTrace());

        RequestTest request = new RequestTest();
        trace.sendRequest(request);
        try {
            request.waitForCompletion();
            request.getArrayTree();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Return an array of the trees
    public Node<ProfileData> getTree() {
        return fRoot;
    }

    // Return an array of the trees
    public ArrayList<Node<ProfileData>> getArrayTree() {
        return ArrayECCTs;
    }

    /**
     * Abstract event request to fill a tree
     */

    private class RequestTest extends TmfEventRequest {

        Node<ProfileData> fNode;
        GraphvizVisitor dot;

        private ITmfEventField first;

        public RequestTest() {
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);

            fNode = Node.create(new ProfileData(0, "root"));
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

            final String eventName = event.getType().getName();
            ProfileData data;
            Random rand = new Random();
            System.out.println(eventName);
            // This is used for delimiting the tree:
            if (eventName.equals(Sdelimiter)) {

                ITmfEventField content = event.getContent();
                for (ITmfEventField field : content.getFields()) {
                    if (field.getValue().equals("begin")) {
                        fNode = Node.create(new ProfileData(0, "root"));
                        parent = fNode;
                    }
                    if (field.getValue().equals("end")) {
                        ArrayECCTs.add(fNode);
                        parent = null;
                    }
                }
            }

            if (eventName.equals(fEntry)) {
                first = Iterables.get(event.getContent().getFields(), 0);
                String label = first.toString();
                Long start = event.getTimestamp().getValue();
                // RandomNumber
                int n = rand.nextInt(100) + 1;
                aux = Node.create(new ProfileData(0, label, start, null, n));

                // put as a children on a call graph:
                if (parent != null) {
                    parent.addChild(aux);
                    parent = aux;
                }

            } else {
                if (eventName.contains(fExit)) {
                    first = Iterables.get(event.getContent().getFields(), 0);
                    String label = first.toString();
                    System.out.println(label);
                    if (parent == null) {
                        System.out.println("Parent null"); //$NON-NLS-1$
                        fNode = Node.create(new ProfileData(0, "root"));
                        parent = fNode;
                    }
                    data = parent.fProfileData;
                    Long end = event.getTimestamp().getValue();
                    data.setEndTime(end);
                    long duration = data.getEndTime() - data.getStartTime();
                    // System.out.println(label + " duration" + duration);
                    data.setDuration(duration);
                    parent.setProfileData(data);
                    if (parent.getParent() != null) {
                        parent = parent.getParent();
                    }
                }
            }

        }

        @Override
        public void handleCompleted() {

            System.out.println("Sucess");
            ProfileTraversal.levelOrderTraversal(fRoot, dot);
            // Tracepoint mechanism:
            if (ArrayECCTs.size() == 0) {
                ArrayECCTs.add(fRoot);
                hashECCTs = new LinkedHashMap[ArrayECCTs.size()];
            } else {
                // Array:
                if (diff) {
                    hashECCTs = new LinkedHashMap[ArrayECCTs.size() + 1];
                } else {
                    hashECCTs = new LinkedHashMap[ArrayECCTs.size()];
                }
            }
            int i;
            for (i = 0; i < ArrayECCTs.size(); i++) {
                hashECCTs[i] = createHash(ArrayECCTs.get(i));
            }

            // Make the differential with a random tree:
            if (diff == true) {
                if (ArrayECCTs.size() > 1) {
                    if (treeDif == null) {
                        Random rn = new Random();
                        int a = rn.nextInt(9);
                        int b = rn.nextInt(9);
                        diffTrees(hashECCTs[a], hashECCTs[b]);
                        hashECCTs[ArrayECCTs.size()] = treeDif; // put the tree
                                                                // on
                                                                // the last size
                    }
                }
            }
            organizeRoot();
            organizeStartEnd();
            // organizeGaps();
        }

        // This function returns the fRoot
        public Node<ProfileData> getTree() {
            return fRoot;
        }

        // This function returns the fRoots - ArrayList of fRoots;
        public ArrayList<Node<ProfileData>> getArrayTree() {
            return ArrayECCTs;
        }

        // This function method runs through the nodes of the root and calculate
        // the duration
        public <T extends IProfileData> void organizeRoot() {

            int size = ArrayECCTs.size();
            Node<ProfileData> tempRoot = null;

            ProfileData profileData;
            long duration = 0;

            for (int i = 0; i < size; i++) {
                tempRoot = ArrayECCTs.get(i);
                duration = 0;
                for (Node<T> node : tempRoot.getChildren()) {
                    profileData = (ProfileData) node.fProfileData;
                    duration += profileData.getDuration();
                }
                profileData = tempRoot.getProfileData();
                profileData.setDuration(duration);
                tempRoot.setProfileData(profileData);
                System.out.println("Duration root " + duration);
            }
            fGap = duration / 10;
        }

        // This function will organize the tree with gaps
        public int childrenGaps(Node<ProfileData> root) {

            // for each node, count the number of children and put the gaps
            // accordingly:

            LinkedList<Node<ProfileData>> queue = new LinkedList<>();
            int numberChildren = 0;
            queue.add(root);
            System.out.println("child");
            while (!queue.isEmpty()) {
                Node<ProfileData> current = queue.poll();
                numberChildren = 0;
                for (Node<ProfileData> child : current.getChildren()) {
                    queue.add(child);
                    numberChildren++;
                    System.out.println(child);
                }
                return numberChildren;
            }
            return numberChildren;
        }

        // This function method runs through the nodes of the root and calculate
        // the duration
        public void organizeStartEnd() {
            try {
                System.out.println("organizeStartEnd");
                int length = hashECCTs.length;
                LinkedHashMap<KeyTree, Node<ProfileData>> temp = null;

                LinkedHashMap<Long, Node<ProfileData>> tempH = null;

                // by level
                ArrayList<Long> listInit;
                Node<ProfileData> parent = null;

                long newDuration;
                int level;
                long start, duration, end;

                for (int i = 0; i < length; i++) {
                    temp = hashECCTs[i];

                    for (KeyTree key : temp.keySet()) {

                        Node eachNode = temp.get(key);
                        ProfileData pd = (ProfileData) eachNode.getProfileData();
                        level = key.getLevel();

                        parent = eachNode.getParent();
                        if (parent != null) {
                            start = parent.getPointer();
                            eachNode.setPointer(start);
                        } else {
                            start = 0;
                        }

                        int nchildren = childrenGaps(eachNode);
                        if (nchildren > 0) {
                            newDuration = pd.getDuration() + (fGap * (nchildren - 1));
                        } else {
                            newDuration = pd.getDuration() + (fGap * (nchildren));
                        }
                        end = start + newDuration + fGap;
                        // updates:
                        pd.setStartTime(start);
                        pd.setEndTime(end);

                        // updates:
                        if (parent != null) {
                            parent.setPointer(end);
                        }
                        eachNode.setProfileData(pd);
                        eachNode.setDur(newDuration);
                        // printf:
                        if (eachNode.getParent() != null) {
                            System.out.println("Node: " + eachNode.toString() + " new duration " + eachNode.getDur() + " duration " + pd.getDuration() + " level " + level + " start: " + pd.getStartTime() + " end: " + pd.getEndTime() + " Parent: "
                                    + eachNode.getParent().getNodeId() + " Pointer " + eachNode.getParent().getPointer()+ "number of children " + nchildren);
                        } else {
                            System.out.println("Node: " + eachNode.toString() + " duration " + pd.getDuration() + " level " + level + " start: " + pd.getStartTime() + " end: " + pd.getEndTime() + " number of children" + nchildren);
                        }
                    }

                }
            } catch (Exception ex) {
                System.out.println("Exception in organizeStartEnd");
            }
        }
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        System.out.println("CCTAnalysisModule is ready");
        return true;
    }

    @Override
    protected void canceling() {
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
     * This function creates a HashMap of <level x label> x Node
     *
     * @param root
     *            a tree first node to be traversed to create the hash
     *
     * @return the map of the tree using keyTree
     */
    public static LinkedHashMap<KeyTree, Node<ProfileData>> createHash(Node<ProfileData> root) {

        Node<ProfileData> current = null;
        Node<ProfileData> pointerParent = null;

        // Linked list
        LinkedHashMap<KeyTree, Node<ProfileData>> hmapZ = new LinkedHashMap<>();
        LinkedList<Node<ProfileData>> queue = new LinkedList<>();
        ArrayList<Node<ProfileData>> nodes = new ArrayList<>();

        int level = 0;

        queue.add(root);
        while (!queue.isEmpty()) {
            current = queue.poll();
            level = 0;
            pointerParent = current.getParent();
            if (pointerParent != null) {
                while (pointerParent != null) {
                    pointerParent = pointerParent.getParent();
                    level++;
                }
            }
            System.out.println("current:" + current + " level " + level + " parent " + current.getParent());
            String label = current.getNodeLabel();
            KeyTree aux1 = new KeyTree(label, level);

            // debug:
            nodes.add(current);

            if (hmapZ.containsKey(aux1)) {
                Node<ProfileData> temp = hmapZ.get(aux1);
                if (temp != null) {
                    System.out.println("merging" + temp + " " + current);
                    temp.mergeNode(current);
                    hmapZ.put(aux1, temp);
                }
            } else {
                hmapZ.put(aux1, current);

            }
            for (Node<ProfileData> child : current.getChildren()) {
                queue.add(child);
            }
            current = null;
        }

        System.out.println("Size: " + nodes.size());
        System.out.println("Hmap: " + hmapZ.size());

        for (KeyTree key : hmapZ.keySet()) {
            @Nullable
            Node<ProfileData> nodex = hmapZ.get(key);
            System.out.println("level " + key.getLevel()+ nodex);
        }
        numberLevels.add(level);
        return hmapZ;// hmap;
    }

    /**
     * This function creates a meanTree with the array of Trees
     *
     * @param root1:
     *            tree for comparison 1
     *
     * @return the resulting is the mean of all them
     */
    public static LinkedHashMap<KeyTree, Node<ProfileData>> meanTree() {

        int size = hashECCTs.length;
        LinkedHashMap<KeyTree, Node<ProfileData>> temp = hashECCTs[0];
        LinkedHashMap<KeyTree, Node<ProfileData>> result = new LinkedHashMap<>();
        ProfileData data;

        Node<ProfileData> initial = null;
        int max = 0;
        Node<ProfileData> value, copy = null;
        for (KeyTree key : temp.keySet()) {
            value = temp.get(key);
            if (value != null) {
                Long duration = value.getProfileData().getDuration();
                duration /= size;
                copy = Node.create(value.getProfileData());
                copy.getProfileData().setDuration(duration);
                result.put(key, copy);
            }
        }

        for (int i = 1; i < size; i++) {
            temp = hashECCTs[i];
            for (KeyTree key : temp.keySet()) {
                value = temp.get(key);
                if (value != null) {
                    initial = result.get(key);
                    if (initial != null) {
                        Long duration = value.getProfileData().getDuration();
                        duration /= size;
                        initial.getProfileData().addDuration(duration);
                        result.put(key, initial);
                    }
                }
            }
        }
        return result;

    }

    /**
     * This function makes the difference of two trees by the differences of
     * their hashMaps, by using the operation minus
     *
     * @param root1:
     *            tree for comparison 1
     * @param root2:
     *            tree for comparison 2
     *
     * @return the resulting is the hash of the difference
     */
    public static LinkedHashMap<KeyTree, Node<ProfileData>> diffTrees(LinkedHashMap<KeyTree, Node<ProfileData>> root1, LinkedHashMap<KeyTree, Node<ProfileData>> root2) {

        LinkedHashMap<KeyTree, Node<ProfileData>> result = new LinkedHashMap<>();

        int max = 0;
        diff = true;
        Node<ProfileData> value, copy = null;
        for (KeyTree key : root1.keySet()) {
            value = root1.get(key);
            if (value != null) {
                copy = Node.create(value.getProfileData());
                if ((root2.get(key) != null) && (copy != null)) {
                    Node<ProfileData> compare = root2.get(key);
                    copy.diff(compare);
                }
                if (key.getLevel() > max) {
                    max = key.getLevel();
                }

                result.put(key, copy);
            }
        }

        // necessary to show the difference, as the last tree:
        numberLevels.add(max);
        treeDif = result;
        hashECCTs[ArrayECCTs.size()] = treeDif;
        return result;
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
     * @author francisco This class implements IProfileData to be implemented on
     *         the tests related with Profiling and ECCT
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

            String content = new String("digraph G { \n");
            if (mode != Mode.COLOR_) {
                if (mode != Mode.ID_) {
                    // Edges and nodes:
                    for (Node<ProfileData> n : result) {
                        if (n.getParent() != null) {
                            // System.out.print(n.getNodeLabel() + " -> " +
                            // n.getParent().getNodeLabel() + "; \n");
                            content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                        } else {
                            // System.out.print(n.getNodeLabel() + "; \n");
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
                            // System.out.print(n.getNodeId() + " -> " +
                            // n.getParent().getNodeId() + "; \n");
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
                        // System.out.print(n.getNodeLabel() + " -> " +
                        // n.getParent().getNodeLabel() + "; \n");
                        content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                    } else {
                        // System.out.print(n.getNodeLabel() + "; \n");
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
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getNumberLevels() {
        return numberLevels.get(0);
    }

    public ArrayList<Integer> getNumberLevelsEach() {
        return numberLevels;

    }

    // This function returns the fRoots - ArrayList of fRoots;
    public LinkedHashMap<KeyTree, Node<ProfileData>>[] getArrayECCTs() {
        return hashECCTs;
    }

    public static void calculateDiff() {

    }
}
