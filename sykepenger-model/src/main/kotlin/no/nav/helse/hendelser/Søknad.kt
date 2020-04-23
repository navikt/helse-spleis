package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.søknadDagturnering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Søknad constructor(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val harAndreInntektskilder: Boolean,
    private val sendtTilNAV: LocalDateTime,
    private val permittert: Boolean
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykdomsperiode: Periode
    private var forrigeTom: LocalDate? = null

    private companion object {
        private const val tidslinjegrense = 16L
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        perioder.onEach { it.sjekkUgyldig(aktivitetslogg) }
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: severe("Søknad inneholder ikke sykdomsperioder")
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje(avskjæringsdato() ?: sykdomsperiode.start) }
        .filter { it.førsteDag().isAfter(sykdomsperiode.start.minusDays(tidslinjegrense)) }
        .merge(søknadDagturnering)

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return sykdomstidslinje().subset(forrigeTom?.plusDays(1), tom)
            .also { trimLeft(tom) }
            ?: severe("Ugydlig subsetting av tidslinjen til søknad")
    }

    override fun nySykdomstidslinje() = NySykdomstidslinje()

    internal fun trimLeft(dato: LocalDate) { forrigeTom = dato }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(periode: Periode): Aktivitetslogg {
        perioder.forEach { it.valider(this) }
        if (harAndreInntektskilder) error("Søknaden inneholder andre inntektskilder")
        if (permittert) warn("Søknaden inneholder permittering - se om det har innvirkning på saken før du utbetaler")
        return aktivitetslogg
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    private fun avskjæringsdato(): LocalDate? = sendtTilNAV.toLocalDate()?.minusMonths(3)?.withDayOfMonth(1)

    sealed class Søknadsperiode(fom: LocalDate, tom: LocalDate) {
        protected val periode = Periode(fom, tom)

        internal companion object {
            fun sykdomsperiode(liste: List<Søknadsperiode>) =
                søknadsperiode(liste.filterIsInstance<Sykdom>())

            fun søknadsperiode(liste: List<Søknadsperiode>) =
                liste
                    .map(Søknadsperiode::periode)
                    .takeIf(List<*>::isNotEmpty)
                    ?.let {
                        it.reduce { champion, challenger ->
                            Periode(
                                fom = minOf(champion.start, challenger.start),
                                tom = maxOf(champion.endInclusive, challenger.endInclusive))
                        }
                    }
        }

        internal abstract fun sykdomstidslinje(avskjæringsdato: LocalDate): Sykdomstidslinje

        internal open fun sjekkUgyldig(aktivitetslogg: Aktivitetslogg) {}
        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, beskjed: String) {
            if (periode.utenfor(søknad.sykdomsperiode)) søknad.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.ferie(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Feriedager utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val gradFraSykmelding: Int,
            faktiskSykdomsgrad: Int? = null
        ) : Søknadsperiode(fom, tom) {
            private val grad = (faktiskSykdomsgrad ?: gradFraSykmelding).toDouble()
            override fun sjekkUgyldig(aktivitetslogg: Aktivitetslogg) {
                if (grad > 100) aktivitetslogg.severe("Utregnet grad er over 100")
                if (grad < 0) aktivitetslogg.severe("Utregnet sykdomsgrad er et negativt tall")
            }

            override fun valider(søknad: Søknad) {
                if (grad > gradFraSykmelding) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate) =
                Sykdomstidslinje.sykedager(periode, avskjæringsdato, grad, SøknadDagFactory)
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.sykedager(periode, Double.NaN, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Papirsykmeldingsperiode")
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.studiedager(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Utdanningsperiode")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.permisjonsdager(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Permisjonsperiode")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.egenmeldingsdager(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) {
                if (periode.start < søknad.sykdomsperiode.start.minusDays(tidslinjegrense)) søknad.info("Søknaden inneholder egenmeldingsdager som er mer enn $tidslinjegrense dager før sykmeldingsperioden")
                if (periode.endInclusive > søknad.sykdomsperiode.endInclusive) søknad.warn("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden")
            }
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.ikkeSykedager(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate): Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.utenlandsdager(periode, SøknadDagFactory)

            override fun valider(søknad: Søknad) {
                søknad.error("Søknaden inneholder utelandsopphold")
            }
        }
    }

    internal object SøknadDagFactory : DagFactory {
        override fun arbeidsdag(dato: LocalDate): Arbeidsdag = Arbeidsdag.Søknad(dato)
        override fun egenmeldingsdag(dato: LocalDate): Egenmeldingsdag = Egenmeldingsdag.Søknad(dato)
        override fun feriedag(dato: LocalDate): Feriedag = Feriedag.Søknad(dato)
        override fun permisjonsdag(dato: LocalDate): Permisjonsdag = Permisjonsdag.Søknad(dato)
        override fun studiedag(dato: LocalDate): Studiedag = Studiedag(dato)
        override fun sykedag(dato: LocalDate, grad: Double): Sykedag.Søknad = Sykedag.Søknad(dato, grad)
        override fun kunArbeidsgiverSykedag(dato: LocalDate, grad: Double): KunArbeidsgiverSykedag = KunArbeidsgiverSykedag(dato, grad)
        override fun sykHelgedag(dato: LocalDate, grad: Double): SykHelgedag.Søknad = SykHelgedag.Søknad(dato, grad)
        override fun friskHelgedag(dato: LocalDate): FriskHelgedag = FriskHelgedag.Søknad(dato)
        override fun utenlandsdag(dato: LocalDate): Utenlandsdag = Utenlandsdag(dato)

    }
}
