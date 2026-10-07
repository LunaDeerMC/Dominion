plugins {
    id("java")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// utf-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    testImplementation(project(":api"))
    testImplementation(project(":core"))
    testImplementation("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
