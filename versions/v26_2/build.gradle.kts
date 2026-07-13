plugins {
    id("java")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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
