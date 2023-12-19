package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class FjerneGodkjenningsbehovOgVedtakFattetTest: AbstractEndToEndMediatorTest() {

    @Test
    fun `avsluttet uten vedtak`() {
        val søknadId = sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 16.januar, sykmeldingsgrad = 100)))

        /**
         *  Til Speilvendt:
         *  - Disse feltene blir alltid 0.0 for perioder som går til Avsluttet Uten Utbetaling
         *    I noen sære caser hvor annen AG har vilkårsprøvd skjæringstidspunktet kan det være at de er != 0.0 i dag
         *    men AUU'er vises uansett ikke i Flex (når utbetalingId == null) så det er nok mer tilfeldigheter enn noe som må til
         *      - "sykepengegrunnlag"
         *      - "grunnlagForSykepengegrunnlag"
         *      - "grunnlagForSykepengegrunnlagPerArbeidsgiver"
         *      - "inntekt"
         *
         *  - "begrensning" kan hardkodes til "VET_IKKE"
         *  - "tags" kan hardkodes til tom liste []
         *      Enkelte AUU'er i dag kan nok ha taggen "IngenNyArbeidsgiverperiode" - men ettersom de ikke vises til sykmeldte
         *      kan den bare fjernes 🚮
         *
         *  - "vedtakFattetTidspunkt" kan settes til "avsluttetTidspunkt", eller now() i Spesialist, det er kanskje mer riktig?
         */

        @Language("JSON")
        val forventetUtkastTilVedtak = """
        {
            "@event_name": "utkast_til_vedtak",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom" : "2018-01-01",
            "tom" : "2018-01-16",
            "skjæringstidspunkt": "2018-01-01",
            "sykepengegrunnlag": 0.0,
            "grunnlagForSykepengegrunnlag": 0.0,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver": {},
            "inntekt" : 0.0,
            "begrensning" : "VET_IKKE",
            "hendelser": ["$søknadId"],
            "tags": [],
            "vedtakFattetTidspunkt": "<timestamp>",
            "vedtaksperiodeId": "<uuid>"
        }
        """
        testRapid.assertUtgåendeMelding(forventetUtkastTilVedtak)

        val forventetAvsluttetUtenVedtak = """
        {
            "@event_name": "avsluttet_uten_vedtak",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom" : "2018-01-01",
            "tom" : "2018-01-16",
            "skjæringstidspunkt": "2018-01-01",
            "hendelser": ["$søknadId"],
            "vedtaksperiodeId": "<uuid>",
            "generasjonId": "<uuid>",
            "avsluttetTidspunkt": "<timestamp>"
        }
        """

        testRapid.assertUtgåendeMelding(forventetAvsluttetUtenVedtak)
    }
}