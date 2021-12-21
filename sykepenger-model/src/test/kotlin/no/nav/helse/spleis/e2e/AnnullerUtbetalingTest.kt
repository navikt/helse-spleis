package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.api.TilstandstypeDTO
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class AnnullerUtbetalingTest : AbstractEndToEndTest() {

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(orgnummer = a2)
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
        }
    }

    @Test
    fun `avvis hvis vi ikke finner fagsystemId`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, it.sisteTilstand(1.vedtaksperiode))
        }
    }

    @Test
    fun `annuller siste utbetaling`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        val behovTeller = inspektør.personLogg.behov().size
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            val behov = sisteBehov(Behovtype.Utbetaling)

            @Suppress("UNCHECKED_CAST")
            val statusForUtbetaling = (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"]
            statusForUtbetaling er "OPPH"
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.TilAnnullering
        }

        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            arbeidsgiverOppdrag.size er 2
            (personLogg.behov().size - behovTeller) skalVære 1 ellers personLogg.toString()
        }

        inspektør.arbeidsgiverOppdrag[1].inspektør.also {
            assertEquals(19.januar, it.fom(0))
            assertEquals(26.januar, it.tom(0))
            assertEquals(19.januar, it.datoStatusFom(0))
        }

        sjekkAt(inspektør.personLogg.behov().last()) {
            type er Behovtype.Utbetaling
            detaljer()["maksdato"] er null
            detaljer()["fagområde"] er "SPREF"
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Annullert
        }
    }


    @Test
    fun `Annuller flere fagsystemid for samme arbeidsgiver`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        nyttVedtak(1.mars, 31.mars, 100.prosent, 1.mars)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterUtbetalt(2.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Utbetalt
            vedtaksperioder[1].tilstand er TilstandstypeDTO.Annullert
        }

        sisteBehovErAnnullering(2.vedtaksperiode)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Annullert
            vedtaksperioder[1].tilstand er TilstandstypeDTO.Annullert
        }

        sisteBehovErAnnullering(1.vedtaksperiode)
    }

    private fun sisteBehovErAnnullering(vedtaksperiodeIdInnhenter: IdInnhenter) {
        sjekkAt(inspektør.personLogg.behov().last()) {
            type er Behovtype.Utbetaling
            detaljer()["fagsystemId"] er inspektør.fagsystemId(vedtaksperiodeIdInnhenter)
            hentLinjer()[0]["statuskode"] er "OPPH"
        }
    }

    private fun assertIngenAnnulleringsbehov() {
        assertFalse(
            inspektør.personLogg.behov()
                .filter { it.type == Behovtype.Utbetaling }
                .any {
                    it.hentLinjer().any { linje ->
                        linje["statuskode"] == "OPPH"
                    }
                }
        )
    }

    @Test
    fun `Annuller oppdrag som er under utbetaling feiler`() {
        tilGodkjent(3.januar, 26.januar, 100.prosent, 3.januar)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        sjekkAt(inspektør) {
            personLogg.hasErrorsOrWorse() ellers personLogg.toString()
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.TilUtbetaling
        }

        assertIngenAnnulleringsbehov()
    }

    @Test
    fun `Annuller av oppdrag med feilet utbetaling feiler`() {
        tilGodkjent(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.FEIL)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        sjekkAt(inspektør) {
            personLogg.hasErrorsOrWorse() ellers personLogg.toString()
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Feilet
        }

        assertIngenAnnulleringsbehov()
    }


    @Test
    fun `Kan ikke annullere hvis noen vedtaksperioder er til utbetaling`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))

        sjekkAt(inspektør) {
            personLogg.hasErrorsOrWorse() ellers personLogg.toString()
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.Utbetalt
            vedtaksperioder[1].tilstand er TilstandstypeDTO.TilUtbetaling
        }

        assertIngenAnnulleringsbehov()
    }

    private fun Aktivitetslogg.Aktivitet.Behov.hentLinjer() =
        @Suppress("UNCHECKED_CAST")
        (detaljer()["linjer"] as List<Map<String, Any>>)


    @Test
    fun `Ved feilet annulleringsutbetaling settes utbetaling til annullering feilet`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.FEIL)

        sjekkAt(inspektør) {
            personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            arbeidsgiverOppdrag.size er 2
        }

        sjekkAt(speilApi().arbeidsgivere[0]) {
            vedtaksperioder[0].tilstand er TilstandstypeDTO.AnnulleringFeilet
        }
    }

    @Test
    fun `En enkel periode som er avsluttet som blir annullert blir også satt i tilstand TilAnnullering`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
        }
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
    }

    @Test
    fun `Annullering av én periode fører til at alle avsluttede sammenhengende perioder blir satt i tilstand TilAnnullering`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        forlengVedtak(27.januar, 31.januar, 100.prosent)
        forlengPeriode(1.februar, 20.februar, 100.prosent)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))
            assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(3.vedtaksperiode))
        }
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteTilstand(3.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(3.vedtaksperiode))
            assertTrue(it.utbetalinger.last().inspektør.erAnnullering)
            assertFalse(it.utbetalinger.last().inspektør.erUtbetalt)
        }
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteTilstand(3.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(3.vedtaksperiode))
            assertEquals(Behovtype.Utbetaling, it.personLogg.behov().last().type)
            assertTrue(it.utbetalinger.last().inspektør.erAnnullering)
            assertTrue(it.utbetalinger.last().inspektør.erUtbetalt)
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling()
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AVVIST)
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
    }

    @Test
    fun `Annullering av én periode fører kun til at sammehengende utbetalte perioder blir forkastet og værende i Avsluttet`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        forlengVedtak(27.januar, 30.januar, 100.prosent)
        nyttVedtak(1.mars, 20.mars, 100.prosent, 1.mars)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(3.vedtaksperiode))
        }
        val behovTeller = inspektør.personLogg.behov().size
        håndterAnnullerUtbetaling(fagsystemId = inspektør.arbeidsgiverOppdrag.last().fagsystemId())
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(3.vedtaksperiode))
            assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(3.vedtaksperiode))
        }
    }

    @Test
    fun `publiserer et event ved annullering av full refusjon`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(
            1.vedtaksperiode,
            status = Oppdragstatus.AKSEPTERT
        )

        val annullering = observatør.annulleringer.lastOrNull()
        no.nav.helse.testhelpers.assertNotNull(annullering)

        assertEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), annullering.arbeidsgiverFagsystemId)
        assertNull(annullering.personFagsystemId)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, annullering.fom)
        assertEquals(26.januar, annullering.tom)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(26.januar, utbetalingslinje.tom)
        assertEquals(0, utbetalingslinje.beløp)
        assertEquals(0.0, utbetalingslinje.grad)
    }

    @Test
    fun `publiserer kun ett event ved annullering av utbetaling som strekker seg over flere vedtaksperioder med full refusjon`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        val fagsystemId = inspektør.fagsystemId(1.vedtaksperiode)
        forlengVedtak(27.januar, 20.februar, 100.prosent)
        assertEquals(2, inspektør.vedtaksperiodeTeller)

        håndterAnnullerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(
            1.vedtaksperiode,
            status = Oppdragstatus.AKSEPTERT
        )

        assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2.vedtaksperiode))

        val annulleringer = observatør.annulleringer
        assertEquals(1, annulleringer.size)
        val annullering = annulleringer.lastOrNull()
        no.nav.helse.testhelpers.assertNotNull(annullering)

        assertEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), annullering.arbeidsgiverFagsystemId)
        assertNull(annullering.personFagsystemId)
        assertEquals(19.januar, annullering.fom)
        assertEquals(20.februar, annullering.tom)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(20.februar, utbetalingslinje.tom)
        assertEquals(0, utbetalingslinje.beløp)
        assertEquals(0.0, utbetalingslinje.grad)
    }

    @Test
    fun `setter datoStatusFom som fom dato i annullering hvor graden endres`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        forlengVedtak(27.januar, 20.februar, 30.prosent)
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        val fagsystemId = inspektør.fagsystemId(2.vedtaksperiode)

        håndterAnnullerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(
            2.vedtaksperiode,
            status = Oppdragstatus.AKSEPTERT
        )

        val annulleringer = observatør.annulleringer
        assertEquals(1, annulleringer.size)
        val annullering = annulleringer.last()

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals(19.januar, utbetalingslinje.fom)
    }

    @Test
    fun `kan ikke annullere utbetalingsreferanser som ikke er siste`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        nyttVedtak(3.mars, 26.mars, 100.prosent, 3.mars)

        val fagsystemId = inspektør.fagsystemId(1.vedtaksperiode)
        håndterAnnullerUtbetaling(fagsystemId = fagsystemId)

        assertEquals(0, observatør.annulleringer.size)

        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
    }

    @Test
    fun `annulerer flere fagsystemider baklengs`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        nyttVedtak(1.mars, 31.mars, 100.prosent, 1.mars)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterUtbetalt(2.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        sisteBehovErAnnullering(2.vedtaksperiode)
        assertFalse(inspektør.utbetalinger.last { it.inspektør.arbeidsgiverOppdrag.fagsystemId() == inspektør.fagsystemId(1.vedtaksperiode) }.inspektør.erAnnullering)
        assertTrue(inspektør.utbetalinger.last { it.inspektør.arbeidsgiverOppdrag.fagsystemId() == inspektør.fagsystemId(2.vedtaksperiode) }.inspektør.erAnnullering)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)
        sisteBehovErAnnullering(1.vedtaksperiode)
        assertTrue(inspektør.utbetalinger.last { it.inspektør.arbeidsgiverOppdrag.fagsystemId() == inspektør.fagsystemId(1.vedtaksperiode) }.inspektør.erAnnullering)
        assertTrue(inspektør.utbetalinger.last { it.inspektør.arbeidsgiverOppdrag.fagsystemId() == inspektør.fagsystemId(2.vedtaksperiode) }.inspektør.erAnnullering)
    }

    @Test
    fun `annuller over ikke utbetalt forlengelse`() {
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(27.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        val annullering = inspektør.utbetaling(2)
        sisteBehovErAnnullering(1.vedtaksperiode)
        assertTrue(annullering.inspektør.erAnnullering)
        assertEquals(19.januar, annullering.inspektør.arbeidsgiverOppdrag.førstedato)
        assertEquals(26.januar, annullering.inspektør.arbeidsgiverOppdrag.sistedato)
    }

    @Test
    fun `forlengelse ved inflight annullering`() {
        /*
        Tidligere har vi kun basert oss på utbetalinger i en sluttilstand for å beregne ny utbetaling. Om vi hadde en annullering som var in-flight ville denne
        bli ignorert og det ville bli laget en utbetaling som strakk seg helt tilbake til første del av sammenhengende periode, noe som igjen vil føre til at vi
        lager en duplikat utbetaling.
         */
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))

        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(27.januar, 14.februar, 100.prosent))
        assertTilstander(2.vedtaksperiode, TilstandType.START, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `UtbetalingAnnullertEvent inneholder saksbehandlerident`(){
        nyttVedtak(3.januar, 26.januar, 100.prosent, 3.januar)
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode, status = Oppdragstatus.AKSEPTERT)

        assertEquals("Ola Nordmann", observatør.annulleringer.first().saksbehandlerIdent)
    }

    @Test
    fun `skal ikke forkaste utbetalte perioder, med mindre de blir annullert`() {
        // lag en periode
        nyttVedtak(1.januar, 31.januar)
        // prøv å forkast, ikke klar det
        håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent))
        assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        // annullér
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(1.vedtaksperiode))
        håndterUtbetalt(1.vedtaksperiode)
        // sjekk at _nå_ er den forkasta
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    fun <T> sjekkAt(t: T, init: T.() -> Unit) {
        t.init()
    }
}
