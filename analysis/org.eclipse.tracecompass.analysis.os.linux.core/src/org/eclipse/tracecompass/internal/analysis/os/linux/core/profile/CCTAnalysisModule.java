package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.eclipse.tracecompass.tmf.core.statistics.ITmfStatistics;
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
    private static ArrayList<Node<ProfileData>> ArrayECCTs;
    private static LinkedHashMap<KeyTree, Node<ProfileData>> hashECCTs[];

    Node<ProfileData> aux = null;
    Node<ProfileData> fRoot = Node.create(new ProfileData(0, "root"));
    ArrayList<Node<ProfileData>> fRoots = new ArrayList<>();
    Node<ProfileData> parent = fRoot;
    String Sdelimiter = new String("interval:tracepoint");
    static String fEntry = new String("lttng_ust_cyg_profile:func_entry");
    static String fExit = new String("lttng_ust_cyg_profile:func_exit");
    long fGap;
    static boolean diff;

    // This tree is the differential part:
    static LinkedHashMap<KeyTree, Node<ProfileData>> treeDif;
    static ArrayList<Integer> numberLevels = new ArrayList<>();
    static testStatistics statistics;
    static int threshold = 10;

    /**
     * Default constructor
     */
    public CCTAnalysisModule() {
        super();
        ArrayECCTs = new ArrayList<>();
        hashECCTs = null;
        diff = false;
        statistics = null;
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
            System.out.println(fEntry + " " + fExit);
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
                        diffTrees(hashECCTs[a], hashECCTs[b], threshold);
                        // put the tree on the last size
                        hashECCTs[ArrayECCTs.size()] = treeDif;
                    }
                }
            }

            ArrayList<Long> arrayDuration = organizeStartEnd();
            organizeRoot();

            // create statistics:
            createStatistics(arrayDuration);
        }

        // This function returns the fRoot
        public Node<ProfileData> getTree() {
            return fRoot;
        }

        // This function returns the fRoots - ArrayList of fRoots;
        public ArrayList<Node<ProfileData>> getArrayTree() {
            return ArrayECCTs;
        }

        // This function creates the statistic
        public void createStatistics(ArrayList<Long> aux1) {
            statistics = new testStatistics(aux1);
        }

        // This function method runs through the nodes of the root and calculate
        // the duration
        public <T extends IProfileData> void organizeRoot() {

            int size = ArrayECCTs.size();
            Node<ProfileData> tempRoot = null;

            ProfileData profileData;
            long duration = 0;
            long dur = 0;

            for (int i = 0; i < size; i++) {
                tempRoot = ArrayECCTs.get(i);
                duration = 0;
                dur = 0;
                for (Node<T> node : tempRoot.getChildren()) {
                    profileData = (ProfileData) node.fProfileData;
                    duration += profileData.getDuration();
                    dur += node.getDur();
                }
                profileData = tempRoot.getProfileData();
                profileData.setDuration(duration);
                tempRoot.setProfileData(profileData);
                tempRoot.setDur(dur);
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
        public ArrayList<Long> organizeStartEnd() {

            System.out.println("organizeStartEnd");
            int length = hashECCTs.length;
            LinkedHashMap<KeyTree, Node<ProfileData>> temp = null;
            ArrayList<Long> durationSaved = new ArrayList<>();
            Node<ProfileData> parent1 = null;

            long newDuration;
            int level;
            long start, duration, end;
            try {
                for (int i = 0; i < length; i++) {
                    temp = hashECCTs[i];
                    duration = 0;
                    for (KeyTree key : temp.keySet()) {

                        Node eachNode = temp.get(key);
                        ProfileData data = (ProfileData) eachNode.getProfileData();
                        level = key.getLevel();

                        parent1 = eachNode.getParent();
                        if (parent1 != null) {
                            start = parent1.getPointer();
                            eachNode.setPointer(start);
                        } else {
                            start = 0;
                        }

                        int nchildren = childrenGaps(eachNode);
                        if (nchildren > 0) {
                            newDuration = data.getDuration();// + (fGap *
                                                             // (nchildren -
                                                             // 1));
                        } else {
                            newDuration = data.getDuration(); // + (fGap *
                                                              // (nchildren));
                        }
                        end = data.getDuration() + fGap;
                        // updates:
                        data.setStartTime(start);
                        data.setEndTime(end);

                        // updates:
                        if (parent1 != null) {
                            parent1.setPointer(end);
                        }
                        eachNode.setProfileData(data);
                        eachNode.setDur(newDuration);
                        duration += data.getDuration();
                        // printf:
                        if (eachNode.getParent() != null) {
                            System.out.println(
                                    "Node: " + eachNode.toString() + " new duration " + eachNode.getDur() + " duration " + data.getDuration() + " level " + level + " start: " + data.getStartTime() + " end: " + data.getEndTime() + " Parent: "
                                            + eachNode.getParent().getNodeId() + " Pointer " + eachNode.getParent().getPointer() + "number of children " + nchildren);
                        } else {
                            System.out.println("Node: " + eachNode.toString() + " duration " + data.getDuration() + " level " + level + " start: " + data.getStartTime() + " end: " + data.getEndTime() + " number of children" + nchildren);
                        }
                    }
                    durationSaved.add(duration);

                }
            } catch (Exception ex) {
                System.out.println("Exception in organizeStartEnd");
            }
            return durationSaved;
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
            if (current.getParent() != null) {
                KeyTree auxP = new KeyTree(label, level, current.getParent().getNodeLabel());
            }
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
            System.out.println("level " + key.getLevel() + nodex);
        }
        numberLevels.add(level);
        return hmapZ;// hmap;
    }

    /**
     * This function merge similar trees
     *
     * @param root1:
     *            tree for merging 1
     *
     * @param root2:
     *            tree for merging 2
     * @return the resulting tree
     */
    public static LinkedHashMap<KeyTree, Node<ProfileData>> mergeSimilarTree(LinkedHashMap<KeyTree, Node<ProfileData>> hmap1, LinkedHashMap<KeyTree, Node<ProfileData>> hmap2) {

        LinkedHashMap<KeyTree, Node<ProfileData>> result = new LinkedHashMap<>();

        if (hmap1 != null && hmap2 != null) {
            for (KeyTree key : hmap1.keySet()) {
                @Nullable
                Node<ProfileData> nodex = hmap1.get(key);
                Node<ProfileData> nodey = hmap2.get(key);
                if (nodex != null && nodex.getProfileData() != null) {
                    ProfileData data = nodex.fProfileData;
                    if (nodey.getProfileData() != null) {
                        data.setDuration((data.getDuration() + nodey.getProfileData().getDuration()) / 2);
                    }
                    nodex.setProfileData(data);
                    Node<ProfileData> copy = nodex.copy();
                    copy.setProfileData(data);
                    result.put(key, nodex);
                }

            }
        }
        return result;
    }

    /**
     * This function merge Similar trees on the hash, using mergeSimilarTrees
     *
     * @param: none:
     *             because the parameter is the current hash of trees
     *
     * @return void: because the hash is updated
     */
    public static void mergeTrees() {
        // run through the hashmap:
        // private static LinkedHashMap<KeyTree, Node<ProfileData>> hashECCTs[];
        int i = 0;
        LinkedHashMap<KeyTree, Node<ProfileData>> result = hashECCTs[i];
        LinkedHashMap<KeyTree, Node<ProfileData>> temp, finalResult;

        for (int j = 1; j < hashECCTs.length; j++) {
            if (hashECCTs[j] != null) {
                temp = hashECCTs[j];
                result = mergeSimilarTree(result, temp);
            }
        }
        finalResult = result;

        hashECCTs = null;
        hashECCTs = new LinkedHashMap[1];
        hashECCTs[0] = finalResult;
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

        Node<ProfileData> initial = null;
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
    public static LinkedHashMap<KeyTree, Node<ProfileData>> diffTrees(LinkedHashMap<KeyTree, Node<ProfileData>> root1, LinkedHashMap<KeyTree, Node<ProfileData>> root2, int newTh) {

        LinkedHashMap<KeyTree, Node<ProfileData>> result = new LinkedHashMap<>();

        int max = 0;
        diff = true;
        Node<ProfileData> value, copy = null;
        // differential:
        for (KeyTree key : root1.keySet()) {
            value = root1.get(key);
            if (value != null) {
                copy = Node.create(value.getProfileData());
                copy.setDur(value.getDur());
                if ((root2.get(key) != null)) {
                    Node<ProfileData> compare = root2.get(key);
                    System.out.println("newTh" + newTh);
                    copy.diff(compare, newTh);
                }
                if (key.getLevel() > max) {
                    max = key.getLevel();
                }

                result.put(key, copy);
            }
        }

        // necessary to show the difference, adding as the last tree:
        numberLevels.add(max);
        treeDif = result;
        if (diff) {
            LinkedHashMap[] temp = new LinkedHashMap[ArrayECCTs.size() + 1];
            for (int i = 0; i < ArrayECCTs.size(); i++) {
                temp[i] = hashECCTs[i];
            }
            temp[ArrayECCTs.size()] = treeDif;
            hashECCTs = temp;
        }
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
    public LinkedHashMap<KeyTree, Node<ProfileData>>[] getHashECCTs() {
        return hashECCTs;
    }

    // This is a test for statistics generation for histogram1
    public ITmfStatistics getStatistics() {

        return statistics;
    }

    public class testStatistics implements ITmfStatistics {

        ArrayList<Long> arrayList;

        testStatistics(ArrayList<Long> array) {
            arrayList = array;
        }

        // returns the arrayList
        @Override
        public List<Long> histogramQuery(long start, long end, int nb) {

            return arrayList;
        }

        @Override
        public long getEventsTotal() {
            long nb = arrayList.size();
            return nb;
        }

        @Override
        public Map<@NonNull String, @NonNull Long> getEventTypesTotal() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getEventsInRange(long start, long end) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Map<String, Long> getEventTypesInRange(long start, long end) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void dispose() {
            // TODO Auto-generated method stub

        }

        public int getSize() {
            return arrayList.size();
        }

    }

    private static void setBeginAndEntry(String begin, String end) {
        fEntry = begin;
        fExit = end;
    }

    // This function do again the analysis with different parameters
    public void setParameters(String entry, String exit) {
        if (entry != null && exit != null) {
            setBeginAndEntry(entry, exit);
        }

        super.resetAnalysis();
    }

    public static void classificationTest() {

        int tam = 100;
        int rn = 0;
        int max = 100;
        int min = 0;
        int sum = 0;
        double item;
        double mean = 0;
        double SDAM = 0;

        ArrayList<Integer> arrayTest1 = new ArrayList<>();
        ArrayList<Integer> arrayTest2 = new ArrayList<>();
        ArrayList<Integer> arrayTotal = new ArrayList<>();
        /*
         * for (int i = 0; i < tam; i++) { Random rand = new Random(); rn =
         * rand.nextInt(max - min + 1) + min; array.add(rn); }
         */

        /*
         * Collections.sort(array); int index; index = 0; while (index <
         * arrayTest1.size()) { item = array.get(index); SDAM += (item - mean) *
         * (item - mean); index++; }
         *
         * // JNB int x[]; // for(int j = 1; j<=10; j++){ int j = 10;
         */
        Gaussian(arrayTest1, 10, 100, 3);
        Gaussian(arrayTest2, 10, 10, 3);

        //
        // getJenksBreaks(array, j);

        arrayTotal = arrayTest1;
        arrayTotal.addAll(arrayTest2);
        Collections.shuffle(arrayTotal);

        System.out.print("Total");
        print(arrayTotal);

        variationClassification(arrayTotal);

    }

    public static void variationClassification(ArrayList<Integer> array) {

        // calculate the mean:
        int index = 0;
        int sumSq = 0;
        double meanSq;
        double tolerance = 10;
        ArrayList<Double> meanDistance = new ArrayList<>();
        ArrayList<Integer> resultArray = new ArrayList<>();
        index = 0;

        // First sort:
        Collections.sort(array);

        try {
            // result will be in groups:
            ArrayList<ArrayList<Integer>> groups = new ArrayList<>();

            while (index < array.size()) {
                sumSq += array.get(index) * array.get(index);
                index++;
            }

            meanSq = sumSq / array.size();

            // Variation (mean - sqr(value))
            index = 0;
            double result;
            while (index < array.size()) {
                result = (array.get(index) * array.get(index)) - meanSq;
                meanDistance.add(result);
                index++;
            }

            index = 0;
            double meanNumber;
            Double comparative = null;
            Double last = null;
            System.out.println("start:");
            index = 1;
            while (index < meanDistance.size()) {
                Double number1 = meanDistance.get(index - 1);
                Double number2 = meanDistance.get(index);
                Double total = (number1 - number2);
                System.out.println(number1 + " " + number2 + " " + total);

                if (Math.abs(total) < 600) {
                    System.out.println("in");
                    resultArray.add(array.get(index));
                } else {
                    System.out.println("out");
                    resultArray.add(9999);
                    resultArray.add(array.get(index));
                }
                index ++;
            }
            resultArray.add(9999);

            // System.out.println(" \n Result");
            ArrayList<Integer> temp = new ArrayList<>();
            for (int j = 0; j < resultArray.size(); j++) {
                if (resultArray.get(j) == 9999) {
                    System.out.println(" xxx ");
                    groups.add(temp);
                    temp = new ArrayList<>();
                } else {
                    // System.out.print(resultArray.get(j) + " ");
                    temp.add(resultArray.get(j));
                }
            }
            if (groups.size() > 1) {
                System.out.println("Final Size" + groups.size() + " " + groups.get(0).size() + " " + groups.get(1).size());
            }

        } catch (Exception ex) {
            System.out.println("exception");
        }
    }

    // Insertion standard deviation, this function goes through the array
    public static int standardGroupInsertion(ArrayList<Integer> list) {

        // compare the standard deviation and insert in the array.
        double mean = 0;
        double arraySTD = 0;
        int tolerance = 10;

        ArrayList<Integer> temp = new ArrayList<>();
        ArrayList<ArrayList<Integer>> groups = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            if ((arraySTD + mean + tolerance) < list.get(i)) {
                groups.add(temp);
                temp = new ArrayList<>();
                temp.add(list.get(i));
                mean = calculateMean(temp);
                arraySTD = mean - list.get(i);
                arraySTD *= arraySTD;

            } else {
                temp.add(list.get(i));
                mean = list.get(i);
                arraySTD = 0;
            }
        }

        return groups.size();
    }

    private static double calculateMean(ArrayList<Integer> temp) {

        double sum = 0;
        double mean = 0;
        for (int i = 0; i < temp.size(); i++) {
            sum += temp.get(i);
        }
        mean = sum / temp.size();
        return mean;
    }

    // JNB
    /**
     * @return int[]
     * @param list
     *            com.sun.java.util.collections.ArrayList
     * @param numclass
     *            int
     */
    public static int[] getJenksBreaks(ArrayList<Integer> list, int numclass) {

        // int numclass;
        int numdata = list.size();

        double[][] mat1 = new double[numdata + 1][numclass + 1];
        double[][] mat2 = new double[numdata + 1][numclass + 1];
        double[] st = new double[numdata];

        for (int i = 1; i <= numclass; i++) {
            mat1[1][i] = 1;
            mat2[1][i] = 0;
            for (int j = 2; j <= numdata; j++) {
                mat2[j][i] = Double.MAX_VALUE;
            }
        }
        double v = 0;
        for (int l = 2; l <= numdata; l++) {
            double s1 = 0;
            double s2 = 0;
            double w = 0;
            for (int m = 1; m <= l; m++) {
                int i3 = l - m + 1;

                Integer temp = list.get(i3 - 1);
                double val = temp.doubleValue();

                s2 += val * val;
                s1 += val;

                w++;
                v = s2 - (s1 * s1) / w;
                int i4 = i3 - 1;
                if (i4 != 0) {
                    for (int j = 2; j <= numclass; j++) {
                        if (mat2[l][j] >= (v + mat2[i4][j - 1])) {
                            mat1[l][j] = i3;
                            mat2[l][j] = v + mat2[i4][j - 1];
                        }
                    }
                }
            }

            mat1[l][1] = 1;
            mat2[l][1] = v;
        }

        int k = numdata;

        int[] kclass = new int[numclass];

        kclass[numclass - 1] = list.size() - 1;

        for (int j = numclass; j >= 2; j--) {
            System.out.println("rank = " + mat1[k][j]);
            int id = (int) (mat1[k][j]) - 2;
            System.out.println("val = " + list.get(id));

            kclass[j - 2] = id;
            k = (int) mat1[k][j] - 1;
        }

        return kclass;
    }

    class doubleComp implements Comparator {
        @Override
        public int compare(Object a, Object b) {
            if (((Double) a).doubleValue() < ((Double) b).doubleValue()) {
                return -1;
            }
            if (((Double) a).doubleValue() > ((Double) b).doubleValue()) {
                return 1;
            }
            return 0;
        }
    }

    // this function is the kernel density estimation
    /**
     * @return int[]
     * @param list
     */
    public int[] KDE(ArrayList<Integer> temp) {
        System.out.println("KDE");
        return null;
    }

    // print function:

    public static void print(ArrayList<Integer> array) {
        System.out.println("\n");
        int index = 0;
        while (index < array.size()) {
            System.out.print(array.get(index) + " ");
            index++;
        }
    }

    // Generating a set of normal distritubion
    public static void Gaussian(ArrayList<Integer> array, int tam, double mean, double SD) {

        System.out.println("Gaussian");

        Random r = new Random();

        for (int i = 0; i < tam; i++) {
            // double mySample = r.nextGaussian();
            // array.add(mySample);

            double val = r.nextGaussian() * SD + mean;
            int randonNumber = (int) Math.round(val);
            array.add(randonNumber);
        }

        int index = 0;
        while (index < array.size()) {
            // System.out.format("%.3f ", array.get(index));
            System.out.print(array.get(index) + " ");
            index++;
        }
        System.out.println(" ");
    }
}
