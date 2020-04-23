package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NyDag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.DagFactory
import no.nav.helse.sykdomstidslinje.dag.KunArbeidsgiverSykedag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.søknadDagturnering
import java.time.LocalDate
import java.util.*

class SøknadArbeidsgiver constructor(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val fom: LocalDate
    private val tom: LocalDate
    private var forrigeTom: LocalDate? = null
    private val nySykdomstidslinje: NySykdomstidslinje

    private companion object {
        private const val tidslinjegrense = 16L
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        fom = perioder.minBy { it.fom }?.fom ?: severe("Søknad mangler fradato")
        tom = perioder.maxBy { it.tom }?.tom ?: severe("Søknad mangler tildato")

        nySykdomstidslinje = perioder.map { it.nySykdomstidslinje(kilde) }.merge(noOverlap)
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje(fom) }
        .filter { it.førsteDag().isAfter(fom.minusDays(tidslinjegrense)) }
        .merge(søknadDagturnering)

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return sykdomstidslinje().subset(forrigeTom?.plusDays(1), tom)
            .also { trimLeft(tom) }
            ?: severe("Ugyldig subsetting av tidslinjen til søknad")
    }

    override fun nySykdomstidslinje() = NySykdomstidslinje()

    internal fun trimLeft(dato: LocalDate) { forrigeTom = dato }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(periode: Periode): Aktivitetslogg {
        perioder.forEach { it.valider(this) }
        return aktivitetslogg
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "SøknadArbeidsgiver"

    class Søknadsperiode(
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        private val gradFraSykmelding: Int,
        faktiskGrad: Int? = null
    ) {
        private val faktiskSykdomsgrad = faktiskGrad?.let { 100 - it }
        private val grad = (faktiskSykdomsgrad ?: gradFraSykmelding).toDouble()

        internal fun valider(søknad: SøknadArbeidsgiver, beskjed: String) {
            if (fom < søknad.fom || tom > søknad.tom) søknad.error(beskjed)
        }

        internal fun valider(søknad: SøknadArbeidsgiver) {
            if (grad > gradFraSykmelding) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
        }

        internal fun sykdomstidslinje(avskjæringsdato: LocalDate) =
            Sykdomstidslinje.kunArbeidsgiverSykedager(fom, tom, grad, SøknadDagFactory)

        internal fun nySykdomstidslinje(kilde: Hendelseskilde) = NySykdomstidslinje.kunArbeidsgiverSykedager(fom, tom, gradFraSykmelding, kilde)
    }

    internal object SøknadDagFactory : DagFactory {
        override fun kunArbeidsgiverSykedag(dato: LocalDate, grad: Double): KunArbeidsgiverSykedag = KunArbeidsgiverSykedag(dato, grad)
        override fun sykHelgedag(dato: LocalDate, grad: Double): SykHelgedag.Søknad = SykHelgedag.Søknad(dato, grad)
    }
}
