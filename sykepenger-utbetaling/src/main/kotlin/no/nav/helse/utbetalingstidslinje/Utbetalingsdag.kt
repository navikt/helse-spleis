package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.Økonomi.Companion.erUnderGrensen

sealed class Utbetalingsdag(
    val dato: LocalDate,
    val økonomi: Økonomi
) : Comparable<Utbetalingsdag> {

    internal abstract val prioritet: Int
    override fun compareTo(other: Utbetalingsdag): Int {
        return this.prioritet.compareTo(other.prioritet)
    }

    override fun toString() = "${this.javaClass.simpleName} ($dato) ${økonomi.brukAvrundetGrad { grad -> grad }} %"

    fun avvis(begrunnelse: Begrunnelse) = if (begrunnelse.skalAvvises(this)) this.avvisDag(begrunnelse) else null

    protected open fun avvisDag(begrunnelse: Begrunnelse) = AvvistDag(dato, økonomi, listOf(begrunnelse))
    internal abstract fun kopierMed(økonomi: Økonomi): Utbetalingsdag

    open fun erAvvistMed(begrunnelse: Begrunnelse): AvvistDag? = null

    class ArbeidsgiverperiodeDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 30
        override fun kopierMed(økonomi: Økonomi) = ArbeidsgiverperiodeDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.ArbeidsgiverperiodeDagDto): ArbeidsgiverperiodeDag {
                return ArbeidsgiverperiodeDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class ArbeidsgiverperiodedagNav(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 45
        override fun kopierMed(økonomi: Økonomi) = ArbeidsgiverperiodedagNav(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.ArbeidsgiverperiodeDagNavDto): ArbeidsgiverperiodedagNav {
                return ArbeidsgiverperiodedagNav(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class NavDag(
        dato: LocalDate,
        økonomi: Økonomi
    ) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 50
        override fun kopierMed(økonomi: Økonomi) = NavDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.NavDagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.NavDagDto): NavDag {
                return NavDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class NavHelgDag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 40
        override fun kopierMed(økonomi: Økonomi) = NavHelgDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.NavHelgDagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.NavHelgDagDto): NavHelgDag {
                return NavHelgDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class Fridag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 20
        override fun kopierMed(økonomi: Økonomi) = Fridag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.FridagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.FridagDto): Fridag {
                return Fridag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class Arbeidsdag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 10
        override fun kopierMed(økonomi: Økonomi) = Arbeidsdag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.ArbeidsdagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.ArbeidsdagDto): Arbeidsdag {
                return Arbeidsdag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class AvvistDag(
        dato: LocalDate,
        økonomi: Økonomi,
        val begrunnelser: List<Begrunnelse>
    ) : Utbetalingsdag(dato, økonomi.ikkeBetalt()) {
        override val prioritet = 60
        override fun avvisDag(begrunnelse: Begrunnelse) =
            AvvistDag(dato, økonomi, this.begrunnelser + begrunnelse)


        override fun erAvvistMed(begrunnelse: Begrunnelse) = takeIf { begrunnelse in begrunnelser }
        override fun kopierMed(økonomi: Økonomi) = AvvistDag(dato, økonomi, begrunnelser)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.AvvistDagDto(dato, økonomi, begrunnelser.map { it.dto() })

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.AvvistDagDto): AvvistDag {
                return AvvistDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi),
                    begrunnelser = dto.begrunnelser.map { Begrunnelse.gjenopprett(it) }
                )
            }
        }
    }

    class ForeldetDag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
        override fun kopierMed(økonomi: Økonomi) = ForeldetDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.ForeldetDagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.ForeldetDagDto): ForeldetDag {
                return ForeldetDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }

    class Venteperiodedag(dato: LocalDate, økonomi: Økonomi) :
        Utbetalingsdag(dato, økonomi) {
        override val prioritet = 25
        override fun kopierMed(økonomi: Økonomi) = Venteperiodedag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.VenteperiodedagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.VenteperiodedagDto): Venteperiodedag {
                return Venteperiodedag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
                )
            }
        }
    }


    class UkjentDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
        override val prioritet = 0
        override fun kopierMed(økonomi: Økonomi) = UkjentDag(dato, økonomi)
        override fun dto(dato: LocalDate, økonomi: ØkonomiUtDto) =
            UtbetalingsdagUtDto.UkjentDagDto(dato, økonomi)

        internal companion object {
            fun gjenopprett(dto: UtbetalingsdagInnDto.UkjentDagDto): UkjentDag {
                return UkjentDag(
                    dato = dto.dato,
                    økonomi = Økonomi.gjenopprett(dto.økonomi)
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

        fun gjenopprett(dto: UtbetalingsdagInnDto): Utbetalingsdag {
            return when (dto) {
                is UtbetalingsdagInnDto.ArbeidsdagDto -> Arbeidsdag.gjenopprett(dto)
                is UtbetalingsdagInnDto.ArbeidsgiverperiodeDagDto -> ArbeidsgiverperiodeDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.ArbeidsgiverperiodeDagNavDto -> ArbeidsgiverperiodedagNav.gjenopprett(dto)
                is UtbetalingsdagInnDto.AvvistDagDto -> AvvistDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.ForeldetDagDto -> ForeldetDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.FridagDto -> Fridag.gjenopprett(dto)
                is UtbetalingsdagInnDto.NavDagDto -> NavDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.NavHelgDagDto -> NavHelgDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.UkjentDagDto -> UkjentDag.gjenopprett(dto)
                is UtbetalingsdagInnDto.VenteperiodedagDto -> Venteperiodedag.gjenopprett(dto)
            }
        }
    }

    fun dto() = dto(this.dato, this.økonomi.dto())
    protected abstract fun dto(dato: LocalDate, økonomi: ØkonomiUtDto): UtbetalingsdagUtDto
}
