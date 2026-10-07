plugins {
    id("java")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Keep the Java 25 toolchain for the Minecraft 26 dev bundle, but emit
    // bytecode that Java 21 Paper remappers can read.
    options.compilerArgs.addAll(listOf("-source", "21", "-target", "21"))
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    paperweight.paperDevBundle("26.3.build.+")
    testImplementation(project(":core"))
    testImplementation(project(":api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    workingDir = layout.buildDirectory.dir("test-runtime").get().asFile.apply { mkdirs() }
}

// MC 26 dev bundles ship Mojang-mapped — no reobfuscation needed
tasks.named("reobfJar") {
    enabled = false
}
