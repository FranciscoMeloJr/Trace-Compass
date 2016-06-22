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
    long fstartTime;
    long fendTime;

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
    public ProfileData(int weight, String label, long start) {
        if (weight == 0) {
            fWeight = 0;
        } else {
            fWeight = weight;
        }
        fLabel = label;
        fstartTime = start;
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
        fstartTime = start;
    }

    public void setEndTime(long end) {
        fendTime = end;
    }
    public long getStartTime() {
        return fstartTime;
    }

    public long getEndTime() {
        return fendTime;
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
