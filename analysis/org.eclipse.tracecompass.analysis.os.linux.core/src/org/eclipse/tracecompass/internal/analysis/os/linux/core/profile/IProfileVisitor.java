package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

public interface IProfileVisitor<T extends IProfileData> {

    void visit(Node<T> node);

}
