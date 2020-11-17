package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AVVIST
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class FagsystemIdTilstandTest {

    private val fagsystemIder = mutableListOf<FagsystemId>()
    private lateinit var fagsystemId: FagsystemId
    private val oppdrag = mutableMapOf<Oppdrag, FagsystemId>()
    private val aktivitetslogg get() = person.aktivitetslogg
    private val observer = FagsystemIdObservatør()
    private lateinit var person: Person

    private val inspektør get() = FagsystemIdInspektør(fagsystemIder)

    private companion object {
        private const val AKTØRID = "1234567891011"
        private const val FNR = "12345678910"
        private const val ORGNR = "123456789"
        private const val IDENT = "A199999"
        private const val EPOST = "saksbehandler@nav.no"
        private val MAKSDATO = LocalDate.now()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    @BeforeEach
    fun beforeEach() {
        person = Person(AKTØRID, FNR)
    }

    @Test
    fun `happy path`() {
        opprettOgUtbetal(5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `happy path med flere utbetalinger`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprettOgUtbetal(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "UtbetalingOverført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 14.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 14.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `annullering happy path`() {
        opprettOgUtbetal(5.NAV, 2.HELG, 5.NAV)
        val siste = annuller(0)
        kvitter(0)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv", "AnnulleringOverført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
        assertTomHistorie(0)
    }

    @Test
    fun `annullering når siste er ubetalt`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        val siste = annuller(0)
        kvitter(0)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "AnnulleringOverført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
    }

    @Test
    fun `aktiv ubetalt går tilbake til aktiv ved avslag`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0, godkjent = false)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "Aktiv")
        assertUtbetalingstidslinje(0,1.januar til 7.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 7.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `ubetalt etter aktiv`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 7.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `forsøke annullering med kun ett, ubetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke annullering med når oppdrag overføres`() {
        opprett(5.NAV, 2.HELG)
        utbetal(0)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke å opprette nytt oppdrag når det allerede eksisterer et ikke-utbetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { opprett(5.NAV, 2.HELG, 5.NAV) }
    }

    @Test
    fun `ikke-godkjent periode`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        val siste = utbetal(0, godkjent = false)
        assertTilstander(0, "Ny", "Avvist")
        assertIkkeBehov(Behovtype.Utbetaling, siste)
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `avvist utbetaling`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0)
        kvitter(0, AVVIST)
        assertTilstander(0, "Ny", "UtbetalingOverført", "Avvist")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    private fun opprettOgUtbetal(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, sisteDato: LocalDate? = null) {
        opprett(*dager, startdato = startdato, sisteDato = sisteDato)
        utbetal(0)
        kvitter(0)
    }

    private fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: no.nav.helse.hendelser.Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    private fun assertTilstander(fagsystemIdIndeks: Int, vararg tilstand: String) {
        assertEquals(tilstand.toList(), observer.tilstander(fagsystemIder[fagsystemIdIndeks]))
    }

    private fun assertIkkeBehov(behovtype: Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().none { it.type == behovtype })
    }

    private fun assertBehov(behovtype: Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().last().type == behovtype) { aktivitetslogg.toString() }
    }

    private fun assertTomHistorie(fagsystemIdIndeks: Int) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                assertTrue(it.isEmpty())
            }
    }

    private fun assertHistorie(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                assertEquals(periode.endInclusive, it.sisteDato())
                assertAlleDager(it, periode, *dager)
            }
    }

    private fun assertTomUtbetalingstidslinje(fagsystemIdIndeks: Int) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertTrue(it.isEmpty())
        }
    }

    private fun assertUtbetalingstidslinje(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertEquals(periode.endInclusive, it.sisteDato())
            assertAlleDager(it, periode, *dager)
        }
    }

    private fun utbetal(fagsystemIdIndeks: Int, godkjent: Boolean = true): IAktivitetslogg {
        return Utbetalingsgodkjenning(
            UUID.randomUUID(),
            AKTØRID,
            FNR,
            ORGNR,
            UUID.randomUUID().toString(),
            IDENT,
            EPOST,
            godkjent,
            GODKJENTTIDSPUNKT,
            false
        ).also {
            it.kontekst(person)
            fagsystemIder[fagsystemIdIndeks].håndter(it, MAKSDATO)
        }
    }

    private fun annuller(fagsystemIdIndeks: Int): IAktivitetslogg {
        return AnnullerUtbetaling(
            UUID.randomUUID(),
            AKTØRID,
            FNR,
            ORGNR,
            inspektør.fagsystemId(fagsystemIdIndeks),
            IDENT,
            EPOST,
            GODKJENTTIDSPUNKT
        ).also {
            it.kontekst(person)
            fagsystemIder[fagsystemIdIndeks].håndter(it)
        }
    }

    private fun kvitter(fagsystemIdIndeks: Int, oppdragstatus: Oppdragstatus = AKSEPTERT): IAktivitetslogg {
        return UtbetalingHendelse(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            AKTØRID,
            FNR,
            ORGNR,
            inspektør.fagsystemId(fagsystemIdIndeks),
            oppdragstatus,
            "",
            GODKJENTTIDSPUNKT,
            IDENT,
            EPOST,
            false
        ).also {
            it.kontekst(person)
            fagsystemIder[fagsystemIdIndeks].håndter(it)
        }
    }


    private fun opprett(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, sisteDato: LocalDate? = null): Oppdrag {
        val tidslinje = tidslinjeOf(*dager, startDato = startdato)
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            listOf(1.januar),
            1.januar
        ).betal().let {
            return OppdragBuilder(
                tidslinje,
                ORGNR,
                Fagområde.SykepengerRefusjon,
                sisteDato ?: tidslinje.sisteDato()
            ).result().also {
                fagsystemId = FagsystemId.utvide(fagsystemIder, it, tidslinje, aktivitetslogg)
                fagsystemId.register(observer)
                oppdrag[it] = fagsystemId
            }
        }
    }

}
