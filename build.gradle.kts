import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.30"
//    kotlin("jvm") version "1.3.21"
}

group = "de.mallox.maven"
version = "1.0-SNAPSHOT"

var neo4jVersion = "3.5.5"

repositories {
    mavenLocal()

    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // https://github.com/shrinkwrap/resolver
    api("org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-depchain:3.1.3")
    api("org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-parent:3.1.3")

    api ("org.apache.maven:maven-embedder:3.3.9")

    api("org.eclipse.aether:aether-connector-basic:1.0.2.v20150114")
    api("org.eclipse.aether:aether-transport-wagon:1.0.2.v20150114")
    api("org.apache.maven.wagon:wagon-http:2.9")
    api("org.apache.maven.wagon:wagon-provider-api:2.9")
    api("org.apache.maven.wagon:wagon-http-lightweight:2.9")
    runtime("org.slf4j:slf4j-simple:1.7.26")

    runtime ("org.neo4j.procedure:apoc:3.5.0.2")
    
    implementation (files(org.gradle.internal.jvm.Jvm.current().toolsJar))

    api("org.neo4j:neo4j:$neo4jVersion")
    // api("org.neo4j.driver:neo4j-java-driver:1.7.5")

    api ("com.hubspot.jinjava:jinjava:2.0.11-java7")
    api ("net.lingala.zip4j:zip4j:1.3.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}