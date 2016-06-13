package org.eclipse.tracecompass.tmf.core.tests.profile;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * @author frank
 *
 */
public abstract class CCTAnalysisModule extends TmfAbstractAnalysisModule {
    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.os.linux.core.kernelmemory"; //$NON-NLS-1$

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = checkNotNull(getTrace());

        final @Nullable String dataFileName = getDataFileName();
        if (dataFileName != null) {
            /* See if the data file already exists on disk */
            String dir = TmfTraceManager.getSupplementaryFileDir(trace);
            final Path file = Paths.get(dir, dataFileName);

            if (Files.exists(file)) {
                /* Attempt to read the existing file */
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                    Object[] segmentArray = readObject(ois);
                    final ISegmentStore<ISegment> store = new ArrayListStore<>();
                    for (Object element : segmentArray) {
                        if (element instanceof ISegment) {
                            ISegment segment = (ISegment) element;
                            store.add(segment);
                        }
                    }
                    fSegmentStore = store;
                    sendUpdate(store);
                    return true;
                } catch (IOException | ClassNotFoundException | ClassCastException e) {
                    /*
                     * We did not manage to read the file successfully, we will
                     * just fall-through to rebuild a new one.
                     */
                    try {
                        Files.delete(file);
                    } catch (IOException e1) {
                    }
                }
            }
        }

        ISegmentStore<ISegment> segmentStore = new ArrayListStore<>();
        boolean completed = buildAnalysisSegments(segmentStore, monitor);
        if (!completed) {
            return false;
        }
        fSegmentStore = segmentStore;

        if (dataFileName != null) {
            String dir = TmfTraceManager.getSupplementaryFileDir(trace);
            final Path file = Paths.get(dir, dataFileName);

            /* Serialize the collections to disk for future usage */
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
                oos.writeObject(segmentStore.toArray());
            } catch (IOException e) {
                /*
                 * Didn't work, oh well. We will just re-read the trace next
                 * time
                 */
            }
        }

        sendUpdate(segmentStore);

        return true;
    }
}
