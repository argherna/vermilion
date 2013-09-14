package vermilion.management;

import static vermilion.management.PooledDataSource.ConnectionPoolType.TOMCAT_JDBC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import vermilion.core.NamedRunnable;
import vermilion.core.NamedRunnable.NamedRunnableState;

public class JdbcStateTransition implements StateTransition {

    private final DataSource dataSource;

    private static final Logger logger = Logger
            .getLogger(JdbcStateTransition.class.getName());

    private static final String INSERT_TASK_EXECUTION_SQL = "INSERT INTO "
            + "task_execution (task_name) VALUES (?)";

    private static final String INSERT_TASK_EXEC_STATUS_1_SQL = "INSERT INTO "
            + "task_execution_status (task_exec_id, exec_status) VALUES (?, ?)";

    @Inject
    public JdbcStateTransition(
            @PooledDataSource(TOMCAT_JDBC) Provider<DataSource> dataSourceProvider) {
        this.dataSource = dataSourceProvider.get();
    }

    @Override
    public void abandonded(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.ABANDONED);
    }

    @Override
    public void completed(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.COMPLETED);
    }

    @Override
    public void failed(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.FAILED);
    }

    @Override
    public void starting(NamedRunnable task) {
        Connection conn = null;
        PreparedStatement taskExecutionPs = null;
        PreparedStatement taskExecutionStatusPs = null;
        ResultSet generatedKeys = null;

        try {
            conn = dataSource.getConnection();
            taskExecutionPs = conn.prepareStatement(INSERT_TASK_EXECUTION_SQL,
                    Statement.RETURN_GENERATED_KEYS);
            taskExecutionPs.setString(1, task.getName());

            int affectedRows = taskExecutionPs.executeUpdate();
            if (affectedRows > 0) {
                generatedKeys = taskExecutionPs.getGeneratedKeys();
                if (generatedKeys.next()) {
                    task.setExecutionId(generatedKeys.getInt(1));
                } else {
                    logger.warning("Saved task execution to the database, but was unable to get back an execution Id. State transitions will not be able to be recorded in the database.");
                }
            } else {
                logger.warning("Task execution was not saved to the database. State transition will not be able to be recorded.");
                task.setExecutionId(Integer.MIN_VALUE);
            }

            taskExecutionStatusPs = conn
                    .prepareStatement(INSERT_TASK_EXEC_STATUS_1_SQL);
            taskExecutionStatusPs.setInt(1, task.getExecutionId());
            taskExecutionStatusPs.setString(2,
                    NamedRunnable.NamedRunnableState.STARTING.name());
            affectedRows = taskExecutionStatusPs.executeUpdate();
            if (affectedRows == 0) {
                logger.warning("Task execution status was not saved to the database. State transition will not be able to be recorded.");
            }
        } catch (SQLException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Unable to record state transition in the database.");
            record.setThrown(e);
            logger.log(record);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(taskExecutionPs);
            closeQuietly(taskExecutionStatusPs);
            closeQuietly(conn);
            task.setState(NamedRunnableState.STARTING);
        }
    }

    @Override
    public void started(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.STARTED);
    }

    @Override
    public void stopping(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.STOPPING);
    }

    @Override
    public void stopped(NamedRunnable task) {
        recordTransition(task, NamedRunnableState.STOPPED);
    }

    private void recordTransition(NamedRunnable task, NamedRunnableState state) {
        Connection conn = null;
        PreparedStatement taskExecutionStatusPs = null;

        try {
            conn = dataSource.getConnection();

            taskExecutionStatusPs = conn
                    .prepareStatement(INSERT_TASK_EXEC_STATUS_1_SQL);
            taskExecutionStatusPs.setInt(1, task.getExecutionId());
            taskExecutionStatusPs.setString(2, state.name());
            int affectedRows = taskExecutionStatusPs.executeUpdate();
            if (affectedRows == 0) {
                logger.warning("Task execution status was not saved to the database. State transition will not be able to be recorded.");
            }
        } catch (SQLException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Unable to record state transition in the database.");
            record.setThrown(e);
            logger.log(record);
        } finally {
            closeQuietly(taskExecutionStatusPs);
            closeQuietly(conn);
            task.setState(NamedRunnableState.STARTED);
        }
    }

    private void closeQuietly(ResultSet resultSet) {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
        } catch (SQLException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Unable to release database resources.");
            record.setThrown(e);
            logger.log(record);
        }
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Unable to release database resources.");
            record.setThrown(e);
            logger.log(record);
        }
    }

    private void closeQuietly(Statement stmt) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Unable to release database resources.");
            record.setThrown(e);
            logger.log(record);
        }
    }
}
