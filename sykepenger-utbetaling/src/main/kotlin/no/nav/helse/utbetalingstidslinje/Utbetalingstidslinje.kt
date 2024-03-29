package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.contains
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Økonomi

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

class Utbetalingstidslinje(utbetalingsdager: Collection<Utbetalingsdag>) : Collection<Utbetalingsdag> by utbetalingsdager {
    private val utbetalingsdager = utbetalingsdager.toList()
    private val førsteDato get() = utbetalingsdager.first().dato
    private val sisteDato get() = utbetalingsdager.last().dato

    constructor() : this(mutableListOf())

    init {
        check(utbetalingsdager.distinctBy { it.dato }.size == utbetalingsdager.size) {
            "Utbetalingstidslinjen består av minst én dato som pekes på av mer enn én Utbetalingsdag"
        }
    }

    companion object {
        fun periode(tidslinjer: List<Utbetalingstidslinje>) = tidslinjer
            .filter { it.utbetalingsdager.isNotEmpty() }
            .map { it.periode() }
            .takeUnless { it.isEmpty() }
            ?.reduce(Periode::plus)

        fun avvis(
            tidslinjer: List<Utbetalingstidslinje>,
            avvistePerioder: List<Periode>,
            begrunnelser: List<Begrunnelse>
        ) = tidslinjer.map { it.avvis(avvistePerioder, begrunnelser) }

        fun avvisteDager(
            tidslinjer: List<Utbetalingstidslinje>,
            periode: Periode,
            begrunnelse: Begrunnelse
        ) = tidslinjer.flatMap { it.subset(periode) }.mapNotNull { it.erAvvistMed(begrunnelse) }

        fun betale(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
            return Utbetalingsdag.betale(tidslinjer)
        }

        fun gjenopprett(dto: UtbetalingstidslinjeInnDto): Utbetalingstidslinje {
            return Utbetalingstidslinje(
                utbetalingsdager = dto.dager.map { Utbetalingsdag.gjenopprett(it) }.toMutableList()
            )
        }
    }

    fun er6GBegrenset(): Boolean {
        return utbetalingsdager.any {
            it.økonomi.er6GBegrenset()
        }
    }

    fun accept(visitor: UtbetalingstidslinjeVisitor) {
        visitor.preVisitUtbetalingstidslinje(this, when(this.isEmpty()) {
            true -> null
            else -> this.periode()
        })
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingstidslinje()
    }

    private fun avvis(avvistePerioder: List<Periode>, begrunnelser: List<Begrunnelse>): Utbetalingstidslinje {
        if (begrunnelser.isEmpty()) return this
        return Utbetalingstidslinje(utbetalingsdager.map { utbetalingsdag ->
            val avvistDag = if (utbetalingsdag.dato in avvistePerioder) utbetalingsdag.avvis(begrunnelser) else null
            avvistDag ?: utbetalingsdag
        })
    }

