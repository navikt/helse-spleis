package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.sisteBehov
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AnnullerUtbetalingTest : AbstractEndToEndTest() {

    @Test
    fun `kun én vedtaksperiode skal annulleres`() {
        nyttVedtak(januar)

        val vedtaksperiodeTilAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1)), vedtaksperiodeTilAnnullering)
    }

    @Test
    fun `begge vedtaksperioder annulleres når vi annullerer den første`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        val vedtaksperioderTilAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1), 2.vedtaksperiode.id(a1)), vedtaksperioderTilAnnullering)
    }

    @Test
    fun `begge vedtaksperioder annulleres når vi annullerer den siste`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        val vedtaksperioderTilAnnullering = inspektør.vedtaksperioder(2.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1), 2.vedtaksperiode.id(a1)), vedtaksperioderTilAnnullering)
    }

    @Test
    fun `alle vedtaksperioder annulleres når vi annullerer den midterste`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)

        val vedtaksperioderTilAnnullering = inspektør.vedtaksperioder(2.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1), 2.vedtaksperiode.id(a1), 3.vedtaksperiode.id(a1)), vedtaksperioderTilAnnullering)
    }

    @Test
    fun `annullerer bare i sammenhengende agp`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        nyttVedtak(april, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)

        val vedtaksperioderTilAnnullering = inspektør.vedtaksperioder(2.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1), 2.vedtaksperiode.id(a1)), vedtaksperioderTilAnnullering)
    }

    @Test
    fun `annullerer ikke ennå perioder på tvers av arbeidsgivere ved samme sykefravær`() {
        nyeVedtak(januar, a1, a2)
        forlengVedtak(februar, a1, a2)

        val vedtaksperioderTilAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).annulleringskandidater.map { it.id }.toSet()
        assertEquals(setOf(1.vedtaksperiode.id(a1), 2.vedtaksperiode.id(a1)), vedtaksperioderTilAnnullering)
    }

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        assertThrows<Aktivitetslogg.AktivitetException> { håndterAnnullerUtbetaling(orgnummer = a2) }
        assertTrue(personlogg.harFunksjonelleFeilEllerVerre(), personlogg.toString())
    }

    @Test
    fun `avvis hvis vi ikke finner fagsystemId`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        håndterAnnullerUtbetaling(utbetalingId = UUID.randomUUID())
        inspektør.also {
            assertEquals(AVSLUTTET, it.sisteTilstand(1.vedtaksperiode))
        }
    }

    @Test
    fun `forkaster ikke tidligere perioder ved annullering`() {
        nyttVedtak(januar)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterAnnullerUtbetaling()
        assertEquals(1, observatør.forkastedePerioder())
        assertEquals(AVSLUTTET, observatør.forkastet(2.vedtaksperiode.id(a1)).gjeldendeTilstand)
    }

    @Test
    fun `forkaster senere perioder ved annullering`() {
        nyttVedtak(januar)
        forlengVedtak(februar) // forlengelse
        nyttVedtak(10.mars til 31.mars, vedtaksperiodeIdInnhenter = 3.vedtaksperiode, arbeidsgiverperiode = emptyList())
        håndterSykmelding(Sykmeldingsperiode(1.mai, 20.mai)) // førstegangsbehandling, ny agp
        håndterSøknad(1.mai til 20.mai)
        håndterAnnullerUtbetaling()
        assertEquals(2, observatør.forkastedePerioder())
        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteForkastetPeriodeTilstand(a1, 3.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(a1, 4.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `annuller siste utbetaling`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        val behovTeller = personlogg.behov.size
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertIngenFunksjonelleFeil()
        val behov = personlogg.sisteBehov(Behovtype.Utbetaling)

        @Suppress("UNCHECKED_CAST")
        val statusForUtbetaling = (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"]
        assertEquals("OPPH", statusForUtbetaling)
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
        assertFalse(personlogg.harFunksjonelleFeilEllerVerre())
        assertEquals(2, inspektør.antallUtbetalinger)
        assertEquals(1, personlogg.behov.size - behovTeller)
        inspektør.utbetaling(1).arbeidsgiverOppdrag.inspektør.also {
            assertEquals(19.januar, it.fom(0))
            assertEquals(26.januar, it.tom(0))
            assertEquals(19.januar, it.datoStatusFom(0))
        }
        personlogg.behov.last().also {
            assertEquals(Behovtype.Utbetaling, it.type)
            assertNull(it.detaljer()["maksdato"])
            assertEquals("SPREF", it.detaljer()["fagområde"])
        }
    }

    @Test
    fun `Annuller flere fagsystemid for samme arbeidsgiver`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        nyttVedtak(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
        sisteBehovErAnnullering(2.vedtaksperiode)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
        sisteBehovErAnnullering(1.vedtaksperiode)
    }

    private fun sisteBehovErAnnullering(vedtaksperiodeIdInnhenter: IdInnhenter) {
        personlogg.behov.last().also {
            assertEquals(Behovtype.Utbetaling, it.type)
            assertEquals(inspektør.sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeIdInnhenter), it.detaljer()["fagsystemId"])
            assertEquals("OPPH", it.hentLinjer()[0]["statuskode"])
        }
    }

    private fun assertIngenAnnulleringsbehov() {
        assertFalse(
            personlogg.behov
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
        tilGodkjent(3.januar til 26.januar, 100.prosent)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertTrue(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertIngenAnnulleringsbehov()
    }

    @Test
    fun `Annuller av oppdrag med feilet utbetaling feiler`() {
        tilGodkjent(3.januar til 26.januar, 100.prosent)
        håndterUtbetalt(status = Oppdragstatus.FEIL)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertTrue(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertIngenAnnulleringsbehov()
    }

    @Test
    fun `Kan annullere hvis noen vedtaksperioder er til utbetaling`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        tilGodkjent(mars, 100.prosent, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))

        assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())
        sisteBehovErAnnullering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
    }

    private fun Aktivitet.Behov.hentLinjer() =
        @Suppress("UNCHECKED_CAST")
        (detaljer()["linjer"] as List<Map<String, Any>>)

    @Test
    fun `Ved feilet annulleringsutbetaling settes utbetaling til annullering feilet`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt(status = Oppdragstatus.FEIL)
        assertFalse(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, TIL_INFOTRYGD)
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        assertFalse(personlogg.harFunksjonelleFeilEllerVerre())
        assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, TIL_INFOTRYGD)
    }

    @Test
    fun `En enkel periode som er avsluttet som blir annullert blir også satt i tilstand TilAnnullering`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        inspektør.also {
            assertEquals(AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))
        }
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertFalse(personlogg.harFunksjonelleFeilEllerVerre(), personlogg.toString())
        inspektør.also {
            assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling()
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
        assertFalse(personlogg.harFunksjonelleFeilEllerVerre(), personlogg.toString())
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, TIL_INFOTRYGD)
    }

    @Test
    fun `Annullering av én periode fører kun til at sammehengende utbetalte perioder blir forkastet og værende i Avsluttet`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        forlengVedtak(27.januar til 30.januar, 100.prosent)
        nyttVedtak(1.mars til 20.mars, 100.prosent, vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        val behovTeller = personlogg.behov.size
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling()
        assertFalse(personlogg.harFunksjonelleFeilEllerVerre(), personlogg.toString())
        assertEquals(1, personlogg.behov.size - behovTeller, personlogg.toString())
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET, TIL_INFOTRYGD)
    }

    @Test
    fun `publiserer et event ved annullering av full refusjon`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT
        )

        val annullering = observatør.annulleringer.lastOrNull()
        assertNotNull(annullering)

        val utbetalingInspektør = inspektør.utbetaling(0)
        assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), annullering.arbeidsgiverFagsystemId)
        assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), annullering.personFagsystemId)

        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(3.januar, annullering.fom)
        assertEquals(26.januar, annullering.tom)
    }

    @Test
    fun `publiserer kun ett event ved annullering av utbetaling som strekker seg over flere vedtaksperioder med full refusjon`() {
        createPersonMedToVedtakPåSammeFagsystemId()

        assertEquals(2, inspektør.vedtaksperiodeTeller)

        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        håndterUtbetalt(
            status = Oppdragstatus.AKSEPTERT
        )

        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(2.vedtaksperiode))

        val annulleringer = observatør.annulleringer
        assertEquals(1, annulleringer.size)
        val annullering = annulleringer.lastOrNull()
        assertNotNull(annullering)

        val utbetalingInspektør = inspektør.utbetaling(0)
        assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), annullering.arbeidsgiverFagsystemId)
        assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), annullering.personFagsystemId)
        assertEquals(3.januar, annullering.fom)
        assertEquals(20.februar, annullering.tom)

        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
    }

    @Test
    fun `annuller over ikke utbetalt forlengelse`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 31.januar))
        håndterSøknad(27.januar til 31.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        val annullering = inspektør.utbetaling(2)
        sisteBehovErAnnullering(1.vedtaksperiode)
        assertTrue(annullering.erAnnullering)
        assertEquals(26.januar, annullering.arbeidsgiverOppdrag.inspektør.periode?.endInclusive)
        assertEquals(19.januar, annullering.arbeidsgiverOppdrag.first().inspektør.fom)
        assertEquals(26.januar, annullering.arbeidsgiverOppdrag.last().inspektør.tom)
    }

    @Test
    fun `UtbetalingAnnullertEvent inneholder saksbehandlerident`() {
        nyttVedtak(3.januar til 26.januar, 100.prosent)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)

        assertEquals("Ola Nordmann", observatør.annulleringer.first().saksbehandlerIdent)
    }

    @Test
    fun `skal ikke forkaste utbetalte perioder, med mindre de blir annullert`() {
        // lag en periode
        nyttVedtak(januar)
        // prøv å forkast, ikke klar det
        håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        håndterSøknad(1.februar til 19.februar)
        håndterSøknad(1.februar til 20.februar)

        assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        // annullér
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        håndterUtbetalt()
        // sjekk at _nå_ er den forkasta
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    @Test
    fun `skal kunne annullere tidligere utbetaling dersom siste utbetaling er uten utbetaling`() {
        nyttVedtak(januar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars))
        håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(17.mars, 20.mars))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertFalse(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
    }

    @Test
    fun `annullering av periode medfører at låser på sykdomstidslinje blir forkastet`() {
        nyttVedtak(januar)
        håndterAnnullerUtbetaling()
        inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
            assertEquals(0, it.size)
        }
    }
}
