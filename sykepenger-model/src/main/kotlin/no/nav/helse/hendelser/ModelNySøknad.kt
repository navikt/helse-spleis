package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.implisittDag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelNySøknad(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val rapportertdato: LocalDateTime,
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>,
    aktivitetslogger: Aktivitetslogger
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.NySøknad, aktivitetslogger) {

    private val sykeperioder: List<Sykeperiode>

    init {
        if (sykeperioder.isEmpty()) aktivitetslogger.severe("Ingen sykeperioder")
        this.sykeperioder = sykeperioder.sortedBy { it.first }.map { Sykeperiode(it.first, it.second, it.third) }
        if (!ingenOverlappende()) aktivitetslogger.severe("Sykeperioder overlapper")
    }

    override fun valider(): Aktivitetslogger {
        if (!hundreProsentSykmeldt()) aktivitetslogger.error("Støtter bare 100%% sykmeldt")
        return aktivitetslogger
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Ny søknad")
    }

    private fun hundreProsentSykmeldt() = sykeperioder.all { it.kanBehandles() }

    private fun ingenOverlappende() = sykeperioder.zipWithNext(Sykeperiode::ingenOverlappende).all { it }

    override fun sykdomstidslinje() =
        sykeperioder.map(Sykeperiode::sykdomstidslinje).reduce { acc, linje -> acc.plus(linje, ConcreteSykdomstidslinje.Companion::implisittDag)}

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    override fun accept(visitor: PersonVisitor) {
        visitor.visitNySøknadHendelse(this)
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver, person: Person) {
        arbeidsgiver.håndter(this, person)
    }

    private inner class Sykeperiode(
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val sykdomsgrad: Int
    ) {
        internal fun kanBehandles() = sykdomsgrad == 100

        internal fun sykdomstidslinje() =
            ConcreteSykdomstidslinje.sykedager(fom, tom, Dag.NøkkelHendelseType.Sykmelding)

        internal fun ingenOverlappende(other: Sykeperiode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)
    }
}
