package no.nav.helse.hendelser

import no.nav.helse.FeatureToggle
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
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
    private val harAndreInntektskilder: Boolean,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : SykdomstidslinjeHendelse(meldingsreferanseId, aktivitetslogger, aktivitetslogg) {

    private val fom: LocalDate
    private val tom: LocalDate

    init {
        if (perioder.isEmpty()) aktivitetslogger.severeOld("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: aktivitetslogger.severeOld("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: aktivitetslogger.severeOld("Søknad mangler tildato") }
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Søknad")
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje() }
        .reduce { concreteSykdomstidslinje, other -> concreteSykdomstidslinje.plus(other, SøknadDagFactory::implisittDag, søknadDagturnering) }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(): Aktivitetslogger {
        perioder.forEach { it.valider(this, aktivitetslogger) }
        if ( harAndreInntektskilder ) aktivitetslogger.errorOld("Søknaden inneholder andre inntektskilder")
        return aktivitetslogger
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Sendt Søknad"

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

        internal open fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) {}

        internal fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger, beskjed: String) {
            if (fom < søknad.fom || tom > søknad.tom) aktivitetslogger.errorOld(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ferie(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) =
                valider(søknad, aktivitetslogger, "Søknaden inneholder Feriedager utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.sykedager(fom, tom, faktiskGrad, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) {
                if (grad != 100 && (!FeatureToggle.støtterGradertSykdom)) aktivitetslogger.errorOld("Søknaden inneholder gradert sykdomsperiode")
                if (faktiskGrad < grad && (!FeatureToggle.støtterGradertSykdom)) aktivitetslogger.errorOld("Søker oppgir gradert sykdomsperiode")
            }
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.studiedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.errorOld("Søknaden inneholder en Utdanningsperiode")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.errorOld("Søknaden inneholder en Permisjonsperiode")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, SøknadDagFactory)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, SøknadDagFactory)

            override fun valider(søknad: Søknad, aktivitetslogger: Aktivitetslogger) =
                valider(søknad, aktivitetslogger, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        }
    }

    internal object SøknadDagFactory : DagFactory {
        override fun arbeidsdag(dato: LocalDate): Arbeidsdag = Arbeidsdag.Søknad(dato)
        override fun egenmeldingsdag(dato: LocalDate): Egenmeldingsdag = Egenmeldingsdag.Søknad(dato)
        override fun feriedag(dato: LocalDate): Feriedag = Feriedag.Søknad(dato)
        override fun permisjonsdag(dato: LocalDate): Permisjonsdag = Permisjonsdag.Søknad(dato)
        override fun sykedag(dato: LocalDate, grad: Double): Sykedag = Sykedag.Søknad(dato, grad)
    }
}
