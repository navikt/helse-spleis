package no.nav.helse.person

import no.nav.helse.utbetalingstidslinje.Alder
import java.time.LocalDate

interface IVilkårssporing {
    fun utfall(): Boolean
    override fun toString(): String
}

internal fun <Type, Builder> build(
    buildertype: Buildertype<Type, Builder>,
    block: Builder.() -> Unit
) = buildertype.build(block)

internal interface Buildertype<Type, Builder> {
    fun build(block: Builder.() -> Unit): Type
}

class Paragraf_8_2(
    private val førsteFraværsdag: LocalDate,
    private val arbeidsforhold: List<Pair<LocalDate, LocalDate?>>
) : IVilkårssporing {

    override fun utfall(): Boolean {
        return false
    }

    override fun toString() = "Paragraf_8_2"

    internal class Builder {
        internal lateinit var førsteFraværsdag: LocalDate
        private val arbeidsforhold: MutableList<Pair<LocalDate, LocalDate?>> = mutableListOf()

        internal fun arbeidsforhold(fom: LocalDate, tom: LocalDate?) {
            arbeidsforhold += fom to tom
        }

        internal fun build() = Paragraf_8_2(førsteFraværsdag, arbeidsforhold.toList())

        internal companion object Type : Buildertype<Paragraf_8_2, Builder> {
            override fun build(block: Builder.() -> Unit) =
                Builder().apply(block).let { Paragraf_8_2(it.førsteFraværsdag, it.arbeidsforhold.toList()) }
        }
    }
}

internal class Paragraf_8_1(
    private val førsteFraværsdag: LocalDate,
    private val arbeidsforhold: List<Pair<LocalDate, LocalDate?>>,
    private val ledd1: Ledd_1
) : IVilkårssporing {

    override fun utfall(): Boolean {
        return false && ledd1.utfall()
    }

    override fun toString() = "Paragraf_8_1"

    internal class Ledd_1(private val alder: Alder): IVilkårssporing {
        override fun utfall(): Boolean {
            return false
        }

        override fun toString() = "Paragraf_8_1.Ledd1"
    }

    internal companion object Type : Buildertype<Paragraf_8_1, Type.Builder> {
        override fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        internal class Builder {
            internal lateinit var førsteFraværsdag: LocalDate
            private val arbeidsforhold: MutableList<Pair<LocalDate, LocalDate?>> = mutableListOf()
            private lateinit var ledd_1: Paragraf_8_1.Ledd_1

            internal fun arbeidsforhold(fom: LocalDate, tom: LocalDate?) {
                arbeidsforhold += fom to tom
            }

            internal fun ledd_1(block: Ledd_1.() -> Unit) {
                ledd_1 = Ledd_1().apply(block).build()
            }

            internal class Ledd_1 {
                fun build() = Ledd_1(alder)

                internal lateinit var alder: Alder
            }

            internal fun build(): Paragraf_8_1 {
                require(arbeidsforhold.isNotEmpty())
                return Paragraf_8_1(førsteFraværsdag, arbeidsforhold.toList(), ledd_1)
            }
        }
    }
}

val p2 = build(Paragraf_8_2.Builder) {
    førsteFraværsdag = LocalDate.now()
    arbeidsforhold(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
}

val utfall2 = p2.utfall()
val string2 = p2.toString()

internal val p1 = build(Paragraf_8_1) {
    førsteFraværsdag = LocalDate.now()
    arbeidsforhold(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
    ledd_1 {
        alder = Alder("12312312312")
    }
}

val utfall1 = p1.utfall()
val string1 = p1.toString()
