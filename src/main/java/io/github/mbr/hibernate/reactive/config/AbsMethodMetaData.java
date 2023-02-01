package io.github.mbr.hibernate.reactive.config;

import java.lang.reflect.Method;

public abstract class AbsMethodMetaData implements RepoInterfaceMetaData.MethodMetaData{
    private final Method method;
    private final RepoInterfaceMetaData.IMethodInvoker invoker;
    AbsMethodMetaData(Method method, RepoInterfaceMetaData.IMethodInvoker invoker) {
        this.method = method;
        this.invoker = invoker;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    public RepoInterfaceMetaData.IMethodInvoker getInvoker() {
        return invoker;
    }
}
