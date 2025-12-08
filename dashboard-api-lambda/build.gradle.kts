plugins {
	java
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.fbdco"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group="org.springframework.boot", module="spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("software.amazon.awssdk:sso:2.38.2")
    implementation("software.amazon.awssdk:s3:2.38.2")
    implementation("software.amazon.awssdk:cognitoidentityprovider:2.38.2")
    implementation("software.amazon.awssdk:dynamodb:2.38.2")
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.38.2")
    implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.4")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.jetbrains:annotations:24.1.0")
    testImplementation("com.amazonaws.serverless:aws-serverless-java-container-core:2.1.4")
    testImplementation("com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
    jvmArgs("-javaagent:${classpath.find { it.name.contains("mockito-core") }?.absolutePath}")
}

tasks.register<Zip>("buildZip") {
    from(tasks.named("compileJava"))
    from(tasks.named("processResources"))

    into("lib") {
        from(configurations.compileClasspath.get()) {
            exclude("tomcat-embed-*")
        }
    }
}

tasks.named("build") {
    dependsOn("buildZip")
}

