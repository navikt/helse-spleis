package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class FagsystemIdTest {

    companion object {
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        private const val ORGNR = "123456789"
        private const val IDENT = "A999999"
        private const val EPOST = "saksbehandler@saksbehandlersen.no"
        private val MAKSDATO = LocalDate.now()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private val fagsystemIder = mutableListOf<FagsystemId>()
    private lateinit var fagsystemId: FagsystemId
    private val oppdrag = mutableMapOf<Oppdrag, FagsystemId>()
    private val aktivitetslogg get() = person.aktivitetslogg
    private val observatør = FagsystemIdObservatør()
    private lateinit var person: Person

    private val inspektør get() = FagsystemIdInspektør(fagsystemIder)

    @BeforeEach
    fun beforeEach() {
        person = Person(AKTØRID, FNR)
        fagsystemIder.clear()
        oppdrag.clear()
    }

    @Test
    fun `happy path`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `utbetaling overført`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført")
        assertUtbetalingstidslinje(0,1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertTomHistorie(0)
    }

    @Test
    fun `happy path med flere utbetalinger`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV, 2.HELG)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "UtbetalingOverført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 14.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 14.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `annullering happy path`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        val siste = annuller(0)
        kvitter(0)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "AnnulleringOverført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
        assertTomHistorie(0)
    }

    @Test
    fun `annullering overført`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        val siste = annuller(0)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "AnnulleringOverført")
        assertBehov(Behovtype.Utbetaling, siste)
        assertFalse(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
        assertHistorie(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `annullering når siste er ubetalt`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        val siste = annuller(0)
        kvitter(0)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "AnnulleringOverført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
    }

    @Test
    fun `utbetale på annullert fagsystemId`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        annuller(0)
        kvitter(0)
        assertThrows<IllegalStateException> { utbetal(0) }
    }

    @Test
    fun `aktiv ubetalt går tilbake til aktiv ved avslag`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0, godkjent = false)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "Aktiv")
        assertUtbetalingstidslinje(0,1.januar til 7.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 7.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `ubetalt etter aktiv`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt")
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
        assertTilstander(0, "Initiell", "Ny", "Avvist")
        assertIkkeBehov(Behovtype.Utbetaling, siste)
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `avvist utbetaling`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0)
        kvitter(0, UtbetalingHendelse.Oppdragstatus.AVVIST)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Avvist")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `Ny fagsystemId`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        assertEquals(1, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertTomHistorie(0)
    }

    @Test
    fun simulere() {
        opprett(1.NAV)
        val maksdato = LocalDate.MAX
        val saksbehandler = "Z999999"
        fagsystemId.simuler(aktivitetslogg, maksdato, saksbehandler)
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertSimuleringsbehov(maksdato, saksbehandler)
    }

    @Test
    fun `Nytt element når fagsystemId'er er forskjellige`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 16.AP, 1.NAV)
        assertEquals(2, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv")
        assertTilstander(1, "Initiell", "Ny")
        assertUtbetalingstidslinje(0, 1.januar til 1.januar, NavDag::class)
        assertHistorie(0, 1.januar til 1.januar, NavDag::class)
        assertUtbetalingstidslinje(1, 1.januar til 1.januar, NavDag::class, sisteDato = 18.januar)
        assertUtbetalingstidslinje(1, 2.januar til 17.januar, ArbeidsgiverperiodeDag::class, sisteDato = 18.januar)
        assertUtbetalingstidslinje(1, 18.januar til 18.januar, NavDag::class)
        assertTomHistorie(1)
    }

    @Test
    fun `Nytt element ved ny AGP`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 1.AP, 1.NAV)
        assertEquals(2, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv")
        assertTilstander(1, "Initiell", "Ny")
    }

    @Test
    fun `samme fagsystemId med gap mindre enn 16 dager`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 1.ARB, 1.NAV)
        assertEquals(1, fagsystemIder.size)
    }

    @Test
    fun `legger nytt oppdrag til på eksisterende fagsystemId`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        opprett(16.AP, 5.NAV, 5.NAV(1300))
        assertEquals(1, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "Ubetalt")
    }

    @Test
    fun `Ny fagsystemId når eksisterende fagsystemId er annullert`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        annuller(0)
        kvitter(0)
        opprett(21.UTELATE, 15.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        assertTrue(fagsystemIder[0].erAnnullert())
        assertFalse(fagsystemIder[1].erAnnullert())
        assertTilstander(0, "Initiell", "Ny", "UtbetalingOverført", "Aktiv", "AnnulleringOverført", "Annullert")
        assertTilstander(1, "Initiell", "Ny")
    }

    @Test
    fun `Ny fagsystemId når eksisterende fagsystemId er avvist`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = false)
        opprett(21.UTELATE, 15.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        inspektør.utbetalingstidslinje(1).also { tidslinje ->
            assertEquals(22.januar, tidslinje.førsteDato())
        }
    }

    @Test
    fun `avslag på første oppdrag fjerner ikke dagene`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = false)
        inspektør.utbetalingstidslinje(0).also { tidslinje ->
            assertEquals(21.januar, tidslinje.sisteDato())
        }
    }

    @Test
    fun `avslag på andre oppdrag fjerner dagene`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = true)
        opprettOgUtbetal(0, 16.AP, 15.NAV, godkjent = false)
        inspektør.utbetalingstidslinje(0).also { tidslinje ->
            assertEquals(21.januar, tidslinje.sisteDato())
        }
    }

    @Test
    fun `godkjent utbetalingsgodkjenning`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = true)
        assertFalse(fagsystemId.erTom())
    }

    @Test
    fun `mapper riktig når det finnes flere fagsystemId'er`() {
        val oppdrag1 = opprettOgUtbetal(0, 16.AP, 5.NAV)
        val oppdrag2 = opprettOgUtbetal(1, 16.AP, 5.NAV, startdato = 1.mars)
        val oppdrag1Oppdatert = opprett(16.AP, 5.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        assertEquals(oppdrag1.fagsystemId(), oppdrag1Oppdatert.fagsystemId())
        assertNotEquals(oppdrag2.fagsystemId(), oppdrag1Oppdatert.fagsystemId())
    }

    private fun assertUtbetalingsbehov(maksdato: LocalDate?, saksbehandler: String, saksbehandlerEpost: String, godkjenttidspunkt: LocalDateTime, erAnnullering: Boolean) {
        assertUtbetalingsbehov { utbetalingbehov ->
            assertEquals(maksdato?.toString(), utbetalingbehov["maksdato"])
            assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
            assertEquals(saksbehandlerEpost, utbetalingbehov["saksbehandlerEpost"])
            assertEquals("$godkjenttidspunkt", utbetalingbehov.getValue("godkjenttidspunkt"))
            assertEquals(erAnnullering, utbetalingbehov["annullering"] as Boolean)
        }
    }

    private fun assertSimuleringsbehov(maksdato: LocalDate, saksbehandler: String) {
        assertSimuleringsbehov { utbetalingbehov ->
            assertEquals("$maksdato", utbetalingbehov["maksdato"])
            assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
        }
    }

    private fun assertUtbetalingsbehov(block: (Map<String, Any>) -> Unit) {
        aktivitetslogg.sisteBehov(Behovtype.Utbetaling, block)
    }

    private fun assertSimuleringsbehov(block: (Map<String, Any>) -> Unit) {
        aktivitetslogg.sisteBehov(Behovtype.Simulering, block)
    }

    private fun IAktivitetslogg.sisteBehov(type: Behovtype, block: (Map<String, Any>) -> Unit) {
        this.behov()
            .last { it.type == type }
            .detaljer()
            .also { block(it) }
    }

    private fun opprettOgUtbetal(fagsystemIdIndeks: Int, vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, godkjent: Boolean = true): Oppdrag {
        val oppdrag = opprett(*dager, startdato = startdato)
        utbetal(fagsystemIdIndeks, godkjent = godkjent)
        if (godkjent) {
            assertUtbetalingsbehov(MAKSDATO, IDENT, EPOST, GODKJENTTIDSPUNKT, false)
            kvitter(fagsystemIdIndeks)
        }
        return oppdrag
    }

    private fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
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
        assertEquals(tilstand.toList(), observatør.tilstander(fagsystemIder[fagsystemIdIndeks]))
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
        assertTrue(inspektør.utbetaltTidslinje(fagsystemIdIndeks).isEmpty())
    }

    private fun assertHistorie(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                assertEquals(it, inspektør.utbetaltTidslinje(fagsystemIdIndeks))
                assertEquals(periode.endInclusive, it.sisteDato())
                assertAlleDager(it, periode, *dager)
            }
    }

    private fun assertTomUtbetalingstidslinje(fagsystemIdIndeks: Int) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertTrue(it.isEmpty())
        }
    }

    private fun assertUtbetalingstidslinje(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>, sisteDato: LocalDate = periode.endInclusive) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertEquals(sisteDato, it.sisteDato())
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

    private fun kvitter(fagsystemIdIndeks: Int, oppdragstatus: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT): IAktivitetslogg {
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

    private fun opprett(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar): Oppdrag {
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
                Fagområde.SykepengerRefusjon
            ).result().also {
                fagsystemId = FagsystemId.utvide(fagsystemIder, observatør, it, tidslinje, aktivitetslogg)
                oppdrag[it] = fagsystemId
            }
        }
    }
}
