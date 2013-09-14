package vermilion.management;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface PooledDataSource {

    ConnectionPoolType value() default ConnectionPoolType.TOMCAT_JDBC;
    
    public enum ConnectionPoolType {

        /**
         * Indicates that c3p0 is used for the connection pool.
         */
        C3P0,

        /**
         * Indicates that commons-dbcp is used for the connection pool.
         */
        COMMONS_DBCP,
        
        /**
         * Indicates that Oracle UCP is used for the connection pool.
         */
        ORACLE_UCP,
        
        /**
         * Indicates that tomcat-jdbc is used for the connection pool.
         */
        TOMCAT_JDBC;
        
    }
}
