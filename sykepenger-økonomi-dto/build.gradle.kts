plugins {
    `maven-publish`
}

configure<JavaPluginExtension> {
    withSourcesJar()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.github.navikt.spleis.økonomi"
            artifactId = project.name
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/helse-spleis")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_PASSWORD")
            }
        }
    }
}
