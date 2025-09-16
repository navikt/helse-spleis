package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.dto.deserialisering.InntektsgrunnlagInnDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.etterlevelse.PensjonsgivendeInntektSubsumsjon
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-10 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-35 ledd 2`
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.Opptjening
import no.nav.helse.person.SelvstendigNæringsdrivendeOpptjening
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.berik
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.beverte
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.beverteDeaktiverte
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.deaktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.fastsattÅrsinntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.harFunksjonellEndring
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.harGjenbrukbarInntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.harInntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.lagreTidsnæreInntekter
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.måHaRegistrertOpptjeningForArbeidsgivere
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrMedInntektsmelding
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrMedSaksbehandler
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.sjekkForNyArbeidsgiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.skjønnsfastsett
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.totalOmregnetÅrsinntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.validerSkjønnsmessigAltEllerIntet
import no.nav.helse.person.inntekt.Inntektsgrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.person.inntekt.Inntektsgrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.inntekt.Inntektsgrunnlag.Begrensning.VURDERT_I_INFOTRYGD
import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning.Companion.berik
import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning.Companion.beverte
import no.nav.helse.økonomi.Inntekt

internal class Inntektsgrunnlag(
    private val skjæringstidspunkt: LocalDate,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
    private val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
    private val vurdertInfotrygd: Boolean,
    `6G`: Inntekt? = null
) : Comparable<Inntekt> {

    init {
        arbeidsgiverInntektsopplysninger.validerSkjønnsmessigAltEllerIntet()
    }

    private val `6G`: Inntekt = `6G` ?: Grunnbeløp.`6G`.beløp(skjæringstidspunkt, LocalDate.now())

    // sum av alle inntekter foruten skjønnsmessig fastsatt beløp; da brukes inntekten den fastsatte
    private val omregnetÅrsinntekt = arbeidsgiverInntektsopplysninger.totalOmregnetÅrsinntekt()

    // summen av alle inntekter
    val beregningsgrunnlag = selvstendigInntektsopplysning?.fastsattÅrsinntekt ?: arbeidsgiverInntektsopplysninger.fastsattÅrsinntekt()
    val sykepengegrunnlag = beregningsgrunnlag.coerceAtMost(this.`6G`)
    private val begrensning = if (vurdertInfotrygd) VURDERT_I_INFOTRYGD else if (beregningsgrunnlag > this.`6G`) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET

    internal constructor(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        subsumsjonslogg: Subsumsjonslogg,
        vurdertInfotrygd: Boolean = false
    ) : this(skjæringstidspunkt, arbeidsgiverInntektsopplysninger, selvstendigInntektsopplysning, deaktiverteArbeidsforhold, vurdertInfotrygd) {
        subsumsjonslogg.apply {
            logg(
                `§ 8-10 ledd 2 punktum 1`(
                    erBegrenset = begrensning == ER_6G_BEGRENSET,
                    maksimaltSykepengegrunnlagÅrlig = `6G`.årlig,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig
                )
            )
        }

        if (selvstendigInntektsopplysning != null) {
            subsumsjonslogg.apply {
                logg(
                    `§ 8-35 ledd 2`(
                        pensjonsgivendeInntekter = selvstendigInntektsopplysning.faktaavklartInntekt.pensjonsgivendeInntekter.map {
                            PensjonsgivendeInntektSubsumsjon(
                                årstall = it.årstall,
                                pensjonsgivendeInntekt = it.beløp.årlig,
                                gjennomsnittligG = it.snitt.årlig
                            )
                        },
                        nåværendeGrunnbeløp = selvstendigInntektsopplysning.faktaavklartInntekt.anvendtGrunnbeløp.årlig,
                        skjæringstidspunkt = skjæringstidspunkt,
                        sykepengegrunnlag = sykepengegrunnlag.årlig
                    )
                )
            }
        }
    }

    internal fun beverte(builder: InntekterForBeregning.Builder) {
        arbeidsgiverInntektsopplysninger.beverte(builder)
        selvstendigInntektsopplysning?.beverte(builder)
        deaktiverteArbeidsforhold.beverteDeaktiverte(builder)
    }

    internal companion object {

        internal fun List<Inntektsgrunnlag>.harUlikeGrunnbeløp(): Boolean {
            return map { it.`6G` }.distinct().size > 1
        }

        fun opprett(
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
            deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonslogg: Subsumsjonslogg
        ): Inntektsgrunnlag {
            val alleInntekter = arbeidsgiverInntektsopplysninger + deaktiverteArbeidsforhold
            check(alleInntekter.distinctBy { it.orgnummer }.size == alleInntekter.size) {
                "det er oppgitt duplikat orgnumre i inntektsgrunnlaget: ${alleInntekter.joinToString { it.orgnummer }}"
            }
            return Inntektsgrunnlag(
                arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
                selvstendigInntektsopplysning = selvstendigInntektsopplysning,
                deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
                skjæringstidspunkt = skjæringstidspunkt,
                subsumsjonslogg = subsumsjonslogg
            )
        }

        fun gjenopprett(skjæringstidspunkt: LocalDate, dto: InntektsgrunnlagInnDto): Inntektsgrunnlag {
            return Inntektsgrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiverInntektsopplysninger = dto.arbeidsgiverInntektsopplysninger.map { ArbeidsgiverInntektsopplysning.gjenopprett(it) },
                selvstendigInntektsopplysning = dto.selvstendigInntektsopplysning?.let { SelvstendigInntektsopplysning.gjenopprett(it) },
                deaktiverteArbeidsforhold = dto.deaktiverteArbeidsforhold.map { ArbeidsgiverInntektsopplysning.gjenopprett(it) },
                vurdertInfotrygd = dto.vurdertInfotrygd,
                `6G` = Inntekt.gjenopprett(dto.`6G`)
            )
        }
    }

    internal fun view() = InntektsgrunnlagView(
        sykepengegrunnlag = sykepengegrunnlag,
        omregnetÅrsinntekt = omregnetÅrsinntekt,
        beregningsgrunnlag = beregningsgrunnlag,
        `6G` = `6G`,
        begrensning = begrensning,
        vurdertInfotrygd = vurdertInfotrygd,
        arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
        selvstendigInntektsopplysning = selvstendigInntektsopplysning,
        deaktiverteArbeidsgiverInntektsopplysninger = deaktiverteArbeidsforhold,
        deaktiverteArbeidsforhold = deaktiverteArbeidsforhold.map { it.orgnummer }
    )

    internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harInntekt(organisasjonsnummer)

    internal fun sjekkForNyArbeidsgiver(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening?, orgnummer: String) {
        if (opptjening == null) return
        when (opptjening) {
            is ArbeidstakerOpptjening -> arbeidsgiverInntektsopplysninger.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, orgnummer)
            is SelvstendigNæringsdrivendeOpptjening -> {}
        }
    }

    internal fun måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg: IAktivitetslogg, opptjening: ArbeidstakerOpptjening) {
        arbeidsgiverInntektsopplysninger.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjening)
    }

    internal fun aktiver(orgnummer: String, forklaring: String, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        if (arbeidsgiverInntektsopplysninger.any { it.gjelder(orgnummer) }) return this // Unngår å aktivere om det allerede er aktivt ettersom det ruller tilbake eventuell skjønnsmessig fastsettelse
        return deaktiverteArbeidsforhold.aktiver(arbeidsgiverInntektsopplysninger, orgnummer, forklaring, subsumsjonslogg)
            .let { (deaktiverte, aktiverte) ->
                kopierSykepengegrunnlag(
                    arbeidsgiverInntektsopplysninger = aktiverte,
                    selvstendigInntektsopplysning = this.selvstendigInntektsopplysning,
                    deaktiverteArbeidsforhold = deaktiverte
                )
            }
    }

    internal fun deaktiver(orgnummer: String, forklaring: String, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        if (deaktiverteArbeidsforhold.any { it.gjelder(orgnummer) }) return this // Unngår å deaktivere om det allerede er deaktivert ettersom det ruller tilbake eventuell skjønnsmessig fastsettelse
        return arbeidsgiverInntektsopplysninger.deaktiver(deaktiverteArbeidsforhold, orgnummer, forklaring, subsumsjonslogg)
            .let { (aktiverte, deaktiverte) ->
                kopierSykepengegrunnlag(
                    arbeidsgiverInntektsopplysninger = aktiverte,
                    selvstendigInntektsopplysning = this.selvstendigInntektsopplysning,
                    deaktiverteArbeidsforhold = deaktiverte
                )
            }
    }

    internal fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        return hendelse.overstyr(this, subsumsjonslogg)
    }

    internal fun overstyrArbeidsgiveropplysninger(hendelse: OverstyrArbeidsgiveropplysninger): EndretInntektsgrunnlag? {
        val resultat = this.arbeidsgiverInntektsopplysninger.overstyrMedSaksbehandler(hendelse.arbeidsgiveropplysninger)
        return lagEndring(resultat)
    }

    internal fun skjønnsmessigFastsettelse(hendelse: SkjønnsmessigFastsettelse): EndretInntektsgrunnlag? {
        val resultat = this.arbeidsgiverInntektsopplysninger.skjønnsfastsett(hendelse.arbeidsgiveropplysninger)
        return lagEndring(resultat)
    }

    internal fun nyeArbeidsgiverInntektsopplysninger(
        organisasjonsnummer: String,
        inntekt: ArbeidstakerFaktaavklartInntekt
    ): EndretInntektsgrunnlag? {
        val resultat = arbeidsgiverInntektsopplysninger.overstyrMedInntektsmelding(organisasjonsnummer, inntekt)
        return lagEndring(resultat)
    }

    private fun lagEndring(nyeInntekter: List<ArbeidsgiverInntektsopplysning>): EndretInntektsgrunnlag? {
        val nyttInntektsgrunnlag = kopierSykepengegrunnlagHvisFunksjonellEndring(nyeInntekter, this.selvstendigInntektsopplysning, deaktiverteArbeidsforhold) ?: return null
        return EndretInntektsgrunnlag(
            inntekter = nyeInntekter.mapNotNull { potensiellEndret ->
                val eksisterende = arbeidsgiverInntektsopplysninger.single { eksisterende -> potensiellEndret.orgnummer == eksisterende.orgnummer }

                if (eksisterende.faktaavklartInntekt.id == potensiellEndret.faktaavklartInntekt.id && eksisterende.korrigertInntekt == potensiellEndret.korrigertInntekt) null
                else EndretInntektsgrunnlag.EndretInntekt(
                    inntektFør = eksisterende,
                    inntektEtter = potensiellEndret
                )
            },
            inntektsgrunnlagFør = this,
            inntektsgrunnlagEtter = nyttInntektsgrunnlag
        )
    }

    private fun kopierSykepengegrunnlagHvisFunksjonellEndring(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>
    ): Inntektsgrunnlag? {
        if (!arbeidsgiverInntektsopplysninger.harFunksjonellEndring(this.arbeidsgiverInntektsopplysninger)) return null
        return kopierSykepengegrunnlag(arbeidsgiverInntektsopplysninger, selvstendigInntektsopplysning, deaktiverteArbeidsforhold)
    }

    private fun kopierSykepengegrunnlag(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        nyttSkjæringstidspunkt: LocalDate = skjæringstidspunkt
    ) = Inntektsgrunnlag(
        skjæringstidspunkt = nyttSkjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
        selvstendigInntektsopplysning = selvstendigInntektsopplysning,
        deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
        vurdertInfotrygd = vurdertInfotrygd
    )

    internal fun grunnbeløpsregulering(): Inntektsgrunnlag? {
        val nyttInntektsgrunnlag = kopierSykepengegrunnlag(arbeidsgiverInntektsopplysninger, selvstendigInntektsopplysning, deaktiverteArbeidsforhold)
        if (this.`6G` == nyttInntektsgrunnlag.`6G`) return null
        return nyttInntektsgrunnlag
    }

    internal fun erArbeidsgiverRelevant(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.any { it.gjelder(organisasjonsnummer) }

    internal fun berik(builder: UtkastTilVedtakBuilder) {
        builder.sykepengegrunnlag(
            sykepengegrunnlag = sykepengegrunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
            totalOmregnetÅrsinntekt = omregnetÅrsinntekt,
            seksG = `6G`,
            inngangsvilkårFraInfotrygd = vurdertInfotrygd
        )
        arbeidsgiverInntektsopplysninger.berik(builder)
        selvstendigInntektsopplysning?.berik(builder)
    }

    override fun compareTo(other: Inntekt) = this.sykepengegrunnlag.compareTo(other)
    internal fun er6GBegrenset() = begrensning == ER_6G_BEGRENSET

    fun harGjenbrukbarInntekt(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harGjenbrukbarInntekt(organisasjonsnummer)

    fun lagreTidsnæreInntekter(skjæringstidspunkt: LocalDate, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, nyArbeidsgiverperiode: Boolean) {
        arbeidsgiverInntektsopplysninger.lagreTidsnæreInntekter(skjæringstidspunkt, yrkesaktivitet, aktivitetslogg, nyArbeidsgiverperiode)
    }

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

    internal fun dto() = InntektsgrunnlagUtDto(
        arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.dto() },
        selvstendigInntektsopplysning = this.selvstendigInntektsopplysning?.dto(),
        deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.dto() },
        vurdertInfotrygd = this.vurdertInfotrygd,
        `6G` = this.`6G`.dto(),
        sykepengegrunnlag = this.sykepengegrunnlag.dto(),
        totalOmregnetÅrsinntekt = this.omregnetÅrsinntekt.dto(),
        beregningsgrunnlag = this.beregningsgrunnlag.dto(),
        er6GBegrenset = beregningsgrunnlag > this.`6G`
    )
}

internal data class EndretInntektsgrunnlag(
    val inntekter: List<EndretInntekt>,
    val inntektsgrunnlagFør: Inntektsgrunnlag,
    val inntektsgrunnlagEtter: Inntektsgrunnlag
) {
    data class EndretInntekt(
        val inntektFør: ArbeidsgiverInntektsopplysning,
        val inntektEtter: ArbeidsgiverInntektsopplysning,
    )
}

internal data class InntektsgrunnlagView(
    val sykepengegrunnlag: Inntekt,
    val omregnetÅrsinntekt: Inntekt,
    val beregningsgrunnlag: Inntekt,
    val `6G`: Inntekt,
    val begrensning: Inntektsgrunnlag.Begrensning,
    val vurdertInfotrygd: Boolean,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val selvstendigInntektsopplysning: SelvstendigInntektsopplysning?,
    val deaktiverteArbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val deaktiverteArbeidsforhold: List<String>
)
