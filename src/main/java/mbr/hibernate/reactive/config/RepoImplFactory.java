package mbr.hibernate.reactive.config;

import mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import mbr.hibernate.reactive.ReactiveHibernateRepositoryImpl;
import mbr.hibernate.reactive.ReactivePersistentUnitInfo;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

public class RepoImplFactory {

    private List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos = new ArrayList<>();
    private List<ReactiveHibernateRepositoryImpl> impls;

    void setReactivePersistentUnitInfo(ReactivePersistentUnitInfo persistentUnitInfo) {
        impls.forEach(impl->{
            impl.setReactivePersistentUnitInfo(persistentUnitInfo);
        });
    }
    void createAndRegister(List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos, ConfigurableListableBeanFactory beanFactory){
        this.repos.addAll(repos);
        impls = new ArrayList<>(this.repos.size());
        this.repos.forEach(cl->{
            ReactiveHibernateRepositoryImpl impl = new ReactiveHibernateRepositoryImpl();
            impl.bindInterface(cl);
            impl.setBeanFactory(beanFactory);
            impls.add(impl);
        });
    };
}
