package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.februar
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

internal class KansellerUtbetalingTest : AbstractEndToEndTest() {

    @BeforeEach
    internal fun setup() {
        nyttVedtak(3.januar, 26.januar, 100, 3.januar)
    }

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        håndterKansellerUtbetaling(orgnummer = "999999")
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
        }
    }

    @Test
    fun `avvis hvis vi ikke finner fagsystemId`() {
        håndterKansellerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, it.sisteTilstand(0))
        }
    }

    @Test
    fun `kanseller siste utbetaling`() {
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling()
        sjekkAt {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
            val behov = sisteBehov(1.vedtaksperiode)
            behov.type er Behovtype.Utbetaling
            @Suppress("UNCHECKED_CAST")
            (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"] er "OPPH"
        }
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
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
    fun `En enkel periode som er avsluttet som blir annullert blir også satt i tilstand TilAnnullering`() {
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
        }
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(0))
        }
    }

    @Test
    fun `Annullering av én periode fører til at alle avsluttede sammenhengende perioder blir satt i tilstand TilAnnullering`() {
        forlengVedtak(27.januar, 31.januar, 100)
        forlengPeriode(1.februar, 20.februar, 100)
        inspektør.also {
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.AVSLUTTET, inspektør.sisteTilstand(1))
            assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(2))
        }
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(0))
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(1))
            assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(2))
        }
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(1))
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(2))
            assertEquals(Behovtype.Utbetaling, it.personLogg.behov().last().type)
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        håndterKansellerUtbetaling()
        inspektør.also {
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(0))
        }
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
        }
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        håndterKansellerUtbetaling()
        inspektør.also {
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(0))
        }
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AVVIST)
        inspektør.also {
            assertTrue(it.personLogg.hasErrorsOrWorse())
            assertEquals(TilstandType.TIL_ANNULLERING, inspektør.sisteTilstand(0))
        }
    }

    @Disabled("Slik skal det virke etter at annullering trigger replay")
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
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
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
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

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

    @Test
    fun `publiserer kun ett event ved annullering av utbetaling som strekker seg over flere vedtaksperioder`() {
        val fagsystemId = inspektør.arbeidsgiverOppdrag.first().fagsystemId()
        forlengVedtak(27.januar, 20.februar, 100)
        assertEquals(2, observatør.vedtaksperioder.size)

        håndterKansellerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        val vedtaksperioderIder = observatør.vedtaksperioder.toList()
        assertEquals(TilstandType.TIL_INFOTRYGD, observatør.tilstander[vedtaksperioderIder[0]]?.last())
        assertEquals(TilstandType.TIL_INFOTRYGD, observatør.tilstander[vedtaksperioderIder[1]]?.last())

        val annulleringer = observatør.annulleringer
        assertEquals(1, annulleringer.size)
        val annullering = annulleringer.lastOrNull()
        assertNotNull(annullering)

        assertEquals(fagsystemId, annullering!!.fagsystemId)

        val utbetalingslinje = annullering.utbetalingslinjer.first()
        assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
        assertEquals(19.januar, utbetalingslinje.fom)
        assertEquals(20.februar, utbetalingslinje.tom)
        assertEquals(32913, utbetalingslinje.beløp)
        assertEquals(100.0, utbetalingslinje.grad)
    }


    private inner class TestOppdragInspektør(oppdrag: Oppdrag) : OppdragVisitor {
        val oppdrag = mutableListOf<Oppdrag>()
        val linjer = mutableListOf<Utbetalingslinje>()
        val endringskoder = mutableListOf<Endringskode>()
        val fagsystemIder = mutableListOf<String?>()
        val refFagsystemIder = mutableListOf<String?>()

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
            beløp: Int?,
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
