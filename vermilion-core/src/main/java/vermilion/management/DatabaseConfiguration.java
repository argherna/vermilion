package vermilion.management;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Qualifier for database configurations.
 * 
 * @author andy
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface DatabaseConfiguration {

    /**
     * The database installation location. Defaults to
     * {@link Installation#LOCAL}.
     */
    Installation installation() default Installation.LOCAL;

    /**
     * The database vendor. Defaults to {@link Vendor#HSQLDB}.
     */
    Vendor vendor() default Vendor.HSQLDB;

    /**
     * Describes the type of installation of the database.
     * 
     * @author andy
     * 
     */
    public enum Installation {

        /**
         * Indicates a local installation.
         */
        LOCAL,

        /**
         * Indicates a remote installation.
         */
        REMOTE;
    }

    /**
     * Describes the database vendor.
     * 
     * @author andy
     * 
     */
    public enum Vendor {

        /**
         * Identifies the database type as HSQLDB.
         */
        HSQLDB,

        /**
         * Identifies the database type as MySQL.
         */
        MYSQL,

        /**
         * Identifies the database type as Oracle.
         */
        ORACLE,

        /**
         * Identifies the database type as PostgreSQL.
         */
        POSTGRESQL;

    };
}
