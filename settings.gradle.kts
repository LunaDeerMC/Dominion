plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Dominion"

include(
    "api",
    "core",
    "versions:v1_20_1",
    "versions:v1_21",
    "versions:v1_21_4",
    "versions:v1_21_6",
    "versions:v1_21_8",
    "versions:v1_21_9",
    "versions:v26"
)