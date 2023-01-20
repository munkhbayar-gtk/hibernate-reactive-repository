package mbr.hibernate.reactive.config;

import lombok.extern.slf4j.Slf4j;
import mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import mbr.hibernate.reactive.ReactiveHibernateRepositoryImpl;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.*;

@Slf4j

@Configuration
public class ReactiveRepoBeanRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor {

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

    /*
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.debug("repoImpl setApplicationContext");
        //repoImpl.setApplicationContext(applicationContext);
    }
     */
    private List<Class<? extends ReactiveHibernateCrudRepository<?,?>>> repos() {

        Reflections refs = new Reflections("mbr.demowebflux");
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
