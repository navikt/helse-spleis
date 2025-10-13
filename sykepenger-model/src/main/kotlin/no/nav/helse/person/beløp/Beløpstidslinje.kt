package no.nav.helse.person.beløp

import java.time.LocalDate
import java.util.SortedMap
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig

data class Beløpstidslinje(private val dager: SortedMap<LocalDate, Beløpsdag>) : Collection<Dag> by dager.values {
    private constructor(dager: Map<LocalDate, Beløpsdag>) : this(dager.toSortedMap())
    constructor(dager: List<Beløpsdag> = emptyList()) : this(dager.associateBy { it.dato }.toSortedMap().also {
        require(dager.size == it.size) { "Forsøkte å opprette en beløpstidslinje med duplikate datoer. Det blir for rart for meg." }
    })

    internal constructor(vararg dager: Beløpsdag) : this(dager.toList())

    private val periode = if (dager.isEmpty()) null else dager.firstKey() til dager.lastKey()
    internal operator fun get(dato: LocalDate): Dag = dager[dato] ?: UkjentDag
    override operator fun iterator(): Iterator<Dag> {
        if (periode == null) return emptyList<Dag>().iterator()
        return object : Iterator<Dag> {
            private val periodeIterator = periode.iterator()
            override fun hasNext() = periodeIterator.hasNext()
            override fun next() = this@Beløpstidslinje[periodeIterator.next()]
        }
    }
    operator fun plus(other: Beløpstidslinje) = merge(other) { champion, challenger ->
        if (challenger.kilde.tidsstempel >= champion.kilde.tidsstempel) challenger
        else champion
    }

    internal fun erstatt(other: Beløpstidslinje) = merge(other) { _, challenger -> challenger }

    private fun merge(other: Beløpstidslinje, strategy: (champion: Beløpsdag, challenger: Beløpsdag) -> Beløpsdag): Beløpstidslinje {
        val results = this.dager.toMutableMap()
        other.dager.forEach { (key, dag) ->
            results.merge(key, dag) { champion, challenger ->
                strategy(champion, challenger)
            }
        }
        return Beløpstidslinje(results)
    }

    internal fun medBeløp() = Beløpstidslinje(dager.filterValues { it.beløp != INGEN })

    internal operator fun minus(datoer: Iterable<LocalDate>) = Beløpstidslinje(this.dager.filterKeys { it !in datoer })
    internal operator fun minus(dato: LocalDate) = Beløpstidslinje(this.dager.filterKeys { it != dato })
    internal operator fun minus(other: Beløpstidslinje) = Beløpstidslinje(this.dager.filterValues { it.beløp != other.dager[it.dato]?.beløp })
    internal fun subset(periode: Periode): Beløpstidslinje {
        if (this.periode == null || !this.periode.overlapperMed(periode)) return Beløpstidslinje()
        return Beløpstidslinje(dager.subMap(periode.start, periode.endInclusive.nesteDag).toSortedMap())
    }

    internal fun tilOgMed(dato: LocalDate) = Beløpstidslinje(dager.headMap(dato.nesteDag).toSortedMap())
    internal fun fraOgMed(dato: LocalDate) = Beløpstidslinje(dager.tailMap(dato).toSortedMap())
    private val førsteBeløpsdag = periode?.start?.let { dager.getValue(it) }
    private fun snute(snute: LocalDate) = førsteBeløpsdag?.strekkTilbake(snute) ?: Beløpstidslinje()
    private val sisteBeløpsdag = periode?.endInclusive?.let { dager.getValue(it) }
    private fun hale(hale: LocalDate) = sisteBeløpsdag?.strekkFrem(hale) ?: Beløpstidslinje()

    // Fyller alle hull i beløpstidslinjen (les UkjentDag) med beløp & kilde fra forrige Beløpsdag
    internal fun fyll(): Beløpstidslinje {
        var forrigeBeløpsdag = førsteBeløpsdag ?: return Beløpstidslinje()
        val fylteDager = this.map { dag ->
            when (dag) {
                is Beløpsdag -> dag.also { forrigeBeløpsdag = it }
                is UkjentDag -> forrigeBeløpsdag.copy(dato = forrigeBeløpsdag.dato.nesteDag).also { forrigeBeløpsdag = it }
            }
        }
        return Beløpstidslinje(fylteDager)
    }

