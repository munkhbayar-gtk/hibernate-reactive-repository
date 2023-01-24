package io.github.mbr.hibernate.reactive;

import io.github.mbr.hibernate.reactive.impl._JQL_MethodExecutorImpl;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryPagedMethod;
import lombok.extern.slf4j.Slf4j;
import io.github.mbr.hibernate.reactive.config.RepoInterfaceMetaData;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
public class ReactiveHibernateRepositoryImpl implements InvocationHandler {
    private RepoInterfaceMetaData repoInterfaceMetaData;// Class<? extends ReactiveHibernateCrudRepository<?,?>> repoClass;

    //private IRepoScannedPackages scannedPackages;
    private Object runtimeImplementation;
    private _JQL_MethodExecutorImpl methodExecutor = null;
    public Object getProxiedImplementation() {
        if (runtimeImplementation != null) {
            return runtimeImplementation;
        }
/*        Class<?> newlyLoadedClass = null;
        try{
            newlyLoadedClass = this.classLoader.loadClass(repoInterfaceMetaData.repoInterfaceClass.getCanonicalName());
        }catch (Exception e) {
            log.error("CLASS-LOAD-ERR", e);
            throw new IllegalCallerException(e);
        }*/
        Object retval = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{repoInterfaceMetaData.repoInterfaceClass}, this);
        runtimeImplementation = retval;
        return retval;
    }

    /*
    public void bindInterface(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoClass) {
        this.repoClass = repoClass;
    }
     */
    public void bindRepoInterfaceMetaData(RepoInterfaceMetaData repoInterfaceMetaData) {
        this.repoInterfaceMetaData = repoInterfaceMetaData;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return  methodExecutor.execute(repoInterfaceMetaData, method, args);
        //return method.invoke();
    }


    public void setReactivePersistentUnitInfo(ReactivePersistentUnitInfo unitInfo) throws BeansException {
        methodExecutor = _JQL_MethodExecutorImpl.of(unitInfo);
        log.debug("unitInfo-class-loader: {} {}", unitInfo.getClass().getClassLoader(), ReactivePersistentUnitInfo.class.getClassLoader());
    }

    public void setBeanFactory(ConfigurableListableBeanFactory bf) {
        Object impl = getProxiedImplementation();
        bf.registerResolvableDependency(repoInterfaceMetaData.repoInterfaceClass, impl);
        log.info("[DAO-Repo] class: {} is registered", repoInterfaceMetaData.repoInterfaceClass.getCanonicalName());
    }

    @Override
    public String toString() {
        return "impl";
    }
}
