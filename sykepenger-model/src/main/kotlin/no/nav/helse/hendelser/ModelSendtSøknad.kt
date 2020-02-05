package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelSendtSøknad constructor(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val sendtNav: LocalDateTime,
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
        .map { it.sykdomstidslinje() }
        .reduce { concreteSykdomstidslinje, other -> concreteSykdomstidslinje.plus(other, ConcreteSykdomstidslinje.Companion::implisittDag) }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Søknad

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = sendtNav

    override fun aktørId() = aktørId

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

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

        internal open fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {}

        internal fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger, beskjed: String) {
            if (fom < sendtSøknad.fom || tom > sendtSøknad.tom) aktivitetslogger.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ferie(fom, tom, Dag.NøkkelHendelseType.Søknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Ferie ligger utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.sykedager(fom, tom, Dag.NøkkelHendelseType.Søknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {
                if (grad != 100) aktivitetslogger.error("grad i søknaden er ikke 100%%")
                if (faktiskGrad != 100.0) aktivitetslogger.error("faktisk grad i søknaden er ikke 100%%")
            }
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.studiedager(fom, tom, Dag.NøkkelHendelseType.Søknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.need("Utdanning foreløpig ikke støttet")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, Dag.NøkkelHendelseType.Søknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.need("Permisjon foreløpig ikke støttet")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, Dag.NøkkelHendelseType.Søknad)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje() =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, Dag.NøkkelHendelseType.Søknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Arbeidsdag ligger utenfor sykdomsvindu")
        }
    }
}
