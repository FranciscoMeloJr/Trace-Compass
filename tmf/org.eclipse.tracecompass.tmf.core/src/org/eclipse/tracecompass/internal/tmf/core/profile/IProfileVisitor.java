package org.eclipse.tracecompass.internal.tmf.core.profile;

public interface IProfileVisitor<T extends IProfileData> {

    void visit(Node<T> node);

}
