package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class YtelserHendelseTest : AbstractPersonTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    @Test
    fun `ytelser på feil tidspunkt`() {
        assertThrows<Aktivitetslogg.AktivitetException> { person.håndter(ytelser(vedtaksperiodeId = UUID.randomUUID())) }

        person.håndter(sykmelding())
        person.håndter(ytelser(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(søknad())
        person.håndter(ytelser(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(inntektsmelding())
        person.håndter(ytelser(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_VILKÅRSPRØVING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `historie nyere enn perioden`() {
        val sisteHistoriskeSykedag = førsteSykedag.plusMonths(2)
        håndterYtelser(
            utbetalinger = listOf(
                Utbetalingsperiode(ORGNUMMER, sisteHistoriskeSykedag.minusDays(14),  sisteHistoriskeSykedag, 100.prosent, 1000.daglig)
            ),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    sisteHistoriskeSykedag.minusDays(30),
                    20000.månedlig,
                    true
                )
            )
        )
    }

    @Test
    fun `ugyldig utbetalinghistorikk før inntektsmelding kaster perioden ut`() {
        håndterUgyldigYtelser()
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `ugyldig utbetalinghistorikk etter inntektsmelding kaster perioden ut`() {
        håndterYtelser(ugyldigePerioder = listOf(null to null))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `fordrepengeytelse før periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `fordrepengeytelse i periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `fordrepengeytelse etter periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `svangerskapsytelse før periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `svangerskapsytelse i periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `svangerskapsytelse etter periode`() {
        håndterYtelser(svangerskapsytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun håndterYtelser(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepengeytelse: Periode? = null,
        svangerskapsytelse: Periode? = null,
        ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>> = emptyList()
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        val ytelser = ytelser(
            vedtaksperiodeId = 1.vedtaksperiode,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            foreldrepengeYtelse = foreldrepengeytelse,
            svangerskapYtelse = svangerskapsytelse,
            ugyldigePerioder = ugyldigePerioder
        )
        person.håndter(ytelser)
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser)
    }

    private fun håndterUgyldigYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(
            ytelser(
                vedtaksperiodeId = 1.vedtaksperiode,
                ugyldigePerioder = listOf(null to null),
                foreldrepengeYtelse = null,
                svangerskapYtelse = null
            )
        )
    }

    private fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null,
        ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>> = emptyList()
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "$vedtaksperiodeId",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = "$vedtaksperiodeId",
                arbeidskategorikoder = emptyMap(),
                harStatslønn = false,
                perioder = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                ugyldigePerioder = ugyldigePerioder,
                aktivitetslogg = it,
                besvart = LocalDateTime.now()
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
                aktivitetslogg = it
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            dødsinfo = Dødsinfo(null),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
            dagpenger = Dagpenger(emptyList()),
            aktivitetslogg = it
        )
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            sykmeldingSkrevet = førsteSykedag.atStartOfDay(),
            mottatt = sisteSykedag.atStartOfDay()
        )

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        )

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            null,
            31000.månedlig,
            emptyList()
        )
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 31000.månedlig
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 1.januar(2017))
                )
            )
        )

    private fun simulering() =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        )

}
