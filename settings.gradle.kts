rootProject.name = "sykepenger-spleis"
include("jobs", "sykepenger-api", "sykepenger-model", "sykepenger-mediators", "sykepenger-opprydding-dev")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("testcontainers", "1.17.3")
            version("rapids-and-rivers", "2022.05.19-14.18.e3dc97b518d8")
            version("postgres", "42.5.0")
            version("hikari", "5.0.1")
            version("kotliquery", "1.7.0")
            version("cloudsql", "1.7.0")
            version("flyway", "9.3.1")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgres")
            library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
            library("kotliquery", "com.github.seratch", "kotliquery").versionRef("kotliquery")

            library("cloudsql", "com.google.cloud.sql", "postgres-socket-factory").versionRef("cloudsql")
            library("flyway", "org.flywaydb", "flyway-core").versionRef("flyway")

            library("testcontainers", "org.testcontainers", "postgresql").versionRef("testcontainers")

            bundle("database", listOf("postgresql", "hikari", "kotliquery"))
        }
    }
}