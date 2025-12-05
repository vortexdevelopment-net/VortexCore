package net.vortexdevelopment.vortexcore.database;

import net.vortexdevelopment.vinject.annotation.Repository;
import net.vortexdevelopment.vinject.database.repository.CrudRepository;

@Repository
public interface MigrationRepository extends CrudRepository<MigrationVersion, Long> {

}