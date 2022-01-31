package no.nav.helse.person

import java.time.LocalDate

enum class Paragraf(private val ref: String) {
    PARAGRAF_2("2"),
    PARAGRAF_8_2("8-2"),
    PARAGRAF_8_3("8-3"),
    PARAGRAF_8_4("8-4"),
    PARAGRAF_8_10("8-10"),
    PARAGRAF_8_11("8-11"),
    PARAGRAF_8_12("8-12"),
    PARAGRAF_8_13("8-13"),
    PARAGRAF_8_16("8-16"),
    PARAGRAF_8_17("8-17"),
    PARAGRAF_8_30("8-30"),
    PARAGRAF_8_51("8-51");

    override fun toString(): String {
        return "§$ref"
    }
}

enum class Ledd(private val nummer: Int) {
    LEDD_1(1),
    LEDD_2(2),
    LEDD_3(3),
    LEDD_4(4),
    LEDD_5(5),
    LEDD_6(6);

    override fun toString(): String {
        return "$nummer. ledd"
    }

    internal companion object {
        val Int.ledd get() = enumValues<Ledd>().first { it.nummer == this }
    }
}

enum class Punktum(private val nummer: Int) {
    PUNKTUM_1(1),
    PUNKTUM_2(2),
    PUNKTUM_3(3),
    PUNKTUM_4(4),
    PUNKTUM_5(5),
    PUNKTUM_6(6),
    PUNKTUM_7(7);

    override fun toString(): String {
        return "$nummer. punktum"
    }

    internal companion object {
        val Int.punktum get() = enumValues<Punktum>().filter { it.nummer == this }
        val IntRange.punktum get() = enumValues<Punktum>().filter { it.nummer in this }
    }
}

enum class Bokstav {
    BOKSTAV_A,
    BOKSTAV_B
}

val FOLKETRYGDLOVENS_OPPRINNELSESDATO: LocalDate = LocalDate.of(1997, 2, 28)
