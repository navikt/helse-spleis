package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.SortedMap
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.contains
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodedagNav
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Ventetidsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.Økonomi

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

class Utbetalingstidslinje private constructor(private val utbetalingsdager: SortedMap<LocalDate, Utbetalingsdag>) : Collection<Utbetalingsdag> by utbetalingsdager.values {

    private val førsteDato get() = utbetalingsdager.firstKey()
    private val sisteDato get() = utbetalingsdager.lastKey()
    val totalbeløpPerson = this
        .filter { it is NavDag || it is ArbeidsgiverperiodedagNav }
        .mapNotNull { it.økonomi.personbeløp }
        .summer()
    val totalbeløpRefusjon = this
        .filter { it is NavDag || it is ArbeidsgiverperiodedagNav }
        .mapNotNull { it.økonomi.arbeidsgiverbeløp }
        .summer()
    val totalbeløp = totalbeløpPerson + totalbeløpRefusjon

    constructor(utbetalingsdager: Collection<Utbetalingsdag>) : this(utbetalingsdager.associateBy { it.dato }.toSortedMap()) {
        check(utbetalingsdager.distinctBy { it.dato }.size == utbetalingsdager.size) {
            "Utbetalingstidslinjen består av minst én dato som pekes på av mer enn én Utbetalingsdag"
        }
    }

    constructor() : this(mutableListOf())

    companion object {
        fun periode(tidslinjer: List<Utbetalingstidslinje>) = tidslinjer
            .filter { it.utbetalingsdager.isNotEmpty() }
            .map { it.periode() }
            .takeUnless { it.isEmpty() }
            ?.reduce(Periode::plus)

        fun betale(sykepengegrunnlagBegrenset6G: Inntekt, tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
            return beregnDagForDag(tidslinjer) {
                it.betal(sykepengegrunnlagBegrenset6G)
            }
        }

        fun totalSykdomsgrad(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
            return beregnDagForDag(tidslinjer, Økonomi::totalSykdomsgrad)
        }

        fun beregnDagForDag(tidslinjer: List<Utbetalingstidslinje>, operasjon: (List<Økonomi>) -> List<Økonomi>): List<Utbetalingstidslinje> {
            /**
             * beregn dag-for-dag, lagre resultatet tilbake i listen
             */
            val samletPeriode = periode(tidslinjer) ?: return emptyList()

            // lager kopi for ikke å modifisere på input-tidslinjene
            val result = tidslinjer.map { it.utbetalingsdager.toSortedMap() }
            samletPeriode.forEach { dato ->
                val uberegnet = tidslinjer.map { it[dato].økonomi }
                val beregnet = operasjon(uberegnet)
                // modifiserer kopien
                result.forEachIndexed { index, utbetalingsdager ->
                    val økonomi = beregnet[index]
                    val dagen = utbetalingsdager[dato]
                    if (dagen != null) utbetalingsdager[dato] = dagen.kopierMed(økonomi)
                }
            }
            // nye tidslinjer fra kopi
            return result.map { Utbetalingstidslinje(it) }
        }

        fun gjenopprett(dto: UtbetalingstidslinjeInnDto): Utbetalingstidslinje {
            return Utbetalingstidslinje(
                utbetalingsdager = dto.dager.map { Utbetalingsdag.gjenopprett(it) }.toMutableList()
            )
        }
    }

