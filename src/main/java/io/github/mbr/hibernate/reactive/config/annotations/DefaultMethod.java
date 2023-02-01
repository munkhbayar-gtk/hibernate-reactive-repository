package io.github.mbr.hibernate.reactive.config.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultMethod {
}
