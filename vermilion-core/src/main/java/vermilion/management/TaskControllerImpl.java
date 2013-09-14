package vermilion.management;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import vermilion.core.Listeners;
import vermilion.core.NamedRunnable;
import vermilion.core.Schedule;
import vermilion.core.ScheduledQueuingService;
import vermilion.core.Schedules;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;

/**
 * StandardMBean implementation of TaskController.
 * 
 * <p>
 * Instances of this class can read an optional properties file named
 * {@code tasks.properites}. If this file is present, tasks can be automatically
 * scheduled and started by this class. This is convenient when an instance of
 * this class is constructed in a Main class.
 * </p>
 * 
 * <p>
 * The properties file must be at the top of the classpath. The first property
 * defined has the name {@code tasks}. It's value is a comma and space delimited
 * list of task names to register. For example:
 * 
 * <pre>
 * tasks = SimpleTask, OtherSimpleTask
 * </pre>
 * 
 * Each named task in this list has its own set of properties:
 * 
 * <pre>
 * SimpleTask.runnable = vermilion.core.SimpleTask
 * SimpleTask.execution.interval = 5
 * SimpleTask.execution.timeunit = SECONDS
 * 
 * OtherSimpleTask.runnable = vermilion.core.SimpleTask
 * OtherSimpleTask.execution.interval = 10
 * OtherSimpleTask.execution.timeunit = SECONDS
 * </pre>
 * 
 * The first set of properties sets up the task named "SimpleTask". The name of
 * the {@code runnable} property is the fully qualified class name (
 * {@code vermilion.SimpleTask}). The {@code execution.interval} property is the
 * number of time units to between submitting the instance of the NamedRunnable
 * for execution. The {@code execution.timeunit} property is the time units for
 * the execution interval. The value must be one of the values of the
 * {@linkplain TimeUnit} class in either upper (which is preferred) or lower
 * case.
 * </p>
 * 
 * <p>
 * Instances of this class have a reference to the shared task queue used by the
 * Service instances in this package. The queue can be managed by an instance of
 * this class.
 * </p>
 * 
 * <p>
 * <strong>Implementation note</strong>: A (static) JDK logger
 * {@linkplain Class#getSimpleName() named} for this class logs certain
 * information at {@linkplain Level.CONFIG}, {@linkplain Level.INFO}, and
 * {@linkplain Level.WARNING}.
 * </p>
 * 
 * @author andy
 * @see Properties
 * @see TimeUnit
 * @see Logger
 */
public class TaskControllerImpl extends StandardMBean implements TaskController {

    private static final Logger logger = Logger
            .getLogger(TaskControllerImpl.class.getSimpleName());

    private final BlockingQueue<NamedRunnable> taskQueue;

    private final StateTransition stateTransition;

    private final ConcurrentMap<String, ScheduledQueuingService> services;

    private final ConcurrentMap<Integer, String> idServices = Maps
            .newConcurrentMap();

    private final SequenceGenerator serviceIdSeq = new SequenceGenerator();

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the {@code tasks.properties} file is found at the top of the
     * classpath, it is loaded and the defined tasks are registered, scheduled,
     * and started.
     * </p>
     * 
     * @param mbeanInterface
     *            the ServiceFactory Interface class.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     * @throws NotCompliantMBeanException
     */
    public TaskControllerImpl(Class<?> mbeanInterface,
            BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition) throws NotCompliantMBeanException {
        this(mbeanInterface, false, taskQueue, stateTransition,
                new ConcurrentHashMap<String, ScheduledQueuingService>(), true);
    }

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the {@code tasks.properties} file is found at the top of the
     * classpath, it is loaded and the defined tasks are registered, scheduled,
     * and started.
     * </p>
     * 
     * @param mbeanInterface
     *            the exported management interface.
     * @param isMXBean
     *            flag indicating if this instance is an MXBean.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     */
    public TaskControllerImpl(Class<?> mbeanInterface, boolean isMXBean,
            BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition) {
        this(mbeanInterface, isMXBean, taskQueue, stateTransition,
                new ConcurrentHashMap<String, ScheduledQueuingService>(), true);
    }

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the {@code tasks.properties} file is found at the top of the
     * classpath, it is loaded and the defined tasks are registered, scheduled,
     * and started.
     * </p>
     * 
     * @param implementation
     *            the implementation of this bean.
     * @param mbeanInterface
     *            the exported management interface.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     * @throws NotCompliantMBeanException
     */
    public <T> TaskControllerImpl(T implementation, Class<T> mbeanInterface,
            BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition) throws NotCompliantMBeanException {
        this(implementation, mbeanInterface, true, taskQueue, stateTransition,
                new ConcurrentHashMap<String, ScheduledQueuingService>(), true);
    }

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the {@code tasks.properties} file is found at the top of the
     * classpath, it is loaded and the defined tasks are registered, scheduled,
     * and started.
     * </p>
     * 
     * @param implementation
     *            the implementation of this bean.
     * @param mbeanInterface
     *            the exported management interface.
     * @param isMXBean
     *            flag indicating if this instance is an MXBean.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     */
    public <T> TaskControllerImpl(T implementation, Class<T> mbeanInterface,
            boolean isMXBean, BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition) {
        this(implementation, mbeanInterface, isMXBean, taskQueue,
                stateTransition,
                new ConcurrentHashMap<String, ScheduledQueuingService>(), true);
    }

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the autoStart parameter is {@code true} and the
     * {@code tasks.properties} file is found at the top of the classpath, it is
     * loaded and the defined tasks are registered, scheduled, and started.
     * </p>
     * 
     * <p>
     * By passing a non-empty map of scheduled tasks, these automatically become
     * manageable by this class.
     * </p>
     * 
     * @param mbeanInterface
     *            the exported management interface.
     * @param isMXBean
     *            flag indicating if this instance is an MXBean.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     * @param tasks
     *            pre-populated map of scheduled tasks keyed by a name.
     * @param autoStart
     *            if <code>true</code>, process the configuration file.
     */
    public TaskControllerImpl(Class<?> mbeanInterface, boolean isMXBean,
            BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition,
            ConcurrentMap<String, ScheduledQueuingService> tasks,
            boolean autoStart) {
        super(mbeanInterface, isMXBean);
        this.taskQueue = taskQueue;
        this.stateTransition = stateTransition;
        this.services = tasks;
        if (autoStart) {
            autoStart();
        }
    }

