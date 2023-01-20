package mbr.hibernate.reactive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import javax.persistence.EntityManagerFactory;

@AllArgsConstructor
@Getter
public class ReactivePersistentUnitInfo {
    private final EntityManagerFactory emf;
    private final Mutiny.SessionFactory mutinySessionFactory;
    private final Stage.SessionFactory stageSessionFactory;
}
