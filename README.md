# hibernate-reactive-repository

## Spring WebFlux + Spring Data + Hibernate Reactive
<a href="https://github.com/spring-projects/spring-framework/tree/main/spring-webflux">Spring WebFlux</a> and <a href="https://spring.io/projects/spring-data">Spring Data</a> don't support <a href="https://github.com/hibernate/hibernate-reactive">Hibernate Reactive<a/> natively and you will need to do some boilerplate code work.
  
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
## Usage
1. Create a spring-webflux project
<strong>pom.xml</strong> looks like
```xml
<properties>
    <java.version>17</java.version>
</properties>
<dependences>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny-reactor</artifactId>
        <version>1.7.0</version>
    </dependency>
    <dependency>
        <groupId>org.github.mbr.hibernate</groupId>
        <artifactId>hibernate-reactive-repository</artifactId>
        <version>1.0.6-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-mysql-client</artifactId>
        <version>4.3.7</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependences>
```
2. Create an entity class

```java
import lombok.*;
import javax.persistence.*;

@Getter
@Setter

@Entity
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "name")
    public String name;
}
```
3. Create the hibernate reactive repository
```java
public interface UserRepository extends ReactiveHibernateCrudRepository<User, Long> {
    
}
```
4. Create a Controller
```java
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
}
``` 
5. run app
```bash
>mvn spring-boot:run
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