package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.KonfliktskyDagturnering
import java.time.LocalDate
import java.util.*

class Sykmelding(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykeperioder: List<Sykeperiode>

    init {
        if (sykeperioder.isEmpty()) severe("Ingen sykeperioder")
        this.sykeperioder = sykeperioder.sortedBy { it.first }.map { Sykeperiode(it.first, it.second, it.third) }
        if (!ingenOverlappende()) severe("Sykeperioder overlapper")
    }

    override fun valider(): Aktivitetslogg {
        if (!hundreProsentSykmeldt()) aktivitetslogg.error("Sykmeldingen inneholder graderte sykeperioder")
        return aktivitetslogg
    }

    override fun melding(ignore: String) = "Ny Søknad"

    private fun hundreProsentSykmeldt() = sykeperioder.all { it.kanBehandles() }

    private fun ingenOverlappende() = sykeperioder.zipWithNext(Sykeperiode::ingenOverlappende).all { it }

    override fun sykdomstidslinje() =
        sykeperioder.map(Sykeperiode::sykdomstidslinje).reduce { acc, linje -> acc.plus(linje, SykmeldingDagFactory::implisittDag, KonfliktskyDagturnering)}

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    private inner class Sykeperiode(
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val sykdomsgrad: Int
    ) {
        internal fun kanBehandles() = sykdomsgrad == 100

        internal fun sykdomstidslinje() =
            ConcreteSykdomstidslinje.sykedager(fom, tom, SykmeldingDagFactory)

        internal fun ingenOverlappende(other: Sykeperiode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)
    }

    internal object SykmeldingDagFactory : DagFactory {
        override fun studiedag(dato: LocalDate): Studiedag { error("Studiedag ikke støttet") }
        override fun sykedag(dato: LocalDate): Sykedag = Sykedag.Sykmelding(dato)
        override fun ubestemtdag(dato: LocalDate): Ubestemtdag { error("Ubestemtdag ikke støttet") }
        override fun utenlandsdag(dato: LocalDate): Utenlandsdag { error("Utenlandsdag ikke støttet") }
    }
}
