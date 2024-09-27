package no.nav.helse.etterlevelse

import java.time.LocalDate

data class Subsumsjon(
    val type: Subsumsjonstype,
    val lovverk: String,
    val utfall: Utfall,
    val versjon: LocalDate,
    val paragraf: Paragraf,
    val ledd: Ledd?,
    val punktum: Punktum? = null,
    val bokstav: Bokstav? = null,
    val input: Map<String, Any>,
    val output: Map<String, Any>,
    val kontekster: List<Subsumsjonskontekst>
) {
    val lovreferanse = Lovreferanse(
        lovverk = lovverk,
        paragraf = paragraf,
        ledd = ledd,
        punktum = punktum,
        bokstav = bokstav
    )
    companion object {
        fun enkelSubsumsjon(
            utfall: Utfall,
            lovverk: String,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd?,
            punktum: Punktum? = null,
            bokstav: Bokstav? = null,
            input: Map<String, Any>,
            output: Map<String, Any>,
            kontekster: List<Subsumsjonskontekst>
        ): Subsumsjon {
            return Subsumsjon(
                type = Subsumsjonstype.ENKEL,
                lovverk = lovverk,
                utfall = utfall,
                versjon = versjon,
                paragraf = paragraf,
                ledd = ledd,
                punktum = punktum,
                bokstav = bokstav,
                input = input,
                output = output,
                kontekster = kontekster
            )
        }
        fun periodisertSubsumsjon(
            perioder: Collection<ClosedRange<LocalDate>>,
            lovverk: String,
            utfall: Utfall,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd?,
            punktum: Punktum? = null,
            bokstav: Bokstav? = null,
            output: Map<String, Any> = emptyMap(),
            input: Map<String, Any>,
            kontekster: List<Subsumsjonskontekst>
        ): Subsumsjon {
            val outputMedPerioder = output + mapOf(
                "perioder" to perioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                }
            )
            return Subsumsjon(
                type = Subsumsjonstype.PERIODISERT,
                lovverk = lovverk,
                utfall = utfall,
                versjon = versjon,
                paragraf = paragraf,
                ledd = ledd,
                punktum = punktum,
                bokstav = bokstav,
                input = input,
                output = outputMedPerioder,
                kontekster = kontekster
            )
        }
    }

    enum class Subsumsjonstype {
        ENKEL, PERIODISERT
    }
    enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    fun er(lovreferanse: Lovreferanse) = this.lovreferanse == lovreferanse

    override fun toString(): String {
        return "$lovreferanse [$utfall]"
    }
}

data class Lovreferanse(val lovverk: String, val paragraf: Paragraf?, val ledd: Ledd?, val punktum: Punktum?, val bokstav: Bokstav?) {
    override fun toString(): String {
        val parts = listOfNotNull(lovverk, paragraf?.toString(), ledd?.toString(), punktum?.toString(), bokstav?.toString())
        return parts.joinToString(separator = " ")
    }
}

val folketrygdloven = Lovreferanse(lovverk = "folketrygdloven", paragraf = null, ledd = null, punktum = null, bokstav = null)

fun Lovreferanse.paragraf(paragraf: Paragraf) = copy(paragraf = paragraf)

val Lovreferanse.førsteLedd get() = copy(ledd = Ledd.LEDD_1)
val Lovreferanse.annetLedd get() = copy(ledd = Ledd.LEDD_2)
val Lovreferanse.tredjeLedd get() = copy(ledd = Ledd.LEDD_3)
val Lovreferanse.fjerdeLedd get() = copy(ledd = Ledd.LEDD_4)
val Lovreferanse.femteLedd get() = copy(ledd = Ledd.LEDD_5)
val Lovreferanse.sjetteLedd get() = copy(ledd = Ledd.LEDD_6)

val Lovreferanse.førstePunktum get() = copy(punktum = Punktum.PUNKTUM_1)
val Lovreferanse.annetPunktum get() = copy(punktum = Punktum.PUNKTUM_2)
val Lovreferanse.tredjePunktum get() = copy(punktum = Punktum.PUNKTUM_3)
val Lovreferanse.fjerdePunktum get() = copy(punktum = Punktum.PUNKTUM_4)
val Lovreferanse.femtePunktum get() = copy(punktum = Punktum.PUNKTUM_5)
val Lovreferanse.sjettePunktum get() = copy(punktum = Punktum.PUNKTUM_6)
val Lovreferanse.syvendePunktum get() = copy(punktum = Punktum.PUNKTUM_7)

val Lovreferanse.bokstavA get() = copy(bokstav = Bokstav.BOKSTAV_A)
val Lovreferanse.bokstavB get() = copy(bokstav = Bokstav.BOKSTAV_B)
val Lovreferanse.bokstavC get() = copy(bokstav = Bokstav.BOKSTAV_C)