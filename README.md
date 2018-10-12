log4j2-logstash-layout
================================

[![Build Status](https://travis-ci.org/drmaas/log4j2-logstash-layout.svg?branch=master)](https://travis-ci.org/drmaas/log4j2-logstash-layout)

Log4J2 Layout in Logstash json format.

# Credits

Parts of this work were inspired by https://github.com/majikthys/log4j2-logstash-jsonevent-layout

# Overview

This is a log4j2 layout that produces json that is compliant to logstash v1 spec. JSON produced is a serialization of a given log4j2 LogEvent and is intentionally very similar to that produced by the default log4j2 [JSONLayout](http://logging.apache.org/log4j/2.x/manual/layouts.html). You may use this layout out of the box to connect your java application to a logstash server with maximal speed and minimal redundant processing.

This layout is fast, flexible, and other superlatives starting with f. 

(see http://logging.apache.org/log4j/2.x/manual/layouts.html and  http://logstash.net/) 

# Download
https://search.maven.org/search?q=a:log4j2-logstash-layout

# Getting Started

You'll need an application that uses [log4j2](http://logging.apache.org/) and an install of [logstash](http://logstash.net/). Explaining these perquisites is an exercise left for the reader, but we'll provide you a configuration sample.

(see http://logging.apache.org/ and http://logstash.net/)

## Simple Configuration: Log4j2 system out logger

This is the simplest form of the intended use. Once you have this configuration working, you should be able to customize the configuration to your specific needs.

Example Log4j2 log4j2.xml:

<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="JsonAppender" target="SYSTEM_OUT">
            <LogstashJsonLayout compact="true" eventEol="true" />
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="JsonLogger" level="INFO" additivity="false">
            <AppenderRef ref="JsonAppender" />
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="JsonAppender"/>
        </Root>
    </Loggers>
</Configuration>

### Example output
You should see in your logstash console a message like:

    {
      "@version" : "1",
      "@timestamp" : "2015-07-28T15:24:53.386-07:00",
      "timeMillis" : 1438122293386,
      "thread" : "main",
      "level" : "DEBUG",
      "loggerName" : "org.apache.logging.log4j.core.layout.LogStashJSONLayoutJacksonIT",
      "message" : "Test Message",
      "endOfBatch" : false,
      "loggerFqcn" : "org.apache.logging.log4j.core.layout.LogStashJSONLayoutJacksonIT",
      "Foo": "Bar",
      "A": "B"
    }

Where `Foo` and `A` are values added to the MDC. This layout places MDC attributes at the top level of the log event for easier indexing and readability, and replaces the `contextMap` object. Note tha the `contextMap` object can still be enabled if desired.

### Using in uberjars

In gradle, use the latest [shadow plugin](https://github.com/johnrengelman/shadow). Then configure it as follows:
```
shadowJar {
    ...
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer)
    ...
}
```

## Log4j2 Logstash Layout In More Detail

### Logstash LogEvent JSON Layout:
Conceptually this is a mashup between log4j2 logevent json schema and logstash event schema. The only required elements are @version and @timestamp which, as you'll note, is in logstash's native format so no modification needs to happen here. 
 
#### Log4j2 LogEvent Elements

Log4j2 LogEvent elements have been expanded and a means to omit/include via configuration included. Those elements are:

 * logger: logger name 
 * level: log level
 * thread: thread name
 * message: log message
 * throwable: the thrown exception
 * LocationInfo (element): where the log originated
 * Properties (element): thread context map
 * log : product of sublayout, element or attribute (see below)
 * * : anything... see discussion on Arbitrary KeyValues
 
#### Arbitrary Key Values Pairs
This is a very important mechanism that allows you to include arbitrary key values. This is the means by which you should insert application/server context such as application name, host name, cluster name, cluster location, etc. These can be hardcoded in log4j2.xml or you can use log4j2 [Lookups](http://logging.apache.org/log4j/2.x/manual/lookups.html) to render them from configuration values. Examples are provided of both environment and system lookups are given in the sample log4j2.xml, above.

Project Needs: YOU CAN HELP :)

	* Step by step example
	* Tests need to be fleshed out.
	* Maven Artifact needs to be published to bintray.


### Building from source
We use gradle to build. Don't worry, it's embedded.

To generate a jar file, use:
./gradlew clean build 

This will produce a jar file that you may import into your current project. For example:
build/libs/log4j2-logstash-layout-0.0.1-dev.0.uncommitted+6e1a8df.jar

