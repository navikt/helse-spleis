dependencies {
    implementation("commons-codec:commons-codec:1.13")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
}

tasks {
    withType<org.gradle.api.tasks.testing.Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
        //systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    }
}
