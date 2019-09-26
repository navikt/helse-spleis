plugins {
    id("java")
    id("maven-publish")
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {

            pom {
                name.set("github-package-registry-gradle")
                description.set("A test project for the maven-publish plugin")
                url.set("https://github.com/navikt/github-package-registry-gradle")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/navikt/github-package-registry-gradle.git")
                    developerConnection.set("scm:git:https://github.com/navikt/github-package-registry-gradle.git")
                    url.set("https://github.com/navikt/github-package-registry-gradle")
                }
            }
            from(components["java"])
        }
    }
}