    fun avvis(avvistePerioder: List<Periode>, begrunnelser: Begrunnelse): Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.map { (dato, utbetalingsdag) ->
            val avvistDag = if (dato in avvistePerioder) utbetalingsdag.avvis(begrunnelser) else null
            avvistDag ?: utbetalingsdag
        })
    }

    operator fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        val nyeDager = (tidligsteDato til sisteDato).mapNotNull { dag ->
            val venstre = this.utbetalingsdager[dag]
            val høyre = other.utbetalingsdager[dag]
            when {
                venstre == null && høyre == null -> null
                venstre == null -> høyre!!
                høyre == null -> venstre
                else -> maxOf(venstre, høyre)
            }
        }
        return Utbetalingstidslinje(nyeDager)
    }

    fun harUtbetalingsdager() = sykepengeperiode() != null

    override fun iterator() = this.utbetalingsdager.values.iterator()

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato, other.førsteDato)

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato, other.sisteDato)

    fun periode() = Periode(førsteDato, sisteDato)

    fun sykepengeperiode(): Periode? {
        val første = utbetalingsdager.values.firstOrNull { it is NavDag }?.dato ?: return null
        val siste = utbetalingsdager.values.last { it is NavDag }.dato
        return første til siste
    }

    fun subset(periode: Periode): Utbetalingstidslinje {
        if (isEmpty()) return Utbetalingstidslinje()
        if (periode == periode()) return this
        val subMap = utbetalingsdager.subMap(periode.start, periode.endInclusive.nesteDag)
        return Utbetalingstidslinje(subMap.toSortedMap())
    }

    fun fraOgMed(fom: LocalDate) = Utbetalingstidslinje(utbetalingsdager.tailMap(fom).toSortedMap())
    fun fremTilOgMed(sisteDato: LocalDate) = Utbetalingstidslinje(utbetalingsdager.headMap(sisteDato.nesteDag).toSortedMap())

    operator fun get(dato: LocalDate) =
        utbetalingsdager[dato] ?: UkjentDag(dato, Økonomi.ikkeBetalt())

    override fun toString(): String {
        return utbetalingsdager.values.joinToString(separator = "") {
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
                    Ventetidsdag::class -> "V"
                    else -> "?"
                }
        }.trim()
    }

    fun negativEndringIBeløp(other: Utbetalingstidslinje): Boolean {
        val endringTotalbeløp = this.totalbeløp - other.totalbeløp
        val endringPersonbeløp = this.totalbeløpPerson - other.totalbeløpPerson
        if (endringPersonbeløp < Inntekt.INGEN) return true
        return endringTotalbeløp < Inntekt.INGEN
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

        fun addVentetidsdag(dato: LocalDate, økonomi: Økonomi) {
            add(Ventetidsdag(dato, økonomi))
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
        MeldingTilNavDagUtenforVentetid -> BegrunnelseDto.MeldingTilNavDagUtenforVentetid
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

    data object SykepengedagerOppbrukt : Begrunnelse()
    data object SykepengedagerOppbruktOver67 : Begrunnelse()
    data object MinimumInntekt : Begrunnelse()
    data object MinimumInntektOver67 : Begrunnelse()
    data object EgenmeldingUtenforArbeidsgiverperiode : Begrunnelse()
    data object MeldingTilNavDagUtenforVentetid : Begrunnelse()
    data object AndreYtelserForeldrepenger : Begrunnelse()
    data object AndreYtelserAap : Begrunnelse()
    data object AndreYtelserOmsorgspenger : Begrunnelse()
    data object AndreYtelserPleiepenger : Begrunnelse()
    data object AndreYtelserSvangerskapspenger : Begrunnelse()
    data object AndreYtelserOpplaringspenger : Begrunnelse()
    data object AndreYtelserDagpenger : Begrunnelse()
    data object MinimumSykdomsgrad : Begrunnelse() {
        override fun skalAvvises(utbetalingsdag: Utbetalingsdag) = utbetalingsdag is NavDag || utbetalingsdag is ArbeidsgiverperiodedagNav
    }

    data object EtterDødsdato : Begrunnelse()
    data object Over70 : Begrunnelse()
    data object ManglerOpptjening : Begrunnelse()
    data object ManglerMedlemskap : Begrunnelse()
    data object NyVilkårsprøvingNødvendig : Begrunnelse()

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
                BegrunnelseDto.MeldingTilNavDagUtenforVentetid -> MeldingTilNavDagUtenforVentetid
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
