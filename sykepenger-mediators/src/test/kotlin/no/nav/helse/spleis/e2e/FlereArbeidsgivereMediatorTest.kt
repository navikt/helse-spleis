package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.InntektskildeDTO
import no.nav.syfo.kafka.felles.InntektskildetypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class FlereArbeidsgivereMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `tillater søknader med flere arbeidsforhold`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        )
    }

    @Test
    fun `tillater ikke søknader med !ANDRE_ARBEIDSFORHOLD`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.JORDBRUKER_FISKER_REINDRIFTSUTOVER, true)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater søknader med ANDRE_ARBEIDSFORHOLD med og uten sykmelding - når det finnes flere arbeidsgivere med sykdom `() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer1")
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), orgnummer = "orgnummer2")
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, false)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en periode - inntektsmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_SØKNAD_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 0,
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 30.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 30.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig periode - inntektsmelding før søknad`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 14.januar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 16.januar, tom = 20.januar, sykmeldingsgrad = 100))

        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 14.januar), Periode(16.januar, 19.januar)), førsteFraværsdag = 16.januar)

        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 16.januar, tom = 20.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_SØKNAD_FERDIG_GAP",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "AVVENTER_SØKNAD_UFERDIG_GAP",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en forlengelsesperiode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, status = SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater ikke søknader med ANDRE_ARBEIDSFORHOLD ved en uferdig forlengelsesperiode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, status = SimuleringMessage.Simuleringstatus.OK)

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            vedtaksperiodeIndeks = 1,
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true))
        )

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `sender riktig orgnummer i trenger_ikke_inntektsmelding for arbeidsgiveren som venter på inntektsmelding`() {
        val a1 = "arbeidsgiver 1"
        val a2 = "arbeidsgiver 2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a2)
        sendSøknad(
            1,
            listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.FOSTERHJEMGODTGJORELSE, true)),
            sendtNav = LocalDateTime.MAX,
            orgnummer = a2
        )

        val melding = testRapid.inspektør.meldinger("trenger_ikke_inntektsmelding")
        val orgnumre = melding.map { it["organisasjonsnummer"].asText() }.sorted()
        val vedtaksperiodeIder = melding.map { UUID.fromString(it["vedtaksperiodeId"].asText()) }.sorted()

        assertEquals(listOf("arbeidsgiver 1"), orgnumre)
        assertEquals(listOf(testRapid.inspektør.vedtaksperiodeId(0)), vedtaksperiodeIder)
    }

    @Test
    fun `sender riktig orgnummer i trenger_ikke_inntektsmelding for alle arbeidsgivere som venter på inntektsmelding`() {
        val a1 = "arbeidsgiver 1"
        val a2 = "arbeidsgiver 2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a2)
        sendSøknad(1, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a2)
        sendNyPåminnelse(
            vedtaksperiodeIndeks = 0,
            orgnummer = a1,
            tilstandsendringstidspunkt = LocalDateTime.MIN,
            tilstandType = TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )

        val melding = testRapid.inspektør.meldinger("trenger_ikke_inntektsmelding")
        val orgnumre = melding.map { it["organisasjonsnummer"].asText() }.sorted()
        val vedtaksperiodeIder = melding.map { UUID.fromString(it["vedtaksperiodeId"].asText()) }.sorted()

        assertEquals(listOf(testRapid.inspektør.vedtaksperiodeId(0), testRapid.inspektør.vedtaksperiodeId(1)).sorted(), vedtaksperiodeIder)
        assertEquals(listOf("arbeidsgiver 1", "arbeidsgiver 2").sorted(), orgnumre)
    }

    @Test
    fun `overstyring av arbeidsforhold fører til tilstandsendring`() {
        val a1 = "ag1"
        val a2 = "ag2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
        sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, orgnummer = a1)
        sendYtelser(0, orgnummer = a1)
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = 1.januar,
            orgnummer = a1,
            inntekter = sammenligningsgrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(1000.0, a2),
                )
            ),
            arbeidsforhold = listOf(
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                TestMessageFactory.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(1000.0, a2),
                )
            )
        )
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringArbeidsforhold(1.januar, listOf(TestMessageFactory.ArbeidsforholdOverstyrt(a2, false)))
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING"
        )
    }
}