    /**
     * Constructs a new ServiceFactoryImpl.
     * 
     * <p>
     * If the autoStart parameter is {@code true} and the
     * {@code tasks.properties} file is found at the top of the classpath, it is
     * loaded and the defined tasks are registered, scheduled, and started.
     * </p>
     * 
     * <p>
     * By passing a non-empty map of scheduled tasks, these automatically become
     * manageable by this class.
     * </p>
     * 
     * @param implementation
     *            the implementation of this bean.
     * @param mbeanInterface
     *            the exported management interface.
     * @param isMXBean
     *            flag indicating if this instance is an MXBean.
     * @param taskQueue
     *            shared task queue.
     * @param stateTransition
     *            the StateTransition for the tasks.
     * @param tasks
     *            pre-populated map of scheduled tasks keyed by a name.
     * @param autoStart
     *            if <code>true</code>, process the configuration file.
     */
    public <T> TaskControllerImpl(T implementation, Class<T> mbeanInterface,
            boolean isMXBean, BlockingQueue<NamedRunnable> taskQueue,
            StateTransition stateTransition,
            ConcurrentMap<String, ScheduledQueuingService> tasks,
            boolean autoStart) {
        super(implementation, mbeanInterface, isMXBean);
        this.taskQueue = taskQueue;
        this.stateTransition = stateTransition;
        this.services = tasks;

        if (autoStart) {
            autoStart();
        }
    }

    private void autoStart() {
        Properties tasks = loadTaskProperties();
        if (tasks.size() > 0) {
            List<String> taskNames = Lists.newArrayList(tasks.getProperty(
                    "tasks").split(",\\s*"));
            for (String taskName : taskNames) {
                String taskClassname = tasks.getProperty(String.format(
                        "%s.runnable", taskName));
                Long initialDelay = Long.valueOf(tasks.getProperty(
                        String.format("%s.execution.initialDelay", taskName),
                        "0"));
                Long interval = Long.valueOf(tasks.getProperty(String.format(
                        "%s.execution.interval", taskName)));
                String timeunit = tasks.getProperty(String.format(
                        "%s.execution.timeunit", taskName));
                startTask(taskName, taskClassname, initialDelay, interval,
                        timeunit);
            }
        } else {
            logger.fine("No tasks specified for automatic start. Skipping.");
        }
    }

