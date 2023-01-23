package io.github.mbr.hibernate.reactive.config.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ScanHibernateReactiveComponents {

    String baseRepositoryPackages[] = new String[]{};
    String baseEntitiesPackages[] = new String[]{};


}
