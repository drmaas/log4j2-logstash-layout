import com.bmuschko.gradle.nexus.NexusPlugin

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath("com.bmuschko:gradle-nexus-plugin:2.3.1")
    }
}

group = "me.drmaas"

plugins {
    java
    id("nebula.release") version "6.3.0"
    `maven`
    `maven-publish`
}

apply<JavaLibraryPlugin>()
apply<NexusPlugin>()

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    compile("org.apache.logging.log4j:log4j-api:2.11.1")
    compile("org.apache.logging.log4j:log4j-core:2.11.1")

    compile("org.apache.commons:commons-csv:1.2")

    // JACKSON for JSONification
    compile("com.fasterxml.jackson.core:jackson-core:2.9.6")
    compile("com.fasterxml.jackson.core:jackson-annotations:2.9.6")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.6")
    compile("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.6")
    compile("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.9.6")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.6")

    // Testing stuff, hamcrest comes first please
    testCompile("org.hamcrest:hamcrest-all:1.3")
    testCompile("org.testng:testng:6.14.3")
    testCompile("commons-collections:commons-collections:3.2.2")

    //This JSON Hamcrest Matcher is pretty useful (https://github.com/hertzsprung/hamcrest-json)
    testCompile("uk.co.datumedge:hamcrest-json:0.2")
}

tasks.withType(Test::class) {
    useTestNG()
}

tasks {
    val wrapper by creating(Wrapper::class) {
        gradleVersion = "4.10.2"
    }
    val sourceJar by creating(Jar::class) {
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }
    getByName<Upload>("uploadArchives") {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    pom.project {
                        withGroovyBuilder {
                            "name"("log4j2-logstash-jsonevent-layout")
                            "description"("Log4J2 Layout as a Logstash json_event")
                            "url"("https://github.com/drmaas/log4j2-logstash-jsonevent-layout")
                            "inceptionYear"("2018")
                            "scm" {
                                "url"("https://github.com/drmaas/log4j2-logstash-jsonevent-layout")
                                "connection"("scm:https://drmaas@github.com/drmaas/log4j2-logstash-jsonevent-layout.git")
                                "developerConnection"("scm:git://github.com/drmaas/log4j2-logstash-jsonevent-layout.git")
                            }
                            "licenses" {
                                "license" {
                                    "name"("The Apache Software License, Version 2.0")
                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    "distribution"("repo")
                                }
                            }
                            "developers" {
                                "developer" {
                                    "id"("drmaas")
                                    "name"("Dan Maas")
                                    "email"("drmaas@gmail.com")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


