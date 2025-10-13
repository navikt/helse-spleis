plugins {
    `maven-publish`
}

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver"))
    api(project(":sykepenger-økonomi-dto"))

    testImplementation(testFixtures(project(":sykepenger-primitiver")))

    // fordi BeløptidslinjeDsl bruker assertEquals()
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
}

configure<JavaPluginExtension> {
    withSourcesJar()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.github.navikt.spleis"
            artifactId = "sykepenger-okonomi"
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
