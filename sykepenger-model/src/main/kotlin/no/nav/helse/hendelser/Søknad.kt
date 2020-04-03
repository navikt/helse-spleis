package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
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
    private val perioder: List<Periode>,
    private val harAndreInntektskilder: Boolean,
    private val sendtTilNAV: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val fom: LocalDate
    private val tom: LocalDate
    private var forrigeTom: LocalDate? = null

    private companion object {
        private const val tidslinjegrense = 16L
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: severe("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: severe("Søknad mangler tildato") }
    }

    override fun sykdomstidslinje() = perioder
        .map{ it.sykdomstidslinje(avskjæringsdato() ?: fom) }
        .filter { it.førsteDag().isAfter(fom.minusDays(tidslinjegrense)) }
        .merge(søknadDagturnering)

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return sykdomstidslinje().subset(forrigeTom?.plusDays(1), tom)
            .also { trimLeft(tom) }
            ?: severe("Ugydlig subsetting av tidslinjen til søknad")
    }

    internal fun trimLeft(dato: LocalDate) { forrigeTom = dato }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(): Aktivitetslogg {
        perioder.forEach { it.valider(this) }
        if ( harAndreInntektskilder ) error("Søknaden inneholder andre inntektskilder")
        return aktivitetslogg
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    private fun avskjæringsdato(): LocalDate? = sendtTilNAV?.toLocalDate()?.minusMonths(3)?.withDayOfMonth(1)

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(avskjæringsdato: LocalDate): Sykdomstidslinje

        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, beskjed: String) {
            if (fom < søknad.fom || tom > søknad.tom) søknad.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.ferie(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Feriedager utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val gradFraSykmelding: Int,
            faktiskSykdomsgrad: Int? = null
        ) : Periode(fom, tom) {
            private val grad = (faktiskSykdomsgrad ?: gradFraSykmelding).toDouble()

            override fun valider(søknad: Søknad) {
                if (grad < 0) søknad.error("Utregnet sykdomsgrad er < 0")
                if (grad > gradFraSykmelding) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate) =
                Sykdomstidslinje.sykedager(fom, tom, avskjæringsdato, grad, SøknadDagFactory)
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.studiedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Utdanningsperiode")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.permisjonsdager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Permisjonsperiode")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.egenmeldingsdager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad) {
                if (fom < søknad.fom.minusDays(tidslinjegrense)) søknad.warn("Søknaden inneholder egenmeldingsdager som er mer enn $tidslinjegrense dager før sykmeldingsperioden")
                if (tom > søknad.tom) søknad.warn("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden")
            }
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.ikkeSykedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate): Periode(fom, tom) {
            override fun sykdomstidslinje(ignore: LocalDate) =
                Sykdomstidslinje.utenlandsdager(fom, tom, SøknadDagFactory)

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
