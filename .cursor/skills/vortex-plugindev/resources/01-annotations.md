# Annotations

## Registry Annotations != @Component

`@RegisterListener`, `@Command`, `@Api`, `@RegisterReloadHook` are handled by `@Registry` handlers. They are not `@Component`.

Runtime flow:
1. Handler runs at `RegistryOrder.COMPONENTS`
2. `dependencyContainer.newInstance(clazz)` creates and injects the instance
3. `@Component` registration only if `@Component` is explicitly on the class

## Do NOT

```java
// WRONG - redundant @Component
@Component
@RegisterListener
public class MyListener implements Listener {
    @Inject private MyConfig config;
}
```

```java
// RIGHT
@RegisterListener
public class MyListener implements Listener {
    @Inject private MyConfig config;
}
```

## When @Component IS Required

```java
// Another class injects this manager
@Component
public class OrderService {
    @Inject private PaymentGateway gateway;
}

// API binding - interfaces auto-register; no registerSubclasses required
@Component
public class StackedEntityManagerImpl implements StackedEntityManager { }

// Multiple impls of same interface - name producer and qualify consumer
@Component(name = "primary")
public class PrimaryPortImpl implements Port { }

@Qualifier("secondary")
@Component
public class SecondaryPortImpl implements Port { }

@Component
public class Consumer {
    @Inject @Qualifier("secondary")
    private Port port;
}
```

Injecting a shared interface without `@Qualifier` when multiple beans exist reports `VINJECT-DEP-002` at build time. Inject the concrete class instead, or name/qualify beans as shown above.

## YAML Holders

```java
// Usually no @Component
@YamlDirectory(dir = "wands", target = SellWandTypeEntry.class)
@RegisterReloadHook
public final class WandsDirectory {
    @YamlCollection
    private Map<String, SellWandType> wands = new ConcurrentHashMap<>();
}
```

Add `@Component` on a YAML holder only if other classes `@Inject` the holder type and you need explicit singleton semantics. Injection via `@Inject WandsDirectory` works without `@Component` because YAML instances are registered in the container during the YAML phase.

## @Root.componentAnnotations

IDE-only (IntelliJ). Does not affect runtime. Do not use it as a reason to skip or add `@Component`.

## Lifecycle on Registry Classes

`@PostConstruct` works on `@RegisterListener` and `@Command` classes (fires during `newInstance()`).

`@OnLoad` is for YAML/entity hydration only - use `@PostConstruct` for general setup.

## References

- [docs/annotations.md](../../../docs/annotations.md)
- `VInject/docs/components-and-injection.md`
- `VortexCore/.../handler/RegisterListenerHandler.java`
