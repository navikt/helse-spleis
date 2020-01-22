package no.nav.helse

internal object Topics {
    const val søknadTopic = "syfo-soknad-v2"
    const val inntektsmeldingTopic = "privat-sykepenger-inntektsmelding"
    const val rapidTopic = "privat-helse-sykepenger-rapid-v1"

    val hendelseKildeTopics = listOf(
        inntektsmeldingTopic,
        søknadTopic
    )
}
