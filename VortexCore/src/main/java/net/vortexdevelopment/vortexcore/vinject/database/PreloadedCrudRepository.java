package net.vortexdevelopment.vortexcore.vinject.database;

import net.vortexdevelopment.vinject.annotation.database.EnableCaching;
import net.vortexdevelopment.vinject.database.cache.CachePolicy;
import net.vortexdevelopment.vinject.database.repository.CrudRepository;

/**
 * A CRUD repository with preloaded static caching enabled.
 * Should only be used where we know the data set is small
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
@EnableCaching(
        preload = true,
        policy = CachePolicy.STATIC,
        maxSize = Integer.MAX_VALUE
)
public interface PreloadedCrudRepository<T, ID> extends CrudRepository<T, ID> {
}
