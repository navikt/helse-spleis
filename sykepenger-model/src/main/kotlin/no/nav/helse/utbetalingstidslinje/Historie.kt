package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate

internal class Historie(infotrygdhistorikk: Infotrygdhistorikk) {
    private val infotrygdbøtte = Historikkbøtte()
    private val spleisbøtte = Historikkbøtte(konverterUtbetalingstidslinje = true)
    private val sykdomstidslinjer get() = infotrygdbøtte.sykdomstidslinjer() + spleisbøtte.sykdomstidslinjer()

    init {
        infotrygdhistorikk.append(infotrygdbøtte)
    }

    internal fun periodetype(orgnr: String, periode: Periode): Periodetype {
        return beregnPeriodetype(orgnr, periode).somPeriodetype()
    }

    private fun beregnPeriodetype(orgnr: String, periode: Periode): InternPeriodetype {
        val skjæringstidspunkt = skjæringstidspunkt(orgnr, periode)
        return when {
            skjæringstidspunkt in periode -> InternPeriodetype.PERIODE_MED_SKJÆRINGSTIDSPUNKT
            infotrygdbøtte.harBetalt(orgnr, skjæringstidspunkt) -> {
                val sammenhengendePeriode = Periode(skjæringstidspunkt, periode.start.minusDays(1))
                when {
                    spleisbøtte.harBetalt(orgnr, sammenhengendePeriode) -> InternPeriodetype.FORLENGELSE_MED_OPPHAV_I_INFOTRYGD
                    else -> InternPeriodetype.FØRSTE_PERIODE_SOM_FORLENGER_INFOTRYGD
                }
            }
            !spleisbøtte.harBetalt(orgnr, skjæringstidspunkt) -> InternPeriodetype.PERIODE_MED_FØRSTE_UTBETALING
            else -> InternPeriodetype.FORLENGELSE_MED_OPPHAV_I_SPLEIS
        }
    }

    internal fun beregnUtbetalingstidslinje(
        organisasjonsnummer: String,
        periode: Periode,
        inntektshistorikk: Inntektshistorikk,
        arbeidsgiverRegler: ArbeidsgiverRegler,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Utbetalingstidslinje {
        val builder = UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = skjæringstidspunkter(periode),
            inntektshistorikk = inntektshistorikk,
            forlengelseStrategy = { dagen -> erArbeidsgiverperiodenGjennomførtFør(organisasjonsnummer, dagen) },
            arbeidsgiverRegler = arbeidsgiverRegler
        )
        val utbetalingstidlinje = builder.result(sykdomstidslinje(organisasjonsnummer).fremTilOgMed(periode.endInclusive))
        return infotrygdhistorikk.fjernHistorikk(utbetalingstidlinje, organisasjonsnummer, spleisbøtte.tidligsteDato(organisasjonsnummer))
    }

    internal fun erForlengelse(orgnr: String, periode: Periode) =
        beregnPeriodetype(orgnr, periode).erForlengelse()

    internal fun forlengerInfotrygd(orgnr: String, periode: Periode) =
        beregnPeriodetype(orgnr, periode).opphavInfotrygd()

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

    internal fun add(orgnummer: String, tidslinje: Utbetalingstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    internal fun add(orgnummer: String, tidslinje: Sykdomstidslinje) {
        spleisbøtte.add(orgnummer, tidslinje)
    }

    private fun erArbeidsgiverperiodenGjennomførtFør(organisasjonsnummer: String, dagen: LocalDate): Boolean {
        val skjæringstidspunkt = skjæringstidspunkt(organisasjonsnummer, dagen til dagen)
        return infotrygdbøtte.harBetalt(organisasjonsnummer, skjæringstidspunkt)
    }

    private fun sykdomstidslinje(orgnummer: String) =
        infotrygdbøtte.sykdomstidslinje(orgnummer).merge(spleisbøtte.sykdomstidslinje(orgnummer), replace)

    internal companion object {
        private const val ALLE_ARBEIDSGIVERE = "UKJENT"
    }

    internal class Historikkbøtte(private val konverterUtbetalingstidslinje: Boolean = false) {
        private val utbetalingstidslinjer = mutableMapOf<String, Utbetalingstidslinje>()
        private val sykdomstidslinjer = mutableMapOf<String, Sykdomstidslinje>()

        internal fun tidligsteDato(orgnummer: String): LocalDate {
            val førsteUtbetalingsdag = utbetalingstidslinje(orgnummer).sykepengeperiode()?.start
            val førsteSykdomstidslinjedag = sykdomstidslinje(orgnummer).periode()?.start
            return listOfNotNull(førsteUtbetalingsdag, førsteSykdomstidslinjedag).minOrNull()
                ?: throw IllegalArgumentException("Finner ingen første dag! Både sykdomstidslinjen og historiske utbetalinger er helt tom")
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
                add(orgnummer, Utbetalingstidslinje.konverter(tidslinje))
                if (eksisterende != null) add(orgnummer, eksisterende)
            }
        }

        internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Sykdomstidslinje) {
            sykdomstidslinjer.merge(orgnummer, tidslinje) { venstre, høyre -> venstre.merge(høyre, replace) }
        }

        internal fun harBetalt(orgnr: String, periode: Periode) =
            utbetalingstidslinje(orgnr).harBetalt(periode)

        internal fun harBetalt(orgnr: String, dato: LocalDate) =
            utbetalingstidslinje(orgnr).harBetalt(dato)
    }

    private enum class InternPeriodetype(private val periodetype: Periodetype) {
        PERIODE_MED_SKJÆRINGSTIDSPUNKT(FØRSTEGANGSBEHANDLING),
        PERIODE_MED_FØRSTE_UTBETALING(FØRSTEGANGSBEHANDLING),
        FORLENGELSE_MED_OPPHAV_I_SPLEIS(FORLENGELSE),
        FØRSTE_PERIODE_SOM_FORLENGER_INFOTRYGD(OVERGANG_FRA_IT),
        FORLENGELSE_MED_OPPHAV_I_INFOTRYGD(INFOTRYGDFORLENGELSE);

        fun opphavInfotrygd() = this in listOf(FORLENGELSE_MED_OPPHAV_I_INFOTRYGD, FØRSTE_PERIODE_SOM_FORLENGER_INFOTRYGD)
        fun erForlengelse() = this != PERIODE_MED_SKJÆRINGSTIDSPUNKT
        fun somPeriodetype() = periodetype
    }
}
