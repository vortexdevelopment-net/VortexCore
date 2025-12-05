package net.vortexdevelopment.vortexcore.database;

import java.sql.Connection;

public interface DatabaseConnector {

    Connection getConnection();

    void connect(VoidConnection connection);

    <T> T connect(ConnectionResult<T> connection);

    static interface VoidConnection {

        void connect(Connection connection) throws Exception;

    }

    static interface ConnectionResult<T> {

        T connect(Connection connection) throws Exception;

    }
}
