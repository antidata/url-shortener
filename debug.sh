java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5004 -XX:+CMSClassUnloadingEnabled -Xmx16096M -Xss2M -jar `dirname $0`/sbt-launch.jar "$@"
