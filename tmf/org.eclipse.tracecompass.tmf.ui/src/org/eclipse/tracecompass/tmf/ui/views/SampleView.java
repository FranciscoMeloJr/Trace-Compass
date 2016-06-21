package org.eclipse.tracecompass.tmf.ui.views;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackEntry;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackEvent;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

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
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */

    //Override this method:
    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        // Load the module:
        System.out.println("buildEntryList " + trace.getName());
        Iterable<CCTAnalysisModule> iter = TmfTraceUtils.getAnalysisModulesOfClass(trace, CCTAnalysisModule.class);
        CCTAnalysisModule module = null;
        for (IAnalysisModule mod: iter) {
            if (mod instanceof CCTAnalysisModule) {
                module = (CCTAnalysisModule) mod;
                break;
            }
        }
        System.out.println(module);
        if (module == null) {
            return;
        }
        module.schedule();
        module.waitForCompletion();

        //TimeGraphEntry threadParent;
        //ITimeEvent x; //ITimeEvent

        // Read the StateHistory, but we will not use it
        //Node<TestData> node = module.getTree();

        /*
         TimeGraphEntry can have children: then they appear as child in the tree viewer on the left
         ITimeEvent are the intervals that gets drawn in the time graph view on the right side
         */

    }

    //Override this method:
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


    protected List<ITimeEvent> getEventList(TimeGraphEntry tgentry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        CallStackEntry entry = (CallStackEntry) tgentry;
        ITmfStateSystem ss = entry.getStateSystem();

        List<ITimeEvent> eventList;

        long start = Math.max(0, 10); //long start = Math.max(startTime, ss.getStartTime());
        long end = Math.min(10, 10); //long end = Math.min(endTime, ss.getCurrentEndTime() + 1);

        //the same validation:
        if (end <= start) {
            return null;
        }

        eventList.add(new CallStackEvent(entry, time, duration, value));


    }
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }

}
