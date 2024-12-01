package me.ceze88.vortexcore.database;

import org.jooq.DSLContext;

import java.sql.Connection;

public interface DatabaseConnector {

    Connection getConnection();

    void connect(VoidConnection connection);

    <T> T connect(ConnectionResult<T> connection);

    void connectDSL(VoidDSLConnection connection);

    <T> T connectDSL(DSLConnectionResult<T> connection);

    static interface VoidConnection {

        void connect(Connection connection) throws Exception;

    }

    static interface ConnectionResult<T> {

        T connect(Connection connection) throws Exception;

    }

    static interface VoidDSLConnection {

        void connect(DSLContext dslContext) throws Exception;

    }

    static interface DSLConnectionResult<T> {

        T connect(DSLContext dslContext) throws Exception;

    }
}
