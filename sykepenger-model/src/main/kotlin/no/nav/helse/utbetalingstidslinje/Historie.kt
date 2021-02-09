package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.Person
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Historie() {

    internal constructor(person: Person) : this() {
        person.append(spleisbøtte)
    }

    internal constructor(person: Person, utbetalingshistorikk: Utbetalingshistorikk) : this(person) {
        utbetalingshistorikk.append(infotrygdbøtte)
    }

    private val infotrygdbøtte = Historikkbøtte()
    private val spleisbøtte = Historikkbøtte(konverterUtbetalingstidslinje = true)
    private val sykdomstidslinjer get() = infotrygdbøtte.sykdomstidslinjer() + spleisbøtte.sykdomstidslinjer()

    internal fun periodetype(orgnr: String, periode: Periode): Periodetype {
        val skjæringstidspunkt = skjæringstidspunkt(orgnr, periode)
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

    internal fun beregnUtbetalingstidslinje(
        organisasjonsnummer: String,
        periode: Periode,
        inntektshistorikk: Inntektshistorikk,
        arbeidsgiverRegler: ArbeidsgiverRegler
    ): Utbetalingstidslinje {
        val builder = UtbetalingstidslinjeBuilder(
            inntektshistorikk = inntektshistorikk,
            forlengelseStrategy = { dagen -> erArbeidsgiverperiodenGjennomførtFør(organisasjonsnummer, dagen) },
            skjæringstidspunkter = skjæringstidspunkter(periode),
            arbeidsgiverRegler = arbeidsgiverRegler
        )
        return byggUtbetalingstidslinje(organisasjonsnummer, builder::result)
    }

    internal fun beregnUtbetalingstidslinjeVol2(
        organisasjonsnummer: String,
        periode: Periode,
        inntektshistorikk: InntektshistorikkVol2,
        arbeidsgiverRegler: ArbeidsgiverRegler
    ): Utbetalingstidslinje {
        val builder = UtbetalingstidslinjeBuilderVol2(
            sammenhengendePeriode = sammenhengendePeriode(periode),
            skjæringstidspunkter = skjæringstidspunkter(periode),
            inntektshistorikk = inntektshistorikk,
            forlengelseStrategy = { dagen -> erArbeidsgiverperiodenGjennomførtFør(organisasjonsnummer, dagen) },
            arbeidsgiverRegler = arbeidsgiverRegler
        )
        return byggUtbetalingstidslinje(organisasjonsnummer, builder::result)
    }

    private fun byggUtbetalingstidslinje(organisasjonsnummer: String, builder: (Sykdomstidslinje) -> Utbetalingstidslinje) =
        builder(sykdomstidslinje(organisasjonsnummer))
            .minus(infotrygdbøtte.utbetalingstidslinje(organisasjonsnummer))
            .let { it.gjøreKortere(minOf(it.førsteSykepengedag() ?: LocalDate.MAX, spleisbøtte.tidligsteDato(organisasjonsnummer))) }

    internal fun erForlengelse(orgnr: String, periode: Periode) =
        periodetype(orgnr, periode) != FØRSTEGANGSBEHANDLING

    internal fun forlengerInfotrygd(orgnr: String, periode: Periode) =
        periodetype(orgnr, periode) in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE)

    internal fun sammenhengendePeriode(periode: Periode) =
        skjæringstidspunkt(periode)?.let { periode.oppdaterFom(it) } ?: periode

    internal fun avgrensetPeriode(orgnr: String, periode: Periode) =
        Periode(maxOf(periode.start, skjæringstidspunkt(orgnr, periode)), periode.endInclusive)

    internal fun skjæringstidspunkt(periode: Periode) =
        Sykdomstidslinje.skjæringstidspunkt(periode.endInclusive, sykdomstidslinjer)

    private fun skjæringstidspunkt(orgnr: String, periode: Periode) =
        sykdomstidslinje(orgnr).skjæringstidspunkt(periode.endInclusive) ?: periode.start

    internal fun skjæringstidspunkter(periode: Periode) =
        skjæringstidspunkter(periode.endInclusive)

    private fun skjæringstidspunkter(kuttdato: LocalDate) =
        Sykdomstidslinje.skjæringstidspunkter(kuttdato, sykdomstidslinjer)

    internal fun utbetalingstidslinje(periode: Periode) =
        (infotrygdbøtte.utbetalingstidslinje() + spleisbøtte.utbetalingstidslinje()).kutt(periode.endInclusive)

    internal fun add(orgnummer: String, tidslinje: Utbetalingstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    internal fun add(orgnummer: String, tidslinje: Sykdomstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    private fun er16DagerEllerMerMellom(forrigeSykedag: LocalDate, førsteSykedag: LocalDate, sykdomstidslinje: Sykdomstidslinje) =
        sykdomstidslinje.subset(Periode(forrigeSykedag.plusDays(1), førsteSykedag.minusDays(1))).har16EllerFlereDagerGap()

    internal fun skjæringstidspunktFørGapMindreEnn16Dager(orgnummer: String, nyFørsteSykedag: LocalDate) : LocalDate? {
        val sykdomstidslinje = sykdomstidslinje(orgnummer)
        val forrigeSykedag = sykdomstidslinje.forrigeSykedagFør(nyFørsteSykedag) ?: return null
        if(er16DagerEllerMerMellom(forrigeSykedag, nyFørsteSykedag, sykdomstidslinje)) return null
        return sykdomstidslinje.skjæringstidspunkt(forrigeSykedag)
    }

    private fun erArbeidsgiverperiodenGjennomførtFør(organisasjonsnummer: String, dagen: LocalDate): Boolean {
        if (infotrygdbøtte.erUtbetaltDag(organisasjonsnummer, dagen)) return true
        val skjæringstidspunkt = skjæringstidspunkt(organisasjonsnummer, dagen til dagen)
        if (skjæringstidspunkt == dagen) return false
        if (infotrygdbøtte.erUtbetaltDag(organisasjonsnummer, skjæringstidspunkt)) return true
        return spleisbøtte.erUtbetaltDag(organisasjonsnummer, skjæringstidspunkt)
    }

    private fun sykdomstidslinje(orgnummer: String) =
        infotrygdbøtte.sykdomstidslinje(orgnummer).merge(spleisbøtte.sykdomstidslinje(orgnummer), replace)

    internal companion object {
        private const val ALLE_ARBEIDSGIVERE = "UKJENT"
        private fun Utbetalingsdag.erSykedag() =
            this is NavDag || this is NavHelgDag || this is ArbeidsgiverperiodeDag || this is AvvistDag
    }

    internal class Historikkbøtte(private val konverterUtbetalingstidslinje: Boolean = false) {
        private val utbetalingstidslinjer = mutableMapOf<String, Utbetalingstidslinje>()
        private val sykdomstidslinjer = mutableMapOf<String, Sykdomstidslinje>()

        internal fun tidligsteDato(orgnummer: String): LocalDate {
            val førsteUtbetalingsdag = utbetalingstidslinje(orgnummer).førsteSykepengedag()
            val førsteSykdomstidslinjedag = sykdomstidslinje(orgnummer).periode()?.start
            return listOfNotNull(førsteUtbetalingsdag, førsteSykdomstidslinjedag).minOrNull() ?: LocalDate.MIN
        }

        internal fun sykdomstidslinjer() = sykdomstidslinjer.values.toList()
        internal fun sykdomstidslinje(orgnummer: String) =
            sykdomstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Sykdomstidslinje() }.merge(
                sykdomstidslinjer.getOrElse(orgnummer) { Sykdomstidslinje() }, replace
            )

        internal fun utbetalingstidslinje() =
            utbetalingstidslinjer.values.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

        internal fun utbetalingstidslinje(orgnummer: String) =
            utbetalingstidslinjer.getOrElse(ALLE_ARBEIDSGIVERE) { Utbetalingstidslinje() } +
                utbetalingstidslinjer.getOrElse(orgnummer) { Utbetalingstidslinje() }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
            utbetalingstidslinjer.merge(orgnummer, tidslinje, Utbetalingstidslinje::plus)
            if (konverterUtbetalingstidslinje) {
                // for å ta høyde for forkastet historikk, men ved overlapp vinner den eksisterende historikken
                val eksisterende = sykdomstidslinjer[orgnummer]
                add(orgnummer, konverter(tidslinje))
                if (eksisterende != null) add(orgnummer, eksisterende)
            }
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
                utbetalingstidslinje
                    .mapNotNull {
                        when {
                            !it.dato.erHelg() && it.erSykedag() -> Dag.Sykedag(it.dato, it.økonomi.medGrad(), INGEN)
                            !it.dato.erHelg() && it is Fridag -> Dag.Feriedag(it.dato, INGEN)
                            it.dato.erHelg() && it.erSykedag() -> Dag.SykHelgedag(it.dato, it.økonomi.medGrad(), INGEN)
                            it is Arbeidsdag -> Dag.Arbeidsdag(it.dato, INGEN)
                            it is ForeldetDag -> Dag.ForeldetSykedag(it.dato, it.økonomi.medGrad(), INGEN)
                            else -> null
                        }?.let { sykedag -> it.dato to sykedag }
                    }
                    .toMap()
                    .let(::Sykdomstidslinje)

            private fun Økonomi.medGrad() = Økonomi.sykdomsgrad(reflection { grad, _, _, _, _, _, _, _, _ -> grad }.prosent)
        }
    }
}
