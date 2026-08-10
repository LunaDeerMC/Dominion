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
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    paperweight.paperDevBundle("26.2.build.+")
}

// MC 26 dev bundles ship Mojang-mapped — no reobfuscation needed
tasks.named("reobfJar") {
    enabled = false
}
