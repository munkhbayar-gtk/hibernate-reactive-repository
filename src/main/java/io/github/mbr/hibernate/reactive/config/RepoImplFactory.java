package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import io.github.mbr.hibernate.reactive.ReactiveHibernateRepositoryImpl;
import io.github.mbr.hibernate.reactive.ReactivePersistentUnitInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RepoImplFactory {

    private List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos = new ArrayList<>();
    private List<ReactiveHibernateRepositoryImpl> impls;

    void setReactivePersistentUnitInfo(ReactivePersistentUnitInfo persistentUnitInfo) {
        impls.forEach(impl->{
            impl.setReactivePersistentUnitInfo(persistentUnitInfo);
        });
    }
    void createAndRegister(List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos,
                           ConfigurableListableBeanFactory beanFactory){
        this.repos.addAll(repos);
        impls = new ArrayList<>(this.repos.size());
        this.repos.forEach(cl->{
            ReactiveHibernateRepositoryImpl impl = new ReactiveHibernateRepositoryImpl();
            //impl.bindInterface(cl);
            impl.bindRepoInterfaceMetaData(createMetaData(cl));
            impl.setBeanFactory(beanFactory);
            impls.add(impl);
        });
    };

    private RepoInterfaceMetaData createMetaData(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass) {
        return RepoInterfaceMetaData.of(repoInterfaceClass);
    }
}
