package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal.KeyTree;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.MLR.Matrix;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.MLR.MultiLinear;
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
    private static int EcctSize;

    Node<ProfileData> aux = null;
    Node<ProfileData> fRoot = Node.create(new ProfileData(0, "root"));
    ArrayList<Node<ProfileData>> fRoots = new ArrayList<>();
    Node<ProfileData> parent = fRoot;
    // String used to split the tree:
    static String Sdelimiter = new String("interval:tracepoint");
    static String SInfo = new String("interval:getinfo");
    static String fEntry = new String("lttng_ust_cyg_profile:func_entry");
    static String fExit = new String("lttng_ust_cyg_profile:func_exit");
    long fGap;
    static boolean diff;

    // This tree is the differential part:
    static LinkedHashMap<KeyTree, Node<ProfileData>> treeDif;
    static ArrayList<Integer> numberLevels = new ArrayList<>();
    static TestStatistic statistics;
    static int threshold = 10;

    // This is for the correlation part:
    static ArrayList<Double> traceInfo;

    /**
     * Default constructor
     */
    public CCTAnalysisModule() {
        super();
        ArrayECCTs = new ArrayList<>();
        hashECCTs = null;
        diff = false;
        statistics = null;
        EcctSize = 0;
        traceInfo = new ArrayList<>();
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
                    if (field.getValue().equals("context")) {
                        System.out.println("Context" + field.getValue()); // $NON-NLS-1$
                    }
                }
            }

            // This is used for putting additional information:
            if (eventName.equals(SInfo)) {

                ITmfEventField content = event.getContent();
                for (ITmfEventField field : content.getFields()) {
                    System.out.println(field + " " + field.getValue());
                    if (field.getValue().equals("cache")) {
                        System.out.println("Cache" + field.getValue()); // $NON-NLS-1$
                    }
                    if (field.toString().contains("my_integer_field")) {
                        System.out.println("Value " + field.getValue()); // $NON-NLS-1$
                        // put as a children on a call graph:
                        if (parent != null) {
                            String info = field.getValue().toString();
                            parent.getProfileData().fTestValue = Integer.parseInt(info);
                        }
                    }
                }
            }
            if (eventName.equals(fEntry)) {
                first = Iterables.get(event.getContent().getFields(), 0);
                String label = first.toString();
                Long start = event.getTimestamp().getValue();
                // RandomNumber
                // int n = rand.nextInt(100) + 1;
                aux = Node.create(new ProfileData(0, label, start, null));

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
                EcctSize = hashECCTs.length;
            } else {
                // Array:
                if (diff) {
                    hashECCTs = new LinkedHashMap[ArrayECCTs.size() + 1];
                    EcctSize = hashECCTs.length;
                } else {
                    hashECCTs = new LinkedHashMap[ArrayECCTs.size()];
                    EcctSize = hashECCTs.length;
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

            // Calculate the CV:
            calculateCV();
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
            statistics = new TestStatistic(aux1);
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
                            // + (fGap * (nchildren - 1));
                            newDuration = data.getDuration();
                        } else {
                            // (fGap * (nchildren));
                            newDuration = data.getDuration();
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
     * This function creates a HashMap of <(level x label) x Node>
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
            if (current.fProfileData.fTestValue > 0) {
                System.out.println("Label " + current.getNodeLabel() + current.fProfileData.fTestValue);
            }
            // System.out.println("current:" + current + " level " + level + "
            // parent " + current.getParent());
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
            // System.out.println("level " + key.getLevel() + nodex);
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
                    if (nodey != null && nodey.getProfileData() != null) {
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
        EcctSize = 1;

        calculateCV();
    }

    /**
     * This function merges an arraylist of trees, updating the tree in the
     * final part
     *
     * @param: the
     *             position of the two trees
     *
     * @return void: because the hash is updated
     */
    public static void mergeArray(ArrayList<Integer> positions) {

        int j, i, k = 0;
        LinkedHashMap<KeyTree, Node<ProfileData>> result;
        LinkedHashMap<KeyTree, Node<ProfileData>> temp;

        LinkedHashMap<KeyTree, Node<ProfileData>> finalResult[];

        // temp is the first tree:
        if (positions.size() == 2) {
            mergeTwoTrees(positions.get(0), positions.get(1));
            return;
        }

        if (positions.size() > 2) {
            System.out.println("merging " + positions.size());
            result = hashECCTs[positions.get(0)];
            for (j = 1; j < positions.size(); j++) {
                k = positions.get(j);
                if (hashECCTs[k] != null) {
                    temp = hashECCTs[k];
                    result = mergeSimilarTree(result, temp);
                }
                hashECCTs[k] = null;
            }
            hashECCTs[positions.get(0)] = result;

            for (j = 0, i = 0; i < hashECCTs.length; i++) {
                if (hashECCTs[i] != null) {
                    j++;
                }
            }
            finalResult = new LinkedHashMap[j];

            for (j = 0, i = 0; i < hashECCTs.length; i++) {
                if (hashECCTs[i] != null) {
                    finalResult[j] = hashECCTs[i];
                    j++;
                }
            }

            hashECCTs = null;
            hashECCTs = finalResult;
            System.out.println("New size" + hashECCTs.length);
            EcctSize = hashECCTs.length;

        } else {
            System.out.println("merging nothing");
        }

    }

    /**
     * This function merges two trees, updating the tree
     *
     * @param: the
     *             position of the two trees
     *
     * @return void: because the hash is updated
     */
    public static void mergeTwoTrees(int position1, int position2) {
        LinkedHashMap<KeyTree, Node<ProfileData>> tree1 = hashECCTs[position1];
        LinkedHashMap<KeyTree, Node<ProfileData>> tree2 = hashECCTs[position2];
        LinkedHashMap<KeyTree, Node<ProfileData>> result;

        LinkedHashMap<KeyTree, Node<ProfileData>> finalResult[] = hashECCTs;

        result = mergeSimilarTree(tree1, tree2);
        hashECCTs[position1] = result;
        hashECCTs[position2] = null;

        int j, i;
        for (j = 0, i = 0; i < hashECCTs.length; i++) {
            if (hashECCTs[i] != null) {
                finalResult[j] = hashECCTs[i];
                j++;
            }

        }

        hashECCTs = finalResult;
        EcctSize = hashECCTs.length;
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
        EcctSize = hashECCTs.length;
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
                    // System.out.println("newTh" + newTh);
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
            LinkedHashMap[] temp = new LinkedHashMap[EcctSize + 1];
            for (int i = 0; i < EcctSize; i++) {
                temp[i] = hashECCTs[i];
            }
            temp[EcctSize] = treeDif;
            hashECCTs = temp;
            // The diff:
            // EcctSize +=1;
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

    public int getNumberLevels() {
        return numberLevels.get(0);
    }

    public ArrayList<Integer> getNumberLevelsEach() {
        return numberLevels;

    }

    public int getEcctSize() {
        return EcctSize;

    }

    // This function returns the fRoots - ArrayList of fRoots;
    public LinkedHashMap<KeyTree, Node<ProfileData>>[] getHashECCTs() {
        return hashECCTs;
    }

    // This is a test for statistics generation for histogram1
    public ITmfStatistics getStatistics() {

        return statistics;
    }

    private static void setBeginAndEntry(String begin, String end, String interval) {
        fEntry = begin;
        fExit = end;
        if (interval != null) {
            Sdelimiter = interval;
        }
    }

    // This function do again the analysis with different parameters
    public void setParameters(String entry, String exit, String interval) {
        if (entry != null && exit != null) {
            if (interval != null) {
                setBeginAndEntry(entry, exit, interval);
            } else {
                setBeginAndEntry(entry, exit, null);
            }
        }

        super.resetAnalysis();
    }

    public static void classificationTest() {

        ArrayList<Integer> arrayTest1 = new ArrayList<>();
        ArrayList<Integer> arrayTest2 = new ArrayList<>();
        ArrayList<Integer> arrayTest3 = new ArrayList<>();
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
        Classification.Gaussian(arrayTest1, 10, 100, 3);
        Classification.Gaussian(arrayTest2, 10, 1000, 3);
        Classification.Gaussian(arrayTest3, 10, 500, 3);
        //
        // getJenksBreaks(array, j);

        arrayTotal = arrayTest1;
        arrayTotal.addAll(arrayTest2);
        arrayTotal.addAll(arrayTest3);
        Collections.shuffle(arrayTotal);

        System.out.print("Total");
        // print(arrayTotal);

        variationClassification(arrayTotal, null);

    }

    // This function Classify a hash called by SampleView, for each function
    // there is a classification:
    public static void variationClassificationF() {

        // each hash in the array:
        LinkedHashMap<KeyTree, Node<ProfileData>> eachHashECCT = new LinkedHashMap<>();
        // node <-> duration:
        LinkedHashMap<Node<ProfileData>, Long> durationNodeHash = new LinkedHashMap<>();

        // For each tree:
        for (int i = 0; i < hashECCTs.length; i++) {
            eachHashECCT = hashECCTs[i];

            // for each function:
            for (KeyTree key : eachHashECCT.keySet()) {

                Node<ProfileData> temp = eachHashECCT.get(key);
                if (temp != null) {
                    durationNodeHash.put(temp, temp.getDur());
                }
            }
        }

        System.out.print("Size" + durationNodeHash.size());
        System.out.print("Range Classification");
        Classification temp = new Classification(durationNodeHash);
        temp.doSimulation();

    }

    // This function Classify a hash called by SampleView:
    public static void variationClassification(Object parameter, LinkedHashMap<Double, Node<ProfileData>> hash) {
        // Can be Integer or Double:
        Classification temp = null;
        if (hash != null) {
            temp = new Classification(parameter, hash);
        } else {
            temp = new Classification(parameter);
        }
        // calculate the mean:
        try {
            // result will be in groups:

            // temp.calculateMeanArray();

            // First sort:
            temp.doSimulation();

            // ArrayList<ArrayList<Integer>> groups = new ArrayList<>();

            // double meanSq = temp.getMeanSq();

            // Variation (mean - sqr(value))
            // temp.calculateMeanArray();
            // System.out.println("Simulation:");

            // Do the simulation:
            // temp.Simulation();
            // System.out.println("Size in " + groups.size() + " groups");
            // temp.display();

        } catch (Exception ex) {
            System.out.println("exception variation");
        }
    }

    // Insertion standard deviation, this function goes through the array
    public static int mean(ArrayList<Integer> list) {
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
        }

        return sum / list.size();
    }

    /**
     * @author francisco This Class is used to classify the data in several
     *         ways: JNB, KDE, variation and Opk-means
     */
    static class Classification {
        double meanSq = 0;
        boolean isFunction;
        boolean isInteger;
        ArrayList<Integer> arrayIntegers;
        ArrayList<Double> arrayDouble;
        ArrayList<Double> meanDistance = new ArrayList<>();

        ArrayList<ArrayList<Integer>> groups1 = new ArrayList<>();
        ArrayList<ArrayList<Double>> groups2 = new ArrayList<>();

        // result: Node <-> String:
        static LinkedHashMap<Node<ProfileData>, String> resultNodeGroup;

        // hash: String <-> Double:
        static LinkedHashMap<String, Integer> hashGroupInteger = null;
        // hash: String <-> Integer:
        static LinkedHashMap<String, Double> hashGroupDouble = null;

        static // hash: node <-> duration:
        LinkedHashMap<Node<ProfileData>, Long> durationNodeHash = null;

        // hash: Double <-> Node:
        LinkedHashMap<Double, Node<ProfileData>> hashDoubleNode = new LinkedHashMap<>();
        LinkedHashMap<Integer, Node<ProfileData>> hashIntegerNode = new LinkedHashMap<>();

        // Constructor = For tests:
        Classification(Object parameter) {
            if (parameter instanceof ArrayList<?>) {
                if (((ArrayList<?>) parameter).get(0) instanceof Integer) {
                    ArrayList<Integer> arrayI = (ArrayList<Integer>) parameter;
                    arrayIntegers = new ArrayList<>();
                    arrayIntegers = arrayI;
                    isInteger = true;
                    groups1 = new ArrayList<>();
                } else {
                    ArrayList<Double> arrayD = (ArrayList<Double>) parameter;
                    arrayDouble = new ArrayList<>();
                    arrayDouble = arrayD;
                    groups2 = new ArrayList<>();
                    isInteger = false;
                }
            } else {
                LinkedHashMap<Node<ProfileData>, Long> durationNode = (LinkedHashMap<Node<ProfileData>, Long>) parameter;
                durationNodeHash = durationNode;
                isFunction = true;
            }

        }

        // Constructor 2 = for simulations:
        Classification(Object parameter, LinkedHashMap<Double, Node<ProfileData>> hashNodes) {
            this(parameter);
            hashDoubleNode = hashNodes;
        }

        public void doSimulation() {
            if (isFunction) {
                System.out.println("Function classification");
                SimulationF();
            } else {
                if (isInteger) {
                    SimulationI();
                } else {
                    System.out.println("Double classification");
                    SimulationD();
                }
            }
        }

        // This function will return the classification result:
        public LinkedHashMap<Node<ProfileData>, String> getResult() {

            // run through the result to create a map of groups:
            if (isInteger) {

                // run the keys: <String, Integer>
                for (String key : hashGroupInteger.keySet()) {
                    System.out.println(key + " " + hashGroupInteger.get(key));

                    Integer in = hashGroupInteger.get(key);
                    String string = key;
                    Node<ProfileData> node = hashIntegerNode.get(in);
                    System.out.println(key + " " + in);
                    // put node and the result
                    resultNodeGroup.put(node, string);
                }
            }
            return resultNodeGroup;
        }

        // Constructor:
        Classification(ArrayList<Integer> arrayI, ArrayList<Double> arrayD) {
            if (arrayD == null) {
                arrayIntegers = new ArrayList<>();
                arrayIntegers = arrayI;
                isInteger = true;
                groups1 = new ArrayList<>();

            } else {
                arrayDouble = new ArrayList<>();
                arrayDouble = arrayD;
                groups2 = new ArrayList<>();
            }
        }

        // Display the result:
        public void display() {
            if (isInteger) {
                printI(groups1);
            } else {
                printD(groups2);
            }
        }

        public void SimulationF() {

            // function <-> duration
            LinkedHashMap<String, ArrayList<Long>> durationFunction = new LinkedHashMap<>();
            // group <-> duration:
            LinkedHashMap<String, ArrayList<Long>> result;

            System.out.println("SimulationF");
            // run over the durationHash, with all the nodes:
            for (Node<ProfileData> key : durationNodeHash.keySet()) {
                @Nullable
                Long duration = durationNodeHash.get(key);
                String functionLabel = key.getNodeLabel();
                System.out.println(key.getNodeLabel() + " " + durationNodeHash.get(key));
                addValuesL(functionLabel, duration, durationFunction);
            }

            // Function addr=0x400baa Groups:1
            // 1 [3240015, 3273372, 3336086, 3357447, 3381480, 3413221, 3421644,
            // 3435595, 3435714, 3435715]
            for (String eachFunction : durationFunction.keySet()) {
                result = rangeClassification(durationFunction.get(eachFunction), 0.5);
                int nKeys = showClassification(result);
                System.out.println("Function " + eachFunction + " Groups:" + nKeys);
                // function+<node, arrayDuration>+<group, arrayList>
                putFunctionGroup(eachFunction, result);
            }

        }

        // Function to put the result in the nodes:
        private static void putFunctionGroup(String function, LinkedHashMap<String, ArrayList<Long>> result) {

            // Put the result in the nodes iterating over the result:
            for (Node<ProfileData> keyNode : durationNodeHash.keySet()) {
                String label = keyNode.getNodeLabel();
                if (function == label) {
                    // Find the duration in the array:
                    for (String key : result.keySet()) {
                        for (int i = 0; i < result.get(key).size(); i++) {
                            Long duration = result.get(key).get(i);
                            if (duration == keyNode.getDur()) {
                                keyNode.setGroup(key);
                                System.out.println("Node" + keyNode.getNodeId() + "group" + keyNode.getGroup());
                            }
                        }
                    }
                }
            }

        }

        // RangeClassification: method
        public LinkedHashMap<String, ArrayList<Long>> rangeClassification(ArrayList<Long> arrayD, double tolerance) {
            // calculate the mean:
            ArrayList<Long> array = arrayD;

            // Group <-> Integer
            LinkedHashMap<String, ArrayList<Long>> hashGroupNumber = new LinkedHashMap<>();

            // First sort:
            Collections.sort(array);

            printArrayL(array);
            // all the array is pointing to group 1:
            // initiateI(array, hashGroupNumber);

            int group = 1;

            Long previous = array.get(0);
            Long temp = previous;
            // double tolerance = 0.5; // 1 = 100%, 0.5 = 200% tolerance,
            // put the first:
            addValuesL(Integer.toString(group), temp, hashGroupNumber);

            for (int i = 1; i < array.size(); i++) {
                temp = array.get(i);
                // System.out.println(temp + " " + previous + " " + group);
                if (temp > (previous + (previous / tolerance))) {
                    group++;
                }
                addValuesL(Integer.toString(group), temp, hashGroupNumber);
                previous = temp;
            }

            return hashGroupNumber;
        }

        // Integer
        // Range Classification
        public void SimulationI() {

            ArrayList<Integer> array = arrayIntegers;

            // Group <-> Integer
            LinkedHashMap<String, ArrayList<Integer>> hashGroupNumber = new LinkedHashMap<>();

            // First sort:
            Collections.sort(array);

            printArray(array);
            try {
                // all the array is pointing to group 1:
                // initiateI(array, hashGroupNumber);

                int group = 1;

                Integer previous = array.get(0);
                Integer temp = previous;
                double tolerance = 0.5; // 1 = 100%, 0.5 = 200% tolerance,
                // put the first:
                addValues(Integer.toString(group), temp, hashGroupNumber);

                for (int i = 1; i < array.size(); i++) {
                    temp = array.get(i);
                    System.out.println(temp + " " + previous + " " + group);
                    if (temp > (previous + (previous / tolerance))) {
                        group++;
                    }
                    addValues(Integer.toString(group), temp, hashGroupNumber);
                    previous = temp;
                }

                showClassification(hashGroupNumber);

            } catch (

            Exception ex) {
                System.out.println("Exception Simulation");
            }

        }

        // Multimap function 1:
        private static void addValues(String key, Integer value, LinkedHashMap<String, ArrayList<Integer>> hashMap) {
            ArrayList tempList = null;
            if (hashMap.containsKey(key)) {
                tempList = hashMap.get(key);
                if (tempList == null) {
                    tempList = new ArrayList();
                }
                tempList.add(value);
            } else {
                tempList = new ArrayList();
                tempList.add(value);
            }
            hashMap.put(key, tempList);
        }

        // Multimap function 3:
        private static void addValuesL(String key, Long temp, LinkedHashMap<String, ArrayList<Long>> hashMap) {
            ArrayList tempList = null;
            if (hashMap.containsKey(key)) {
                tempList = hashMap.get(key);
                if (tempList == null) {
                    tempList = new ArrayList();
                }
                tempList.add(temp);
            } else {
                tempList = new ArrayList();
                tempList.add(temp);
            }
            hashMap.put(key, tempList);
        }

        // Multimap function 2:
        private static void addValuesD(String key, Double value, LinkedHashMap<String, ArrayList<Double>> hashMap) {
            ArrayList tempList = null;
            if (hashMap.containsKey(key)) {
                tempList = hashMap.get(key);
                if (tempList == null) {
                    tempList = new ArrayList();
                }
                tempList.add(value);
            } else {
                tempList = new ArrayList();
                tempList.add(value);
            }
            hashMap.put(key, tempList);
        }

        // Double:
        public void SimulationD() {

            // calculate the mean:
            ArrayList<Double> array = arrayDouble;

            // Group <-> Integer
            LinkedHashMap<String, ArrayList<Double>> hashGroupNumber = new LinkedHashMap<>();

            // First sort:
            Collections.sort(array);

            printArrayD(array);
            try {
                // all the array is pointing to group 1:
                // initiateI(array, hashGroupNumber);

                int group = 1;

                Double previous = array.get(0);
                Double temp = previous;
                double tolerance = 0.5; // 1 = 100%, 0.5 = 200% tolerance,
                // put the first:
                addValuesD(Integer.toString(group), temp, hashGroupNumber);

                for (int i = 1; i < array.size(); i++) {
                    temp = array.get(i);
                    System.out.println(temp + " " + previous + " " + group);
                    if (temp > (previous + (previous / tolerance))) {
                        group++;
                    }
                    addValuesD(Integer.toString(group), temp, hashGroupNumber);
                    previous = temp;
                }

                // Display the classification:
                showClassification(hashGroupNumber);

            } catch (

            Exception ex) {
                System.out.println("exception");
            }

        }

        // This initiate a classification array:
        private static int showClassification(LinkedHashMap<String, ?> hash) {

            // run through the array:
            System.out.println("Classification");
            int nKeys = 0;
            for (String key : hash.keySet()) {
                System.out.println(key + " " + hash.get(key));
                nKeys++;
            }
            return nKeys;
        }

        public void calculateMeanArray() {
            if (isInteger) {
                int index = 0;
                double result;
                while (index < arrayIntegers.size()) {
                    result = (arrayIntegers.get(index) * arrayIntegers.get(index)) - meanSq;
                    meanDistance.add(result);
                    index++;
                }
            } else {
                int index = 0;
                double result;

                while (index < arrayDouble.size()) {
                    result = (arrayDouble.get(index) * arrayDouble.get(index)) - meanSq;
                    meanDistance.add(result);
                    index++;
                }
            }
        }

        public double getMeanSq() {
            return meanSq;
        }

        public Integer get(int index, int x) {
            if (isInteger) {
                return arrayIntegers.get(index);
            }
            return x;

        }

        public Double get(int index) {
            return arrayDouble.get(index);
        }

        ArrayList<?> getArrayList() {
            if (isInteger) {
                return arrayIntegers;
            }
            return arrayDouble;
        }

        // Sort:
        void sort() {
            if (isInteger) {
                Collections.sort(arrayIntegers);
            } else {
                Collections.sort(arrayDouble);
            }
        }

        int getSize() {
            int size = 0;
            if (isInteger) {
                size = arrayIntegers.size();
            } else {
                size = arrayDouble.size();
            }
            return size;
        }

        // Sum Squares:
        void SumSq() {
            int index = 0;

            if (isInteger) {
                int sumSq = 0;

                while (index < arrayIntegers.size()) {
                    sumSq = arrayIntegers.get(index) * arrayIntegers.get(index);
                    index++;
                }
                meanSq = sumSq / arrayIntegers.size();
            } else {
                double sumSq = 0;
                while (index < arrayDouble.size()) {
                    sumSq = arrayDouble.get(index) * arrayDouble.get(index);
                    index++;
                }
                meanSq = sumSq / arrayDouble.size();
            }

        }

        // Insertion standard deviation, this function goes through the array
        public static int variationI(ArrayList<Integer> list) {
            int max = list.get(0);
            int min = list.get(0);
            int number;

            for (int index = 0; index < list.size(); index++) {
                number = list.get(index);
                if (number > max) {
                    max = list.get(index);
                }
                if (number < min) {
                    max = list.get(index);
                }
            }
            return max - min;
        }

        // Insertion standard deviation, this function goes through the array
        public static double variationD(ArrayList<Double> list) {
            double max = list.get(0);
            double min = list.get(0);
            double number;

            for (int index = 0; index < list.size(); index++) {
                number = list.get(index);
                if (number > max) {
                    max = list.get(index);
                }
                if (number < min) {
                    max = list.get(index);
                }
            }
            return max - min;
        }

        // print integer - change this function here:
        public static void printArray(ArrayList<Integer> array) {

            for (int i = 0; i < array.size(); i++) {
                System.out.print(array.get(i) + " ");
            }
            System.out.println(" ");
        }

        // print long - change this function here:
        public static void printArrayL(ArrayList<Long> array) {

            for (int i = 0; i < array.size(); i++) {
                System.out.print(array.get(i) + " ");
            }
            System.out.println(" ");
        }

        // print double - change this function here:
        public static void printArrayD(ArrayList<Double> array) {

            for (int i = 0; i < array.size(); i++) {
                System.out.print(array.get(i) + " ");
            }
            System.out.println(" ");
        }

        // print integer - change this function here:
        public static void printI(ArrayList<ArrayList<Integer>> groups) {
            System.out.println("\n");
            int index;
            ArrayList<Integer> temp;

            for (int i = 0; i < groups.size(); i++) {
                index = 0;
                temp = groups.get(i);
                while (index < temp.size()) {
                    Integer each = temp.get(index);
                    // put the number of the group, in the hash
                    hashGroupInteger.put(Integer.toString(i), each);
                    System.out.print(temp.get(index) + " ");
                    index++;

                }
                System.out.println("\n");
            }
        }

        // print double:
        public static void printD(ArrayList<ArrayList<Double>> groups) {
            System.out.println("\n");
            int index;
            ArrayList<Double> temp;

            for (int i = 0; i < groups.size(); i++) {
                index = 0;
                temp = groups.get(i);
                while (index < temp.size()) {
                    System.out.print(temp.get(index) + " ");
                    index++;
                }
                System.out.println("\n");
            }
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
            // double[] st = new double[numdata];

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

        // This function calculates the groups of an arrayList of doubles:
        public static ArrayList<ArrayList<Double>> executeD(ArrayList<Double> durationList) {

            ArrayList<ArrayList<Double>> result = new ArrayList<>();
            ArrayList<Double> miniGroups = new ArrayList<>();

            // calculate the mean:
            ArrayList<Double> array = durationList;

            // First sort:
            Collections.sort(array);

            printArrayD(array);
            // all the array is pointing to group 1:
            // initiateI(array, hashGroupNumber);

            int group = 1;

            Double previous = array.get(0);
            Double temp = previous;
            double tolerance = 0.5; // 1 = 100%, 0.5 = 200% tolerance,
            miniGroups = new ArrayList<>();
            miniGroups.add(temp);
            for (int i = 1; i < array.size(); i++) {
                temp = array.get(i);
                System.out.println(temp + " " + previous + " " + group);
                if (temp > (previous + (previous / tolerance))) {
                    result.add(miniGroups);
                    miniGroups = new ArrayList<>();
                }
                miniGroups.add(temp);
                previous = temp;
            }
            result.add(miniGroups);

            return result;
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

        // this function is the Kernel Density Estimation
        /**
         * @return int[]
         * @param list
         */
        /*
         * public int[] KDE(ArrayList<Integer> temp) {
         * System.out.println("KDE"); return null; }
         */

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

        // Run the Classification - 1 for the whole tree, 2 for each function:
        public static boolean RunClassification(int kind) {

            // Getting the data:
            Node<ProfileData> eachECCTs;
            double duration;

            ArrayList<Double> durationList = new ArrayList<>();
            LinkedHashMap<Double, Node<ProfileData>> hash = new LinkedHashMap<>();

            if (hashECCTs.length > 1) {

                for (int i = 0; i < EcctSize; i++) {
                    eachECCTs = ArrayECCTs.get(i);
                    duration = eachECCTs.getProfileData().getDuration();
                    durationList.add(Double.valueOf(duration));

                    // link between node x duration:
                    hash.put(duration, eachECCTs);
                }

                // Run the classification method for the whole duration:
                if (kind == 1) {
                    RunVariationClassifier(durationList, hash);
                }
                // Run the classification method for functions separately
                if (kind == 2) {
                    variationClassificationF();
                }
                // Run the OptimazedK-Means method for functions separately
                if (kind == 3) {
                    RunKMean(durationList, hash);
                }
                // Run the KDE method for functions separately
                if (kind == 4) {
                    RunKDE(durationList, hash);
                } else {
                    // Run the Jenks Natural Breaks method for functions
                    // separately
                    callJNB(durationList);
                }
            } else {
                System.out.println("At least more than one group");
                return false;
            }
            return true;
        }

        // CAll Weka Tests:
        public static void RunKDE(ArrayList<Double> durationList, LinkedHashMap<Double, Node<ProfileData>> hash) {
            // test
            if (durationList == null) {
                WekaTests.Classifier();
            } else {
                // Use KDE with the real data:
                WekaTests.Classifier(durationList, hash);

            }
        }

        // Call the JNB:
        public static void callJNB(ArrayList<Double> durationList) {

            int n = 10;
            ArrayList<Integer> durationInteger = new ArrayList<>();

            for (int i = 1; i < n; i++) {
                durationInteger.add(durationList.get(i).intValue());
            }
            for (int i = 1; i < n; i++) {
                System.out.println("With " + i + "group");
                Classification.getJenksBreaks(durationInteger, i);
            }

        }

        // This function Call the Variation classification and update the Tree:
        public static void RunVariationClassifier(ArrayList<Double> durationList, LinkedHashMap<Double, Node<ProfileData>> hash) {
            ArrayList<Integer> positionMerge = new ArrayList<>();

            ArrayList<ArrayList<Double>> result = Classification.executeD(durationList);

            System.out.println("Result " + result);
            Node<ProfileData> temp;
            // Put the solution:
            for (int i = 0; i < result.size(); i++) {
                ArrayList<Double> eachGroup = result.get(i);
                positionMerge = new ArrayList<>();
                for (int j = 0; j < eachGroup.size(); j++) {
                    Double duration = eachGroup.get(j);
                    temp = hash.get(duration);
                    // set the duration <-> group
                    temp.setGroup(Integer.toString(i + 1));
                    // Merge the trees within this group:
                    positionMerge.add(findPositionInHash(duration));
                }
                // Merge the positions:
                mergeArray(positionMerge);
            }

        }

        // This function Call the OptimazedK-means and update the Tree:
        public static void RunKMean(ArrayList<Double> durationList, LinkedHashMap<Double, Node<ProfileData>> hash) {
            ArrayList<Integer> positionMerge = new ArrayList<>();
            if (durationList != null) {
                // Classification with arrayDouble
                // This test all the combinations:
                int bestK = KMean.test(durationList);
                if (bestK > 0) {
                    // Execute with 2, which is the best k:
                    ArrayList<ArrayList<Double>> result = KMean.executeD(bestK, durationList);
                    System.out.println("Result " + result);
                    Node<ProfileData> temp;
                    // Put the solution:
                    for (int i = 0; i < result.size(); i++) {
                        ArrayList<Double> eachGroup = result.get(i);
                        positionMerge = new ArrayList<>();
                        for (int j = 0; j < eachGroup.size(); j++) {
                            Double duration = eachGroup.get(j);
                            temp = hash.get(duration);
                            // set the duration <-> group
                            temp.setGroup(Integer.toString(i + 1));
                            // Merge the trees within this group:
                            positionMerge.add(findPositionInHash(duration));
                        }
                        // Merge the positions:
                        mergeArray(positionMerge);
                    }

                } else {
                    mergeTrees();
                    System.out.println("Not enough groups ");
                }

            } else {
                // Test:
                KMean.test();
            }
        }

        // Find the position in the hash:
        public static int findPositionInHash(Double Duration) {
            double duration;
            for (int i = 0; i < EcctSize; i++) {
                Node<ProfileData> eachECCTs = ArrayECCTs.get(i);
                duration = eachECCTs.getProfileData().getDuration();

                if (Duration == duration) {
                    return i;
                }
            }
            return 0;
        }
    }

    // Calculate the STD for each function in the executions:
    // CV = STd / Mean
    public static void calculateCV() {
        LinkedHashMap<KeyTree, Node<ProfileData>> eachECCTs;
        System.out.println("calculateCV");
        // Run through the nodes and display them:
        for (int i = 0; i < EcctSize; i++) {
            eachECCTs = hashECCTs[i];
            for (KeyTree key : eachECCTs.keySet()) {

                Node<ProfileData> eachNode = eachECCTs.get(key);
                ArrayList<Long> runsNode = eachNode.fProfileData.eachRun;
                System.out.print(eachNode.getNodeLabel() + " " + eachNode.getDur());
                // STD:
                long var = TestStatistic.calculateSTDandCV(runsNode, 1);
                eachNode.setVariation(var);
            }
        }
    }

    public static boolean RunClassification(int i) {
        Boolean answer = Classification.RunClassification(i);

        return answer;
    }

    // Correlates the the second tracepoint with the duration:
    public static String correlationInfoTrace() {
        System.out.println("Correlation");
        // Reading the values:
        LinkedHashMap<KeyTree, Node<ProfileData>> eachECCTs;
        LinkedHashMap<Node<ProfileData>, Double> infoNodeHash = new LinkedHashMap<>();
        Double value;

        // To calculate the duration:
        double duration;
        ArrayList<Double> durationList = new ArrayList<>();
        traceInfo = new ArrayList<>();

        // Tracepoint info:
        for (int i = 0; i < EcctSize; i++) {
            eachECCTs = hashECCTs[i];

            for (KeyTree key : eachECCTs.keySet()) {
                Node<ProfileData> node = eachECCTs.get(key);
                int testedValue = node.fProfileData.fTestValue;
                if (testedValue > 0) {
                    value = (double) testedValue;
                    traceInfo.add((double) testedValue);
                    infoNodeHash.put(node, value);
                }
            }
        }

        // Duration:
        Node<ProfileData> eachTree;
        for (int i = 0; i < EcctSize; i++) {
            eachTree = ArrayECCTs.get(i);
            duration = eachTree.getProfileData().getDuration();
            System.out.print(duration + " ");
            durationList.add(Double.valueOf(duration));
        }

        for (int i = 0; i < traceInfo.size(); i++) {
            System.out.print(traceInfo.get(i) + " ");
        }

        // call the correlation:
        Double result = TestStatistic.calculateCorrelation(durationList, traceInfo);
        String resultString;
        if (result >= 0.75) {
            System.out.print("Strong correlated");
            resultString = ("Strong correlated");
        } else {
            if (result <= 0.25) {
                System.out.print("Not correlated");
                resultString = ("Not correlated");
            } else {
                System.out.print("Weak correlated");
                resultString = ("Weak correlated");
            }
        }

        return resultString;
    }

    //Multi Linear Regression:
    public static void MRL(int i) {
        //Test:
        if(i ==0 ){
        Matrix X = new Matrix(new double[][]{{4,0,1},{7,1,1},{6,1,0},{2,0,0},{3,0,1}});
        Matrix Y = new Matrix(new double[][]{{27},{29},{23},{20},{21}});
        MultiLinear ml = new MultiLinear(X, Y);
        try {
            Matrix beta = ml.calculate();
            System.out.println(beta);
            //BMI =  beta0  +   beta1 * Diet_Score +  beta2  * Male +  beta3  * age
            //beta0  = 9.25,  beta1 = 4.75, beta2 = -13.5, beta3 = -1.25
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        }
        //Taking the information:
        else{

        }
    }

}
