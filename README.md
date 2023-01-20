# hibernate-reactive-repository

## Spring WebFlux + Spring Data + Hibernate Reactive
<a href="https://github.com/spring-projects/spring-framework/tree/main/spring-webflux">Spring WebFlux</a> and <a href="https://spring.io/projects/spring-data">Spring Data</a> don't support <a href="https://github.com/hibernate/hibernate-reactive">Hibernate Reactive<a/> natively and you will need to do some work.
  
If you are familiar with Spring-Data-JPA and the following code snippet  
```java
public interface UserRepository extends JpaRepository<User, Long> {

}
```

You can use the <strong>ReactiveHibernateCrudRepository</a> interface in a reactive way
```java
public interface UserRepository extends ReactiveHibernateCrudRepository<User, Long> {
    
}
```
