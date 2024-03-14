package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dto.UtbetalingsdagDto
import no.nav.helse.dto.ØkonomiDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.utbetalingslinjer.Beløpkilde
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.Økonomi.Companion.erUnderGrensen
import no.nav.helse.økonomi.ØkonomiVisitor

sealed class Utbetalingsdag(
    val dato: LocalDate,
    val økonomi: Økonomi
) : Comparable<Utbetalingsdag> {

    internal abstract val prioritet: Int
    fun beløpkilde(): Beløpkilde = BeløpkildeAdapter(økonomi)
    override fun compareTo(other: Utbetalingsdag): Int {
        return this.prioritet.compareTo(other.prioritet)
    }

    override fun toString() = "${this.javaClass.simpleName} ($dato) ${økonomi.brukAvrundetGrad { grad-> grad }} %"

    fun avvis(begrunnelser: List<Begrunnelse>) = begrunnelser
        .filter { it.skalAvvises(this) }
        .takeIf(List<*>::isNotEmpty)
        ?.let(::avvisDag)

    protected open fun avvisDag(begrunnelser: List<Begrunnelse>) = AvvistDag(dato, økonomi, begrunnelser)
    protected abstract fun kopierMed(økonomi: Økonomi): Utbetalingsdag

    abstract fun accept(visitor: UtbetalingsdagVisitor)

    open fun erAvvistMed(begrunnelse: Begrunnelse): AvvistDag? = null

    class ArbeidsgiverperiodeDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 30
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = ArbeidsgiverperiodeDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.ArbeidsgiverperiodeDagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.ArbeidsgiverperiodeDagDto): ArbeidsgiverperiodeDag {
                return ArbeidsgiverperiodeDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class ArbeidsgiverperiodedagNav(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 45
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = ArbeidsgiverperiodedagNav(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.ArbeidsgiverperiodeDagNavDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.ArbeidsgiverperiodeDagNavDto): ArbeidsgiverperiodedagNav {
                return ArbeidsgiverperiodedagNav(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class NavDag(
        dato: LocalDate,
        økonomi: Økonomi
    ) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 50
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = NavDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.NavDagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.NavDagDto): NavDag {
                return NavDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class NavHelgDag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 40
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = NavHelgDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.NavHelgDagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.NavHelgDagDto): NavHelgDag {
                return NavHelgDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class Fridag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 20
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = Fridag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.FridagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.FridagDto): Fridag {
                return Fridag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class Arbeidsdag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 10
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = Arbeidsdag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.ArbeidsdagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.ArbeidsdagDto): Arbeidsdag {
                return Arbeidsdag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class AvvistDag(
        dato: LocalDate,
        økonomi: Økonomi,
        val begrunnelser: List<Begrunnelse>
    ) : Utbetalingsdag(dato, økonomi.lås()) {
        override val prioritet = 60
        override fun avvisDag(begrunnelser: List<Begrunnelse>) =
            AvvistDag(dato, økonomi, this.begrunnelser + begrunnelser)

        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)

        override fun erAvvistMed(begrunnelse: Begrunnelse) = takeIf { begrunnelse in begrunnelser }
        override fun kopierMed(økonomi: Økonomi) = AvvistDag(dato, økonomi, begrunnelser)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.AvvistDagDto(dato, økonomi, begrunnelser.map { it.dto() })
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.AvvistDagDto): AvvistDag {
                return AvvistDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, true),
                    begrunnelser = dto.begrunnelser.map { Begrunnelse.gjenopprett(it) }
                )
            }
        }
    }

    class ForeldetDag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = ForeldetDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.ForeldetDagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.ForeldetDagDto): ForeldetDag {
                return ForeldetDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    class UkjentDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 0
        override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
        override fun kopierMed(økonomi: Økonomi) = UkjentDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiDto) =
            UtbetalingsdagDto.UkjentDagDto(dato, økonomi)
        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagDto.UkjentDagDto): UkjentDag {
                return UkjentDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi, false)
                )
            }
        }
    }

    companion object {
        fun dagerUnderGrensen(tidslinjer: List<Utbetalingstidslinje>): List<Periode> {
            return periode(tidslinjer)
                ?.filter { dato -> tidslinjer.map { it[dato].økonomi }.erUnderGrensen() }
                ?.grupperSammenhengendePerioder()
                ?: emptyList()
        }

        fun betale(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
            return periode(tidslinjer)?.fold(tidslinjer) { resultat, dato ->
                try {
                    tidslinjer
                        .map { it[dato].økonomi }
                        .betal()
                        .mapIndexed { index, økonomi ->
                            Utbetalingstidslinje(resultat[index].map {
                                if (it.dato == dato) it.kopierMed(økonomi) else it
                            })
                        }
                } catch (err: Exception) {
                    throw IllegalArgumentException("Klarte ikke å utbetale for dag=$dato, fordi: ${err.message}", err)
                }
            } ?: tidslinjer
        }

        fun totalSykdomsgrad(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje> {
            return periode(tidslinjer)?.fold(tidslinjer) { tidslinjer1, dagen ->
                // regner ut totalgrad for alle økonomi på samme dag
                val dager = Økonomi.totalSykdomsgrad(tidslinjer1.map { it[dagen].økonomi })
                // oppdaterer tidslinjen til hver ag med nytt økonomiobjekt
                tidslinjer1.zip(dager) { tidslinjen, økonomi ->
                    Utbetalingstidslinje(tidslinjen.map { if (it.dato == dagen) it.kopierMed(økonomi) else it })
                }
            } ?: tidslinjer
        }

        fun gjenopprett(dto: UtbetalingsdagDto): Utbetalingsdag {
            return when (dto) {
                is UtbetalingsdagDto.ArbeidsdagDto -> Arbeidsdag.gjenopprett(dto)
                is UtbetalingsdagDto.ArbeidsgiverperiodeDagDto -> ArbeidsgiverperiodeDag.gjenopprett(dto)
                is UtbetalingsdagDto.ArbeidsgiverperiodeDagNavDto -> ArbeidsgiverperiodedagNav.gjenopprett(dto)
                is UtbetalingsdagDto.AvvistDagDto -> AvvistDag.gjenopprett(dto)
                is UtbetalingsdagDto.ForeldetDagDto -> ForeldetDag.gjenopprett(dto)
                is UtbetalingsdagDto.FridagDto -> Fridag.gjenopprett(dto)
                is UtbetalingsdagDto.NavDagDto -> NavDag.gjenopprett(dto)
                is UtbetalingsdagDto.NavHelgDagDto -> NavHelgDag.gjenopprett(dto)
                is UtbetalingsdagDto.UkjentDagDto -> UkjentDag.gjenopprett(dto)
            }
        }
    }

    fun dto() = dto(this.dato, this.økonomi.dto())
    protected abstract fun dto(dato: LocalDate, økonomi: ØkonomiDto): UtbetalingsdagDto
}

/**
 * Tilpasser Økonomi så det passer til Beløpkilde-porten til utbetalingslinjer
 */
internal class BeløpkildeAdapter(økonomi: Økonomi): Beløpkilde, ØkonomiVisitor {
    private var arbeidsgiverbeløp: Int? = null
    private var personbeløp: Int? = null
    init {
        økonomi.accept(this)
    }
    override fun arbeidsgiverbeløp(): Int = arbeidsgiverbeløp!!
    override fun personbeløp(): Int = personbeløp!!

    override fun visitAvrundetØkonomi(
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?
    ) {
        this.arbeidsgiverbeløp = arbeidsgiverbeløp
        this.personbeløp = personbeløp
    }
}