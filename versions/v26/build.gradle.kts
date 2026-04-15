plugins {
    id("java")
    id("io.papermc.paperweight.userdev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

// utf-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    paperweight.paperDevBundle("26.1.2.build.+")
}

// MC 26 dev bundle ships Mojang-mapped — no reobfuscation needed
tasks.named("reobfJar") {
    enabled = false
}
