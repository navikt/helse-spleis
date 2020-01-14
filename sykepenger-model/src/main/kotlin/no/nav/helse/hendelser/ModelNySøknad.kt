package no.nav.helse.hendelser

import no.nav.helse.hendelser.ModelNySøknad.Sykeperiode
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
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
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.NySøknad) {

    private val sykeperioder: List<Sykeperiode>

    init {
        require(sykeperioder.isNotEmpty()) { "Ingen sykeperioder" }
        this.sykeperioder = sykeperioder.sortedBy { it.first }.map { Sykeperiode(it.first, it.second, it.third) }
        require(erUnder100()) { "Bare sykdomsgrad lik 100% støttes" }
        require(ingenOverlappende()) { "Sykeperioder overlapper" }
    }

    private inner class Sykeperiode(
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val sykdomsgrad: Int
    ) {
        internal fun kanBehandles() = sykdomsgrad == 100

        internal fun sykdomstidslinje() =
            ConcreteSykdomstidslinje.sykedager(fom, tom, this@ModelNySøknad)

        internal fun ingenOverlappende(other: Sykeperiode): Boolean {
            return maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)
        }
    }

    override fun kanBehandles() = erUnder100() && ingenOverlappende()

    private fun erUnder100() = sykeperioder.all { it.kanBehandles() }

    private fun ingenOverlappende(): Boolean {
        return sykeperioder.zipWithNext(Sykeperiode::ingenOverlappende).all { it }
    }

    override fun sykdomstidslinje() =
        sykeperioder.map(Sykeperiode::sykdomstidslinje).reduce(ConcreteSykdomstidslinje::plus)

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    override fun toJson() = "" // Should not be part of Model events
}