    operator fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        return this.utvide(tidligsteDato, sisteDato).binde(other.utvide(tidligsteDato, sisteDato))
    }

    fun harUtbetalingsdager() = sykepengeperiode() != null

    override fun iterator() = this.utbetalingsdager.iterator()

    private fun binde(
        other: Utbetalingstidslinje
    ) = Utbetalingstidslinje(
        this.utbetalingsdager.zip(other.utbetalingsdager) { venstre, høyre -> maxOf(venstre, høyre) }.toMutableList()
    )

    private fun Builder.addUkjentDagHåndterHelg(dato: LocalDate) {
        if (dato.erHelg()) return addFridag(dato, Økonomi.ikkeBetalt())
        addUkjentDag(dato)
    }
    private fun utvide(tidligsteDato: LocalDate, sisteDato: LocalDate): Utbetalingstidslinje {
        val original = this
        return Builder().apply {
            tidligsteDato.datesUntil(original.førsteDato)
                .forEach { addUkjentDagHåndterHelg(it) }
            original.utbetalingsdager.forEach { add(it) }
            original.sisteDato.plusDays(1)
                .datesUntil(sisteDato.plusDays(1))
                .forEach { addUkjentDagHåndterHelg(it) }
        }.build()
    }

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato, other.førsteDato)

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato, other.sisteDato)

    fun periode() = Periode(førsteDato, sisteDato)

    fun sykepengeperiode(): Periode? {
        val første = utbetalingsdager.firstOrNull { it is NavDag }?.dato ?: return null
        val siste = utbetalingsdager.last { it is NavDag }.dato
        return første til siste
    }

    fun subset(periode: Periode): Utbetalingstidslinje {
        if (isEmpty()) return Utbetalingstidslinje()
        if (periode == periode()) return this
        return Utbetalingstidslinje(utbetalingsdager.filter { it.dato in periode }.toMutableList())
    }

    fun kutt(sisteDato: LocalDate) = subset(LocalDate.MIN til sisteDato)

    operator fun get(dato: LocalDate) =
        if (isEmpty() || dato !in periode()) UkjentDag(dato, Økonomi.ikkeBetalt())
        else utbetalingsdager.first { it.dato == dato }

    override fun toString(): String {
        return utbetalingsdager.joinToString(separator = "") {
            (if (it.dato.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
                when (it::class) {
                    NavDag::class -> "N"
                    NavHelgDag::class -> "H"
                    Arbeidsdag::class -> "A"
                    ArbeidsgiverperiodeDag::class -> "P"
                    Fridag::class -> "F"
                    AvvistDag::class -> "X"
                    UkjentDag::class -> "U"
                    ForeldetDag::class -> "O"
                    else -> "?"
                }
        }.trim()
    }

    class Builder {
        private val utbetalingsdager = mutableListOf<Utbetalingsdag>()

        fun build() = Utbetalingstidslinje(utbetalingsdager)

        fun addArbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
            add(ArbeidsgiverperiodeDag(dato, økonomi))
        }

        fun addArbeidsgiverperiodedagNav(dato: LocalDate, økonomi: Økonomi) {
            add(ArbeidsgiverperiodedagNav(dato, økonomi))
        }

        fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
            add(NavDag(dato, økonomi))
        }

        fun addArbeidsdag(dato: LocalDate, økonomi: Økonomi) {
            add(Arbeidsdag(dato, økonomi))
        }

        fun addFridag(dato: LocalDate, økonomi: Økonomi) {
            add(Fridag(dato, økonomi))
        }

        fun addHelg(dato: LocalDate, økonomi: Økonomi) {
            add(NavHelgDag(dato, økonomi))
        }

        fun addUkjentDag(dato: LocalDate) =
            add(UkjentDag(dato, Økonomi.ikkeBetalt()))

        fun addAvvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelser: List<Begrunnelse>) {
            add(AvvistDag(dato, økonomi, begrunnelser))
        }

        fun addForeldetDag(dato: LocalDate, økonomi: Økonomi) {
            add(ForeldetDag(dato, økonomi))
        }

        internal fun add(dag: Utbetalingsdag) {
            utbetalingsdager.add(dag)
        }
    }

    fun dto() = UtbetalingstidslinjeUtDto(
        dager = this.map { it.dto() }
    )

    private val Utbetalingsdag.avslag get() = this is AvvistDag || this is ForeldetDag
    fun behandlingsresultat(periode: Periode): String {
        val relevantUtbetalingstidslinje = this.subset(periode)
        val relevanteDager = relevantUtbetalingstidslinje.utbetalingsdager.filter { it is NavDag || it.avslag }
        return when {
            relevanteDager.isEmpty() -> "Avslag"
            relevanteDager.all { it.avslag } -> "Avslag"
            relevanteDager.all { it is NavDag } -> "Innvilget"
            relevanteDager.any { it is NavDag } && relevanteDager.any { it.avslag } -> "DelvisInnvilget"
            else -> throw IllegalStateException("Klarte ikke å utlede behandlingsresultat fra utbetalingstidslinjen $relevantUtbetalingstidslinje")
        }
    }
}

sealed class Begrunnelse {

    open fun skalAvvises(utbetalingsdag: Utbetalingsdag) = utbetalingsdag is AvvistDag || utbetalingsdag is NavDag || utbetalingsdag is ArbeidsgiverperiodedagNav

