package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.søknadDagturnering
import java.time.LocalDate
import java.util.*

class Søknad constructor(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Periode>,
    private val harAndreInntektskilder: Boolean
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val fom: LocalDate
    private val tom: LocalDate

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: severe("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: severe("Søknad mangler tildato") }
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje() }
        .reduce { concreteSykdomstidslinje, other -> concreteSykdomstidslinje.plus(other, SøknadDagFactory::implisittDag, søknadDagturnering) }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(): Aktivitetslogg {
        perioder.forEach { it.valider(this, aktivitetslogg) }
        if ( harAndreInntektskilder ) error("Søknaden inneholder andre inntektskilder")
        return aktivitetslogg
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Sendt Søknad"

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

        internal open fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) {}

        internal fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg, beskjed: String) {
            if (fom < søknad.fom || tom > søknad.tom) aktivitetslogg.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ferie(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) =
                valider(søknad, aktivitetslogg, "Søknaden inneholder Feriedager utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.sykedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) {
                if (grad != 100) aktivitetslogg.error("Søknaden inneholder gradert sykdomsperiode")
                if (faktiskGrad != 100.0) aktivitetslogg.error("Søker oppgir gradert sykdomsperiode")
            }
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.studiedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) =
                aktivitetslogg.error("Søknaden inneholder en Utdanningsperiode")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) =
                aktivitetslogg.error("Søknaden inneholder en Permisjonsperiode")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, SøknadDagFactory)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogg: Aktivitetslogg) =
                valider(søknad, aktivitetslogg, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        }
    }

    internal object SøknadDagFactory : DagFactory {
        override fun arbeidsdag(dato: LocalDate): Arbeidsdag = Arbeidsdag.Søknad(dato)
        override fun egenmeldingsdag(dato: LocalDate): Egenmeldingsdag = Egenmeldingsdag.Søknad(dato)
        override fun feriedag(dato: LocalDate): Feriedag = Feriedag.Søknad(dato)
        override fun permisjonsdag(dato: LocalDate): Permisjonsdag = Permisjonsdag.Søknad(dato)
        override fun sykedag(dato: LocalDate): Sykedag = Sykedag.Søknad(dato)
    }
}
