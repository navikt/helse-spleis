val commonsCodecVersion = "1.15"

dependencies {
    api("commons-codec:commons-codec:$commonsCodecVersion")

    implementation(project(":sykepenger-primitiver"))
    implementation(project(":sykepenger-aktivitetslogg"))
}