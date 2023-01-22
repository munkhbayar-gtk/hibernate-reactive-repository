package org.github.mbr.hibernate.reactive.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@ToString

@Slf4j

@Configuration
@ConfigurationProperties(prefix="spring")
public class ReactivePersistenceProperties implements  InitializingBean {

    public ReactivePersistenceProperties(){
        id = new Random().nextInt(10000);
        log.debug("ReactivePersistentUnitConfigImpl: {} {}", id, this);

    }
    private Map<String, String> datasource = new HashMap<>();
    private JpaProps jpa = new JpaProps();

    public Map<String, String> getDataSource() {
        return datasource;
    }
    public Map<String, String> getHibernateProperties() {
        return jpa.hibernate();
    }

    int id = 1;
    public int id() {
        return id;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("initialized: {}", this);
    }

    @Setter
    @ToString
    public static class JpaProps {
        //properties
        private Map<String, String> properties = new HashMap<>();
        private Map<String, String> hibernate = new HashMap<>();

        public final Map<String, String> hibernate() {
            Map<String, String> ret = new HashMap<>();
            properties.forEach((k,v)->{
                if(Objects.equals(k.toLowerCase(), "hibernate")) {
                    ret.put(k, v);
                }
            });
            hibernate.forEach((k,v)->{
                ret.put("hibernate." + k, v);
            });
            return ret;
        }
    }
}
