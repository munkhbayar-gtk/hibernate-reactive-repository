# hibernate-reactive-repository

## Spring WebFlux + Spring Data + Hibernate Reactive
<a href="https://github.com/spring-projects/spring-framework/tree/main/spring-webflux">Spring WebFlux</a> and <a href="https://spring.io/projects/spring-data">Spring Data</a> don't support <a href="https://github.com/hibernate/hibernate-reactive">Hibernate Reactive<a/> natively and you will need to do some work.
  
If you are familiar with <strong>Spring-Data-JPA</strong> and the following code snippet  
```java
public interface UserRepository extends JpaRepository<User, Long> {

}
```

You can use the <strong>ReactiveHibernateCrudRepository</strong> interface in a reactive way
```java
public interface UserRepository extends ReactiveHibernateCrudRepository<User, Long> {
    
}
```

## spring-devtools class loader issue resolution
if you are using 
<a href="https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/using-boot-devtools.html">spring-boot-devtools</a>
<br>
create <strong>spring-devtools.properties</strong> in the following path
[PROJECT_PATH]/src/main/resources/META-INF

add the content below and save 

```properties
restart.include.hibernate-reactive-repository=/hibernate-reactive-repository.*.jar
```