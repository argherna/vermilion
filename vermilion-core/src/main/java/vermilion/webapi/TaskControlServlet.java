package vermilion.webapi;

import java.io.IOException;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import vermilion.management.TaskController;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

/**
 * Controls tasks via HTTP requests.
 * 
 * 
 * @author andy
 * 
 */
@WebServlet(name = "TaskControl", urlPatterns = { "/tasks", "/tasks/*" })
public class TaskControlServlet extends HttpServlet {

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 6788902427551784072L;

    /**
     * Handles HTTP GET requests.
     * 
     * <p>
     * If successful, this method returns all the started Tasks as id/name
     * pairs. The sort order for the tasks is not guaranteed.
     * </p>
     * 
     * <p>
     * This method can return the following HTTP status codes:
     * <ul>
     * <li><strong>200</strong>: invocation was successful and the id/name pairs
     * for all started tasks is returned.</li>
     * <li><strong>500</strong>: an internal error occurred and an error message
     * will be returned.</li>
     * </ul>
     * </p>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        JMXConnector jmxc = null;

        try {
            jmxc = createJMXConnector();
            TaskController taskControllerProxy = createTaskControllerProxy(jmxc);
            Object[][] tasks = taskControllerProxy.tasks();

            resp.setContentType(MediaType.JSON_UTF_8.toString());
            JsonGenerator jsonGenerator = Json.createGenerator(resp
                    .getOutputStream());
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStartArray("tasks");
            for (Object[] task : tasks) {
                jsonGenerator.writeStartObject()
                        .write("id", ((Integer) task[0]).intValue())
                        .write("name", (String) task[1]).writeEnd();
            }
            jsonGenerator.writeEnd().writeEnd().flush();
            jsonGenerator.close();

            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (MalformedObjectNameException e) {
            prepareInternalServerErrorJson(resp);
            return;
        } finally {
            if (jmxc != null) {
                jmxc.close();
            }
        }
    }

    /**
     * Handles HTTP PUT requests.
     * 
     * <p>
     * The task named in the input is queued immediately, disregarding any
     * Schedule associated with it.
     * </p>
     * 
     * <p>
     * This method can return the following HTTP status codes:
     * <ul>
     * <li><strong>202</strong>: invocation was successful, the given task name
     * is queued and a status message is returned.</li>
     * <li><strong>500</strong>: an internal error occurred and an error message
     * will be returned.</li>
     * </ul>
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        MediaType mediaType = MediaType.parse(req
                .getHeader(HttpHeaders.CONTENT_TYPE));
        if (!(mediaType.equals(MediaType.JSON_UTF_8) || mediaType
                .equals(MediaType.JSON_UTF_8.withoutParameters()))) {
            prepareUnsupportedMediaTypeJson(resp);
            return;

        } else {

            String currentKey = "";
            String taskName = "";
            JsonParser parser = Json.createParser(req.getInputStream());
            while (parser.hasNext()) {
                switch (parser.next()) {
                case KEY_NAME:
                    currentKey = parser.getString();
                    break;
                case VALUE_STRING:
                    if ("task_name".equals(currentKey) && taskName.isEmpty()) {
                        taskName = parser.getString();
                    }
                default:
                    break;
                }
            }
            parser.close();

            if (stringValueNotSet().apply(taskName)) {
                prepareBadRequestJson(resp);
                return;
            }

            JMXConnector jmxc = null;
            try {
                jmxc = createJMXConnector();
                TaskController taskControllerProxy = createTaskControllerProxy(jmxc);
                taskControllerProxy.queueImmediately(taskName);

                prepareSuccessStatus(resp, HttpServletResponse.SC_ACCEPTED,
                        "Task queued");

            } catch (MalformedObjectNameException e) {
                prepareInternalServerErrorJson(resp);
                return;
            } finally {
                if (jmxc != null) {
                    jmxc.close();
                }
            }

        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer taskId = Integer.parseInt(req.getPathInfo().substring(1));
        JMXConnector jmxc = null;
        try {
            jmxc = createJMXConnector();
            TaskController taskControllerProxy = createTaskControllerProxy(jmxc);
            taskControllerProxy.stopTask(taskId);

            prepareSuccessStatus(resp, HttpServletResponse.SC_OK,
                    "Task is no longer scheduled");
            return;
        } catch (MalformedObjectNameException e) {
            prepareInternalServerErrorJson(resp);
            return;
        } finally {
            if (jmxc != null) {
                jmxc.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        MediaType mediaType = MediaType.parse(req
                .getHeader(HttpHeaders.CONTENT_TYPE));
        if (!(mediaType.equals(MediaType.JSON_UTF_8) || mediaType
                .equals(MediaType.JSON_UTF_8.withoutParameters()))) {
            prepareUnsupportedMediaTypeJson(resp);
            return;
        } else {

            String currentKey = "";
            String taskName = "";
            String taskClassname = "";
            String timeunit = "";
            Long initialDelay = Long.MIN_VALUE;
            Long interval = Long.MIN_VALUE;
            boolean inTask = false;

            JsonParser parser = Json.createParser(req.getInputStream());
            while (parser.hasNext()) {
                switch (parser.next()) {
                case START_OBJECT:
                    if ("task".equals(currentKey)) {
                        inTask = true;
                    }
                    break;

                case KEY_NAME:
                    currentKey = parser.getString();
                    break;

                case VALUE_STRING:
                    if (inTask) {
                        if ("task_name".equals(currentKey)
                                && taskName.isEmpty()) {
                            taskName = parser.getString();
                        } else if ("task_classname".equals(currentKey)
                                && taskClassname.isEmpty()) {
                            taskClassname = parser.getString();
                        } else if ("time_unit".equals(currentKey)
                                && timeunit.isEmpty()) {
                            timeunit = parser.getString();
                        }
                    }
                    break;

                case VALUE_NUMBER:
                    if (inTask) {
                        if ("initial_delay".equals(currentKey)
                                && initialDelay.equals(Long.MIN_VALUE)) {
                            initialDelay = parser.getLong();
                        } else if ("interval".equals(currentKey)
                                && interval.equals(Long.MIN_VALUE)) {
                            interval = parser.getLong();
                        }
                    }
                    break;

                case END_OBJECT:
                    if (inTask) {
                        inTask = false;
                    }
                    break;

                default:
                    break;
                }
            }
            parser.close();

            Predicate<Long> longValueNotSet = longValueNotSet();
            Predicate<String> stringValueNotSet = stringValueNotSet();
            if (stringValueNotSet.apply(taskName)) {
                prepareBadRequestJson(resp);
                return;
            } else if (stringValueNotSet.apply(taskClassname)) {
                prepareBadRequestJson(resp);
                return;
            } else if (stringValueNotSet.apply(timeunit)) {
                prepareBadRequestJson(resp);
                return;
            } else if (longValueNotSet.apply(interval)) {
                prepareBadRequestJson(resp);
                return;
            } else if (longValueNotSet.apply(initialDelay)) {
                prepareBadRequestJson(resp);
                return;
            }

            JMXConnector jmxc = null;
            try {
                jmxc = createJMXConnector();
                TaskController taskControllerProxy = createTaskControllerProxy(jmxc);
                Integer taskId = taskControllerProxy.startTask(taskName,
                        taskClassname, initialDelay, interval, timeunit);

                resp.setHeader(
                        HttpHeaders.LOCATION,
                        String.format("%s://%s:%d/%s/%d", req.getScheme(),
                                req.getServerName(), req.getServerPort(),
                                req.getServletPath(), taskId.intValue()));
                prepareSuccessStatus(resp, HttpServletResponse.SC_CREATED,
                        "Task started");
                return;
            } catch (MalformedObjectNameException e) {
                prepareInternalServerErrorJson(resp);
                return;
            } finally {
                if (jmxc != null) {
                    jmxc.close();
                }
            }

        }
    }

    private JMXConnector createJMXConnector() throws IOException {
        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi");
        return JMXConnectorFactory.connect(url, null);
    }

    private TaskController createTaskControllerProxy(JMXConnector jmxc)
            throws MalformedObjectNameException, IOException {
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName objectName = new ObjectName(TaskController.OBJECT_NAME);
        return JMX.newMBeanProxy(mbsc, objectName, TaskController.class);
    }

    private void prepareSuccessStatus(HttpServletResponse resp, int httpStatus,
            String message) throws IOException {
        resp.setContentType(MediaType.JSON_UTF_8.toString());
        resp.setStatus(httpStatus);

        JsonGenerator jsonGenerator = Json.createGenerator(resp
                .getOutputStream());
        jsonGenerator.writeStartObject();
        jsonGenerator.write("status", message).writeEnd().flush();
        jsonGenerator.close();
    }

    private void prepareInternalServerErrorJson(HttpServletResponse resp)
            throws IOException {
        resp.setContentType(MediaType.JSON_UTF_8.toString());
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        JsonGenerator jsonGenerator = Json.createGenerator(resp
                .getOutputStream());
        jsonGenerator.writeStartObject();
        jsonGenerator
                .write("message", "Unable to connect to internal service.")
                .writeEnd().flush();
        jsonGenerator.close();
    }

    private void prepareBadRequestJson(HttpServletResponse resp)
            throws IOException {
        resp.setContentType(MediaType.JSON_UTF_8.toString());
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        JsonGenerator jsonGenerator = Json.createGenerator(resp
                .getOutputStream());
        jsonGenerator.writeStartObject();
        jsonGenerator.write("message", "Malformed request: Json error.")
                .writeEnd().flush();
        jsonGenerator.close();
    }

    private void prepareUnsupportedMediaTypeJson(HttpServletResponse resp)
            throws IOException {
        resp.setContentType(MediaType.JSON_UTF_8.toString());
        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);

        JsonGenerator jsonGenerator = Json.createGenerator(resp
                .getOutputStream());
        jsonGenerator.writeStartObject();
        jsonGenerator
                .write("message",
                        "The server refused this request because the request entity is in a format not supported by the requested resource for the requested method.")
                .writeEnd().flush();
        jsonGenerator.close();
    }

    /**
     * Create and return a {@link Predicate} for evaluating conditions specified
     * on a String.
     * 
     * @return a Predicate for evaluating conditions on a String.
     */
    private static Predicate<String> stringValueNotSet() {
        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return Strings.nullToEmpty(input).isEmpty();
            }
        };
    }

    /**
     * Create and return a {@link Predicate} for evaluating conditions specified
     * on a Long.
     * 
     * @return a Predicate for evaluating conditions on a Long.
     */
    private static Predicate<Long> longValueNotSet() {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long input) {
                return input == null || input.longValue() == Long.MIN_VALUE;
            }
        };
    }
}
