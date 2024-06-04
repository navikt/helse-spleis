package no.nav.helse.person.aktivitetslogg

import java.time.LocalDate
import java.util.UUID

class PeriodeMedSammeSkjæringstidspunkt(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val periode: ClosedRange<LocalDate>
)
class GodkjenningsbehovBuilder(
    private val erForlengelse: Boolean,
    private val kanAvvises: Boolean,
    periode: ClosedRange<LocalDate>,
    private val behandlingId: UUID,
    private val perioderMedSammeSkjæringstidspunkt: List<PeriodeMedSammeSkjæringstidspunkt>,
    private val hendelser: Set<UUID>
) {
    private val tags: MutableSet<String> = mutableSetOf()
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var vilkårsgrunnlagId: UUID
    private val periodeFom: LocalDate = periode.start
    private val periodeTom: LocalDate = periode.endInclusive
    private lateinit var periodetype: UtbetalingPeriodetype
    private val førstegangsbehandling = !erForlengelse
    private lateinit var utbetalingtype: String
    private lateinit var inntektskilde: UtbetalingInntektskilde
    private lateinit var orgnummereMedRelevanteArbeidsforhold: Set<String>
    private val omregnedeÅrsinntekter: MutableList<Map<String, Any>> = mutableListOf()

    init {
        if (førstegangsbehandling) tags.add("Førstegangsbehandling")
        else tags.add("Forlengelse")
    }

    fun tagFlereArbeidsgivere(antall: Int) {
        if (antall > 1) tags.add("FlereArbeidsgivere")
        else tags.add("EnArbeidsgiver")
    }
    fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    fun vilkårsgrunnlagId(vilkårsgrunnlagId: UUID) = apply {
        this.vilkårsgrunnlagId = vilkårsgrunnlagId
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
        if (erInfotrygd) {
            tags.add("InngangsvilkårFraInfotrygd")
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
        val antallTagsFør = tags.size

        if (arbeidsgiverNettoBeløp > 0) tags.add("Arbeidsgiverutbetaling")
        else if (arbeidsgiverNettoBeløp < 0) tags.add("NegativArbeidsgiverutbetaling")

        if (personNettoBeløp > 0) tags.add("Personutbetaling")
        else if (personNettoBeløp < 0) tags.add("NegativPersonutbetaling")

        if (arbeidsgiverNettoBeløp == 0 && personNettoBeløp == 0) tags.add("IngenUtbetaling")

        check(tags.size > antallTagsFør) { "arbeidsgiverNettoBeløp=$arbeidsgiverNettoBeløp, personNettoBeløp=$personNettoBeløp burde bli minst én ny tag." }
    }

    fun tagBehandlingsresultat(behandlingsresultat: String) {
        tags.add(behandlingsresultat)
    }
    fun tagSykepengegrunnlagErUnder2G() {
        tags.add("SykepengegrunnlagUnder2G")
    }
    fun tagSykepengegrunnlagEr6GBegrenset() {
        tags.add("6GBegrenset")
    }
    fun tagIngenNyArbeidsgiverperiode() {
        tags.add("IngenNyArbeidsgiverperiode")
    }

    fun tagTilkommenInntekt() {
        tags.add("TilkommenInntekt")
    }

    fun omregnedeÅrsinntekter(orgnummer: String, omregnetÅrsinntekt: Double) = apply {
        omregnedeÅrsinntekter.add(mapOf("organisasjonsnummer" to orgnummer, "beløp" to omregnetÅrsinntekt))
    }

    fun build() = mapOf(
        "periodeFom" to periodeFom.toString(),
        "periodeTom" to periodeTom.toString(),
        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
        "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
        "periodetype" to periodetype.name,
        "førstegangsbehandling" to førstegangsbehandling,
        "utbetalingtype" to utbetalingtype,
        "inntektskilde" to inntektskilde.name,
        "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
        "tags" to tags,
        "kanAvvises" to kanAvvises,
        "omregnedeÅrsinntekter" to omregnedeÅrsinntekter,
        "behandlingId" to behandlingId.toString(),
        "hendelser" to hendelser,
        "perioderMedSammeSkjæringstidspunkt" to perioderMedSammeSkjæringstidspunkt.map {
            mapOf(
                "vedtaksperiodeId" to it.vedtaksperiodeId.toString(),
                "behandlingId" to it.behandlingId.toString(),
                "fom" to it.periode.start.toString(),
                "tom" to it.periode.endInclusive.toString()
            )
        }
    )

    fun tags() = tags.toSet()

}