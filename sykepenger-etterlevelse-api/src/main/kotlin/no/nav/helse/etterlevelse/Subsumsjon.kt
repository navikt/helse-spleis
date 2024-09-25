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

    init {
        val kritiskeTyper = setOf(KontekstType.Fødselsnummer, KontekstType.Organisasjonsnummer)
        check(kritiskeTyper.all { kritiskType ->
            kontekster.count { it.type == kritiskType } == 1
        }) {
            "en av $kritiskeTyper mangler/har duplikat:\n${kontekster.joinToString(separator = "\n")}"
        }
        // todo: sjekker for mindre enn 1 også ettersom noen subsumsjoner skjer på arbeidsgivernivå. det burde vi forsøke å flytte/fikse slik at
        // alt kan subsummeres i kontekst av en behandling.
        check(kontekster.count { it.type == KontekstType.Vedtaksperiode } <= 1) {
            "det er flere kontekster av ${KontekstType.Vedtaksperiode}:\n${kontekster.joinToString(separator = "\n")}"
        }
    }

    enum class Subsumsjonstype {
        ENKEL, PERIODISERT
    }
    enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    override fun toString(): String {
        return "$paragraf $ledd ${if (punktum == null) "" else punktum} ${if (bokstav == null) "" else bokstav} [$utfall]"
    }
}
