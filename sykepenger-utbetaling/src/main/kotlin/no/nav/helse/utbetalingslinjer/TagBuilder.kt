package no.nav.helse.utbetalingslinjer

class TagBuilder() {
    private val tags: MutableSet<String> = mutableSetOf()

    fun tag6GBegrenset() = tags.add("6G_BEGRENSET")
    fun tagFlereArbeidsgivere(antall: Int) {
        if( antall > 1) tags.add("FLERE_ARBEIDSGIVERE")
        else tags.add("EN_ARBEIDSGIVER")
    }
    fun tagUtbetaling(arbeidsgiverNettoBeløp: Int, personNettoBeløp: Int) {
        if (arbeidsgiverNettoBeløp > 0) tags.add("ARBEIDSGIVERUTBETALING")
        else if (arbeidsgiverNettoBeløp < 0) tags.add("NEGATIV_ARBEIDSGIVERUTBETALING")

        if (personNettoBeløp > 0) tags.add("PERSONUTBETALING")
        else if (personNettoBeløp < 0) tags.add("NEGATIV_PERSONUTBETALING")

        if (arbeidsgiverNettoBeløp == 0 && personNettoBeløp == 0) tags.add("INGEN_UTBETALING")
    }


    fun build(): Set<String> {
        return tags.toSet()
    }


}