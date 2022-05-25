val commonsCodecVersion = "1.15"
val jsonassertVersion = "1.5.0"

dependencies {
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
