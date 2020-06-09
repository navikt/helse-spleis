package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag.Companion.noOverlap
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import java.time.LocalDate
import java.util.*

class SøknadArbeidsgiver constructor(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>
) : SykdomstidslinjeHendelse(meldingsreferanseId, Søknad::class) {

    private val fom: LocalDate
    private val tom: LocalDate
    private val sykdomstidslinje: Sykdomstidslinje

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        fom = perioder.minBy { it.fom }?.fom ?: severe("Søknad mangler fradato")
        tom = perioder.maxBy { it.tom }?.tom ?: severe("Søknad mangler tildato")

        sykdomstidslinje = perioder.map { it.sykdomstidslinje(kilde) }.merge(noOverlap)
    }

    override fun sykdomstidslinje() = sykdomstidslinje

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

        internal fun sykdomstidslinje(kilde: Hendelseskilde) = Sykdomstidslinje.sykedager(fom, tom, gradFraSykmelding, kilde)
    }
}
