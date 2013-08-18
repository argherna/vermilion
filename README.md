vermilion
=========

A task scheduler

## Desecription

Vermilion is intended to be a replacement for Java processes run by cron on unix systems. By using a long-running JVM process and (mostly) standard Java APIs, frequently run Java processes are able to be executed more quickly without having to wait for the JVM to be spun up for each run. 

## Features

* Implement your own tasks using standard Java APIs
* JMX and Web API access to controlling scheduling and execution of tasks

## Requirements

* JDK/JRE 1.7 (build and run)
* Maven 3.0.4 or newer (build)

## Starting/Stopping

### Start

Edit [.vermilionrc](https://github.com/argherna/vermilion/blob/master/vermilion-cli/src/main/resources/bin/.vermilionrc) to set your `JAVA_HOME` environment variable, then run [start_vm.sh](https://github.com/argherna/vermilion/blob/master/vermilion-cli/src/main/resources/bin/start_vm.sh). 

### Stop

#. Using [jps](http://docs.oracle.com/javase/7/docs/technotes/tools/share/jps.html), find the PID with the class name `Main`.
#. Issue a `kill PID`. The vermilion instance should shut down shortly (allowing for certain tasks to complete). To kill the process immediately, issue a `kill -9 PID`. This will force vermilion to shut down, but it will leave any running tasks in an inconsistent state.

## Implementing Tasks

The interface `vermilion.core.NamedRunnable` extends the `java.lang.Runnable` interface by adding a getter and setter for a Name of a task. An example [SimpleTask](https://github.com/argherna/vermilion/blob/master/vermilion-core/src/main/java/vermilion/core/SimpleTask.java) shows how to implement this.

You can edit the `tasks.properties` file to have your task started when you spin up a vermilion instance. 

## JMX

Vermilion can be controlled via JMX port 9999. Use [VisualVm](http://visualvm.java.net) with the MBeans plugin to access methods that control what vermilion does.

## HTTP API

Vermilion can be controlled via HTTP. The HTTP API uses JSON to send and receive messages.
