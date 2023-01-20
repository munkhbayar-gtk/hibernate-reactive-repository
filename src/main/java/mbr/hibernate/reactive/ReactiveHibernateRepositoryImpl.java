package mbr.hibernate.reactive;

import lombok.extern.slf4j.Slf4j;
import mbr.hibernate.reactive.impl._JQL_MethodExecutorImpl;
import mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReactiveHibernateRepositoryImpl implements InvocationHandler {
    private Class<? extends ReactiveHibernateCrudRepository<?,?>> repoClass;

    //private IRepoScannedPackages scannedPackages;
    private Object runtimeImplementation;
    private _JQL_MethodExecutorImpl methodExecutor = null;
    public Object getProxiedImplementation() {
        if (runtimeImplementation != null) {
            return runtimeImplementation;
        }
        Object retval = Proxy.newProxyInstance(ReactiveHibernateRepositoryImpl.class.getClassLoader(),
                new Class[]{repoClass}, this);
        runtimeImplementation = retval;
        return retval;
    }

    public void bindInterface(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoClass) {
        this.repoClass = repoClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RepositoryMethod repoMethodAnnotation = method.getAnnotation(RepositoryMethod.class);
        if(repoMethodAnnotation != null) {
            return methodExecutor.execute(repoClass, method, args);
        }
        log.debug("method is default: {}", method.isDefault());
        throw new RuntimeException(method.getName() + " is not executable, is default: " + method.isDefault());
        //return method.invoke();
    }


    public void setReactivePersistentUnitInfo(ReactivePersistentUnitInfo unitInfo) throws BeansException {
        methodExecutor = _JQL_MethodExecutorImpl.of(unitInfo);
    }

    public void setBeanFactory(ConfigurableListableBeanFactory bf) {
        Object impl = getProxiedImplementation();
        bf.registerResolvableDependency(repoClass, impl);
        log.info("[DAO-Repo] class: {} is registered", repoClass.getCanonicalName());
    }
}
