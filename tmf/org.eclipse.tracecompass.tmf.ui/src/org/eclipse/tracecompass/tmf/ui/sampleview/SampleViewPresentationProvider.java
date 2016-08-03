package org.eclipse.tracecompass.tmf.ui.sampleview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.ui.sampleview.SampleView.EventEntry;
import org.eclipse.tracecompass.tmf.ui.sampleview.SampleView.EventNode;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

/**
 * @author francisco
 *
 */
// Class Presentation Provider:
public class SampleViewPresentationProvider extends TimeGraphPresentationProvider {

    /** Number of colors used for call stack events */
    public static final int NUM_COLORS = 360;

    private SampleView fSampleView;

    private Integer fAverageCharWidth;

    private enum State {
        //Gray:
        MULTIPLE(new RGB(100, 100, 100)), EXEC(new RGB(0, 200, 0)),
        //Red:
        RED(new RGB(100, 0, 0)),
        //Green:
        GREEN(new RGB(0, 100,0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Constructor
     *
     * @since 1.2
     */
    public SampleViewPresentationProvider() {
    }

    // In house method 1
    /**
     * @param view
     * @since 2.0
     */
    public void setSampleView(SampleView view) {
        fSampleView = view;
    }

    // In house method 2
    /**
     * @since 2.0
     */
    public SampleView getSampleView() {
        return fSampleView;

    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        return Messages.SampleViewPresentationProvider_NodeColumn;
    }

    @Override
    public StateItem[] getStateTable() {
        final float saturation = 0.6f;
        final float brightness = 0.6f;
        StateItem[] stateTable = new StateItem[NUM_COLORS + 1];
        stateTable[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        for (int i = 0; i < NUM_COLORS; i++) {
            RGB rgb = new RGB(i, saturation, brightness);
            stateTable[i + 1] = new StateItem(rgb, State.EXEC.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {

        if (event instanceof EventEntry) {
            EventEntry EventEntry = (EventEntry) event;
            return EventEntry.getNodeId();
        }
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }

        // Change:
        if (event instanceof EventNode) {
            EventNode eventNode = (EventNode) event;
            int color = eventNode.getColor();
            if (color == 0) {
                return State.MULTIPLE.ordinal();
            }
            if (color == 1) {
                return State.RED.ordinal();
            }
            if (color == -1) {
                return State.GREEN.ordinal();
            }
        }
        return State.MULTIPLE.ordinal();
    }

    @Override
    public String getEventName(ITimeEvent event) {
        String ret = null;
        if (event instanceof EventEntry) {
            EventEntry entry = (EventEntry) event.getEntry();
            // ITmfStateSystem ss = entry.getStateSystem();
            try {
                ret = new String(entry.getName() + " function");
            } catch (TimeRangeException e) {
                Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
            }
            return ret;
        }
        if (event instanceof TimeEvent) {
            ret = new String("Function name" + " function");
        }
        return ret;
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (fAverageCharWidth == null) {
            fAverageCharWidth = gc.getFontMetrics().getAverageCharWidth();
        }
        if (bounds.width <= fAverageCharWidth) {
            return;
        }
        if (event instanceof EventNode) {
            EventNode entry = (EventNode) event;
            String label = entry.toString();

            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
            Utils.drawText(gc, label, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
        }
        if (!(event instanceof EventEntry)) {
            return;
        }
        EventEntry entry = (EventEntry) event.getEntry();
        // ITmfStateSystem ss = entry.getStateSystem();
        try {
            // ITmfStateValue value = ss.querySingleState(event.getTime(),
            // entry.getQuark()).getStateValue();
            // if (!value.isNull()) {
            String name = fSampleView.getFunctionName();
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
            Utils.drawText(gc, name, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
            // }
        } catch (TimeRangeException e) {
            Activator.getDefault().logError("Error", e); //$NON-NLS-1$
        }
    }

}