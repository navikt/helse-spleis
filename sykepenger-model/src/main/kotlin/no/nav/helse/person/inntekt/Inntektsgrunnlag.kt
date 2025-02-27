package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Grunnbeløp
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.dto.deserialisering.InntektsgrunnlagInnDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-10 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-3 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Opptjening
import no.nav.helse.person.UtbetalingInntektskilde
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.berik
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.beverte
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.beverteDeaktiverte
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.deaktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.faktaavklarteInntekter
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.fastsattÅrsinntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.finnEndringsdato
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
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.VilkårsprøvdSkjæringstidspunkt
import no.nav.helse.økonomi.Inntekt

internal class Inntektsgrunnlag private constructor(
    private val alder: Alder,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    private val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
    private val vurdertInfotrygd: Boolean,
    `6G`: Inntekt? = null
) : Comparable<Inntekt> {

    init {
        arbeidsgiverInntektsopplysninger.validerSkjønnsmessigAltEllerIntet()
    }

    private val `6G`: Inntekt = `6G` ?: Grunnbeløp.`6G`.beløp(skjæringstidspunkt, LocalDate.now())

    // sum av alle inntekter foruten skjønnsmessig fastsatt beløp; da brukes inntekten den fastsatte
    private val omregnetÅrsinntekt = arbeidsgiverInntektsopplysninger.totalOmregnetÅrsinntekt(skjæringstidspunkt)

    // summen av alle inntekter
    private val beregningsgrunnlag = arbeidsgiverInntektsopplysninger.fastsattÅrsinntekt(skjæringstidspunkt)
    private val sykepengegrunnlag = beregningsgrunnlag.coerceAtMost(this.`6G`)
    private val begrensning = if (vurdertInfotrygd) VURDERT_I_INFOTRYGD else if (beregningsgrunnlag > this.`6G`) ER_6G_BEGRENSET else ER_IKKE_6G_BEGRENSET

    private val forhøyetInntektskrav = alder.forhøyetInntektskrav(skjæringstidspunkt)
    private val minsteinntekt = (if (forhøyetInntektskrav) `2G` else halvG).minsteinntekt(skjæringstidspunkt)
    private val oppfyllerMinsteinntektskrav = beregningsgrunnlag >= minsteinntekt

    internal constructor(
        alder: Alder,
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        subsumsjonslogg: Subsumsjonslogg,
        vurdertInfotrygd: Boolean = false
    ) : this(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd) {
        subsumsjonslogg.apply {
            logg(
                `§ 8-10 ledd 2 punktum 1`(
                    erBegrenset = begrensning == ER_6G_BEGRENSET,
                    maksimaltSykepengegrunnlagÅrlig = `6G`.årlig,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig
                )
            )
            subsummerMinsteSykepengegrunnlag(alder, skjæringstidspunkt, this)
        }
    }

    private fun subsummerMinsteSykepengegrunnlag(
        alder: Alder,
        skjæringstidspunkt: LocalDate,
        subsumsjonslogg: Subsumsjonslogg
    ) {
        if (alder.forhøyetInntektskrav(skjæringstidspunkt))
            subsumsjonslogg.logg(
                `§ 8-51 ledd 2`(
                    oppfylt = oppfyllerMinsteinntektskrav,
                    skjæringstidspunkt = skjæringstidspunkt,
                    alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt),
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = minsteinntekt.årlig
                )
            )
        else
            subsumsjonslogg.logg(
                `§ 8-3 ledd 2 punktum 1`(
                    oppfylt = oppfyllerMinsteinntektskrav,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = minsteinntekt.årlig
                )
            )
    }

    internal fun beverte(builder: InntekterForBeregning.Builder) {
        builder.medGjeldende6G(`6G`)
        arbeidsgiverInntektsopplysninger.beverte(builder)
        deaktiverteArbeidsforhold.beverteDeaktiverte(builder)
    }

    internal companion object {

        internal fun List<Inntektsgrunnlag>.harUlikeGrunnbeløp(): Boolean {
            return map { it.`6G` }.distinct().size > 1
        }

        fun opprett(
            alder: Alder,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            subsumsjonslogg: Subsumsjonslogg
        ): Inntektsgrunnlag {
            val alleInntekter = arbeidsgiverInntektsopplysninger + deaktiverteArbeidsforhold
            check(alleInntekter.distinctBy { it.orgnummer }.size == alleInntekter.size) {
                "det er oppgitt duplikat orgnumre i inntektsgrunnlaget: ${alleInntekter.joinToString { it.orgnummer }}"
            }
            return Inntektsgrunnlag(
                alder = alder,
                arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
                deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
                skjæringstidspunkt = skjæringstidspunkt,
                subsumsjonslogg = subsumsjonslogg
            )
        }

        fun ferdigSykepengegrunnlag(
            alder: Alder,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
            vurdertInfotrygd: Boolean,
            `6G`: Inntekt? = null
        ): Inntektsgrunnlag {
            return Inntektsgrunnlag(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd, `6G`)
        }

        fun gjenopprett(alder: Alder, skjæringstidspunkt: LocalDate, dto: InntektsgrunnlagInnDto): Inntektsgrunnlag {
            return Inntektsgrunnlag(
                alder = alder,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgiverInntektsopplysninger = dto.arbeidsgiverInntektsopplysninger.map { ArbeidsgiverInntektsopplysning.gjenopprett(it) },
                deaktiverteArbeidsforhold = dto.deaktiverteArbeidsforhold.map { ArbeidsgiverInntektsopplysning.gjenopprett(it) },
                vurdertInfotrygd = dto.vurdertInfotrygd,
                `6G` = Inntekt.gjenopprett(dto.`6G`)
            )
        }
    }

    internal fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunktperiode: Periode, periode: Periode, subsumsjonslogg: Subsumsjonslogg): List<Utbetalingstidslinje> {
        val tidslinjeperiode = Utbetalingstidslinje.periode(tidslinjer) ?: return tidslinjer
        if (tidslinjeperiode.starterEtter(skjæringstidspunktperiode) || tidslinjeperiode.endInclusive < skjæringstidspunkt) return tidslinjer

        val avvisningsperiode = skjæringstidspunktperiode.start til minOf(tidslinjeperiode.endInclusive, skjæringstidspunktperiode.endInclusive)
        val avvisteDager = avvisningsperiode.filter { dato ->
            val faktor = if (alder.forhøyetInntektskrav(dato)) `2G` else halvG
            beregningsgrunnlag < faktor.minsteinntekt(skjæringstidspunkt)
        }
        if (avvisteDager.isEmpty()) return tidslinjer
        val (avvisteDagerOver67, avvisteDagerTil67) = avvisteDager.partition { alder.forhøyetInntektskrav(it) }

        if (avvisteDagerOver67.isNotEmpty()) {
            alder.fraOgMedFylte67(
                oppfylt = false,
                utfallFom = avvisteDagerOver67.min(),
                utfallTom = avvisteDagerOver67.max(),
                periodeFom = periode.start,
                periodeTom = periode.endInclusive,
                beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                minimumInntektÅrlig = `2G`.minsteinntekt(avvisteDagerOver67.min()).årlig,
                jurist = subsumsjonslogg
            )
        }
        val dager = listOf(
            Begrunnelse.MinimumInntektOver67 to avvisteDagerOver67.grupperSammenhengendePerioder(),
            Begrunnelse.MinimumInntekt to avvisteDagerTil67.grupperSammenhengendePerioder()
        )
        return dager.fold(tidslinjer) { result, (begrunnelse, perioder) ->
            Utbetalingstidslinje.avvis(result, perioder, listOf(begrunnelse))
        }
    }

    internal fun view() = InntektsgrunnlagView(
        sykepengegrunnlag = sykepengegrunnlag,
        omregnetÅrsinntekt = omregnetÅrsinntekt,
        beregningsgrunnlag = beregningsgrunnlag,
        `6G` = `6G`,
        begrensning = begrensning,
        vurdertInfotrygd = vurdertInfotrygd,
        minsteinntekt = minsteinntekt,
        oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav,
        arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
        deaktiverteArbeidsgiverInntektsopplysninger = deaktiverteArbeidsforhold,
        deaktiverteArbeidsforhold = deaktiverteArbeidsforhold.map { it.orgnummer }
    )

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        if (oppfyllerMinsteinntektskrav) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
        else aktivitetslogg.varsel(RV_SV_1)
        return oppfyllerMinsteinntektskrav && !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harInntekt(organisasjonsnummer)

    internal fun sjekkForNyArbeidsgiver(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening?, orgnummer: String) {
        if (opptjening == null) return
        arbeidsgiverInntektsopplysninger.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, orgnummer)
    }

    internal fun måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening?) {
        if (opptjening == null) return
        arbeidsgiverInntektsopplysninger.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjening)
    }

    internal fun aktiver(orgnummer: String, forklaring: String, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        if (arbeidsgiverInntektsopplysninger.any { it.gjelder(orgnummer) }) return this // Unngår å aktivere om det allerede er aktivt ettersom det ruller tilbake eventuell skjønnsmessig fastsettelse
        return deaktiverteArbeidsforhold.aktiver(arbeidsgiverInntektsopplysninger, orgnummer, forklaring, subsumsjonslogg)
            .let { (deaktiverte, aktiverte) ->
                kopierSykepengegrunnlag(
                    arbeidsgiverInntektsopplysninger = aktiverte,
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
                    deaktiverteArbeidsforhold = deaktiverte
                )
            }
    }

    internal fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        return hendelse.overstyr(this, subsumsjonslogg)
    }

    internal fun overstyrArbeidsgiveropplysninger(hendelse: OverstyrArbeidsgiveropplysninger, subsumsjonslogg: Subsumsjonslogg): EndretInntektsgrunnlag? {
        val resultat = this.arbeidsgiverInntektsopplysninger.overstyrMedSaksbehandler(hendelse.arbeidsgiveropplysninger)
        return lagEndring(resultat, subsumsjonslogg)
    }

    internal fun skjønnsmessigFastsettelse(hendelse: SkjønnsmessigFastsettelse, subsumsjonslogg: Subsumsjonslogg): EndretInntektsgrunnlag? {
        val resultat = this.arbeidsgiverInntektsopplysninger.skjønnsfastsett(hendelse.arbeidsgiveropplysninger)
        return lagEndring(resultat, subsumsjonslogg)
    }

    internal fun nyeArbeidsgiverInntektsopplysninger(
        organisasjonsnummer: String,
        inntekt: FaktaavklartInntekt,
        subsumsjonslogg: Subsumsjonslogg
    ): EndretInntektsgrunnlag? {
        val resultat = arbeidsgiverInntektsopplysninger.overstyrMedInntektsmelding(organisasjonsnummer, inntekt)
        return lagEndring(resultat, subsumsjonslogg)
    }

    private fun lagEndring(nyeInntekter: List<ArbeidsgiverInntektsopplysning>, subsumsjonslogg: Subsumsjonslogg): EndretInntektsgrunnlag? {
        val nyttInntektsgrunnlag = kopierSykepengegrunnlagOgValiderMinsteinntekt(nyeInntekter, deaktiverteArbeidsforhold, subsumsjonslogg)
        val endringFom = nyttInntektsgrunnlag.finnEndringsdato(this) ?: return null
        return EndretInntektsgrunnlag(
            inntekter = nyeInntekter.mapNotNull { potensiellEndret ->
                val eksisterende = arbeidsgiverInntektsopplysninger.single { eksisterende -> potensiellEndret.orgnummer == eksisterende.orgnummer }

                if (eksisterende.faktaavklartInntekt.id == potensiellEndret.faktaavklartInntekt.id && eksisterende.korrigertInntekt == potensiellEndret.korrigertInntekt) null
                else EndretInntektsgrunnlag.EndretInntekt(
                    inntektFør = eksisterende,
                    inntektEtter = potensiellEndret
                )
            },
            endringFom = endringFom,
            inntektsgrunnlagFør = this,
            inntektsgrunnlagEtter = nyttInntektsgrunnlag
        )
    }

    private fun kopierSykepengegrunnlagOgValiderMinsteinntekt(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        subsumsjonslogg: Subsumsjonslogg
    ): Inntektsgrunnlag {
        return kopierSykepengegrunnlag(arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold).apply {
            subsummerMinsteSykepengegrunnlag(alder, skjæringstidspunkt, subsumsjonslogg)
        }
    }

    private fun kopierSykepengegrunnlag(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        nyttSkjæringstidspunkt: LocalDate = skjæringstidspunkt
    ) = Inntektsgrunnlag(
        alder = alder,
        skjæringstidspunkt = nyttSkjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
        deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
        vurdertInfotrygd = vurdertInfotrygd
    )

    internal fun grunnbeløpsregulering(): Inntektsgrunnlag? {
        val nyttInntektsgrunnlag = kopierSykepengegrunnlag(arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold)
        if (this.`6G` == nyttInntektsgrunnlag.`6G`) return null
        return nyttInntektsgrunnlag
    }

    internal fun inntektskilde() = when {
        arbeidsgiverInntektsopplysninger.size > 1 -> UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE
        else -> UtbetalingInntektskilde.EN_ARBEIDSGIVER
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
    }

    override fun compareTo(other: Inntekt) = this.sykepengegrunnlag.compareTo(other)
    internal fun er6GBegrenset() = begrensning == ER_6G_BEGRENSET
    private fun finnEndringsdato(other: Inntektsgrunnlag): LocalDate? {
        check(this.skjæringstidspunkt == other.skjæringstidspunkt) {
            "Skal bare sammenlikne med samme skjæringstidspunkt"
        }
        return arbeidsgiverInntektsopplysninger.finnEndringsdato(other.arbeidsgiverInntektsopplysninger)
    }

    fun harGjenbrukbarInntekt(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harGjenbrukbarInntekt(organisasjonsnummer)

    fun lagreTidsnæreInntekter(skjæringstidspunkt: LocalDate, arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg, nyArbeidsgiverperiode: Boolean) {
        arbeidsgiverInntektsopplysninger.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver, aktivitetslogg, nyArbeidsgiverperiode)
    }

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

    internal fun dto() = InntektsgrunnlagUtDto(
        arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.dto() },
        deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.dto() },
        vurdertInfotrygd = this.vurdertInfotrygd,
        `6G` = this.`6G`.dto(),
        sykepengegrunnlag = this.sykepengegrunnlag.dto(),
        totalOmregnetÅrsinntekt = this.omregnetÅrsinntekt.dto(),
        beregningsgrunnlag = this.beregningsgrunnlag.dto(),
        er6GBegrenset = beregningsgrunnlag > this.`6G`,
        forhøyetInntektskrav = this.forhøyetInntektskrav,
        minsteinntekt = this.minsteinntekt.dto(),
        oppfyllerMinsteinntektskrav = this.oppfyllerMinsteinntektskrav
    )

    internal fun faktaavklarteInntekter() = VilkårsprøvdSkjæringstidspunkt(
        `6G` = `6G`,
        inntekter = arbeidsgiverInntektsopplysninger.faktaavklarteInntekter(),
        deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.orgnummer }
    )
}

internal data class EndretInntektsgrunnlag(
    val inntekter: List<EndretInntekt>,
    val endringFom: LocalDate,
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
    val minsteinntekt: Inntekt,
    val oppfyllerMinsteinntektskrav: Boolean,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val deaktiverteArbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    val deaktiverteArbeidsforhold: List<String>
)
