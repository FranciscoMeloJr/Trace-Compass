package org.eclipse.tracecompass.internal.analysis.timing.ui.flamegraph;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.tracecompass.internal.analysis.timing.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;

public class Methods {

    private static boolean fDiff;
    int threshold;
    private Action fSortByUnknown;

    Methods() {
        System.out.println("Methods");
    }

    // this function is related with the threshold comparison:
    IAction selectThreshold(int i) {
        // IAction action1 = new Action(Integer.toString(i),
        // IAction.AS_CHECK_BOX){ };
        // IAction.AS_RADIO_BUTTON
        IAction action = new Action(Integer.toString(i), IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                threshold = i;
            }

        };
        action.setToolTipText("Select the threshold for comparison");
        if ((!fDiff) && (i == 0)) {
            action.setChecked(true);
        }
        return action;

    }

    Action getMergeAction() {
        Action mergeButton = null;
        mergeButton = new Action() {
            @Override
            public void run() {
                System.out.println("Automatic merge");
            }
        };
        mergeButton.setText("Grouping selection");
        mergeButton.setToolTipText("This button will automatically merge similiar executions");
        mergeButton.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_CONFLICT));

        return mergeButton;
    }

    // Mod:
    Action getSortByUnknown() {
        if (fSortByUnknown == null) {
            fSortByUnknown = new Action("Differential", IAction.AS_CHECK_BOX) {
                @Override
                public void run() {

                }
            };
            fSortByUnknown.setToolTipText("Differential");
        }
        return fSortByUnknown;
    }
}
