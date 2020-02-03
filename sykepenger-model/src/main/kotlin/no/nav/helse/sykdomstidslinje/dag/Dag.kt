package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.tournament.dagTurnering
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal abstract class Dag internal constructor(
    internal val dagen: LocalDate,
    internal val hendelse: SykdomstidslinjeHendelse
) :
    ConcreteSykdomstidslinje() {

    internal val erstatter: MutableList<Dag> = mutableListOf()

    internal abstract fun dagType(): JsonDagType

    internal fun erHelg() = this.dagen.erHelg()

    override fun førsteDag() = dagen
    override fun sisteDag() = dagen
    override fun hendelser(): Set<SykdomstidslinjeHendelse> = setOf(hendelse) + erstatter.flatMap { it.hendelser() }
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate) = if (dato == dagen) this else null

    internal fun erstatter(vararg dager: Dag): Dag {
        erstatter.addAll(dager
            .filterNot { it is ImplisittDag }
            .flatMap { it.erstatter + it })
        return this
    }

    fun dagerErstattet(): List<Dag> = erstatter

    internal open fun beste(other: Dag): Dag = dagTurnering.slåss(this, other)

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse

    enum class NøkkelHendelseType {
        Sykmelding,
        Søknad,
        Inntektsmelding
    }

    internal enum class Nøkkel {
        I,
        WD_A,
        WD_IM,
        S_SM,
        S_A,
        V_A,
        V_IM,
        Le_Areg,
        Le_A,
        SW,
        SRD_IM,
        SRD_A,
        EDU,
        OI_Int,
        OI_A,
        DA,
        Undecided,
    }

    internal abstract fun nøkkel(): Nøkkel

    companion object {
        internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}

private val helgedager = listOf<DayOfWeek>(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
internal fun LocalDate.erHelg() = this.dayOfWeek in helgedager
