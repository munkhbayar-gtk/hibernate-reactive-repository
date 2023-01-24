package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactivePersistentUnitInfo;
import lombok.extern.slf4j.Slf4j;
import io.github.mbr.hibernate.reactive.config.annotations.ScanHibernateReactiveComponents;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.provider.ReactivePersistenceProvider;
import org.hibernate.reactive.stage.Stage;
import org.reflections.Reflections;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

@Slf4j

@Configuration
public class ReactivePersistenceUnitBeanConfig {


    @Bean
    public EntityManagerFactory emf(ReactivePersistenceProperties props,
                                    RepoImplFactory repoImplFactory,
                                    ApplicationContext applicationContext) {
        log.debug("creating emf: {} {}", props.id(), props);
        Properties properties = hibernateProperties(props);

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ScanHibernateReactiveComponents.class);

        List<String[]> entityPackagesToScan = new ArrayList<>();
        List<String[]> repositoryPackagesToScan = new ArrayList<>();


        if(!beans.isEmpty()){
            beans.forEach((k,v)->{
                ScanHibernateReactiveComponents configAnnotation =
                        v.getClass().getAnnotation(ScanHibernateReactiveComponents.class);
                repositoryPackagesToScan.add(configAnnotation.baseRepositoryPackages);
                entityPackagesToScan.add(configAnnotation.baseEntitiesPackages);
            });
        }

        List<String> entityClasses = collectEntityClasses(entityPackagesToScan);

        //repoImplFactory.setRepositoryPackagesToScan(entityPackagesToScan);

        return new ReactivePersistenceProvider()
                .createContainerEntityManagerFactory(persistenceUnitInfo(properties, entityClasses), properties);
    }

    @Bean
    public Mutiny.SessionFactory mutinySessionFactory(EntityManagerFactory emf) {
        return emf.unwrap(Mutiny.SessionFactory.class);
    }

    @Bean
    public Stage.SessionFactory stageSessionFactory(EntityManagerFactory emf) {
        return emf.unwrap(Stage.SessionFactory.class);
    }

    @Bean
    public ReactivePersistentUnitInfo createPersistentUnitInfo(EntityManagerFactory emf,
                                                               Mutiny.SessionFactory mSessionFactory,
                                                               Stage.SessionFactory sSessionFactory,
                                                               RepoImplFactory repoImplFactory,
                                                               ApplicationContext applicationContext
    ){
        ReactivePersistentUnitInfo ret = new ReactivePersistentUnitInfo(emf, mSessionFactory, sSessionFactory);
        repoImplFactory.setReactivePersistentUnitInfo(ret);

        //Set<?> annos = applicationContext.findAllAnnotationsOnBean(null, ScanHibernateReactiveComponents.class, true);
        //log.debug("annos: {}", annos);

        return ret;
    }

    private Properties hibernateProperties(ReactivePersistenceProperties props) {
        Properties prop = new Properties();
        //prop.putAll(props.getHibernateProperties());
        Map<String, String> hibernateProperties = props.getJpaProperties("hibernate.");
        log.debug("hb: {}", hibernateProperties);
        hibernateProperties.forEach((k,v)->{
            prop.put("hibernate."+k, v);
        });
        Map<String, String> properties = props.getJpaProperties("properties.hibernate.");
        log.debug("hb: {}", properties);
        prop.putAll(properties);

        props.getDataSource().forEach((k,v)->{
            prop.put("hibernate.connection."+k, v);
        });

        log.debug("properties: {}", prop);
        return prop;
    }
    private List<String> collectEntityClasses (List<String[]> entityPackages) {
        List<String> packages = new ArrayList<>();
        entityPackages.forEach((arr)->{
            packages.addAll(List.of(arr));
        });
        Reflections refs = new Reflections(packages.toArray(new String[]{}));

        Set<Class<?>> annotatedEntities = refs.get(SubTypes.of(TypesAnnotated.with(Entity.class)).asClass());

        return annotatedEntities.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.toList());
    }
    private PersistenceUnitInfo persistenceUnitInfo(Properties properties, List<String> entityClasses) {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "default";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return ReactivePersistenceProvider.class.getCanonicalName();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(this.getClassLoader().getResources(""));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return entityClasses;//.stream().map(Class::getCanonicalName).collect(Collectors.toList());
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return SharedCacheMode.UNSPECIFIED;
            }

            @Override
            public ValidationMode getValidationMode() {
                return ValidationMode.AUTO;
            }

            @Override
            public Properties getProperties() {
                return properties;
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return "2.2";
            }

            @Override
            public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }
}
