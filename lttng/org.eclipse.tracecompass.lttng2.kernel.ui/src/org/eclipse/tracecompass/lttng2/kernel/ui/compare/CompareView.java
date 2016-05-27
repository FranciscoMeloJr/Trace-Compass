package org.eclipse.tracecompass.lttng2.kernel.ui.compare;

import java.awt.Composite;

import org.eclipse.swt.SWT;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.swtchart.Chart;
import org.swtchart.ISeries.SeriesType;

public class CompareView extends TmfView {
    private static final String SERIES_NAME = "Series";
    private static final String Y_AXIS_TITLE = "Signal";
    private static final String X_AXIS_TITLE = "Time";
    private static final String FIELD = "value"; // The name of the field that we want to display on the Y axis
    private static final String VIEW_ID = "org.eclipse.tracecompass.lttng2.kernel.ui.compare.compareview";
    private Chart chart;
    private ITmfTrace currentTrace;
    public CompareView(String viewName) {
        super(viewName);
        // TODO Auto-generated constructor stub
    }

    public void createPartControl(Composite parent) {
        chart = new Chart((org.eclipse.swt.widgets.Composite) parent, SWT.BORDER);
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getXAxis(0).getTitle().setText(X_AXIS_TITLE);
        chart.getAxisSet().getYAxis(0).getTitle().setText(Y_AXIS_TITLE);
        chart.getSeriesSet().createSeries(SeriesType.LINE, SERIES_NAME);
        chart.getLegend().setVisible(false);

    }
    @Override
    public void setFocus() {
        chart.setFocus();

    }



}
