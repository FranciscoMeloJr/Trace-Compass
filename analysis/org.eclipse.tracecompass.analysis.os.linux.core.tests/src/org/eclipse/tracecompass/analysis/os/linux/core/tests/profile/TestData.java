package org.eclipse.tracecompass.analysis.os.linux.core.tests.profile;

import org.eclipse.tracecompass.analysis.os.linux.core.tests.profile.TestProfileTree.Color;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;

public class TestData implements IProfileData {

    private int fWeight;
    private String fLabel;
    private Color fColor;

    public TestData(int weight, String label) {
        fWeight = weight;
        fLabel = label;
        fColor = Color.Grey;
    }

    @Override
    public int getWeight() {
        return fWeight;
    }

    @Override
    public String getLabel() {
        return fLabel;
    }

    public String getColor() {
        return fColor.toString();
    }

    public void setWeight(int newfWeight) {
        fWeight = newfWeight;
    }

    public void setLabel(String newfLabel) {
        fLabel = newfLabel;
    }

    public void setColor(Color newfColor) {
        fColor = newfColor;
    }

    @Override
    public void merge(IProfileData other) {
        if (!(other instanceof TestData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        TestData data = (TestData) other;
        if (fLabel.equals(data.getLabel())) {
            fWeight += data.getWeight();
        }
    }

    @Override
    public IProfileData minus(IProfileData other) {
        if (!(other instanceof TestData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        TestData data = (TestData) other;
        if (data.getWeight() > fWeight) {
            fColor = Color.Green;
        } else {
            fColor = Color.Red;
        }
        fWeight = fWeight - data.getWeight();
        return new TestData(fWeight - data.getWeight(), fLabel);
    }

    @Override
    public String toString() {
        return fLabel + "," + fWeight;
    }

    @Override
    public boolean equals(IProfileData other) {
        if (!(other instanceof TestData)) {
            throw new IllegalArgumentException("wrong type for minus operation");
        }
        TestData data = (TestData) other;
        return fLabel.equals(data.getLabel());
    }

    public void addWeight(int value) {
        fWeight = fWeight + value;
    }

    @Override
    public int minus(IProfileData other) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int minus(IProfileData other, int th) {
        // TODO Auto-generated method stub
        return 0;
    }

}