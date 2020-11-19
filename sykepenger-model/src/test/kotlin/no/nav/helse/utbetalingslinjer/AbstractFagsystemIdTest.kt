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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbstractFagsystemIdTest {
    protected companion object {
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        private const val ORGNR = "123456789"
        const val IDENT = "A999999"
        const val EPOST = "saksbehandler@saksbehandlersen.no"
        private const val AVSTEMMINGSNØKKEL = 1L
        val MAKSDATO = LocalDate.now()
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
            Assertions.assertEquals(maksdato?.toString(), utbetalingbehov["maksdato"])
            Assertions.assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
            Assertions.assertEquals(saksbehandlerEpost, utbetalingbehov["saksbehandlerEpost"])
            Assertions.assertEquals("$godkjenttidspunkt", utbetalingbehov.getValue("godkjenttidspunkt"))
            Assertions.assertEquals(erAnnullering, utbetalingbehov["annullering"] as Boolean)
        }
    }

    protected fun assertSimuleringsbehov(maksdato: LocalDate, saksbehandler: String) {
        assertSimuleringsbehov { utbetalingbehov ->
            Assertions.assertEquals("$maksdato", utbetalingbehov["maksdato"])
            Assertions.assertEquals(saksbehandler, utbetalingbehov["saksbehandler"])
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

    protected fun opprettOgUtbetal(fagsystemIdIndeks: Int, vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, godkjent: Boolean = true): Oppdrag {
        val oppdrag = opprett(*dager, startdato = startdato)
        utbetal(fagsystemIdIndeks, godkjent = godkjent)
        if (godkjent) {
            assertUtbetalingsbehov(MAKSDATO, IDENT, EPOST, GODKJENTTIDSPUNKT, false)
            overført(fagsystemIdIndeks)
            kvitter(fagsystemIdIndeks)
        }
        return oppdrag
    }

    protected fun assertAlleDager(utbetalingstidslinje: Utbetalingstidslinje, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        utbetalingstidslinje.subset(periode).also { tidslinje ->
            Assertions.assertTrue(tidslinje.all { it::class in dager }) {
                val ulikeDager = tidslinje.filter { it::class !in dager }
                "Forventet at alle dager skal være en av: ${dager.joinToString { it.simpleName ?: "UKJENT" }}.\n" +
                    ulikeDager.joinToString(prefix = "  - ", separator = "\n  - ", postfix = "\n") {
                        "${it.dato} er ${it::class.simpleName}"
                    } + "\nUtbetalingstidslinje:\n" + tidslinje.toString() + "\n"
            }
        }
    }

    protected fun assertTilstander(fagsystemIdIndeks: Int, vararg tilstand: String) {
        Assertions.assertEquals(tilstand.toList(), observatør.tilstander(fagsystemIder[fagsystemIdIndeks]))
    }

    protected fun assertIkkeBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        Assertions.assertTrue(aktivitetslogg.behov().none { it.type == behovtype })
    }

    protected fun assertBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, aktivitetslogg: IAktivitetslogg = this.aktivitetslogg) {
        Assertions.assertTrue(aktivitetslogg.behov().last().type == behovtype) { aktivitetslogg.toString() }
    }

    protected fun assertTomHistorie(fagsystemIdIndeks: Int) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                Assertions.assertTrue(it.isEmpty())
            }
        Assertions.assertTrue(inspektør.utbetaltTidslinje(fagsystemIdIndeks).isEmpty())
    }

    protected fun assertHistorie(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>) {
        Historie.Historikkbøtte()
            .also { fagsystemIder[fagsystemIdIndeks].append(ORGNR, it) }
            .utbetalingstidslinje()
            .also {
                Assertions.assertEquals(it, inspektør.utbetaltTidslinje(fagsystemIdIndeks))
                Assertions.assertEquals(periode.endInclusive, it.sisteDato())
                assertAlleDager(it, periode, *dager)
            }
    }

    protected fun assertTomUtbetalingstidslinje(fagsystemIdIndeks: Int) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            Assertions.assertTrue(it.isEmpty())
        }
    }

    protected fun assertUtbetalingstidslinje(fagsystemIdIndeks: Int, periode: Periode, vararg dager: KClass<out Utbetalingstidslinje.Utbetalingsdag>, sisteDato: LocalDate = periode.endInclusive) {
        inspektør.utbetalingstidslinje(fagsystemIdIndeks).also {
            Assertions.assertEquals(sisteDato, it.sisteDato())
            assertAlleDager(it, periode, *dager)
        }
    }

    protected fun utbetal(fagsystemIdIndeks: Int, godkjent: Boolean = true): IAktivitetslogg {
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

    protected fun opprett(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar): Oppdrag {
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
                fagsystemId = FagsystemId.utvide(fagsystemIder, observatør, it, tidslinje, MAKSDATO, aktivitetslogg)
                oppdrag[it] = fagsystemId
            }
        }
    }
}
