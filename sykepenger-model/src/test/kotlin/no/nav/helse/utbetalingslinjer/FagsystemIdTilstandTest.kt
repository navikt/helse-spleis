package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AVVIST
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.Utbetalingsdager
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class FagsystemIdTilstandTest {

    private val fagsystemIder = mutableListOf<FagsystemId>()
    private lateinit var fagsystemId: FagsystemId
    private val oppdrag = mutableMapOf<Oppdrag, FagsystemId>()
    private val aktivitetslogg get() = person.aktivitetslogg
    private val observer = Observer()
    private lateinit var person: Person

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
        assertEquals(listOf("Ny", "UtbetalingOverført", "Aktiv"), observer.tilstander(fagsystemId))
        assertBehov(Behovtype.Utbetaling)
    }

    @Test
    fun `happy path med flere utbetalinger`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprettOgUtbetal(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        assertEquals(listOf("Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "UtbetalingOverført", "Aktiv"), observer.tilstander(fagsystemId))
        assertBehov(Behovtype.Utbetaling)
    }

    @Test
    fun `annullering happy path`() {
        opprettOgUtbetal(5.NAV, 2.HELG, 5.NAV)
        val siste = annuller()
        kvitter()
        assertEquals(listOf("Ny", "UtbetalingOverført", "Aktiv", "AnnulleringOverført", "Annullert"), observer.tilstander(fagsystemId))
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
    }

    @Test
    fun `annullering når siste er ubetalt`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        val siste = annuller()
        kvitter()
        assertEquals(listOf("Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "AnnulleringOverført", "Annullert"), observer.tilstander(fagsystemId))
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
    }

    @Test
    fun `aktiv ubetalt går tilbake til aktiv ved avslag`() {
        opprettOgUtbetal(5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(godkjent = false)
        assertEquals(listOf("Ny", "UtbetalingOverført", "Aktiv", "Ubetalt", "Aktiv"), observer.tilstander(fagsystemId))
    }

    @Test
    fun `forsøke annullering med kun ett, ubetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { annuller() }
    }

    @Test
    fun `forsøke annullering med når oppdrag overføres`() {
        opprett(5.NAV, 2.HELG)
        utbetal()
        assertThrows<IllegalStateException> { annuller() }
    }

    @Test
    fun `forsøke å opprette nytt oppdrag når det allerede eksisterer et ikke-utbetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { opprett(5.NAV, 2.HELG, 5.NAV) }
    }

    @Test
    fun `ikke-godkjent periode`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        val siste = utbetal(godkjent = false)
        assertEquals(listOf("Ny", "Avvist"), observer.tilstander(fagsystemId))
        assertIkkeBehov(Behovtype.Utbetaling, siste)
    }

    @Test
    fun `avvist utbetaling`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal()
        kvitter(oppdragstatus = AVVIST)
        assertEquals(listOf("Ny", "UtbetalingOverført", "Avvist"), observer.tilstander(fagsystemId))
    }

    private fun opprettOgUtbetal(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, sisteDato: LocalDate? = null) {
        opprett(*dager, startdato = startdato, sisteDato = sisteDato)
        utbetal()
        kvitter()
    }

    private fun assertIkkeBehov(behovtype: Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().none { it.type == behovtype })
    }

    private fun assertBehov(behovtype: Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().last().type == behovtype) { aktivitetslogg.toString() }
    }

    private fun utbetal(fagsystemId: FagsystemId = this.fagsystemId, godkjent: Boolean = true): IAktivitetslogg {
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
            fagsystemId.håndter(it, MAKSDATO)
        }
    }

    private fun annuller(fagsystemId: FagsystemId = this.fagsystemId): IAktivitetslogg {
        return AnnullerUtbetaling(
            UUID.randomUUID(),
            AKTØRID,
            FNR,
            ORGNR,
            fagsystemId.fagsystemId(),
            IDENT,
            EPOST,
            GODKJENTTIDSPUNKT
        ).also {
            it.kontekst(person)
            fagsystemId.håndter(it)
        }
    }

    private fun kvitter(fagsystemId: FagsystemId = this.fagsystemId, oppdragstatus: Oppdragstatus = AKSEPTERT): IAktivitetslogg {
        return UtbetalingHendelse(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            AKTØRID,
            FNR,
            ORGNR,
            fagsystemId.fagsystemId(),
            oppdragstatus,
            "",
            GODKJENTTIDSPUNKT,
            IDENT,
            EPOST,
            false
        ).also {
            it.kontekst(person)
            fagsystemId.håndter(it)
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
                fagsystemId = FagsystemId.kobleTil(fagsystemIder, it, aktivitetslogg)
                fagsystemId.register(observer)
                oppdrag[it] = fagsystemId
            }
        }
    }

    private class Observer : FagsystemIdObserver {
        private val tilstander = mutableMapOf<FagsystemId, MutableList<String>>()

        fun tilstander(fagsystemId: FagsystemId) = tilstander[fagsystemId] ?: fail { "OH NO" }

        override fun tilstandEndret(fagsystemId: FagsystemId, gammel: String, ny: String) {
            tilstander.getOrPut(fagsystemId) { mutableListOf(gammel) }.add(ny)
        }

        override fun utbetalt() {
            super.utbetalt()
        }

        override fun annullert() {
            super.annullert()
        }

        override fun kvittert() {
            super.kvittert()
        }

        override fun overført() {
            super.overført()
        }
    }

}
