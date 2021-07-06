package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.Toggles
import java.time.LocalDate
import kotlin.streams.toList

sealed class DateRange {
    abstract fun dates(): List<LocalDate>
    abstract fun toMap(): Map<String, LocalDate>
    abstract operator fun plus(other: LocalDate): DateRange
    abstract fun canBeJoinedBy(other: LocalDate): Boolean

    class Single(val dato: LocalDate) : DateRange() {
        override fun dates(): List<LocalDate> = listOf(dato)
        override fun toMap(): Map<String, LocalDate> = mapOf("dato" to dato)
        override fun plus(other: LocalDate) = Range(dato, other)
        override fun canBeJoinedBy(other: LocalDate): Boolean =
            dato.plusDays(1) == other
    }

    class Range(val fom: LocalDate, val tom: LocalDate) : DateRange() {
        override fun dates(): List<LocalDate> = fom.datesUntil(tom.plusDays(1)).toList()
        override fun toMap(): Map<String, LocalDate> = mapOf("fom" to fom, "tom" to tom)
        override fun plus(other: LocalDate) = Range(fom, other)
        override fun canBeJoinedBy(other: LocalDate): Boolean =
            tom.plusDays(1) == other
    }

    companion object {
        @JvmStatic
        @JsonCreator
        private fun create(
            @JsonProperty("dato") dato: LocalDate?,
            @JsonProperty("fom") fom: LocalDate?,
            @JsonProperty("tom") tom: LocalDate?
        ) = if (dato == null) {
            Range(fom!!, tom!!)
        } else {
            Single(dato)
        }
    }
}

data class DataHolder(
    var range: DateRange,
    val extraData: Map<String, Any?>
) {
    fun toMap() = extraData + range.toMap()

    fun canBeJoinedBy(date: LocalDate, data: Map<String, Any?>) =
        range.canBeJoinedBy(date) && extraData == data
}

class DateRanges {
    private val ranges = mutableListOf<DataHolder>()
    fun plus(date: LocalDate, data: Map<String, Any?>) = apply {
        val last = ranges.lastOrNull()
        if (last != null && last.canBeJoinedBy(date, data) && Toggles.DatoRangeJson.enabled) {
            last.range += date
        } else {
            ranges.add(DataHolder(DateRange.Single(date), data))
        }
    }

    fun toList() = ranges.map { it.toMap() }
}
