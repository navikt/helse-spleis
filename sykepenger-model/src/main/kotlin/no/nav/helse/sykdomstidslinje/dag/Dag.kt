package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.tournament.Dagturnering
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal abstract class Dag internal constructor(
    internal val dagen: LocalDate,
    internal val kildehendelse: Kildehendelse
) :
    ConcreteSykdomstidslinje() {

    override fun førsteDag() = dagen
    override fun sisteDag() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate) = if (dato == dagen) this else null

    internal open fun beste(other: Dag, turnering: Dagturnering): Dag = turnering.beste(this, other)

    override fun length() = 1

    enum class Kildehendelse {
        Sykmelding,
        Søknad,
        Inntektsmelding
    }

    internal enum class Turneringsnøkkel {
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

    internal abstract fun turneringsnøkkel(): Turneringsnøkkel

    companion object {
        internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}

private val helgedager = listOf(SATURDAY, SUNDAY)
internal fun LocalDate.erHelg() = this.dayOfWeek in helgedager

internal fun LocalDate.harTilstøtende(other: LocalDate) =
    when (this.dayOfWeek) {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY -> this.plusDays(1) == other
        FRIDAY -> other in this.plusDays(1)..this.plusDays(3)
        SATURDAY -> other in this.plusDays(1)..this.plusDays(2)
        else -> false
    }
