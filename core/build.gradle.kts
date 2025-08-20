plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// utf-8
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    maven("https://repo.mikeprimm.com/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:v2.6.2")
    compileOnly("us.dynmap:DynmapCoreAPI:3.4")
    compileOnly("xyz.jpenilla:squaremap-api:1.3.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("maven.modrinth:pl3xmap:1.20.4-489")
}
