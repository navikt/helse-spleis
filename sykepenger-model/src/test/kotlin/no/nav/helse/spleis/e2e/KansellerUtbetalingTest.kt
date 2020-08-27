package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class KansellerUtbetalingTest: AbstractEndToEndTest() {

    private var vedtaksperiodeTeller: Int = 0

    @BeforeEach
    internal fun setup() {
        nyttVedtak(3.januar, 26.januar, 100, 3.januar)
    }

    @Test fun `avvis hvis arbeidsgiver er ukjent`() {
        håndterKansellerUtbetaling(orgnummer = "999999")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test
    fun `avvis hvis vi ikke finner fagsystemId`() {
        håndterKansellerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test
    fun `kanseller siste utbetaling`() {
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
            assertEquals(2, it.arbeidsgiverOppdrag.size)
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            TestOppdragInspektør(it.arbeidsgiverOppdrag[1]).also { oppdragInspektør ->
                assertEquals(
                    Utbetalingslinje(19.januar, 26.januar, 1431, 1431, 100.0),
                    oppdragInspektør.linjer[0]
                )
                assertEquals(Endringskode.ENDR, oppdragInspektør.endringskoder[0])
                assertNull(oppdragInspektør.refFagsystemIder[0])
            }
            it.personLogg.behov().last().also {
                assertEquals(Behovtype.Utbetaling, it.type)
                assertEquals(null, it.detaljer()["maksdato"])
                assertEquals("Ola Nordmann", it.detaljer()["saksbehandler"])
                assertEquals("SPREF", it.detaljer()["fagområde"])
            }

        }
    }

    @Test
    fun `En enkel periode som blir annullert blir også satt i tilstand TilInfotrygd`() {
        val behovTeller = inspektør.personLogg.behov().size
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
        }
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
        }
    }

    @Test
    fun `Annullering av én periode fører til at alle sammenhengende perioder blir satt i tilstand TilInfotrygd`() {
        forlengVedtak(27.januar, 30.januar, 100)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1))
        }
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(1))
        }
    }

    @Test
    fun `Annullering av én periode fører kun til at sammehengende perioder blir satt i tilstand TilInfotrygd`() {
        forlengVedtak(27.januar, 30.januar, 100)
        nyttVedtak(1.mars, 20.mars, 100, 1.mars)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(2))
        }
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling(fagsystemId = inspektør.arbeidsgiverOppdrag.first().fagsystemId())
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(1))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
        }
    }

    @Test
    fun `publiserer et event ved annullering`() {
        val fagsystemId = inspektør.arbeidsgiverOppdrag.first().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemId)

        val annullering = observatør.annulleringer.lastOrNull()
        assertNotNull(annullering)

        assertEquals(fagsystemId, annullering!!.fagsystemId)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(26.januar, utbetalingslinje.tom)
        assertEquals(8586, utbetalingslinje.beløp)
        assertEquals(100.0, utbetalingslinje.grad)
    }

    private fun forlengVedtak(fom: LocalDate, tom: LocalDate, grad: Int) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        vedtaksperiodeTeller += 1
        val id = vedtaksperiodeTeller.vedtaksperiode
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterYtelser(id)
        håndterSimulering(id)
        håndterUtbetalingsgodkjenning(id, true)
        håndterUtbetalt(id, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    private fun nyttVedtak(fom: LocalDate, tom: LocalDate, grad: Int, førsteFraværsdag: LocalDate) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        vedtaksperiodeTeller += 1
        val id = vedtaksperiodeTeller.vedtaksperiode
        håndterInntektsmeldingMedValidering(id, listOf(Periode(fom, fom.plusDays(15))), førsteFraværsdag = førsteFraværsdag)
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterVilkårsgrunnlag(id, INNTEKT)
        håndterYtelser(id)   // No history
        håndterSimulering(id)
        håndterUtbetalingsgodkjenning(id, true)
        håndterUtbetalt(id, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    private inner class TestOppdragInspektør(oppdrag: Oppdrag) : OppdragVisitor {
        internal val oppdrag = mutableListOf<Oppdrag>()
        internal val linjer = mutableListOf<Utbetalingslinje>()
        internal val endringskoder = mutableListOf<Endringskode>()
        internal val fagsystemIder = mutableListOf<String?>()
        internal val refFagsystemIder = mutableListOf<String?>()

        init {
            oppdrag.accept(this)
        }

        override fun preVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int) {
            this.oppdrag.add(oppdrag)
            fagsystemIder.add(oppdrag.fagsystemId())
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            aktuellDagsinntekt: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?
        ) {
            linjer.add(linje)
            endringskoder.add(endringskode)
            refFagsystemIder.add(refFagsystemId)
        }

    }
}
