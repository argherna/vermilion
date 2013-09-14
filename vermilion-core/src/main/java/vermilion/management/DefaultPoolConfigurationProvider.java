package vermilion.management;

import javax.inject.Provider;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import vermilion.management.DatabaseConfiguration.Installation;
import vermilion.management.DatabaseConfiguration.Vendor;

/**
 * A {@linkplain Provider} for a {@linkplain PoolConfiguration} that configures
 * a database connection for a local HSQLDB installation.
 * 
 * <p>
 * This is a code-as-configuration class, enforcing configurations to be correct
 * at compile time.
 * </p>
 * 
 * @author andy
 * 
 */
@DatabaseConfiguration(installation = Installation.LOCAL, vendor = Vendor.HSQLDB)
public class DefaultPoolConfigurationProvider implements
        Provider<PoolConfiguration> {

    @Override
    public PoolConfiguration get() {
        PoolConfiguration poolConfig = new PoolProperties();
        poolConfig.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        poolConfig.setUrl("jdbc:hsqldb:mem:.");
        poolConfig.setUsername("sa");
        poolConfig.setPassword("");
        poolConfig.setInitialSize(5);
        poolConfig.setMaxActive(10);
        poolConfig.setMaxIdle(7);
        poolConfig.setJmxEnabled(true);
        poolConfig.setJdbcInterceptors(getJdbcInterceptorsClassnames());
        return poolConfig;
    }

    private String getJdbcInterceptorsClassnames() {
        return new StringBuilder()
                .append("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;")
                .append("org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;")
                .append("org.apache.tomcat.jdbc.pool.interceptor.StatementCache")
                .toString();
    }

}
