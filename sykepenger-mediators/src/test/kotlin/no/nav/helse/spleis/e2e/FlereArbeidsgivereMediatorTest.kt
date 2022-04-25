package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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

internal class FlereArbeidsgivereMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `tillater søknader med flere arbeidsforhold`() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer1"
        )
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer2"
        )
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        assertTilstander(
            0,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
        )

        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            1,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
        )
    }

    @Test
    fun `tillater ikke søknader med !ANDRE_ARBEIDSFORHOLD`() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer1"
        )
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer2"
        )
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.JORDBRUKER_FISKER_REINDRIFTSUTOVER, true)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            0,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "TIL_INFOTRYGD"
        )
        assertTilstander(
            1,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `tillater søknader med ANDRE_ARBEIDSFORHOLD med og uten sykmelding - når det finnes flere arbeidsgivere med sykdom `() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer1"
        )
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            orgnummer = "orgnummer2"
        )
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, true)),
            orgnummer = "orgnummer1"
        )

        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            andreInntektskilder = listOf(InntektskildeDTO(InntektskildetypeDTO.ANDRE_ARBEIDSFORHOLD, false)),
            orgnummer = "orgnummer2"
        )

        assertTilstander(
            0,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
        )
        assertTilstander(
            1,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
        )
    }

    @Test
    fun `sender riktig orgnummer i trenger_ikke_inntektsmelding for arbeidsgiveren som venter på inntektsmelding`() {
        val a1 = "arbeidsgiver 1"
        val a2 = "arbeidsgiver 2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a2)
        sendSøknad(
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
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a2)
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a2)
        sendNyPåminnelse(
            vedtaksperiodeIndeks = 0,
            orgnummer = a1,
            tilstandsendringstidspunkt = LocalDateTime.MIN,
            tilstandType = TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
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
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), orgnummer = a1)
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
        sendOverstyringArbeidsforhold(1.januar, listOf(TestMessageFactory.ArbeidsforholdOverstyrt(a2, true)))
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        assertTilstander(
            0,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVVENTER_BLOKKERENDE_PERIODE",
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
