package no.nav.helse.hendelser

import no.nav.helse.person.Problemer
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ModelSendtSøknad(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val rapportertdato: LocalDateTime,
    private val perioder: List<Periode>,
    private val problemer: Problemer
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.SendtSøknad) {

    private val fom: LocalDate
    private val tom: LocalDate

    init {
        if (perioder.isEmpty()) problemer.severe("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: problemer.severe("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: problemer.severe("Søknad mangler tildato") }
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje(this) }
        .reduce(ConcreteSykdomstidslinje::plus)

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Søknad

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    override fun toJson() = "" // Should not be part of Model events

    override fun kanBehandles() = !valider().hasErrors()

    internal fun valider(): Problemer {
        perioder.forEach { it.valider(problemer) }
        return problemer
    }


    sealed class Periode {
        internal abstract fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad): ConcreteSykdomstidslinje
        internal open fun valider(problemer: Problemer) {}

        class Ferie(private val fom: LocalDate, private val tom: LocalDate) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ferie(fom, tom, sendtSøknad)
        }

        class Sykdom(
            internal val fom: LocalDate,
            internal val tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.sykedager(fom, tom, sendtSøknad)

            override fun valider(problemer: Problemer) {
                if (grad != 100) problemer.error("grad i søknaden er ikke 100%%")
                if (faktiskGrad != 100.0) problemer.error("faktisk grad i søknaden er ikke 100%%")
            }
        }

        class Utdanning(private val fom: LocalDate, private val tom: LocalDate?) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.utenlandsdager(fom, tom ?: sendtSøknad.tom, sendtSøknad)
        }

        class Permisjon(internal val fom: LocalDate, internal val tom: LocalDate) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, sendtSøknad)
        }

        class Egenmelding(internal val fom: LocalDate, internal val tom: LocalDate) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, sendtSøknad)
        }

        inner class Arbeid(internal val fom: LocalDate, internal val tom: LocalDate) : Periode() {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, sendtSøknad)
        }
    }
}
