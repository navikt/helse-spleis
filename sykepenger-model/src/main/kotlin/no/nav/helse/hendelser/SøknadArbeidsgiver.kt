package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.Companion.arbeidsdagerVinner
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SøknadArbeidsgiver(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val sykdomsperioder: List<Sykdom>,
    private val arbeidsperiode: List<Arbeid> = emptyList(),
    sykmeldingSkrevet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, sykmeldingSkrevet, Søknad::class) {

    private val fom: LocalDate
    private val tom: LocalDate
    private val sykdomstidslinje: Sykdomstidslinje

    init {
        if (sykdomsperioder.isEmpty()) severe("Søknad må inneholde perioder")
        fom = sykdomsperioder.minOfOrNull { it.fom } ?: severe("Søknad mangler fradato")
        tom = sykdomsperioder.maxOfOrNull { it.tom } ?: severe("Søknad mangler tildato")

        val tidslinjeSydom = sykdomsperioder.map { it.sykdomstidslinje(kilde) }
        val tidslinjeArbeid = arbeidsperiode.map { it.sykdomstidslinje(kilde) }
        sykdomstidslinje = (tidslinjeSydom + tidslinjeArbeid).merge(arbeidsdagerVinner)
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(periode: Periode): IAktivitetslogg {
        sykdomsperioder.forEach { it.valider(this) }
        arbeidsperiode.forEach { it.valider(this) }
        return this
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "SøknadArbeidsgiver"

    class Sykdom(
        internal val fom: LocalDate,
        internal val tom: LocalDate,
        private val sykmeldingsgrad: Prosentdel,
        arbeidshelse: Prosentdel? = null
    ) {
        private val søknadsgrad = arbeidshelse?.not()
        private val grad = søknadsgrad ?: sykmeldingsgrad

        internal fun valider(søknad: SøknadArbeidsgiver) {
            if (søknadsgrad != null && søknadsgrad > sykmeldingsgrad) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
        }

        internal fun sykdomstidslinje(kilde: Hendelseskilde) = Sykdomstidslinje.sykedager(fom, tom, grad, kilde)
    }

    class Arbeid(
        internal val fom: LocalDate,
        internal val tom: LocalDate,
    ) {
        internal fun valider(søknad: SøknadArbeidsgiver) {
            val arbeidGjenopptatt = fom
            if (arbeidGjenopptatt < søknad.fom || arbeidGjenopptatt > søknad.tom) søknad.error("Ugyldig input: arbeidGjenopptatt utenfor søknadsperioden")
        }

        internal fun sykdomstidslinje(kilde: Hendelseskilde) =
            Sykdomstidslinje.arbeidsdager(fom, tom, kilde)
    }
}
