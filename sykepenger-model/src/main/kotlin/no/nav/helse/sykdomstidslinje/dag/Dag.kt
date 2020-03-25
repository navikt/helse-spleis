package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.person.SykdomstidslinjeVisitor
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal abstract class Dag internal constructor(internal val dagen: LocalDate) {

    internal abstract fun accept(visitor: SykdomstidslinjeVisitor)

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
