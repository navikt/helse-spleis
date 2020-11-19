package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.Utbetalingsdager
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbstractFagsystemIdTest {
    protected companion object {
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        const val ORGNR = "123456789"
        const val IDENT = "A999999"
        const val EPOST = "saksbehandler@saksbehandlersen.no"
        private const val AVSTEMMINGSNØKKEL = 1L
        val MAKSDATO = LocalDate.now()
        const val FORBUKTE_DAGER = 100
        const val GJENSTÅENDE_DAGER = 148
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    protected val fagsystemIder = mutableListOf<FagsystemId>()
    protected lateinit var fagsystemId: FagsystemId
    private val oppdrag = mutableMapOf<Oppdrag, FagsystemId>()
    protected val aktivitetslogg get() = person.aktivitetslogg
    private val observatør = FagsystemIdObservatør()
    private lateinit var person: Person

    protected val inspektør get() = FagsystemIdInspektør(fagsystemIder)

    @BeforeEach
    fun beforeEach() {
        person = Person(AKTØRID, FNR)
        fagsystemIder.clear()
        oppdrag.clear()
    }

    protected fun assertUtbetalingsbehov(maksdato: LocalDate?, saksbehandler: String, saksbehandlerEpost: String, godkjenttidspunkt: LocalDateTime, erAnnullering: Boolean) {
        assertUtbetalingsbehov { utbetalingbehov ->
            assertEquals(maksdato?.toString(), utbetalingbehov["maksdato"])
            assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
            assertEquals(saksbehandlerEpost, utbetalingbehov["saksbehandlerEpost"])
            assertEquals("$godkjenttidspunkt", utbetalingbehov.getValue("godkjenttidspunkt"))
            assertEquals(erAnnullering, utbetalingbehov["annullering"] as Boolean)
        }
    }

    protected fun assertSimuleringsbehov(maksdato: LocalDate, saksbehandler: String) {
        assertSimuleringsbehov { utbetalingbehov ->
            assertEquals("$maksdato", utbetalingbehov["maksdato"])
            assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
        }
    }

    protected fun assertUtbetalingsbehov(block: (Map<String, Any>) -> Unit) {
        aktivitetslogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling, block)
    }

    protected fun assertSimuleringsbehov(block: (Map<String, Any>) -> Unit) {
        aktivitetslogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering, block)
    }

    private fun IAktivitetslogg.sisteBehov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, block: (Map<String, Any>) -> Unit) {
        this.behov()
            .last { it.type == type }
            .detaljer()
            .also { block(it) }
    }

    protected fun opprettOgUtbetal(fagsystemIdIndeks: Int, vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, godkjent: Boolean = true, automatiskBehandling: Boolean = false): FagsystemId? {
        val fagsystemId = opprett(*dager, startdato = startdato)
        klargjør(fagsystemIdIndeks)
        utbetal(fagsystemIdIndeks, godkjent = godkjent, automatiskBehandling = automatiskBehandling)
        if (godkjent) {
            assertUtbetalingsbehov(MAKSDATO, IDENT, EPOST, GODKJENTTIDSPUNKT, false)
            overført(fagsystemIdIndeks)
            kvitter(fagsystemIdIndeks)
        }
        return fagsystemId
    }

    protected fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
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

    protected fun assertTilstander(fagsystemIdIndeks: Int, vararg tilstand: String) {
        assertEquals(tilstand.toList(), observatør.tilstander(fagsystemIder[fagsystemIdIndeks]))
    }

    protected fun assertIkkeBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().none { it.type == behovtype })
    }

    protected fun assertBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        assertTrue(aktivitetslogg.behov().last().type == behovtype) { aktivitetslogg.toString() }
    }

    protected fun assertTomHistorie(fagsystemIdIndeks: Int) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                assertTrue(it.isEmpty())
            }
        assertTrue(inspektør.utbetaltTidslinje(fagsystemIdIndeks).isEmpty())
    }

    protected fun assertHistorie(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                assertEquals(it, inspektør.utbetaltTidslinje(fagsystemIdIndeks))
                assertEquals(periode.endInclusive, it.sisteDato())
                assertAlleDager(it, periode, *dager)
            }
    }

    protected fun assertTomUtbetalingstidslinje(fagsystemIdIndeks: Int) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertTrue(it.isEmpty())
        }
    }

    protected fun assertUtbetalingstidslinje(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>, sisteDato: LocalDate = periode.endInclusive) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            assertEquals(sisteDato, it.sisteDato())
            assertAlleDager(it, periode, *dager)
        }
    }

    protected fun klargjør(fagsystemIdIndeks: Int) {
        fagsystemIder[fagsystemIdIndeks].klargjør(MAKSDATO, FORBUKTE_DAGER, GJENSTÅENDE_DAGER)
    }

    protected fun utbetal(fagsystemIdIndeks: Int, godkjent: Boolean = true, automatiskBehandling: Boolean = false): IAktivitetslogg {
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
            automatiskBehandling
        ).also {
            it.kontekst(person)
            fagsystemIder[fagsystemIdIndeks].håndter(it)
        }
    }

    protected fun annuller(fagsystemIdIndeks: Int): IAktivitetslogg {
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

    protected fun overført(fagsystemIdIndeks: Int): IAktivitetslogg {
        return UtbetalingOverført(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            AKTØRID,
            FNR,
            ORGNR,
            inspektør.fagsystemId(fagsystemIdIndeks),
            AVSTEMMINGSNØKKEL,
            LocalDateTime.now()
        ).also {
            it.kontekst(person)
            fagsystemIder[fagsystemIdIndeks].håndter(it)
        }
    }

    protected fun kvitter(fagsystemIdIndeks: Int, oppdragstatus: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT): IAktivitetslogg {
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

    protected fun opprett(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar): FagsystemId? {
        val tidslinje = tidslinjeOf(*dager, startDato = startdato)
        return MaksimumUtbetaling(listOf(tidslinje), aktivitetslogg, listOf(1.januar), 1.januar)
            .also { it.betal() }
            .let {
                OppdragBuilder(tidslinje, ORGNR, Fagområde.SykepengerRefusjon)
                    .result(fagsystemIder, observatør, aktivitetslogg)
            }?.also { fagsystemId = it }
    }
}
