package vermilion.webapi;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import vermilion.core.Listeners;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Web application server Service.
 * 
 * <p>
 * Instances of this service expose the web api used to monitor and control the
 * tasks.
 * </p>
 * 
 * @author andy
 * 
 */
public class WebAppServerService extends AbstractService {

    private volatile Tomcat tomcat;

    private volatile ExecutorService internalExecutor;

    /**
     * Construct a WebAppServerService to listen on port 8080.
     */
    public WebAppServerService() {
        this(8080);
    }

    /**
     * Construct a new WebAppServerService.
     * 
     * @param port
     *            the port the web application server will listen on.
     */
    @Inject
    public WebAppServerService(int port) {
        tomcat = new Tomcat();
        tomcat.setPort(port);

        File docbase = new File(System.getProperty("java.io.tmpdir"));
        Context root = tomcat.addContext("", docbase.getAbsolutePath());

        Tomcat.addServlet(root, "TaskControl", new TaskControlServlet());
        root.addServletMapping("/tasks", "TaskControl");
        root.addServletMapping("/tasks/*", "TaskControl");
    }

    @Override
    protected void doStart() {
        internalExecutor = Executors.newSingleThreadExecutor();
        addListener(
                Listeners
                        .createExecutorServiceControlListener(internalExecutor),
                MoreExecutors.sameThreadExecutor());
        internalExecutor.execute(new Runnable() {
            public void run() {
                try {
                    tomcat.start();
                    notifyStarted();
                    doRun();
                } catch (LifecycleException e) {
                    notifyFailed(e);
                }
            }
        });
    }

    void doRun() {
        if (isRunning()) {
            tomcat.getServer().await();
        }
    }

    @Override
    protected void doStop() {
        try {
            tomcat.stop();
            tomcat.destroy();
            notifyStopped();
        } catch (LifecycleException e) {
            notifyFailed(e);
        }
    }
}
