package io.github.mbr.hibernate.reactive.config.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScanHibernateReactiveComponents {

    String[] baseRepositoryPackages() default {};
    String[] baseEntitiesPackages() default {};


}
