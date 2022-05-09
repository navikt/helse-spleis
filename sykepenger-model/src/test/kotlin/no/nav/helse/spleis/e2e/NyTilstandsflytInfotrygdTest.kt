package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.ForlengelseFraInfotrygd.IKKE_ETTERSPURT
import no.nav.helse.person.ForlengelseFraInfotrygd.NEI
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.AvventerBlokkerendePeriode
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.september
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal class NyTilstandsflytInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `enkel infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        utbetalPeriode(1.vedtaksperiode)
    }

    @Test
    fun `Infotrygdhistorikk som ikke medfører forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 30.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `Forlengelse av en infotrygdforlengelse trenger ikke vente på inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Oppdager at vi er en infotrygdforlengelse dersom den første perioden fortsatt er i AvventerHistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        /* Periode 1 går nå videre til AvventerHistorikk.
        Dersom vi nå mottar en søknad for periode 2 før periode 1 håndterer ytelser,
        vil vi ikke enda ha lagret infotrygdinntekten på skjæringstidspunktet */
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Infotrygdovergang blir blokkert av tidligere vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.februar, 28.februar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 15.februar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Forlengelse uten IT-historikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterUtbetalingshistorikk(vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `GAP til infotrygdforlengelse skal vente på inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(10.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `spør etter infotrygdhistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertEtterspurt(
            løsning = Utbetalingshistorikk::class,
            type = Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = ORGNUMMER
        )
    }

    @Test
    fun `Forlengelse av en infotrygdforlengelse - skal ikke vente på inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `Ping pong - venter ikke på inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar),)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 30000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, 30000.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)
        assertTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `Forlengelse av ping pong - skal ikke vente på IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar),)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 30000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, 30000.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)

        assertTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTilstand(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Kort periode som forlenger infotrygd`() {
        val historikk = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.august, 17.august, 100.prosent, 1000.daglig)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 1.august, INNTEKT, true)
        )

        håndterSykmelding(Sykmeldingsperiode(18.august, 2.september, 100.prosent))
        håndterSøknad(Sykdom(18.august, 2.september, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            *historikk.toTypedArray(),
            inntektshistorikk = inntektsopplysning
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `spør om utbetalingshistorikk i AvventerInntektsmeldingEllerHistorikk ved påminnelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        assertEtterspurt(
            løsning = Utbetalingshistorikk::class,
            type = Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = ORGNUMMER
        )
    }

    @Test
    fun `oppdager at vi er en infotrygdforlengelse når infotrygdhistorikken tilstøter en periode i AvsluttetUtenUtbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(10.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 9.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        )
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `oppdager at vi er en infotrygdforlengelse når vi tilstøter en periode i AvsluttetUtenUtbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 19.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 19.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)
        )) // antar at noe har skjedd med en periode som vi har i AvsluttetUtenUtbetaling som har ført til utbetaling i infotrygd
        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `forlengelse av ping-pong, ny periode som forlenger ping-pong-perioden går til AvventerHistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        utbetalPeriode(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `annen arbeidsgiver forlenger infotrygdforlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay(),
            orgnummer = a1
        )
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay(),
            orgnummer = a1
        )
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD, orgnummer = a2)
    }

    @Test
    fun `vedtaksperiode som sitter fast i AvventerBlokkerendePeriode fordi forlengelseFraInfotrygd ikke er satt`() {
        /*
            Etter migrering V150 migrerte vi AvventerUferdig -> AvventerBlokkerendePeriode. Nye vedtaksperioder går
            innom AvventerInntektsmeldingEllerHistorikk som setter et forlengseFraInfotrygd-flagg.
            gjenopptaBehandlingNy vil ikke sende periode i AvventerBlokkerendePeriode videre til AvventerHistorikk dersom
            forlengeseFraInfotrygd-flagget ikke er satt. Før ny tilstandsflyt ble flagget satt i AvventerHistorikk.
         */
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            utbetalinger = arrayOf(
                ArbeidsgiverUtbetalingsperiode(
                    ORGNUMMER,
                    1.januar,
                    31.januar,
                    100.prosent,
                    INNTEKT
                )
            ),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)),
            besvart = LocalDate.EPOCH.atStartOfDay(),
            orgnummer = a1
        )
        tvingForlengelseFraInfotrygd(1.vedtaksperiode, IKKE_ETTERSPURT)
        tvingTilstandTil(1.vedtaksperiode, AvventerBlokkerendePeriode)

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `vedtaksperiode som sitter fast i AvventerInntektsmeldingEllerHistorikk fordi forlengelseFraInfotrygd er satt feilaktig til NEI`() {
        /*
            Periode er stuck i AvventerInntektsmeldingEllerHistorikk fordi enkelte perioder kunne få få
            forlengelseFraInfotrygd-flagget feilaktig satt etter migrering V150.
            Testen løsner disse periodene ved å få sette forlengelseFraInfotrygd-flagget riktig ved påminnelse.
            Trello: https://trello.com/c/pWsuIC1W/2058-periode-som-stopper-opp-i-avventerblokkerendeperiode
         */
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        utbetalPeriode(3.vedtaksperiode)

        tvingForlengelseFraInfotrygd(3.vedtaksperiode, NEI)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterUtbetalingshistorikk(4.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        håndterPåminnelse(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(4.vedtaksperiode, AVVENTER_HISTORIKK)
    }


    @Test
    fun `vedtaksperiode som sitter fast i AvventerBlokkerendePeriode fordi forlengelseFraInfotrygd er satt feilaktig til NEI`() {
        /*
            Periode er stuck i AvventerBlokkerendePeriode fordi enkelte perioder kunne få få
            forlengelseFraInfotrygd-flagget feilaktig satt etter migrering V150.
            Testen løsner disse periodene ved å få sette forlengelseFraInfotrygd-flagget riktig ved påminnelse.
            Trello: https://trello.com/c/pWsuIC1W/2058-periode-som-stopper-opp-i-avventerblokkerendeperiode
        */
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        tvingForlengelseFraInfotrygd(3.vedtaksperiode, NEI)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        tvingTilstandTil(4.vedtaksperiode, AvventerBlokkerendePeriode)

        assertTilstand(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        håndterPåminnelse(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstand(4.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `Sørger for at en periode kommer seg videre dersom det er en tidligere periode enn den tilstøtende som er overgang fra IT` () {
        /*
            Periode er stuck i AvventerBlokkerendePeriode fordi enkelte perioder kunne få få
            forlengelseFraInfotrygd-flagget feilaktig satt etter migrering V150.
            Testen løsner disse periodene ved å få sette forlengelseFraInfotrygd-flagget riktig ved påminnelse.
            Trello: https://trello.com/c/pWsuIC1W/2058-periode-som-stopper-opp-i-avventerblokkerendeperiode
        */
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        utbetalPeriode(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        utbetalPeriode(3.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        tvingForlengelseFraInfotrygd(4.vedtaksperiode, NEI)
        håndterUtbetalingshistorikk(4.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        utbetalPeriode(4.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mai, 30.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 30.mai, 100.prosent))
        håndterUtbetalingshistorikk(5.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        håndterPåminnelse(5.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(5.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    /*
       Hjelpefunksjon for å tvinge tilstanden til å være en bestemt tilstand.
       Brukt for å gjenskape en situasjon etter migrering som vi ikke klarer å gjenprodusere med blanke ark
     */
    private fun tvingTilstandTil(vedtaksperiodeId: IdInnhenter, nyTilstand: Vedtaksperiodetilstand) {
        val vedtaksperiode = inspektør.vedtaksperioder(vedtaksperiodeId)
        val tilstand = Vedtaksperiode::class.memberProperties.first { it.name == "tilstand" } as KMutableProperty<*>
        tilstand.isAccessible = true
        tilstand.setter.call(vedtaksperiode, nyTilstand)
    }

    private fun tvingForlengelseFraInfotrygd(vedtaksperiodeId: IdInnhenter, flagg: ForlengelseFraInfotrygd) {
        val vedtaksperiode = inspektør.vedtaksperioder(vedtaksperiodeId)
        val forlengelseFraInfotrygd =
            Vedtaksperiode::class.memberProperties.first { it.name == "forlengelseFraInfotrygd" } as KMutableProperty<*>
        forlengelseFraInfotrygd.isAccessible = true
        forlengelseFraInfotrygd.setter.call(vedtaksperiode, flagg)
    }

    private fun utbetalPeriode(vedtaksperiode: IdInnhenter) {
        håndterYtelser(vedtaksperiode)
        håndterSimulering(vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode)
        håndterUtbetalt()
    }
}