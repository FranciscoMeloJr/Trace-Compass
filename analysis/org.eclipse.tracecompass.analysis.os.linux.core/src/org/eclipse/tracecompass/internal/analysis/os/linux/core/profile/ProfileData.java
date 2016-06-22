package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;

/**
 * @author frank
 * @since 2.0 This Class is for tests related with CCTAnalysis
 *
 */
public class ProfileData implements IProfileData {

    private String fLabel;
    int fWeight;
    long startTime;
    long endTime;

    // Constructor:
    public ProfileData(int weight, String label) {
        if (weight == 0) {
            fWeight = 0;
        } else {
            fWeight = weight;
        }
        fLabel = label;
    }

    // Constructor:
    public ProfileData(long weight, String label) {
        fWeight = (int) weight;
        fLabel = label;
    }

    // Add to the weight:
    public void addWeight(int value) {
        fWeight += value;
    }

    @Override
    public void merge(IProfileData other) {
        // TODO Auto-generated method stub

    }

    @Override
    public IProfileData minus(IProfileData other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean equals(IProfileData other) {
        if (!(other instanceof ProfileData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        ProfileData data = (ProfileData) other;
        if (fLabel.equals(data.getLabel())) {
            if (fWeight == data.getWeight()) {
                return true;
            }
        }
        return false;
    }

    //Change for StackCall
    public void setStartTime(long start) {
        startTime = start;
    }

    public void setEndTime(long end) {
        endTime = end;
    }
    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public String getLabel() {
        return fLabel;
    }

    @Override
    public int getWeight() {
        return fWeight;
    }

    @Override
    public String toString() {
        return new String(fWeight + " " + fLabel);
    }

}
