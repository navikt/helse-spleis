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
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderVol2
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Historie() {

    internal constructor(person: Person): this() {
        person.append(spleisbøtte)
    }

    internal constructor(person: Person, utbetalingshistorikk: Utbetalingshistorikk): this(person) {
        utbetalingshistorikk.append(infotrygdbøtte)
    }

    private val infotrygdbøtte = Historikkbøtte()
    private val spleisbøtte = Historikkbøtte(konverterUtbetalingstidslinje = true)
    private val sykdomstidslinjer get() = infotrygdbøtte.sykdomstidslinjer() + spleisbøtte.sykdomstidslinjer()

    internal fun periodetype(orgnr: String, periode: Periode): Periodetype {
        val skjæringstidspunkt = sykdomstidslinje(orgnr).skjæringstidspunkt(periode.endInclusive) ?: periode.start
        return when {
            skjæringstidspunkt in periode -> FØRSTEGANGSBEHANDLING
            infotrygdbøtte.erUtbetaltDag(orgnr, skjæringstidspunkt) -> {
                val sammenhengendePeriode = Periode(skjæringstidspunkt, periode.start.minusDays(1))
                when {
                    spleisbøtte.harOverlappendeHistorikk(orgnr, sammenhengendePeriode) -> INFOTRYGDFORLENGELSE
                    else -> OVERGANG_FRA_IT
                }
            }
            else -> FORLENGELSE
        }
    }

    internal fun beregnUtbetalingstidslinje(organisasjonsnummer: String, periode: Periode, inntektshistorikk: Inntektshistorikk, arbeidsgiverRegler: ArbeidsgiverRegler): Utbetalingstidslinje {
        return UtbetalingstidslinjeBuilder(
            sammenhengendePeriode = sammenhengendePeriode(periode),
            inntektshistorikk = inntektshistorikk,
            forlengelseStrategy = { harInfotrygdopphav(organisasjonsnummer, periode) },
            arbeidsgiverRegler = arbeidsgiverRegler
        ).result(sykdomstidslinje(organisasjonsnummer)).minus(infotrygdtidslinje(organisasjonsnummer))

    }

    internal fun beregnUtbetalingstidslinjeVol2(organisasjonsnummer: String, periode: Periode, inntektshistorikk: InntektshistorikkVol2, arbeidsgiverRegler: ArbeidsgiverRegler): Utbetalingstidslinje {
        return UtbetalingstidslinjeBuilderVol2(
            sammenhengendePeriode = sammenhengendePeriode(periode),
            inntektshistorikkVol2 = inntektshistorikk,
            skjæringstidspunkter = skjæringstidspunkter(periode),
            forlengelseStrategy = { harInfotrygdopphav(organisasjonsnummer, periode) },
            arbeidsgiverRegler = arbeidsgiverRegler
        ).result(sykdomstidslinje(organisasjonsnummer)).minus(infotrygdtidslinje(organisasjonsnummer))
    }

    internal fun erForlengelse(orgnr: String, periode: Periode) =
        periodetype(orgnr, periode) != FØRSTEGANGSBEHANDLING

    internal fun forlengerInfotrygd(orgnr: String, periode: Periode) =
        periodetype(orgnr, periode) in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE)

    internal fun erPingPong(orgnr: String, periode: Periode): Boolean {
        val periodetype = periodetype(orgnr, periode)
        if (periodetype !in listOf(FORLENGELSE, INFOTRYGDFORLENGELSE)) return false
        val skjæringstidspunkt = skjæringstidspunkt(periode) ?: return false
        val antallBruddstykker = infotrygdbøtte
            .sykdomstidslinje(orgnr)
            .skjæringstidspunkter(periode.endInclusive)
            .filter { dato -> dato >= skjæringstidspunkt && dato > periode.start.minusMonths(6) }
            .size
        if (periodetype == INFOTRYGDFORLENGELSE) return antallBruddstykker > 1
        return antallBruddstykker >= 1
    }

    internal fun sammenhengendePeriode(periode: Periode) =
        skjæringstidspunkt(periode)?.let { periode.oppdaterFom(it) } ?: periode

    internal fun skjæringstidspunkt(periode: Periode) = Sykdomstidslinje.skjæringstidspunkt(periode.endInclusive, sykdomstidslinjer)
    internal fun skjæringstidspunkter(periode: Periode) = Sykdomstidslinje.skjæringstidspunkter(periode.endInclusive, sykdomstidslinjer)

    internal fun utbetalingstidslinje(periode: Periode) =
        (infotrygdbøtte.utbetalingstidslinje() + spleisbøtte.utbetalingstidslinje()).kutt(periode.endInclusive)

    internal fun add(orgnummer: String, tidslinje: Utbetalingstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    internal fun add(orgnummer: String, tidslinje: Sykdomstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    private fun harInfotrygdopphav(organisasjonsnummer: String, periode: Periode): Boolean {
        val skjæringstidspunkt = skjæringstidspunkter(periode).last()
        return infotrygdbøtte.erUtbetaltDag(organisasjonsnummer, skjæringstidspunkt)
    }
    private fun infotrygdtidslinje(organisasjonsnummer: String) =
        infotrygdbøtte.utbetalingstidslinje(organisasjonsnummer)
    private fun sykdomstidslinje(orgnummer: String) = infotrygdbøtte.sykdomstidslinje(orgnummer).merge(spleisbøtte.sykdomstidslinje(orgnummer), replace)

    private companion object {
        private const val ALLE_ARBEIDSGIVERE = "UKJENT"
        private fun Utbetalingsdag.erSykedag() =
            this is NavDag || this is NavHelgDag || this is ArbeidsgiverperiodeDag || this is AvvistDag
    }

    internal class Historikkbøtte(private val konverterUtbetalingstidslinje: Boolean = false) {
        private val utbetalingstidslinjer = mutableMapOf<String, Utbetalingstidslinje>()
        private val sykdomstidslinjer = mutableMapOf<String, Sykdomstidslinje>()

        internal fun sykdomstidslinjer() = sykdomstidslinjer.values
        internal fun sykdomstidslinje(orgnummer: String) = sykdomstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Sykdomstidslinje() }.merge(
            sykdomstidslinjer.getOrElse(orgnummer) { Sykdomstidslinje() }, replace)

        internal fun utbetalingstidslinje() = utbetalingstidslinjer.values.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

        internal fun utbetalingstidslinje(orgnummer: String) =
            utbetalingstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Utbetalingstidslinje() } +
                utbetalingstidslinjer.getOrElse(orgnummer) { Utbetalingstidslinje() }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
            utbetalingstidslinjer.merge(orgnummer, tidslinje, Utbetalingstidslinje::plus)
            if (konverterUtbetalingstidslinje) add(orgnummer, konverter(tidslinje)) // for å ta høyde for forkastet historikk
        }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Sykdomstidslinje) {
            sykdomstidslinjer.merge(orgnummer, tidslinje) { venstre, høyre -> venstre.merge(høyre, replace) }
        }

        internal fun harOverlappendeHistorikk(orgnr: String, periode: Periode) =
            sykdomstidslinje(orgnr).subset(periode).any { it !is UkjentDag }

        internal fun erUtbetaltDag(orgnr: String, dato: LocalDate) =
            utbetalingstidslinje(orgnr)[dato].erSykedag()

        internal companion object {
            internal fun konverter(utbetalingstidslinje: Utbetalingstidslinje) =
                Sykdomstidslinje(utbetalingstidslinje.map {
                    it.dato to when {
                        !it.dato.erHelg() && it.erSykedag() -> Dag.Sykedag(it.dato, it.økonomi.medGrad(), INGEN)
                        !it.dato.erHelg() && it is Fridag -> Dag.Feriedag(it.dato, INGEN)
                        it.dato.erHelg() && it.erSykedag() -> Dag.SykHelgedag(it.dato, it.økonomi.medGrad(), INGEN)
                        it is Arbeidsdag -> Dag.Arbeidsdag(it.dato, INGEN)
                        it is ForeldetDag -> Dag.ForeldetSykedag(it.dato, it.økonomi.medGrad(), INGEN)
                        else -> UkjentDag(it.dato, INGEN)
                    }
                }.associate { it })

            private fun Økonomi.medGrad() = Økonomi.sykdomsgrad(reflection { grad, _, _, _, _, _, _ -> grad }.prosent)
        }
    }
}
