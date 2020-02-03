package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelSendtSøknad(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val rapportertdato: LocalDateTime,
    private val perioder: List<Periode>,
    aktivitetslogger: Aktivitetslogger
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.SendtSøknad, aktivitetslogger) {

    private val fom: LocalDate
    private val tom: LocalDate

    init {
        if (perioder.isEmpty()) aktivitetslogger.severe("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: aktivitetslogger.severe("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: aktivitetslogger.severe("Søknad mangler tildato") }
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Sendt søknad")
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje(this) }
        .reduce(ConcreteSykdomstidslinje::plus)

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Søknad

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    override fun kanBehandles() = !valider().hasErrors()

    override fun valider(): Aktivitetslogger {
        perioder.forEach { it.valider(this, aktivitetslogger) }
        return aktivitetslogger
    }

    override fun accept(visitor: PersonVisitor) {
        visitor.visitSendtSøknadHendelse(this)
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver, person: Person) {
        arbeidsgiver.håndter(this, person)
    }

    internal fun continueIfNoErrors(vararg steps: ValidationStep, onError: () -> Unit) {
        aktivitetslogger.continueIfNoErrors(*steps) { onError() }
    }

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad): ConcreteSykdomstidslinje

        internal open fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {}

        internal fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger, beskjed: String) {
            if (fom < sendtSøknad.fom || tom > sendtSøknad.tom) aktivitetslogger.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ferie(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Ferie ligger utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.sykedager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {
                if (grad != 100) aktivitetslogger.error("grad i søknaden er ikke 100%%")
                if (faktiskGrad != 100.0) aktivitetslogger.error("faktisk grad i søknaden er ikke 100%%")
            }
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.studiedager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.error("Utdanning foreløpig ikke understøttet")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.error("Permisjon foreløpig ikke understøttet")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, sendtSøknad)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Arbeidsdag ligger utenfor sykdomsvindu")
        }
    }
}
