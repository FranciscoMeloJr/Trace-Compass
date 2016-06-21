package org.eclipse.tracecompass.tmf.ui.views;

import java.awt.List;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.Node;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileData;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackEntry;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackEvent;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 *
 * @since 2.0
 */

public class SampleView extends CallStackView {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID1 = "org.eclipse.tracecompass.tmf.ui.views.SampleView";

    /**
     * The constructor.
     */
    public SampleView() {
        super();
    }

    /**
     * This method will create the Entry List from the node:
     */

    @SuppressWarnings("restriction")
    // Override this method:
    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        // Load the module:
        System.out.println("buildEntryList " + trace.getName());
        Iterable<CCTAnalysisModule> iter = TmfTraceUtils.getAnalysisModulesOfClass(trace, CCTAnalysisModule.class);
        CCTAnalysisModule module = null;
        // Selects only the CCTAnalysis module
        for (IAnalysisModule mod : iter) {
            if (mod instanceof CCTAnalysisModule) {
                module = (CCTAnalysisModule) mod;
                System.out.println("Module" + module);
                break;
            }
        }

        if (module == null) {
            return;
        }
        // Modules:
        module.schedule();
        module.waitForCompletion();

        // Read the tree but we will not use it
        Node<ProfileData> root = module.getTree(); // how to solve this problem?
                                                   // ProfileData class?

        long start = 0;

        TimeGraphEntry threadParent;
        ITimeEvent x; // ITimeEvent

        /*
         * TimeGraphEntry can have children: then they appear as child in the
         * tree viewer on the left ITimeEvent are the intervals that gets drawn
         * in the time graph view on the right side
         */

        LinkedList<Node<ProfileData>> queue = new LinkedList<>();

        // Tree traversal:

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<ProfileData> current = queue.poll();
            for (Node<ProfileData> child : current.getChildren()) {
                queue.add(child);
            }

            // Creating the callStackEntry
            CallStackEntry callStackEntry = new CallStackEntry(threadName, stackLevelQuark, level, processId, trace, ss);
            callStackParent.addChild(callStackEntry); // Create the Entry on the
                                                      // entry list

            System.out.println(current);
        }

    }

    // Override this method, now to build the status of the events on the list
    @Override
    private void buildStatusEvents(ITmfTrace trace, Node<TestData> entry, IProgressMonitor monitor, long start, long end) {

        long resolution = Math.max(1, (end));
        List<ITimeEvent> eventList = getEventList(entry, start, end + 1, resolution, monitor);
        if (eventList != null) {
            entry.setEventList(eventList);
            System.out.println(entry);
        }
        if (trace == getTrace()) {
            redraw();
        }
    }

    // List of events:
    protected List<ITimeEvent> getEventList(TimeGraphEntry tgentry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        CallStackEntry entry = (CallStackEntry) tgentry;
        ITmfStateSystem ss = entry.getStateSystem();

        List<ITimeEvent> eventList;

        long start = Math.max(0, 10); // long start = Math.max(startTime,
                                      // ss.getStartTime());
        long end = Math.min(10, 10); // long end = Math.min(endTime,
                                     // ss.getCurrentEndTime() + 1);

        // the same validation:
        if (end <= start) {
            return null;
        }

        eventList.add(new CallStackEvent(entry, time, duration, value));

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

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }

}
