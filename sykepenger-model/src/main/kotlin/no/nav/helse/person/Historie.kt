package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.Periodetype.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Historie() {

    internal constructor(utbetalingshistorikk: Utbetalingshistorikk): this() {
        utbetalingshistorikk.append(infotrygdbøtte)
    }

    private companion object {
        private const val ALLE_ARBEIDSGIVERE = "UKJENT"
        private fun Utbetalingsdag.erSykedag() = this is NavDag || this is NavHelgDag || this is ArbeidsgiverperiodeDag
    }

    private val infotrygdbøtte = Historikkbøtte()
    private val spleisbøtte = Historikkbøtte()
    private val sykdomstidslinjer get() = infotrygdbøtte.sykdomstidslinjer() + spleisbøtte.sykdomstidslinjer()
    private fun utbetalingstidslinje(orgnummer: String) = infotrygdbøtte.utbetalingstidslinje(orgnummer) + spleisbøtte.utbetalingstidslinje(orgnummer)
    private fun sykdomstidslinje(orgnummer: String) = infotrygdbøtte.sykdomstidslinje(orgnummer).merge(spleisbøtte.sykdomstidslinje(orgnummer), replace)

    internal fun periodetype(orgnr: String, periode: Periode): Periodetype {
        val skjæringstidspunkt = sykdomstidslinje(orgnr).skjæringstidspunkt(periode.endInclusive) ?: periode.start
        return when {
            skjæringstidspunkt in periode -> FØRSTEGANGSBEHANDLING
            infotrygdbøtte.utbetalingstidslinje(orgnr)[skjæringstidspunkt].erSykedag() -> {
                val sammenhengendePeriode = Periode(skjæringstidspunkt, periode.start.minusDays(1))
                when {
                    spleisbøtte.harOverlappendeHistorikk(orgnr, sammenhengendePeriode) -> INFOTRYGDFORLENGELSE
                    else -> OVERGANG_FRA_IT
                }
            }
            else -> FORLENGELSE
        }
    }

    internal fun forlengerInfotrygd(orgnr: String, periode: Periode) =
        periodetype(orgnr, periode) in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE)

    internal fun erPingPong(orgnr: String, periode: Periode): Boolean {
        val periodetype = periodetype(orgnr, periode)
        if (periodetype !in listOf(FORLENGELSE, INFOTRYGDFORLENGELSE)) return false
        val skjæringstidspunkt = skjæringstidspunkt(periode.endInclusive) ?: return false
        val antallBruddstykker = infotrygdbøtte
            .sykdomstidslinje(orgnr)
            .skjæringstidspunkter(periode.endInclusive)
            .filter { dato -> dato >= skjæringstidspunkt && dato > periode.start.minusMonths(6) }
            .size
        if (periodetype == INFOTRYGDFORLENGELSE) return antallBruddstykker > 1
        return antallBruddstykker >= 1
    }

    private fun sammenhengendePeriode(orgnr: String, periode: Periode) =
        sykdomstidslinje(orgnr).skjæringstidspunkt(periode.endInclusive)
            ?.let { periode.oppdaterFom(it) }

    internal fun skjæringstidspunkt(tom: LocalDate) = Sykdomstidslinje.skjæringstidspunkt(tom, sykdomstidslinjer)
    internal fun skjæringstidspunkter(tom: LocalDate) = Sykdomstidslinje.skjæringstidspunkter(tom, sykdomstidslinjer)

    internal fun add(orgnummer: String, tidslinje: Utbetalingstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
        add(orgnummer, Historikkbøtte.konverter(tidslinje)) // for å ta høyde for forkastet historikk
    }

    internal fun add(orgnummer: String, tidslinje: Sykdomstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    internal class Historikkbøtte {
        private val utbetalingstidslinjer = mutableMapOf<String, Utbetalingstidslinje>()
        private val sykdomstidslinjer = mutableMapOf<String, Sykdomstidslinje>()

        internal fun sykdomstidslinjer() = sykdomstidslinjer.values
        internal fun sykdomstidslinje(orgnummer: String) = sykdomstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Sykdomstidslinje() }.merge(
            sykdomstidslinjer.getOrElse(orgnummer) { Sykdomstidslinje() }, replace)

        internal fun utbetalingstidslinje(orgnummer: String) =
            utbetalingstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Utbetalingstidslinje() } +
                utbetalingstidslinjer.getOrElse(orgnummer) { Utbetalingstidslinje() }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
            utbetalingstidslinjer.merge(orgnummer, tidslinje, Utbetalingstidslinje::plus)
        }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Sykdomstidslinje) {
            sykdomstidslinjer.merge(orgnummer, tidslinje) { venstre, høyre -> venstre.merge(høyre, replace) }
        }

        internal fun harOverlappendeHistorikk(orgnr: String, periode: Periode) =
            sykdomstidslinje(orgnr).subset(periode).any { it !is UkjentDag }

        internal companion object {
            internal fun konverter(utbetalingstidslinje: Utbetalingstidslinje) =
                Sykdomstidslinje(utbetalingstidslinje.map {
                    it.dato to when {
                        !it.dato.erHelg() && it.erSykedag() -> Dag.Sykedag(it.dato, it.økonomi.medGrad(), INGEN)
                        it.dato.erHelg() && it.erSykedag() -> Dag.SykHelgedag(it.dato, it.økonomi.medGrad(), INGEN)
                        else -> UkjentDag(it.dato, INGEN)
                    }
                }.associate { it })

            private fun Økonomi.medGrad() = Økonomi.sykdomsgrad(reflection { grad, _, _, _, _, _, _ -> grad }.prosent)
        }
    }
}
