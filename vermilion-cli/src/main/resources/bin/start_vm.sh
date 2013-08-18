#!/bin/bash

# ------------------------------------------------------------------------------
#
# start_vr.sh
#
# Starts vermilion.
#
# Arguments:
# <ARGUMENTS>
#
# author: andy <URL:mailto:argherna@gmail.com>
#
# ------------------------------------------------------------------------------


# Find the directory we're running in (important for setting the classpath).
#
source="${BASH_SOURCE[0]}"
dir="$( dirname "$SOURCE" )"
while [ -h "$source" ]
do 
  source="$(readlink "$source")"
  [[ $source != /* ]] && source="$dir/$source"
  dir="$( cd -P "$( dirname "$source"  )" && pwd )"
done
dir="$( cd -P "$( dirname "$source" )" && pwd )"
dir="$(dirname "$dir")"

# ------------------------------------------------------------------------------
#
#	      Pre-run checks (if these fail, then don't even run)
#
# ------------------------------------------------------------------------------

# Source in bin/.vermilionrc to get important system variables.
#
if [ -r $dir/bin/.vermilionrc ]; then
  . $dir/bin/.vermilionrc
fi

# Verify JAVA_HOME has been set, else exit.
#
if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME not set. Exiting."
  exit 1
fi


# ------------------------------------------------------------------------------
#
#				Script variables
#
# ------------------------------------------------------------------------------

# Set the classpath needed by the Java program. This will consist of adding the
# conf directory (since it is convention for Java programs to load configuration
# files from the classpath) and the jar files in the lib directory.
#
classpath=$dir/conf
for jarfile in $dir/lib/*.jar; do
  classpath=${classpath}:${jarfile}
done

# Set the JVM options and system properties.
# 
# The JVM running the Java program will be configured with:
# - Minimum heap size of 8 megabytes
# - Maximum heap size of 32 megabytes
# - classpath as above
#
java_opts="-Xms8m"
java_opts="${java_opts} -Xmx32m"
java_opts="${java_opts} -cp ${classpath}"

# The JVM running the Java program will use the system properties:
#
# - java.util.logging.config.file
#       Location of jul configuration file.
#
sysprops="-Djava.util.logging.config.file=${dir}/conf/logging.properties"
sysprops="${sysprops} -Djava.io.tmpdir=${dir}/temp"
sysprops="${sysprops} -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
sysprops="${sysprops} -Dcom.sun.management.jmxremote.port=9999"
sysprops="${sysprops} -Dcom.sun.management.jmxremote.authenticate=false"
sysprops="${sysprops} -Dcom.sun.management.jmxremote.ssl=false"

# The absolute class name for the class containing the main method.
#
mainclass="vermilion.runtime.Main"

# ------------------------------------------------------------------------------
#
#				Script execution
#
# ------------------------------------------------------------------------------

# Execute the Java program. Pass in any arguments passed to this script.
#
$JAVA_HOME/bin/java \
  ${java_opts} \
  ${sysprops} \
  ${mainclass} \
  $*