    private Properties loadTaskProperties() {
        Properties tasks = new Properties();
        URL tasksPropertiesFile = getClass().getResource("/tasks.properties");
        if (tasksPropertiesFile != null) {

            try (InputStream tasksIn = tasksPropertiesFile.openStream()) {

                tasks.load(tasksIn);

            } catch (IOException e) {
                LogRecord record = new LogRecord(Level.CONFIG,
                        "NON-FATAL: Could not load tasks.properties file.");
                record.setThrown(e);
                logger.log(record);
            }

        } else {
            logger.config("tasks.properties not found.");
        }
        return tasks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer startTask(String taskName, String taskClassname,
            Long initialDelay, Long interval, String timeunit) {

        NamedRunnable task = loadRunnable(taskClassname);
        task.setName(taskName);
        Schedule schedule = Schedules.createSchedule(initialDelay, interval,
                TimeUnit.valueOf(timeunit.toUpperCase()));
        ScheduledQueuingService sqs = new ScheduledQueuingService(schedule,
                task, taskQueue);
        sqs.addListener(Listeners.createLoggingServiceListener(taskName),
                MoreExecutors.sameThreadExecutor());
        Future<State> serviceStarted = sqs.start();

        Integer id = Integer.MIN_VALUE;
        try {
            if (serviceStarted.get() == State.RUNNING) {
                services.put(taskName, sqs);
                id = serviceIdSeq.next();
                idServices.put(id, taskName);
                logger.info("Task scheduling service has been created and started.");
            }
        } catch (InterruptedException | ExecutionException e) {
            LogRecord record = new LogRecord(Level.WARNING,
                    "Exception during task scheduling service start. Service will not be started.");
            record.setThrown(e);
            logger.log(record);
        }

        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTask(String taskName) {
        Service serviceToStop = services.get(taskName);
        if (serviceToStop != null) {
            Future<State> serviceStopped = serviceToStop.stop();

            try {
                if (serviceStopped.get() == State.TERMINATED) {
                    logger.warning("Scheduling service for " + taskName
                            + " stopped.");
                }
            } catch (InterruptedException | ExecutionException e) {
                LogRecord record = new LogRecord(Level.WARNING,
                        "Exception during service stop.");
                record.setThrown(e);
                logger.log(record);
            } finally {
                services.remove(taskName);
                for (Integer taskId : idServices.keySet()) {
                    if (taskName.equals(idServices.get(taskId))) {
                        idServices.remove(taskId);
                        break;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTask(Integer taskId) {
        String taskName = idServices.get(taskId);
        if (taskName != null) {
            Service serviceToStop = services.get(taskName);
            if (serviceToStop != null) {
                Future<State> serviceStopped = serviceToStop.stop();

                try {
                    if (serviceStopped.get() == State.TERMINATED) {
                        logger.warning("Scheduling service for " + taskName
                                + " stopped.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LogRecord record = new LogRecord(Level.WARNING,
                            "Exception during service stop.");
                    record.setThrown(e);
                    logger.log(record);
                } finally {
                    services.remove(taskName);
                    idServices.remove(taskId);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[][] tasks() {

        Object[][] tasks = new Object[idServices.size()][2];
        int i = 0;
        for (Integer taskId : idServices.keySet()) {
            tasks[i][0] = taskId;
            tasks[i++][1] = idServices.get(taskId);
        }

        return tasks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queueImmediately(String taskName) {
        ScheduledQueuingService toQueue = services.get(taskName);
        if (toQueue != null) {
            taskQueue.add(toQueue.getTask());
            logger.info(String.format("%s queued.", taskName));
        }
    }

    private NamedRunnable loadRunnable(String classname) {
        NamedRunnable runnable = null;

        try {
            @SuppressWarnings("unchecked")
            Class<NamedRunnable> runnableClass = (Class<NamedRunnable>) Class
                    .forName(classname);

            runnable = (NamedRunnable) Proxy.newProxyInstance(
                    StateTransition.class.getClassLoader(),
                    new Class<?>[] { NamedRunnable.class },
                    new StatefulNamedRunnable(runnableClass.newInstance(),
                            stateTransition));
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException e) {

            LogRecord record = new LogRecord(Level.CONFIG,
                    "Could not instantiate " + classname + ".");
            record.setThrown(e);
            logger.log(record);

            runnable = null;
        }

        return runnable;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanParameterInfo serviceNameParam = new MBeanParameterInfo(
                "serviceName", "java.lang.String",
                "Name of the service to manipulate.");

        MBeanParameterInfo taskNameParam = new MBeanParameterInfo("taskName",
                "java.lang.String", "Name of the task to manipulate.");

        MBeanParameterInfo serviceClassnameParam = new MBeanParameterInfo(
                "taskClassname", "java.lang.String",
                "Class name of a NamedRunnable that is submitted to the task queue.");

        MBeanParameterInfo executionIntervalParam = new MBeanParameterInfo(
                "interval", "java.lang.Long", "Execution interval.");

        MBeanParameterInfo timeUnitParam = new MBeanParameterInfo(
                "intervalTimeunit", "java.lang.String",
                "Time unit for the execution interval.");

        MBeanOperationInfo[] operations = {
                new MBeanOperationInfo("startTask",
                        "Starts a new scheduled task.",
                        new MBeanParameterInfo[] { serviceClassnameParam,
                                executionIntervalParam, timeUnitParam },
                        "java.lang.Integer", MBeanOperationInfo.ACTION),
                new MBeanOperationInfo("stopTask",
                        "Removes the named scheduled service.",
                        new MBeanParameterInfo[] { serviceNameParam }, "void",
                        MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(
                        "queueImmediately",
                        "Adds the named task to the queue; allows ad-hoc submission of a task.",
                        new MBeanParameterInfo[] { taskNameParam }, "void",
                        MBeanOperationInfo.ACTION),
                new MBeanOperationInfo("tasks",
                        "Lists the currently started tasks with an identifier",
                        null, "java.lang.Object[][]", MBeanOperationInfo.INFO) };

        return new MBeanInfo(getClass().getName(), "ServiceFactory MBean",
                null, null, operations, null);
    }

    @Override
    public void preDeregister() throws Exception {
        super.preDeregister();
        logger.warning("Shutting down started services.");

        try {
            for (String serviceName : services.keySet()) {
                ListenableFuture<?> svcStop = services.get(serviceName).stop();
                svcStop.addListener(
                        Listeners.createServiceStopListener(serviceName),
                        MoreExecutors.sameThreadExecutor());
                svcStop.get(500l, TimeUnit.MILLISECONDS);
            }
        } finally {
            services.clear();
            idServices.clear();
        }
    }
}
