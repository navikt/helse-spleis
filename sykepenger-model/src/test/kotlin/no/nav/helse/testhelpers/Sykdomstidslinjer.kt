package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.nS
    get() = NySykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nU
    get() = NySykdomstidslinje.arbeidsgiverdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nK
    get() = NySykdomstidslinje.foreldetSykedag(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nA
    get() = NySykdomstidslinje.arbeidsdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.n_
    get() = NySykdomstidslinje(
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nF
    get() = NySykdomstidslinje.feriedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nEDU
    get() = NySykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap({ it }, { NyStudiedag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nH
    get() = NySykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, NyDag>({ it }, { NySykHelgedag(it, 100.0, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nP
    get() = NySykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, NyDag>({ it }, { NyPermisjonsdag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nUT
    get() = NySykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, NyDag>({ it }, { NyUtenlandsdag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private object TestHendelse : SykdomstidslinjeHendelse(UUID.randomUUID()) {
    private const val UNG_PERSON_FNR_2018 = "12020052345"
    private const val AKTØRID = "42"
    private const val ORGNUMMER = "987654321"

    override fun nySykdomstidslinje() = NySykdomstidslinje()
    override fun nySykdomstidslinje(tom: LocalDate) = NySykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
