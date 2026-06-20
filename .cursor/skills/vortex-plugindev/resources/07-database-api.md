# Database & API

## Entity + Repository

```java
@Entity
public class PlayerData {
    @Id private UUID id;
    @Column private int balance; // wrapper types, not primitives
}

@Repository
public interface PlayerDataRepository extends CrudRepository<PlayerData, UUID> { }
```

Requires `vinject-maven-plugin` for `@Entity`.

Initialize DB in `onPluginLoad()`: `initDatabase(new _1_InitialMigration());`

## API Module Pattern

```java
// API module - optional @Component(name) when multiple impls exist
@Component(name = "stacker")
public class StackedEntityManagerImpl implements StackedEntityManager { }

// Or inject concrete impl / use @Qualifier on consumer
@Api
public final class VortexStackerApi {
    @Inject @Getter private static StackedEntityManager entityManager;
}
```

Interfaces are auto-registered for `@Component` implementations - `registerSubclasses` is optional for extra aliases only.

When multiple beans implement the same interface, use `@Component(name = "...")` on producers and `@Inject @Qualifier("...")` on consumers. The analyzer reports `VINJECT-DEP-002` without a qualifier.

## References

- `VInject/docs/database_and_repositories.md`
- VortexStacker-API + VortexStacker-Plugin layout