    fun dto() = when (this) {
        AndreYtelserAap -> BegrunnelseDto.AndreYtelserAap
        AndreYtelserDagpenger -> BegrunnelseDto.AndreYtelserDagpenger
        AndreYtelserForeldrepenger -> BegrunnelseDto.AndreYtelserForeldrepenger
        AndreYtelserOmsorgspenger -> BegrunnelseDto.AndreYtelserOmsorgspenger
        AndreYtelserOpplaringspenger -> BegrunnelseDto.AndreYtelserOpplaringspenger
        AndreYtelserPleiepenger -> BegrunnelseDto.AndreYtelserPleiepenger
        AndreYtelserSvangerskapspenger -> BegrunnelseDto.AndreYtelserSvangerskapspenger
        EgenmeldingUtenforArbeidsgiverperiode -> BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode
        EtterDødsdato -> BegrunnelseDto.EtterDødsdato
        ManglerMedlemskap -> BegrunnelseDto.ManglerMedlemskap
        ManglerOpptjening -> BegrunnelseDto.ManglerOpptjening
        MinimumInntekt -> BegrunnelseDto.MinimumInntekt
        MinimumInntektOver67 -> BegrunnelseDto.MinimumInntektOver67
        MinimumSykdomsgrad -> BegrunnelseDto.MinimumSykdomsgrad
        NyVilkårsprøvingNødvendig -> BegrunnelseDto.NyVilkårsprøvingNødvendig
        Over70 -> BegrunnelseDto.Over70
        SykepengedagerOppbrukt -> BegrunnelseDto.SykepengedagerOppbrukt
        SykepengedagerOppbruktOver67 -> BegrunnelseDto.SykepengedagerOppbruktOver67
    }

    object SykepengedagerOppbrukt : Begrunnelse()
    object SykepengedagerOppbruktOver67 : Begrunnelse()
    object MinimumInntekt : Begrunnelse()
    object MinimumInntektOver67 : Begrunnelse()
    object EgenmeldingUtenforArbeidsgiverperiode : Begrunnelse()
    object AndreYtelserForeldrepenger: Begrunnelse()
    object AndreYtelserAap: Begrunnelse()
    object AndreYtelserOmsorgspenger: Begrunnelse()
    object AndreYtelserPleiepenger: Begrunnelse()
    object AndreYtelserSvangerskapspenger: Begrunnelse()
    object AndreYtelserOpplaringspenger: Begrunnelse()
    object AndreYtelserDagpenger: Begrunnelse()
    object MinimumSykdomsgrad : Begrunnelse() {
        override fun skalAvvises(utbetalingsdag: Utbetalingsdag) = utbetalingsdag is NavDag || utbetalingsdag is ArbeidsgiverperiodedagNav
    }
    object EtterDødsdato : Begrunnelse()
    object Over70 : Begrunnelse()
    object ManglerOpptjening : Begrunnelse()
    object ManglerMedlemskap : Begrunnelse()
    object NyVilkårsprøvingNødvendig : Begrunnelse()

    companion object {
        internal fun gjenopprett(dto: BegrunnelseDto): Begrunnelse {
            return when (dto) {
                BegrunnelseDto.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
                BegrunnelseDto.AndreYtelserAap -> AndreYtelserAap
                BegrunnelseDto.AndreYtelserDagpenger -> AndreYtelserDagpenger
                BegrunnelseDto.AndreYtelserForeldrepenger -> AndreYtelserForeldrepenger
                BegrunnelseDto.AndreYtelserOmsorgspenger -> AndreYtelserOmsorgspenger
                BegrunnelseDto.AndreYtelserOpplaringspenger -> AndreYtelserOpplaringspenger
                BegrunnelseDto.AndreYtelserPleiepenger -> AndreYtelserPleiepenger
                BegrunnelseDto.AndreYtelserSvangerskapspenger -> AndreYtelserSvangerskapspenger
                BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
                BegrunnelseDto.EtterDødsdato -> EtterDødsdato
                BegrunnelseDto.ManglerMedlemskap -> ManglerMedlemskap
                BegrunnelseDto.ManglerOpptjening -> ManglerOpptjening
                BegrunnelseDto.MinimumInntekt -> MinimumInntekt
                BegrunnelseDto.MinimumInntektOver67 -> MinimumInntektOver67
                BegrunnelseDto.MinimumSykdomsgrad -> MinimumSykdomsgrad
                BegrunnelseDto.NyVilkårsprøvingNødvendig -> NyVilkårsprøvingNødvendig
                BegrunnelseDto.Over70 -> Over70
                BegrunnelseDto.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
            }
        }
    }

}
