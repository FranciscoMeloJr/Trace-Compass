package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.estimators.KernelEstimator;

public class WekaTests {
    public static void main(String args[]) throws Exception {
        System.out.println("wekatests");
        Classifier();
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
            KernelEstimator nb = new KernelEstimator(0.1);

            nb.addValues(dataset); //nb.buildClassifier(dataset);
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
}