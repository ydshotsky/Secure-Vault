<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <springProperty scope="context" name="LOKI_URL" source="LOKI_URL"
                    defaultValue="http://localhost:3100/loki/api/v1/push"/>
    <springProperty scope="context" name="LOKI_USER" source="LOKI_USERNAME"
                    defaultValue="000000"/>
    <springProperty scope="context" name="LOKI_PASS" source="LOKI_PASSWORD"
                    defaultValue="none"/>

    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http class="com.github.loki4j.logback.JavaHttpSender">
            <url>${LOKI_URL}</url>
            <auth>
                <username>${LOKI_USER}</username>
                <password>${LOKI_PASS}</password>
            </auth>
        </http>
        <format>
            <label>
                <pattern>app=secure-vault,host=${HOSTNAME},level=%level</pattern>
            </label>
            <message>
                <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            </message>
            <sortByTime>true</sortByTime>
        </format>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>
</configuration>