package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.UgyldigPeriode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class YtelserHendelseTest : AbstractEndToEndTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    @Test
    fun `ytelser på feil tidspunkt`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            person.håndter(ytelser(vedtaksperiodeIdInnhenter = { UUID.randomUUID() }))
        }

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        person.håndter(ytelser(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        person.håndter(ytelser(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_VILKÅRSPRØVING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `historie nyere enn perioden`() {
        val sisteHistoriskeSykedag = førsteSykedag.plusMonths(2)
        håndterYtelser(
            utbetalinger = listOf(
                ArbeidsgiverUtbetalingsperiode(ORGNUMMER, sisteHistoriskeSykedag.minusDays(14),  sisteHistoriskeSykedag, 100.prosent, 1000.daglig)
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
        håndterYtelser(ugyldigePerioder = listOf(UgyldigPeriode(null, null, null)))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `fordrepengeytelse før periode`() {
        ferdigstill(håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1))))

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
        ferdigstill(håndterYtelser(foreldrepengeytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10))))

        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `svangerskapsytelse før periode`() {
        ferdigstill(håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1))))

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
        ferdigstill(håndterYtelser(svangerskapsytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10))))

        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun håndterYtelser(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepengeytelse: Periode? = null,
        svangerskapsytelse: Periode? = null,
        ugyldigePerioder: List<UgyldigPeriode> = emptyList()
    ) : Ytelser {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        val ytelser = ytelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            foreldrepengeYtelse = foreldrepengeytelse,
            svangerskapYtelse = svangerskapsytelse,
            ugyldigePerioder = ugyldigePerioder
        )

        ytelser.håndter(Person::håndter)
        return ytelser
    }

    private fun ferdigstill(ytelser: Ytelser) {
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        ytelser.håndter(Person::håndter)
        håndterSimulering(1.vedtaksperiode)
    }

    private fun håndterUgyldigYtelser() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        person.håndter(
            ytelser(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                ugyldigePerioder = listOf(UgyldigPeriode(null, null, null)),
                foreldrepengeYtelse = null,
                svangerskapYtelse = null
            )
        )
    }

    private fun ytelser(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null,
        ugyldigePerioder: List<UgyldigPeriode> = emptyList()
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "${vedtaksperiodeIdInnhenter.id(ORGNUMMER)}",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = "${vedtaksperiodeIdInnhenter.id(ORGNUMMER)}",
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

}
