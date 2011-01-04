
# path to where the Akiban Server will store its data files
SERVER_DATAPATH="/tmp/server_data"

# port on which to start the Akiban Server
SERVER_PORT="5140"

# Specifies the default port over which the akiban server will be available for
# JMX connections.
JMX_PORT="8080"

# Here we create the arguments that will get passed to the jvm when
# starting the akiban server.

JVM_OPTS="$JVM_OPTS -Dcserver.datapath=$SERVER_DATAPATH"
JVM_OPTS="$JVM_OPTS -Dcserver.port=$SERVER_PORT"

# enable assertions.  what kind of difference to performance does this make?
JVM_OPTS="$JVM_OPTS -ea"

# enable thread priorities, primarily so we can give periodic tasks
# a lower priority to avoid interfering with client workload
JVM_OPTS="$JVM_OPTS -XX:+UseThreadPriorities"
# allows lowering thread priority without being root.  see
# http://tech.stolsvik.com/2010/01/linux-java-thread-priorities-workaround.html
JVM_OPTS="$JVM_OPTS -XX:ThreadPriorityPolicy=42"

calculate_heap_size()
{
    case "`uname`" in
        Linux)
            system_memory_in_mb=`free -m | awk '/Mem:/ {print $2}'`
            MAX_HEAP_SIZE=$((system_memory_in_mb / 2))M
            return 0
        ;;
        *)
            MAX_HEAP_SIZE=1024M
            return 1
        ;;
    esac
}

# The amount of memory to allocate to the JVM at startup, you almost
# certainly want to adjust this for your environment. If left commented
# out, the heap size will be automatically determined by calculate_heap_size
# MAX_HEAP_SIZE="4G"
if [ "x$MAX_HEAP_SIZE" = "x" ]; then
    calculate_heap_size
fi

# min and max heap sizes should be set to the same value to avoid
# stop-the-world GC pauses during resize, and so that we can lock the
# heap in memory on startup to prevent any of it from being swapped
# out.
JVM_OPTS="$JVM_OPTS -Xms$MAX_HEAP_SIZE"
JVM_OPTS="$JVM_OPTS -Xmx$MAX_HEAP_SIZE"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError" 

# GC logging options -- uncomment to enable
# JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
# JVM_OPTS="$JVM_OPTS -XX:+PrintGCTimeStamps"
# JVM_OPTS="$JVM_OPTS -XX:+PrintClassHistogram"
# JVM_OPTS="$JVM_OPTS -XX:+PrintTenuringDistribution"
# JVM_OPTS="$JVM_OPTS -XX:+PrintGCApplicationStoppedTime"
# JVM_OPTS="$JVM_OPTS -Xloggc:/var/log/akiban/gc.log"

# Prefer binding to IPv4 network intefaces (when net.ipv6.bindv6only=1). See 
# http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6342561 (short version:
# comment out this entry to enable IPv6 support).
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true"

# jmx: metrics and administration interface
# 
# add this if you're having trouble connecting:
# JVM_OPTS="$JVM_OPTS -Djava.rmi.server.hostname=<public name>"
# 
# see 
# http://blogs.sun.com/jmxetc/entry/troubleshooting_connection_problems_in_jconsole
# for more on configuring JMX through firewalls, etc. (Short version:
# get it working with no firewall first.)
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT" 
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.ssl=false" 
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.authenticate=false" 

JVM_OPTS="$JVM_OPTS -Xrunjdwp:transport=dt_socket,address=8000,suspend=n,server=y"

