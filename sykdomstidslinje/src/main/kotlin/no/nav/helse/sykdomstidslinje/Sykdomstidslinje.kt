package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sykdomstidslinje.dag.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

abstract class Sykdomstidslinje {
    abstract fun startdato(): LocalDate
    abstract fun sluttdato(): LocalDate
    abstract fun antallSykedagerHvorViTellerMedHelg(): Int
    abstract fun antallSykedagerHvorViIkkeTellerMedHelg(): Int
    abstract fun flatten(): List<Dag>
    abstract fun length(): Int
    abstract fun accept(visitor: SykdomstidslinjeVisitor)

    internal abstract fun sisteHendelse(): SykdomstidslinjeHendelse
    internal abstract fun dag(dato: LocalDate, hendelse: SykdomstidslinjeHendelse): Dag

    fun toJson(): String = objectMapper.writeValueAsString(jsonRepresentation())

    internal fun jsonRepresentation(): JsonTidslinje {
        val dager = flatten().map { it.toJsonDag() }
        val hendelser = flatten().flatMap { it.toJsonHendelse() }.distinctBy { it.hendelseId() }.map { it.toJson() }
        return JsonTidslinje(dager = dager, hendelser = hendelser)
    }

    operator fun plus(other: Sykdomstidslinje): Sykdomstidslinje {
        if (this.length() == 0) return other
        if (other.length() == 0) return this

        if (this.startdato().isAfter(other.startdato())) return other + this

        val datesUntil = this.førsteStartdato(other).datesUntil(this.sisteSluttdato(other).plusDays(1)).toList()
        val intervalEtterKonflikter =
            datesUntil
                .map { this.dag(it, this.sisteHendelse()).beste(other.dag(it, other.sisteHendelse())) }

        return CompositeSykdomstidslinje(intervalEtterKonflikter)
    }

    fun antallDagerMellom(other: Sykdomstidslinje) =
        when {
            this.length() == 0 || other.length() == 0 -> throw IllegalStateException("Kan ikke regne antall dager mellom tidslinjer, når én eller begge er tomme.")
            inneholder(other) -> -min(this.length(), other.length())
            overlapperMed(other) -> max(this.avstandMedOverlapp(other), other.avstandMedOverlapp(this))
            else -> min(this.avstand(other), other.avstand(this))
        }

    fun overlapperMed(other: Sykdomstidslinje) =
        when {
            this.length() == 0 || other.length() == 0 -> false
            else -> this.harGrenseInnenfor(other) || other.harGrenseInnenfor(this)
        }

    fun erUtenforOmfang(): Boolean {
        return flatten().any { it::class in arrayOf(Permisjonsdag::class, Ubestemtdag::class) }
                || syketilfeller().size != 1
    }

    private fun førsteStartdato(other: Sykdomstidslinje) =
        if (this.startdato().isBefore(other.startdato())) this.startdato() else other.startdato()

    private fun sisteSluttdato(other: Sykdomstidslinje) =
        if (this.sluttdato().isAfter(other.sluttdato())) this.sluttdato() else other.sluttdato()

    private fun avstand(other: Sykdomstidslinje) =
        this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() - 1

    private fun avstandMedOverlapp(other: Sykdomstidslinje) =
        -(this.sluttdato().until(other.startdato(), ChronoUnit.DAYS).absoluteValue.toInt() + 1)

    private fun erDelAv(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun inneholder(other: Sykdomstidslinje) =
        this.harBeggeGrenseneInnenfor(other) || other.harBeggeGrenseneInnenfor(this)

    private fun harBeggeGrenseneInnenfor(other: Sykdomstidslinje) =
        this.startdato() in other.startdato()..other.sluttdato() && this.sluttdato() in other.startdato()..other.sluttdato()

    private fun harGrenseInnenfor(other: Sykdomstidslinje) =
        this.startdato() in (other.startdato()..other.sluttdato())

    internal fun trim(): Sykdomstidslinje {
        val days = flatten()
            .dropWhile { it.antallSykedagerHvorViTellerMedHelg() < 1 }
            .dropLastWhile { it.antallSykedagerHvorViTellerMedHelg() < 1 }
        return CompositeSykdomstidslinje(days)
    }

    fun syketilfeller(): List<Syketilfelle> {
        check(flatten().none { it::class == Permisjonsdag::class }) { "Syketilfeller kan ikke beregnes på sykdomstidslinjer med permisjonsdager." }
        check(flatten().none { it::class == Ubestemtdag::class }) { "Syketilfeller kan ikke beregnes på sykdomstidslinjer med ubestemte dager." }
        val visitor = SyketilfelleSplitter()
        this.accept(visitor)

        return visitor.results()
    }

    companion object {
        fun sykedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) Sykedag(
                gjelder,
                hendelse
            ) else SykHelgedag(
                gjelder,
                hendelse
            )

        fun egenmeldingsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            Egenmeldingsdag(gjelder, hendelse)

        fun ferie(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            Feriedag(
                gjelder,
                hendelse
            )

        fun ikkeSykedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) Arbeidsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun utenlandsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) Utenlandsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun sykedager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                sykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun egenmeldingsdager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                egenmeldingsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ferie(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ferie(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun ikkeSykedager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                ikkeSykedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun utenlandsdager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                utenlandsdag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun studiedag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) Studiedag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun studiedager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(fra.datesUntil(til.plusDays(1)).map {
                studiedag(
                    it,
                    hendelse
                )
            }.toList())
        }

        fun permisjonsdag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) Permisjonsdag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )

        fun permisjonsdager(fra: LocalDate, til: LocalDate, hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
            require(!fra.isAfter(til)) { "fra må være før eller lik til" }
            return CompositeSykdomstidslinje(
                fra.datesUntil(til.plusDays(1))
                    .map { permisjonsdag(it, hendelse) }
                    .toList())
        }

        fun fromJson(
            json: String,
            deserializer: SykdomstidslinjeHendelse.Deserializer
        ): Sykdomstidslinje {
            val jsonTidslinje = objectMapper.readTree(json)

            val map = gruppererHendelserPrHendelsesId(jsonTidslinje["hendelser"], deserializer)
            val dager = jsonTidslinje["dager"].map { jsonDagFromJson(it) }

            return CompositeSykdomstidslinje.fromJsonRepresentation(dager, map)
        }

        private fun jsonDagFromJson(it: JsonNode): JsonDag {
            return JsonDag(
                JsonDagType.valueOf(it["type"].asText()),
                LocalDate.parse(it["dato"].asText()),
                it["hendelseId"].asText(),
                it["erstatter"].map { jsonDagFromJson(it) })
        }


        private fun gruppererHendelserPrHendelsesId(
            json: JsonNode,
            deserializer: SykdomstidslinjeHendelse.Deserializer
        ): Map<String, SykdomstidslinjeHendelse> {
            return json.map { deserializer.deserialize(it) }
                .groupBy(keySelector = { it.hendelseId() })
                .mapValues { (_, v) -> v.first() }
        }


        private fun erArbeidsdag(dato: LocalDate) =
            dato.dayOfWeek != DayOfWeek.SATURDAY && dato.dayOfWeek != DayOfWeek.SUNDAY

        internal fun implisittDag(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) =
            if (erArbeidsdag(gjelder)) ImplisittDag(
                gjelder,
                hendelse
            ) else ImplisittDag(
                gjelder,
                hendelse
            )
    }
}
