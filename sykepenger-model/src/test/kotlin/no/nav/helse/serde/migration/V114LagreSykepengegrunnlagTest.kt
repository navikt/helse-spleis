package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V114LagreSykepengegrunnlagTest : MigrationTest(V114LagreSykepengegrunnlag()) {

    @Test
    fun `Sykepengegrunnlag lagres riktig for flere arbeidsgivere, en med inntektsmelding og med inntekter fra skatt`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedInntektsmeldingOgSkattExpected.json",
            originalJson = "/migrations/114/personMedInntektsmeldingOgSkattOriginal.json"
        )
    }

    @Test
    fun `Sykepengegrunnlag lagres riktig for person med IT-historikk`() {
        assertMigration(
            expectedJson = "/migrations/114/personOvergangFraITUtenSkattExpected.json",
            originalJson = "/migrations/114/personOvergangFraITUtenSkattOriginal.json"
        )
    }

    @Test
    fun `Én arbeidsgiver med IT-historikk og én med skatteopplysninger - vilkårsgrunnlaget skal kun legge IT-historikken til grunn ved Infotrygd-vilkårsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/114/personOvergangFraITMedSkatteopplysningerExpected.json",
            originalJson = "/migrations/114/personOvergangFraITMedSkatteopplysningerOriginal.json"
        )
    }

    @Test
    fun `Migrerer riktig for overgang fra infotrygd med dato ulikt skjæringstidspunkt og manglende inntektsopplysning for vilkårsgrunnlag`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedRartSkjæringstidspunktFraITExpected.json",
            originalJson = "/migrations/114/personMedRartSkjæringstidspunktFraITOriginal.json"
        )
    }

    @Test
    fun `Inntektsopplysning fra inntektsmelding med dato ulik fra skjæringstidspunkt prioriteres over skatteopplysning`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedRartSkjæringstidspunktFraIMExpected.json",
            originalJson = "/migrations/114/personMedRartSkjæringstidspunktFraIMOriginal.json"
        )
    }

    @Test
    fun `Tre inntektsmeldinger, IM som er lagt til grunn på vedtaksperioden er den midterste og ligger ikke på skjæringstidspunkt - velger riktig`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedTreIMExpected.json",
            originalJson = "/migrations/114/personMedTreIMOriginal.json"
        )
    }

    @Test
    fun `Flere arbeidsgivere med ulik fom skal bruke IM fra tidligste fom og skatteopplysninger fra seneste fom`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedFlereAGUlikFomExpected.json",
            originalJson = "/migrations/114/personMedFlereAGUlikFomOriginal.json"
        )
    }

    @Test
    fun `Forkastede vedtaksperioder`() {
        assertMigration(
            expectedJson = "/migrations/114/personMedForkastedeVedtaksperioderExpected.json",
            originalJson = "/migrations/114/personMedForkastedeVedtaksperioderOriginal.json"
        )
    }
}
