package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.estimators.KernelEstimator;

public class WekaTests {
    public static void main(String args[]) throws Exception {
        System.out.println("wekatests");
        //Classifier();
        KDE();
    }

    public static void Classifier() {
        System.out.println("Classifier");
        // load dataset
        try {
            DataSource source = new DataSource("/home/frank/Desktop/TraceCompare/iris.arff");
            Instances dataset = source.getDataSet();
            // set class index to the last attribute
            dataset.setClassIndex(dataset.numAttributes() - 1);
            // create and build the classifier!
            NaiveBayes nb = new NaiveBayes();
            nb.buildClassifier(dataset);
            // print out capabilities
            System.out.println(nb.getCapabilities().toString());

            SMO svm = new SMO();
            svm.buildClassifier(dataset);
            System.out.println(svm.getCapabilities().toString());

            String[] options = new String[4];
            options[0] = "-C";
            options[1] = "0.11";
            options[2] = "-M";
            options[3] = "3";
            J48 tree = new J48();
            tree.setOptions(options);
            tree.buildClassifier(dataset);
            System.out.println(tree.getCapabilities().toString());
            System.out.println(tree.graph());
        } catch (Exception ex) {
            System.out.println("Exception Classifier");
        }
    }

    //KDE
    public static void KDE() {
        System.out.println("KDE");
        // load dataset
        try {
            DataSource source = new DataSource("/home/frank/Desktop/TraceCompare/iris.arff");
            Instances dataset = source.getDataSet();
            // set class index to the last attribute
            dataset.setClassIndex(dataset.numAttributes() - 1);
            // create and build the classifier!
            KernelEstimator kde = new KernelEstimator(0.1);

            // print out capabilities
            System.out.println(kde.getCapabilities().toString());

        } catch (Exception ex) {
            System.out.println("Exception Classifier");
        }
    }
    // JNB
    /**
     * @return int[]
     * @param list
     *            com.sun.java.util.collections.ArrayList
     * @param numclass
     *            int
     */
    public static int[] getJenksBreaks(ArrayList<Integer> list, int numclass) {

        // int numclass;
        int numdata = list.size();

        double[][] mat1 = new double[numdata + 1][numclass + 1];
        double[][] mat2 = new double[numdata + 1][numclass + 1];
        // double[] st = new double[numdata];

        for (int i = 1; i <= numclass; i++) {
            mat1[1][i] = 1;
            mat2[1][i] = 0;
            for (int j = 2; j <= numdata; j++) {
                mat2[j][i] = Double.MAX_VALUE;
            }
        }
        double v = 0;
        for (int l = 2; l <= numdata; l++) {
            double s1 = 0;
            double s2 = 0;
            double w = 0;
            for (int m = 1; m <= l; m++) {
                int i3 = l - m + 1;

                Integer temp = list.get(i3 - 1);
                double val = temp.doubleValue();

                s2 += val * val;
                s1 += val;

                w++;
                v = s2 - (s1 * s1) / w;
                int i4 = i3 - 1;
                if (i4 != 0) {
                    for (int j = 2; j <= numclass; j++) {
                        if (mat2[l][j] >= (v + mat2[i4][j - 1])) {
                            mat1[l][j] = i3;
                            mat2[l][j] = v + mat2[i4][j - 1];
                        }
                    }
                }
            }

            mat1[l][1] = 1;
            mat2[l][1] = v;
        }

        int k = numdata;

        int[] kclass = new int[numclass];

        kclass[numclass - 1] = list.size() - 1;

        for (int j = numclass; j >= 2; j--) {
            System.out.println("rank = " + mat1[k][j]);
            int id = (int) (mat1[k][j]) - 2;
            System.out.println("val = " + list.get(id));

            kclass[j - 2] = id;
            k = (int) mat1[k][j] - 1;
        }

        return kclass;
    }

    //Use the KDE according to the Classifier:
    public static void Classifier(ArrayList<Double> durationList, LinkedHashMap<Double, Node<ProfileData>> hash) {
        System.out.println(durationList + " " + hash);

    }
}