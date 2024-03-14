package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi

internal typealias BesteStrategy = (Dag, Dag) -> Dag

internal sealed class Dag(
    protected val dato: LocalDate,
    protected val kilde: Hendelseskilde
) {
    private fun name() = javaClass.canonicalName.split('.').last()

    companion object {
        internal val default: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (venstre == høyre) venstre else høyre.problem(venstre)
        }

        internal val override: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            høyre
        }

        internal val sammenhengendeSykdom: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            when (venstre) {
                is Sykedag,
                is SykedagNav,
                is SykHelgedag,
                is Arbeidsgiverdag,
                is ArbeidsgiverHelgedag -> venstre
                is Feriedag,
                is Permisjonsdag -> when (høyre) {
                    is Sykedag,
                    is SykedagNav,
                    is SykHelgedag,
                    is Arbeidsgiverdag,
                    is ArbeidsgiverHelgedag -> høyre
                    else -> venstre
                }
                else -> høyre
            }
        }

        internal val noOverlap: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            høyre.problem(venstre, "Støtter ikke overlappende perioder (${venstre.kilde} og ${høyre.kilde})")
        }

        internal val replace: BesteStrategy = { venstre: Dag, høyre: Dag ->
            if (høyre is UkjentDag) venstre
            else høyre
        }

        fun gjenopprett(dag: SykdomstidslinjeDagDto): Dag {
            return when (dag) {
                is SykdomstidslinjeDagDto.AndreYtelserDto -> AndreYtelser.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto -> ArbeidIkkeGjenopptattDag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ArbeidsdagDto -> Arbeidsdag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto -> ArbeidsgiverHelgedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ArbeidsgiverdagDto -> Arbeidsgiverdag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.FeriedagDto -> Feriedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ForeldetSykedagDto -> ForeldetSykedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.FriskHelgedagDto -> FriskHelgedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.PermisjonsdagDto -> Permisjonsdag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.ProblemDagDto -> ProblemDag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.SykHelgedagDto -> SykHelgedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.SykedagDto -> Sykedag.gjenopprett(dag)
                is SykdomstidslinjeDagDto.SykedagNavDto -> SykedagNav.gjenopprett(dag)
                is SykdomstidslinjeDagDto.UkjentDagDto -> UkjentDag.gjenopprett(dag)
            }
        }
    }

    internal fun kommerFra(hendelse: Melding) = kilde.erAvType(hendelse)
    internal fun kommerFra(hendelse: String) = kilde.erAvType(hendelse)

    internal fun erHelg() = dato.erHelg()

    internal fun problem(other: Dag, melding: String = "Kan ikke velge mellom ${name()} fra $kilde og ${other.name()} fra ${other.kilde}."): Dag =
        ProblemDag(dato, kilde, other.kilde, melding)

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as Dag)

    protected open fun equals(other: Dag) = this.dato == other.dato && this.kilde == other.kilde

    override fun hashCode() = dato.hashCode() * 37 + kilde.hashCode() * 41 + this::class.hashCode()

    override fun toString() = "${this::class.java.simpleName} ($dato) $kilde"

    internal open fun accept(visitor: SykdomstidslinjeDagVisitor) {}

    internal class UkjentDag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)

        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.UkjentDagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.UkjentDagDto): UkjentDag {
                return UkjentDag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class Arbeidsdag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ArbeidsdagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ArbeidsdagDto): Arbeidsdag {
                return Arbeidsdag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class Arbeidsgiverdag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ArbeidsgiverdagDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ArbeidsgiverdagDto): Arbeidsgiverdag {
                return Arbeidsgiverdag(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class Feriedag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.FeriedagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.FeriedagDto): Feriedag {
                return Feriedag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class ArbeidIkkeGjenopptattDag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {
        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto): ArbeidIkkeGjenopptattDag {
                return ArbeidIkkeGjenopptattDag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class FriskHelgedag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)

        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.FriskHelgedagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.FriskHelgedagDto): FriskHelgedag {
                return FriskHelgedag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class ArbeidsgiverHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto): ArbeidsgiverHelgedag {
                return ArbeidsgiverHelgedag(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class Sykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.SykedagDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.SykedagDto): Sykedag {
                return Sykedag(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class ForeldetSykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ForeldetSykedagDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ForeldetSykedagDto): ForeldetSykedag {
                return ForeldetSykedag(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class SykHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.SykHelgedagDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.SykHelgedagDto): SykHelgedag {
                return SykHelgedag(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class SykedagNav(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {
        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.SykedagNavDto(dato, kilde, økonomi.dto().grad)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.SykedagNavDto): SykedagNav {
                return SykedagNav(
                    dato = dto.dato,
                    økonomi = Økonomi.sykdomsgrad(Prosentdel.gjenopprett(dto.grad)),
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class Permisjonsdag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.PermisjonsdagDto(dato, kilde)
        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.PermisjonsdagDto): Permisjonsdag {
                return Permisjonsdag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde)
                )
            }
        }
    }

    internal class ProblemDag(
        dato: LocalDate,
        kilde: Hendelseskilde,
        private val other: Hendelseskilde,
        private val melding: String
    ) : Dag(dato, kilde) {

        internal constructor(dato: LocalDate, kilde: Hendelseskilde, melding: String) : this(dato, kilde, kilde, melding)

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde, other, melding)
        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) = SykdomstidslinjeDagDto.ProblemDagDto(dato, kilde, other.dto(), melding)

        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.ProblemDagDto): ProblemDag {
                return ProblemDag(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde),
                    other = Hendelseskilde.gjenopprett(dto.other),
                    melding = dto.melding
                )
            }
        }
    }

    internal class AndreYtelser(
        dato: LocalDate,
        kilde: Hendelseskilde,
        private val ytelse: AnnenYtelse,
    ) : Dag(dato, kilde) {
        enum class AnnenYtelse {
            Foreldrepenger, AAP, Omsorgspenger, Pleiepenger, Svangerskapspenger, Opplæringspenger, Dagpenger;

            fun dto() = when (this) {
                Foreldrepenger -> YtelseDto.Foreldrepenger
                AAP -> YtelseDto.AAP
                Omsorgspenger -> YtelseDto.Omsorgspenger
                Pleiepenger -> YtelseDto.Pleiepenger
                Svangerskapspenger -> YtelseDto.Svangerskapspenger
                Opplæringspenger -> YtelseDto.Opplæringspenger
                Dagpenger -> YtelseDto.Dagpenger
            }
        }

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde, ytelse)

        override fun dto(dato: LocalDate, kilde: HendelseskildeDto) =
            SykdomstidslinjeDagDto.AndreYtelserDto(dato, kilde, ytelse.dto())

        internal companion object {
            fun gjenopprett(dto: SykdomstidslinjeDagDto.AndreYtelserDto): AndreYtelser {
                return AndreYtelser(
                    dato = dto.dato,
                    kilde = Hendelseskilde.gjenopprett(dto.kilde),
                    ytelse = when (dto.ytelse) {
                        YtelseDto.Foreldrepenger -> AnnenYtelse.Foreldrepenger
                        YtelseDto.AAP -> AnnenYtelse.AAP
                        YtelseDto.Omsorgspenger -> AnnenYtelse.Omsorgspenger
                        YtelseDto.Pleiepenger -> AnnenYtelse.Pleiepenger
                        YtelseDto.Svangerskapspenger -> AnnenYtelse.Svangerskapspenger
                        YtelseDto.Opplæringspenger -> AnnenYtelse.Opplæringspenger
                        YtelseDto.Dagpenger -> AnnenYtelse.Dagpenger
                    }
                )
            }
        }
    }

    internal fun dto() = dto(dato, kilde.dto())
    protected abstract fun dto(dato: LocalDate, kilde: HendelseskildeDto): SykdomstidslinjeDagDto
}

internal interface SykdomstidslinjeDagVisitor {
    fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {}

    fun visitDag(dag: Dag.ArbeidIkkeGjenopptattDag, dato: LocalDate, kilde: Hendelseskilde) {}

    fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.SykedagNav,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {}

    fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: Hendelseskilde,
        other: Hendelseskilde?,
        melding: String
    ) {
    }
    fun visitDag(
        dag: Dag.AndreYtelser,
        dato: LocalDate,
        kilde: Hendelseskilde,
        ytelse: Dag.AndreYtelser.AnnenYtelse
    ) {
    }
}