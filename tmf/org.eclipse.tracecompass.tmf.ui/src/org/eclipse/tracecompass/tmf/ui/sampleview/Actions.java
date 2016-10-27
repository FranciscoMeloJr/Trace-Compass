package org.eclipse.tracecompass.tmf.ui.sampleview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;

/**
 * @author frank
 * @since 2.2
 *
 */
public class Actions {

    /**
     * Button used to test algorithms
     *
     * @return The Action object
     */
    public static Action getTestAction() {
        // resetScale
        Action fInversion = new Action() {
            @Override
            public void run() {
                System.out.println("Tests");
                //taking 2 infos from the trace
                //CCTAnalysisModule.correlationInfoTrace(2, "instructions");
                //taking 3 infos from the trace+ external file:
                CCTAnalysisModule.correlationInfoTrace(2, "instructions");
            }
        };
        fInversion.setText("Test");
        fInversion.setToolTipText("Used to test");
        return fInversion;
    }

    /**
     * Button used to call the MRL algorithm
     *
     * @return The Action object
     */
    public static Action getMRLModel() {
        // resetScale
        Action fModel = new Action() {
            @Override
            public void run() {
                System.out.println("MRL");
                //taking 2 infos from the trace
                CCTAnalysisModule.MRL(2);
            }
        };
        fModel.setText("MRL");
        fModel.setToolTipText("Used to test");
        return fModel;
    }
    /**
     * Get the Inversion action
     *
     * @return The Action object
     */
    public static Action getInversionAction() {
        // resetScale
        Action fInversion = new Action() {
            @Override
            public void run() {
                System.out.println("getInversionAction");
                // CCTAnalysisModule.calculateCV();
            }
        };
        fInversion.setText("Inversion");
        fInversion.setToolTipText("Use the inversion");
        return fInversion;
    }

    /**
     * Button used to correlation evaluation
     *
     * @return The Action object
     */
    public static Action getCorrelationAction() {
        // resetScale
        Action correlationButton = null;
        correlationButton = new Action("xis", IAction.AS_DROP_DOWN_MENU) {
            @Override
            public void run() {
                System.out.println("Tests");
                CCTAnalysisModule.correlationInfoTrace(1, null);
            }
        };

        correlationButton.setText("Correlation");
        correlationButton.setToolTipText("Select the delimiters");
        correlationButton.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_CONFLICT));

        return correlationButton;
    }

    /**
     * Get the KDE action
     *
     * @return The Action object
     */
    public static Action getKDEAction() {
        // resetScale
        Action fKDEAction = new Action() {
            @Override
            public void run() {
                System.out.println("Apply KDE Test");

                // Run over the tree:
                // CCTAnalysisModule.RunKDE();
                CCTAnalysisModule.RunClassification(4);
            }
        };
        fKDEAction.setText("KDE");
        fKDEAction.setToolTipText("Use the KDE method");
        fKDEAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_PIN_VIEW));
        return fKDEAction;
    }

}
