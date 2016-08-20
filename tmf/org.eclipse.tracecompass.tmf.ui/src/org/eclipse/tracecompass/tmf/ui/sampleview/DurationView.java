package org.eclipse.tracecompass.tmf.ui.sampleview;

/*
 /* Based from Alexandre Montplaisir and Bernd Hufmann view
 */
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.statistics.ITmfStatistics;
import org.eclipse.tracecompass.tmf.core.statistics.TmfStatisticsModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.barcharts.TmfBarChartViewer;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;


/**
 * @author frank
 * @since 2.0
 *
 */
public class DurationView extends TmfBarChartViewer {

    public static final String ID = "org.eclipse.tracecompass.tmf.ui.views.DurationView"; //$NON-NLS-1$
    /**
     * Creates a Histogram Viewer instance.
     * @param parent
     *            The parent composite to draw in.
     */
    public DurationView(Composite parent) {
        super(parent, null, null, null, TmfBarChartViewer.MINIMUM_BAR_WIDTH);

        Chart swtChart = getSwtChart();

        IAxis xAxis = swtChart.getAxisSet().getXAxis(0);
        IAxis yAxis = swtChart.getAxisSet().getYAxis(0);

        /* Hide the grid */
        xAxis.getGrid().setStyle(LineStyle.NONE);
        yAxis.getGrid().setStyle(LineStyle.NONE);

        /* Hide the legend */
        swtChart.getLegend().setVisible(false);

        addSeries("Number of events", Display.getDefault().getSystemColor(SWT.COLOR_BLUE).getRGB()); //$NON-NLS-1$
    }

    @Override
    protected void readData(final ISeries series, final long start, final long end, final int nb) {
        if (getTrace() != null) {
            final double y[] = new double[nb];

            Thread thread = new Thread("Histogram viewer update") { //$NON-NLS-1$
                @Override
                public void run() {
                    double x[] = getXAxis(start, end, nb);
                    final long yLong[] = new long[nb];
                    Arrays.fill(y, 0.0);

                    /* Add the values for each trace */
                    for (ITmfTrace trace : TmfTraceManager.getTraceSet(getTrace())) {
                        /* Retrieve the statistics object */
                        final TmfStatisticsModule statsMod =
                               TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfStatisticsModule.class, TmfStatisticsModule.ID);
                        if (statsMod == null) {
                            /* No statistics module available for this trace */
                            continue;
                        }
                        statsMod.waitForInitialization();
                        final ITmfStatistics stats = statsMod.getStatistics();
                        if (stats == null) {
                            /*
                             * Should not be null after waitForInitialization()
                             * is called.
                             */
                            throw new IllegalStateException();
                        }
                        List<Long> values = stats.histogramQuery(start, end, nb);

                        for (int i = 0; i < nb; i++) {
                            yLong[i] += values.get(i);
                        }
                    }

                    for (int i = 0; i < nb; i++) {
                        y[i] += yLong[i]; /* casting from long to double */
                    }

                    /* Update the viewer */
                    drawChart(series, x, y);
                }
            };
            thread.start();
        }
        return;
    }
}

