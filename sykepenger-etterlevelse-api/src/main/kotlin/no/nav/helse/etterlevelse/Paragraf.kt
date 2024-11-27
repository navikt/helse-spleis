package no.nav.helse.etterlevelse

import java.time.LocalDate

enum class Paragraf(
    val ref: String,
) {
    PARAGRAF_2("2"),
    PARAGRAF_8_2("8-2"),
    PARAGRAF_8_3("8-3"),
    PARAGRAF_8_9("8-9"),
    PARAGRAF_8_10("8-10"),
    PARAGRAF_8_11("8-11"),
    PARAGRAF_8_12("8-12"),
    PARAGRAF_8_13("8-13"),
    PARAGRAF_8_15("8-15"),
    PARAGRAF_8_16("8-16"),
    PARAGRAF_8_17("8-17"),
    PARAGRAF_8_19("8-19"),
    PARAGRAF_8_28("8-28"),
    PARAGRAF_8_29("8-29"),
    PARAGRAF_8_30("8-30"),
    PARAGRAF_8_48("8-48"),
    PARAGRAF_8_51("8-51"),
    PARAGRAF_22_13("22-13"),
    PARAGRAF_35("35"),
    KJENNELSE_2006_4023("2006-4023"),
    ;

    override fun toString(): String = "§ $ref"
}

enum class Ledd(
    val nummer: Int,
) {
    LEDD_1(1),
    LEDD_2(2),
    LEDD_3(3),
    LEDD_4(4),
    LEDD_5(5),
    LEDD_6(6),
    ;

    override fun toString(): String = "$nummer. ledd"

    companion object {
        val Int.ledd get() = enumValues<Ledd>().first { it.nummer == this }
    }
}

enum class Punktum(
    val nummer: Int,
) {
    PUNKTUM_1(1),
    PUNKTUM_2(2),
    PUNKTUM_3(3),
    PUNKTUM_4(4),
    PUNKTUM_5(5),
    PUNKTUM_6(6),
    PUNKTUM_7(7),
    ;

    init {
        require(nummer > 0) { "Et punktum på være et tall større enn 0" }
    }

    override fun toString(): String = "$nummer. punktum"

    companion object {
        val Int.punktum get() = enumValues<Punktum>().first { it.nummer == this }
        val IntRange.punktum get() = enumValues<Punktum>().filter { it.nummer in this }
    }
}

enum class Bokstav(
    val ref: Char,
) {
    BOKSTAV_A('a'),
    BOKSTAV_B('b'),
    BOKSTAV_C('c'),
    ;

    init {
        val regex = "[a-zæøå]".toRegex()
        require(regex.matches(ref.toString())) { "En bokstav må være en bokstav i det norske alfabetet" }
    }

    override fun toString(): String = "bokstav $ref"
}

val FOLKETRYGDLOVENS_OPPRINNELSESDATO: LocalDate = LocalDate.of(1997, 2, 28)
