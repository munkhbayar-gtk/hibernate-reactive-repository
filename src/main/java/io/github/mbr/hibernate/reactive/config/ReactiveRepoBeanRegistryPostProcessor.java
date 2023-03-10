package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import lombok.extern.slf4j.Slf4j;
import io.github.mbr.hibernate.reactive.config.annotations.ScanHibernateReactiveComponents;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.*;

@Slf4j

@Configuration
public class ReactiveRepoBeanRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private RepoImplFactory factory;//ReactiveHibernateRepositoryImpl repoImpl;
    public ReactiveRepoBeanRegistryPostProcessor() {
        log.debug("repoImpl ReactiveRepoBeanRegistryPostProcessor");
        //repoImpl = new ReactiveHibernateRepositoryImpl();
        factory = new RepoImplFactory();
    }
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        log.debug("repoImpl postProcessBeanDefinitionRegistry");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.debug("repoImpl postProcessBeanFactory");
        //repoImpl.registerRepoInterfaces(repos());
        //repoImpl.setBeanFactory(beanFactory);
        factory.createAndRegister(repos(), beanFactory);
        beanFactory.registerResolvableDependency(RepoImplFactory.class, factory);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ScanHibernateReactiveComponents.class);
        if(beans.isEmpty()){
            throw new IllegalStateException("ScanHibernateReactiveComponents annotation not found");
        }

        List<String[]> _repositoryPackagesToScan = new ArrayList<>();
        beans.forEach((k,v)->{
            Class<?> compClass = v.getClass();
            _repositoryPackagesToScan.add(new String[]{compClass.getPackageName()});

            ScanHibernateReactiveComponents configAnnotation =
                    compClass.getAnnotation(ScanHibernateReactiveComponents.class);
            _repositoryPackagesToScan.add(configAnnotation.baseRepositoryPackages());
        });

        if(_repositoryPackagesToScan.isEmpty()) {
            log.warn("ScanHibernateReactiveComponents.baseRepositoryPackages() = [], empty");
        }
        _repositoryPackagesToScan.forEach((String[] arr)->{
            repositoryPackagesToScan.addAll(List.of(arr));
        });
        log.debug("repositoryPackagesToScan: {}", repositoryPackagesToScan);
    }

    private List<String> repositoryPackagesToScan = new ArrayList<>();
    private List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos() {

        Reflections refs = new Reflections(repositoryPackagesToScan.toArray(new String[]{}));
        Set<Class<?>> subTypes =
                refs.get(SubTypes.of(ReactiveHibernateCrudRepository.class).asClass());

        return subTypes.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private Class<? extends ReactiveHibernateCrudRepository<?,?>> convert(Class<?> clz) {
        String nm = clz.getCanonicalName();
        try{
            return (Class<? extends ReactiveHibernateCrudRepository<?,?>>)Class.forName(nm);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
