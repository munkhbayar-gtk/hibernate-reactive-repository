package io.github.mbr.hibernate.reactive.config;


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

    private Map<String, String> jpa = new HashMap<>();

    //spring.jpa.properties.hibernate.SQL=debug => SQL=debug
    //spring.jpa.hibernate.ddl-auto=create-drop => ddl-auto
    public final Map<String, String> getJpaProperties(String key) {
        Map<String, String> ret = new HashMap<>();
        jpa.forEach((k,v)->{
            if(k.startsWith(key)) {
                String nKey = k.substring(key.length());
                ret.put(nKey, v);
            }
        });
        return ret;
    }

    //private JpaProps jpa = new JpaProps();

    public Map<String, String> getDataSource() {
        return datasource;
    }


    int id = 1;
    public int id() {
        return id;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("initialized: {}", this);
    }
}
