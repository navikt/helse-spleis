package no.nav.helse.person.aktivitetslogg

import java.time.LocalDate

class GodkjenningsbehovBuilder(private val erForlengelse: Boolean, private val kanAvvises: Boolean) {
    private val tags: MutableSet<String> = mutableSetOf()
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var periodeFom: LocalDate
    private lateinit var periodeTom: LocalDate
    private lateinit var periodetype: UtbetalingPeriodetype
    private val førstegangsbehandling = !erForlengelse
    private lateinit var utbetalingtype: String
    private lateinit var inntektskilde: UtbetalingInntektskilde
    private lateinit var orgnummereMedRelevanteArbeidsforhold: Set<String>
    private val omregnedeÅrsinntekter: MutableList<Map<String, Any>> = mutableListOf()

    fun tag6GBegrenset() = tags.add("6G_BEGRENSET")
    fun tagFlereArbeidsgivere(antall: Int) {
        if( antall > 1) tags.add("FLERE_ARBEIDSGIVERE")
        else tags.add("EN_ARBEIDSGIVER")
    }
    fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
    }
    fun periode(fom: LocalDate, tom: LocalDate) = apply {
        this.periodeFom = fom
        this.periodeTom = tom
    }
    fun erInfotrygd(erInfotrygd: Boolean) = apply {
        this.periodetype = when (erForlengelse) {
            true -> when (erInfotrygd) {
                true -> UtbetalingPeriodetype.INFOTRYGDFORLENGELSE
                else -> UtbetalingPeriodetype.FORLENGELSE
            }
            false -> when (erInfotrygd) {
                true -> UtbetalingPeriodetype.OVERGANG_FRA_IT
                else -> UtbetalingPeriodetype.FØRSTEGANGSBEHANDLING
            }
        }
    }
    fun utbetalingtype(utbetalingtype: String) = apply {
        this.utbetalingtype = utbetalingtype
    }
    fun inntektskilde(inntektskilde: UtbetalingInntektskilde) = apply {
        this.inntektskilde = inntektskilde
    }
    fun orgnummereMedRelevanteArbeidsforhold(orgnummereMedRelevanteArbeidsforhold: Set<String>) = apply {
        this.orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
    }
    fun tagUtbetaling(arbeidsgiverNettoBeløp: Int, personNettoBeløp: Int) {
        if (arbeidsgiverNettoBeløp > 0) tags.add("ARBEIDSGIVERUTBETALING")
        else if (arbeidsgiverNettoBeløp < 0) tags.add("NEGATIV_ARBEIDSGIVERUTBETALING")

        if (personNettoBeløp > 0) tags.add("PERSONUTBETALING")
        else if (personNettoBeløp < 0) tags.add("NEGATIV_PERSONUTBETALING")

        if (arbeidsgiverNettoBeløp == 0 && personNettoBeløp == 0) tags.add("INGEN_UTBETALING")
    }

    fun omregnedeÅrsinntekter(orgnummer: String, omregnetÅrsinntekt: Double) = apply {
        omregnedeÅrsinntekter.add(mapOf("organisasjonsnummer" to orgnummer, "beløp" to omregnetÅrsinntekt))
    }

    fun build() = mapOf(
        "periodeFom" to periodeFom.toString(),
        "periodeTom" to periodeTom.toString(),
        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
        "periodetype" to periodetype.name,
        "førstegangsbehandling" to førstegangsbehandling,
        "utbetalingtype" to utbetalingtype,
        "inntektskilde" to inntektskilde.name,
        "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
        "tags" to tags,
        "kanAvvises" to kanAvvises,
        "omregnedeÅrsinntekter" to omregnedeÅrsinntekter
    )


}