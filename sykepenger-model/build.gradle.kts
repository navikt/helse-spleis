val jacksonVersion = "2.10.0"

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("commons-codec:commons-codec:1.11")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
