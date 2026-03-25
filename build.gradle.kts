plugins {
    kotlin("jvm") version "2.3.10"
    `maven-publish`
}

group = "com.github.onlaait"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.apache.logging.log4j:log4j-api-kotlin:1.5.0")
    testImplementation(kotlin("test"))
    testImplementation("org.apache.logging.log4j:log4j-core:2.25.3")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}