    internal fun fyll(periode: Periode) = fyll().strekk(periode).subset(periode)
    internal fun fyll(til: LocalDate) = fyll().strekkFrem(til).tilOgMed(til)

    // Det motsatte av fyll, begrenser den til så liten som mulig, men bevarer nok til at den senere kan fylles tilbake
    internal fun forkort() = Beløpstidslinje(dager.values.distinctBy { it.beløp to it.kilde })

    internal fun strekk(periode: Periode) = snute(periode.start) + this + hale(periode.endInclusive)
    private fun strekkFrem(til: LocalDate) = this + hale(til)
    internal fun førsteDagMedUliktBeløp(other: Beløpstidslinje): LocalDate? {
        val fom = setOfNotNull(periode?.start, other.periode?.start).minOrNull() ?: return null
        val tom = setOfNotNull(periode?.endInclusive, other.periode?.endInclusive).max()
        return (fom til tom).firstOrNull { this.dager[it]?.beløp != other.dager[it]?.beløp }
    }

    internal fun kunIngenRefusjon() =
        filterIsInstance<Beløpsdag>().let { beløpsdager ->
            if (beløpsdager.isEmpty()) false
            else beløpsdager.all { it.beløp == INGEN }
        }

    internal fun dto() = BeløpstidslinjeDto(
        perioder = dager
            .map { (_, dag) ->
                BeløpstidslinjeDto.BeløpstidslinjeperiodeDto(
                    fom = dag.dato,
                    tom = dag.dato,
                    dagligBeløp = dag.beløp.daglig,
                    kilde = BeløpstidslinjeDto.BeløpstidslinjedagKildeDto(
                        meldingsreferanseId = dag.kilde.meldingsreferanseId.dto(),
                        avsender = dag.kilde.avsender.dto(),
                        tidsstempel = dag.kilde.tidsstempel
                    )
                )
            }
            .fold(emptyList()) { result, dag ->
                when {
                    result.isEmpty() -> listOf(dag)
                    result.last().kanUtvidesAv(dag) -> result.dropLast(1) + result.last().copy(tom = dag.tom)
                    else -> result.plusElement(dag)
                }
            }
    )

    companion object {
        fun fra(periode: Periode, beløp: Inntekt, kilde: Kilde) = Beløpstidslinje(periode.map { Beløpsdag(it, beløp, kilde) })
        internal fun gjenopprett(dto: BeløpstidslinjeDto) = Beløpstidslinje(
            dager = dto.perioder
                .flatMap {
                    (it.fom til it.tom).map { dato ->
                        Beløpsdag(
                            dato = dato,
                            beløp = it.dagligBeløp.daglig,
                            kilde = Kilde(
                                meldingsreferanseId = MeldingsreferanseId.gjenopprett(it.kilde.meldingsreferanseId),
                                avsender = Avsender.gjenopprett(it.kilde.avsender),
                                tidsstempel = it.kilde.tidsstempel
                            )
                        )
                    }
                }
        )
    }
}

sealed interface Dag {
    val dato: LocalDate
    val beløp: Inntekt
    val kilde: Kilde
}

data class Beløpsdag(
    override val dato: LocalDate,
    override val beløp: Inntekt,
    override val kilde: Kilde
) : Dag {
    fun strekk(periode: Periode) = Beløpstidslinje(periode.map { copy(dato = it) })
    fun strekkTilbake(datoFør: LocalDate) = if (datoFør > dato) Beløpstidslinje() else strekk(datoFør til dato)
    fun strekkFrem(datoEtter: LocalDate) = if (datoEtter < dato) Beløpstidslinje() else strekk(dato til datoEtter)
}

data object UkjentDag : Dag {
    override val dato get() = error("En ukjent dag har ikke en dato")
    override val beløp get() = error("En ukjent dag har ikke et beløp")
    override val kilde get() = error("En ukjent dag har ikke et kilde")
}
