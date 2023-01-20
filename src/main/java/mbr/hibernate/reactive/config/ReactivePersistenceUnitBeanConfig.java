package mbr.hibernate.reactive.config;

import lombok.extern.slf4j.Slf4j;
import mbr.hibernate.reactive.ReactiveHibernateRepositoryImpl;
import mbr.hibernate.reactive.ReactivePersistentUnitInfo;
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
    public EntityManagerFactory emf(ReactivePersistenceProperties props) {
        log.debug("creating emf: {} {}", props.id(), props);
        Properties properties = hibernateProperties(props);
        List<String> entityClasses = collectEntityClasses(props);
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
                                                               RepoImplFactory repoImplFactory
    ){
        ReactivePersistentUnitInfo ret = new ReactivePersistentUnitInfo(emf, mSessionFactory, sSessionFactory);
        repoImplFactory.setReactivePersistentUnitInfo(ret);
        return ret;
    }

    private Properties hibernateProperties(ReactivePersistenceProperties props) {
        Properties prop = new Properties();
        prop.putAll(props.getHibernateProperties());

        log.debug("hb: {}", prop);

        props.getDataSource().forEach((k,v)->{
            if(Objects.equals("username", k)){
                k = "user";
            }else if(Objects.equals("driverClassName", k)){
                k = "driver";
            }
            prop.put("javax.persistence.jdbc."+k, v);
        });
        Map<String, String> ds = props.getDataSource();
        log.debug("DataSource: {}", ds);
        if(ds != null) {
            prop.put("dialect", ds.getOrDefault("dialect", ""));
        }else{
            log.debug("No DataSource properties injected");
        }

        System.out.println(prop);
        return prop;
    }
    private List<String> collectEntityClasses (ReactivePersistenceProperties props) {
        Reflections refs = new Reflections("mbr");
        Set<Class<?>> annotatedEntities = refs.get(SubTypes.of(TypesAnnotated.with(Entity.class)).asClass());

        return annotatedEntities.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.toList());
    }
    private static PersistenceUnitInfo persistenceUnitInfo(Properties properties, List<String> entityClasses) {
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
