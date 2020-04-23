package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KansellerUtbetalingTest: AbstractEndToEndTest() {

    @BeforeEach internal fun setup() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(0, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterSimulering(0)
        håndterManuellSaksbehandling(0, true)
        håndterUtbetalt(0, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

    @Test internal fun `avvis hvis arbeidsgiver er ukjent`() {
        håndterKansellerUtbetaling(orgnummer = "999999")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test internal fun `avvis hvis vi ikke finner fagsystemId`() {
        håndterKansellerUtbetaling(fagsystemId = "unknown")
        inspektør.also {
            assertTrue(it.personLogg.hasErrors(), it.personLogg.toString())
        }
    }

    @Test internal fun `kanseller siste utbetaling`() {
        val behovTeller = inspektør.personLogg.behov().size
        håndterKansellerUtbetaling()
        inspektør.also {
            assertFalse(it.personLogg.hasErrors(), it.personLogg.toString())
            assertEquals(2, it.arbeidsgiverOppdrag.size)
            assertEquals(1, it.personLogg.behov().size - behovTeller, it.personLogg.toString())
            TestOppdragInspektør(it.arbeidsgiverOppdrag[1]).also { oppdragInspektør ->
                assertEquals(
                    Utbetalingslinje(18.januar, 26.januar, 0, 0.0),
                    oppdragInspektør.linjer[0]
                )
                assertEquals(Endringskode.OPPH, oppdragInspektør.endringskoder[0])
                assertEquals(oppdragInspektør.fagsystemIder[0], oppdragInspektør.refFagsystemIder[0])
            }
            it.personLogg.behov().last().also {
                assertEquals(Behovtype.Utbetaling, it.type)
                assertEquals("", it.detaljer()["maksdato"])
                assertEquals("Ola Nordmann", it.detaljer()["saksbehandler"])
                assertEquals("SPREF", it.detaljer()["fagområde"])
            }

        }
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

        override fun preVisitOppdrag(oppdrag: Oppdrag) {
            this.oppdrag.add(oppdrag)
            fagsystemIder.add(oppdrag.fagsystemId())
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode
        ) {
            linjer.add(linje)
            endringskoder.add(endringskode)
            refFagsystemIder.add(refFagsystemId)
        }

    }
}
