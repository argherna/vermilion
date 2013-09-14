package vermilion.management;

import static vermilion.management.DatabaseConfiguration.Installation.LOCAL;
import static vermilion.management.DatabaseConfiguration.Vendor.HSQLDB;
import static vermilion.management.PooledDataSource.ConnectionPoolType.TOMCAT_JDBC;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;

/**
 * A {@linkplain Provider} of a {@linkplain DataSource}.
 * 
 * <p>
 * There is only 1 instance of a DataSource returned by an instance of this
 * provider. Repeated calls to {@link #get() get} will lazily construct and
 * initialize a DataSource.
 * </p>
 * 
 * <p>
 * This is a code-as-configuration class, enforcing configurations to be correct
 * at compile time.
 * </p>
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger
 * {@linkplain Class#getName() named} for this class logs error messages at
 * {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * 
 */
@PooledDataSource(TOMCAT_JDBC)
public class DefaultDataSourceProvider implements Provider<DataSource> {

    private static final Logger logger = Logger
            .getLogger(DefaultDataSourceProvider.class.getName());

    private final Provider<PoolConfiguration> poolConfigProvider;

    private org.apache.tomcat.jdbc.pool.DataSource dataSource;

    @Inject
    public DefaultDataSourceProvider(
            @DatabaseConfiguration(installation = LOCAL, vendor = HSQLDB) Provider<PoolConfiguration> poolConfigProvider) {
        this.poolConfigProvider = poolConfigProvider;
    }

    @Override
    public DataSource get() {
        if (dataSource == null) {
            dataSource = new org.apache.tomcat.jdbc.pool.DataSource(
                    poolConfigProvider.get());
            try {
                dataSource.createPool();
            } catch (SQLException ex) {

                LogRecord record = new LogRecord(
                        Level.WARNING,
                        "Problem establishing connection pool. Database services won't be able to be used.");
                record.setThrown(ex);
                logger.log(record);
            }
        }
        return dataSource;
    }

}
