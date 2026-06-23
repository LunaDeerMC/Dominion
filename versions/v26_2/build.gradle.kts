plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    paperweight.paperDevBundle("26.2.build.+")
}

// MC 26+ dev bundle ships Mojang-mapped — no reobfuscation needed
tasks.named("reobfJar") {
    enabled = false
}
