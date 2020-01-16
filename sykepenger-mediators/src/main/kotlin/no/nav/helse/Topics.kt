package no.nav.helse

internal object Topics {
    const val søknadTopic = "syfo-soknad-v2"
    const val inntektsmeldingTopic = "privat-sykepenger-inntektsmelding"
    const val behovTopic = "privat-helse-sykepenger-behov"
    const val opprettGosysOppgaveTopic = "privat-helse-sykepenger-opprettGosysOppgave"
    const val vedtaksperiodeEventTopic = "privat-helse-sykepenger-vedtaksperiode-endret"
    const val vedtaksperiodeSlettetEventTopic = "privat-helse-sykepenger-vedtaksperiode-slettet"
    const val utbetalingEventTopic = "privat-helse-sykepenger-utbetaling"
    const val påminnelseTopic = "privat-helse-sykepenger-paminnelser"
    const val helseRapidTopic = "privat-helse-sykepenger-rapid-v1"

    val hendelseKildeTopics = listOf(
        behovTopic,
        påminnelseTopic,
        inntektsmeldingTopic,
        søknadTopic
    )
}
