package vermilion.core;

import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.Service.Listener;

public class Listeners {

    private Listeners() {
        // empty constructor
    }

    public static Listener createLoggingServiceListener(String taskName) {
        return new LoggingServiceListener(taskName);
    }

    public static Runnable createServiceStopListener(String serviceName) {
        return new StopListener(serviceName);
    }

    public static Listener createExecutorServiceControlListener(
            ExecutorService executor) {
        return new ExecutorServiceControlListener(executor);
    }
}
