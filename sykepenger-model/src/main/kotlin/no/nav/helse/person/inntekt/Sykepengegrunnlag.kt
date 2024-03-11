package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Grunnbeløp
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.GjenopplivVilkårsgrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.til
import no.nav.helse.memento.SykepengegrunnlagMemento
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.SykepengegrunnlagVisitor
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.UtbetalingInntektskilde
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.build
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.deaktiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.fastsattÅrsinntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.finn
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.finnEndringsdato
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.harInntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.ingenRefusjonsopplysninger
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.inntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.inntektsdata
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.lagreTidsnæreInntekter
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.markerFlereArbeidsgivere
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.medInntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.medUtbetalingsopplysninger
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.måHaRegistrertOpptjeningForArbeidsgivere
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.omregnedeÅrsinntekter
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrInntekter
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.sjekkForNyArbeidsgiver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.subsummer
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.totalOmregnetÅrsinntekt
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.validerSkjønnsmessigAltEllerIntet
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.person.inntekt.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.inntekt.Sykepengegrunnlag.Begrensning.VURDERT_I_INFOTRYGD
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

internal class Sykepengegrunnlag private constructor(
    private val alder: Alder,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
    private val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
    private val vurdertInfotrygd: Boolean,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
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
    private val avviksprosent = sammenligningsgrunnlag.avviksprosent(omregnetÅrsinntekt)

    internal constructor(
        alder: Alder,
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        subsumsjonObserver: SubsumsjonObserver,
        vurdertInfotrygd: Boolean = false
    ) : this(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, emptyList(), vurdertInfotrygd, sammenligningsgrunnlag) {
        subsumsjonObserver.apply {
            arbeidsgiverInntektsopplysninger.subsummer(this, forrige = emptyList())
            `§ 8-10 ledd 2 punktum 1`(
                erBegrenset = begrensning == ER_6G_BEGRENSET,
                maksimaltSykepengegrunnlagÅrlig = `6G`.reflection { årlig, _, _, _ -> årlig },
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlagÅrlig = beregningsgrunnlag.reflection { årlig, _, _, _ -> årlig }
            )
            subsummerMinsteSykepengegrunnlag(alder, skjæringstidspunkt, this)
        }
    }

    private fun subsummerMinsteSykepengegrunnlag(
        alder: Alder,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        if (alder.forhøyetInntektskrav(skjæringstidspunkt))
            subsumsjonObserver.`§ 8-51 ledd 2`(
                oppfylt = oppfyllerMinsteinntektskrav,
                skjæringstidspunkt = skjæringstidspunkt,
                alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt),
                beregningsgrunnlagÅrlig = beregningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                minimumInntektÅrlig = minsteinntekt.reflection { årlig, _, _, _ -> årlig })
        else
            subsumsjonObserver.`§ 8-3 ledd 2 punktum 1`(
                oppfylt = oppfyllerMinsteinntektskrav,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlagÅrlig = beregningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                minimumInntektÅrlig = minsteinntekt.reflection { årlig, _, _, _ -> årlig })
    }

    internal companion object {
        fun opprett(
            alder: Alder,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            skjæringstidspunkt: LocalDate,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            subsumsjonObserver: SubsumsjonObserver
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(
                alder,
                arbeidsgiverInntektsopplysninger,
                skjæringstidspunkt,
                sammenligningsgrunnlag,
                subsumsjonObserver
            )
        }

        fun ferdigSykepengegrunnlag(
            alder: Alder,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
            deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
            vurdertInfotrygd: Boolean,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            `6G`: Inntekt? = null
        ): Sykepengegrunnlag {
            return Sykepengegrunnlag(alder, skjæringstidspunkt, arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, vurdertInfotrygd, sammenligningsgrunnlag, `6G`)
        }
    }

    internal fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunktperiode: Periode, periode: Periode, subsumsjonObserver: SubsumsjonObserver): List<Utbetalingstidslinje> {
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
                beregningsgrunnlagÅrlig = beregningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                minimumInntektÅrlig = `2G`.minsteinntekt(avvisteDagerOver67.min()).reflection { årlig, _, _, _ -> årlig },
                jurist = subsumsjonObserver
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

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        if (oppfyllerMinsteinntektskrav) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
        else aktivitetslogg.varsel(RV_SV_1)
        return oppfyllerMinsteinntektskrav && !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }


    internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.harInntekt(organisasjonsnummer)

    internal fun sjekkForNyeArbeidsgivere(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: List<String>) {
        val manglerInntekt = organisasjonsnummer.filterNot { harNødvendigInntektForVilkårsprøving(it) }.takeUnless { it.isEmpty() } ?: return
        manglerInntekt.forEach {
            aktivitetslogg.info("Mangler inntekt for $it på skjæringstidspunkt $skjæringstidspunkt")
        }
        aktivitetslogg.varsel(RV_SV_2)
    }

    internal fun sjekkForNyArbeidsgiver(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening?, orgnummer: String) {
        if (opptjening == null) return
        arbeidsgiverInntektsopplysninger.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, orgnummer)
    }

    internal fun måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening?) {
        if (opptjening == null) return
        arbeidsgiverInntektsopplysninger.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjening)
    }

    internal fun markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
        arbeidsgiverInntektsopplysninger.markerFlereArbeidsgivere(aktivitetslogg)
    }

    internal fun aktiver(orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver) =
        deaktiverteArbeidsforhold.aktiver(arbeidsgiverInntektsopplysninger, orgnummer, forklaring, subsumsjonObserver)
            .let { (deaktiverte, aktiverte) ->
                kopierSykepengegrunnlag(
                    arbeidsgiverInntektsopplysninger = aktiverte,
                    deaktiverteArbeidsforhold = deaktiverte
                )
            }

    internal fun deaktiver(orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver) =
        arbeidsgiverInntektsopplysninger.deaktiver(deaktiverteArbeidsforhold, orgnummer, forklaring, subsumsjonObserver)
            .let { (aktiverte, deaktiverte) ->
                kopierSykepengegrunnlag(
                    arbeidsgiverInntektsopplysninger = aktiverte,
                    deaktiverteArbeidsforhold = deaktiverte
                )
            }

    internal fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        return hendelse.overstyr(this, subsumsjonObserver)
    }

    internal fun overstyrArbeidsgiveropplysninger(person: Person, hendelse: OverstyrArbeidsgiveropplysninger, opptjening: Opptjening?, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        val builder = ArbeidsgiverInntektsopplysningerOverstyringer(arbeidsgiverInntektsopplysninger, opptjening, subsumsjonObserver)
        hendelse.overstyr(builder)
        val resultat = builder.resultat()
        arbeidsgiverInntektsopplysninger.forEach { it.arbeidsgiveropplysningerKorrigert(person, hendelse) }
        return kopierSykepengegrunnlagOgValiderMinsteinntekt(resultat, deaktiverteArbeidsforhold, subsumsjonObserver)
    }

    internal fun gjenoppliv(hendelse: GjenopplivVilkårsgrunnlag, nyttSkjæringstidspunkt: LocalDate?): Sykepengegrunnlag? {
        val skjæringstidspunkt = nyttSkjæringstidspunkt ?: this.skjæringstidspunkt
        val nyeArbeidsgiverInntektsopplysninger = hendelse.arbeidsgiverinntektsopplysninger(skjæringstidspunkt)
        if (arbeidsgiverInntektsopplysninger.isNotEmpty() && nyeArbeidsgiverInntektsopplysninger.isNotEmpty()) {
            hendelse.info("Kan ikke gjenopplive sykepengegrunnlag med nye inntektsopplysninger hvor det allerede foreligger innteksopplysninger.")
            return null
        }

        val gjenopplivetArbeidsgiverInntektsopplysninger = nyeArbeidsgiverInntektsopplysninger.takeUnless { it.isEmpty() } ?: arbeidsgiverInntektsopplysninger.map { it.gjenoppliv(this.skjæringstidspunkt, skjæringstidspunkt) }

        if (gjenopplivetArbeidsgiverInntektsopplysninger.isEmpty()) {
            hendelse.info("Kan ikke gjenopplive sykepengegrunnlag uten inntektsopplysninger.")
            return null
        }
        return kopierSykepengegrunnlag(gjenopplivetArbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold, skjæringstidspunkt)
    }

    internal fun skjønnsmessigFastsettelse(hendelse: SkjønnsmessigFastsettelse, opptjening: Opptjening?, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        val builder = ArbeidsgiverInntektsopplysningerOverstyringer(arbeidsgiverInntektsopplysninger, opptjening, subsumsjonObserver)
        hendelse.overstyr(builder)
        val resultat = builder.resultat()
        return kopierSykepengegrunnlagOgValiderMinsteinntekt(resultat, deaktiverteArbeidsforhold, subsumsjonObserver)
    }

    internal fun refusjonsopplysninger(organisasjonsnummer: String): Refusjonsopplysninger =
        arbeidsgiverInntektsopplysninger.refusjonsopplysninger(organisasjonsnummer)

    internal fun inntekt(organisasjonsnummer: String): Inntekt? =
        arbeidsgiverInntektsopplysninger.inntekt(organisasjonsnummer)

    internal fun nyeArbeidsgiverInntektsopplysninger(
        person: Person,
        inntektsmelding: Inntektsmelding,
        subsumsjonObserver: SubsumsjonObserver
    ): Sykepengegrunnlag {
        val builder = ArbeidsgiverInntektsopplysningerOverstyringer(arbeidsgiverInntektsopplysninger, null, subsumsjonObserver)
        inntektsmelding.nyeArbeidsgiverInntektsopplysninger(builder, skjæringstidspunkt)
        val resultat = builder.resultat()
        arbeidsgiverInntektsopplysninger
            .finn(inntektsmelding.organisasjonsnummer())
            ?.arbeidsgiveropplysningerKorrigert(person, inntektsmelding)
        return kopierSykepengegrunnlagOgValiderMinsteinntekt(resultat, deaktiverteArbeidsforhold, subsumsjonObserver)
    }

    private fun kopierSykepengegrunnlagOgValiderMinsteinntekt(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        subsumsjonObserver: SubsumsjonObserver
    ): Sykepengegrunnlag {
        return kopierSykepengegrunnlag(arbeidsgiverInntektsopplysninger, deaktiverteArbeidsforhold).apply {
           subsummerMinsteSykepengegrunnlag(alder, skjæringstidspunkt, subsumsjonObserver)
        }
    }

    private fun kopierSykepengegrunnlag(
        arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysning>,
        nyttSkjæringstidspunkt: LocalDate = skjæringstidspunkt
    ) = Sykepengegrunnlag(
            alder = alder,
            skjæringstidspunkt = nyttSkjæringstidspunkt,
            arbeidsgiverInntektsopplysninger = arbeidsgiverInntektsopplysninger,
            deaktiverteArbeidsforhold = deaktiverteArbeidsforhold,
            vurdertInfotrygd = vurdertInfotrygd,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )

    internal fun grunnbeløpsregulering() = kopierSykepengegrunnlag(
        arbeidsgiverInntektsopplysninger,
        deaktiverteArbeidsforhold
    )

    internal fun accept(visitor: SykepengegrunnlagVisitor) {
        visitor.preVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            avviksprosent,
            omregnetÅrsinntekt,
            beregningsgrunnlag,
            `6G`,
            begrensning,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
        visitor.preVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektsopplysninger)
        arbeidsgiverInntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgiverInntektsopplysninger(arbeidsgiverInntektsopplysninger)
        sammenligningsgrunnlag.accept(visitor)
        visitor.preVisitDeaktiverteArbeidsgiverInntektsopplysninger(deaktiverteArbeidsforhold)
        deaktiverteArbeidsforhold.forEach { it.accept(visitor) }
        visitor.postVisitDeaktiverteArbeidsgiverInntektsopplysninger(deaktiverteArbeidsforhold)
        visitor.postVisitSykepengegrunnlag(
            this,
            skjæringstidspunkt,
            sykepengegrunnlag,
            avviksprosent,
            omregnetÅrsinntekt,
            beregningsgrunnlag,
            `6G`,
            begrensning,
            vurdertInfotrygd,
            minsteinntekt,
            oppfyllerMinsteinntektskrav
        )
    }

    internal fun inntektskilde() = when {
        arbeidsgiverInntektsopplysninger.size > 1 -> UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE
        else -> UtbetalingInntektskilde.EN_ARBEIDSGIVER
    }

    internal fun erArbeidsgiverRelevant(organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.any { it.gjelder(organisasjonsnummer) } || sammenligningsgrunnlag.erRelevant(organisasjonsnummer)

    internal fun utenInntekt(økonomi: Økonomi): Økonomi {
        return økonomi.inntekt(
            aktuellDagsinntekt = INGEN,
            dekningsgrunnlag = INGEN,
            `6G` = `6G`,
            refusjonsbeløp = INGEN
        )
    }
    internal fun medInntekt(organisasjonsnummer: String, dato: LocalDate, økonomi: Økonomi, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi {
        return arbeidsgiverInntektsopplysninger.medInntekt(organisasjonsnummer, `6G`, skjæringstidspunkt, dato, økonomi, regler, subsumsjonObserver) ?: utenInntekt(økonomi)
    }

    internal fun medUtbetalingsopplysninger(organisasjonsnummer: String, dato: LocalDate, økonomi: Økonomi, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver) =
        arbeidsgiverInntektsopplysninger.medUtbetalingsopplysninger(organisasjonsnummer, `6G`, skjæringstidspunkt, dato, økonomi, regler, subsumsjonObserver)

    internal fun build(builder: VedtakFattetBuilder) {
        val fakta = when (vurdertInfotrygd) {
            true -> VedtakFattetBuilder.FastsattIInfotrygdBuilder(omregnetÅrsinntekt)
            false -> VedtakFattetBuilder.FastsattISpleisBuilder(omregnetÅrsinntekt, `6G`, begrensning).apply {
                arbeidsgiverInntektsopplysninger.build(this)
            }
        }.build()
        // TODO: alt sykepengegrunnlagrelatert burde kanskje vært inni én fakta-ting
        builder
            .sykepengegrunnlag(this.sykepengegrunnlag)
            .beregningsgrunnlag(this.beregningsgrunnlag)
            .begrensning(this.begrensning)
            .sykepengegrunnlagsfakta(fakta)
        if (`2G`.beløp(skjæringstidspunkt, LocalDate.now()) > this.sykepengegrunnlag) {
            builder.sykepengergrunnlagErUnder2G()
        }
    }
    override fun equals(other: Any?): Boolean {
        if (other !is Sykepengegrunnlag) return false
        return sykepengegrunnlag == other.sykepengegrunnlag
                 && arbeidsgiverInntektsopplysninger == other.arbeidsgiverInntektsopplysninger
                 && beregningsgrunnlag == other.beregningsgrunnlag
                 && begrensning == other.begrensning
                 && deaktiverteArbeidsforhold == other.deaktiverteArbeidsforhold
    }

    override fun hashCode(): Int {
        var result = sykepengegrunnlag.hashCode()
        result = 31 * result + arbeidsgiverInntektsopplysninger.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        result = 31 * result + begrensning.hashCode()
        result = 31 * result + deaktiverteArbeidsforhold.hashCode()
        return result
    }

    override fun compareTo(other: Inntekt) = this.sykepengegrunnlag.compareTo(other)

    internal fun er6GBegrenset() = begrensning == ER_6G_BEGRENSET

    internal fun finnEndringsdato(other: Sykepengegrunnlag): LocalDate {
        check(this.skjæringstidspunkt == other.skjæringstidspunkt) {
            "Skal bare sammenlikne med samme skjæringstidspunkt"
        }
        return arbeidsgiverInntektsopplysninger.finnEndringsdato(this.skjæringstidspunkt, other.arbeidsgiverInntektsopplysninger)
    }

    fun lagreTidsnæreInntekter(skjæringstidspunkt: LocalDate, arbeidsgiver: Arbeidsgiver, hendelse: IAktivitetslogg, oppholdsperiodeMellom: Periode?) {
        arbeidsgiverInntektsopplysninger.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
    }

    internal fun byggGodkjenningsbehov(builder: GodkjenningsbehovBuilder) {
        builder.inntektskilde(inntektskilde())
        builder.tagFlereArbeidsgivere(arbeidsgiverInntektsopplysninger.size)
        arbeidsgiverInntektsopplysninger.omregnedeÅrsinntekter(builder)
    }

    enum class Begrensning {
        ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD
    }

    internal class ArbeidsgiverInntektsopplysningerOverstyringer(
        private val opprinneligArbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysning>,
        private val opptjening: Opptjening?,
        private val subsumsjonObserver: SubsumsjonObserver
    ) {
        private val nyeInntektsopplysninger = mutableListOf<ArbeidsgiverInntektsopplysning>()

        internal fun leggTilInntekt(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning) {
            nyeInntektsopplysninger.add(arbeidsgiverInntektsopplysning)
        }

        internal fun ingenRefusjonsopplysninger(organisasjonsnummer: String) = opprinneligArbeidsgiverInntektsopplysninger.ingenRefusjonsopplysninger(organisasjonsnummer)

        internal fun resultat(): List<ArbeidsgiverInntektsopplysning> {
            return opprinneligArbeidsgiverInntektsopplysninger.overstyrInntekter(opptjening, nyeInntektsopplysninger, subsumsjonObserver)
        }
    }

    internal fun inntektsdata(skjæringstidspunkt: LocalDate, organisasjonsnummer: String) =
        arbeidsgiverInntektsopplysninger.inntektsdata(skjæringstidspunkt, organisasjonsnummer)

    internal fun ghosttidslinje(organisasjonsnummer: String, sisteDag: LocalDate) =
        arbeidsgiverInntektsopplysninger.firstNotNullOfOrNull { it.ghosttidslinje(organisasjonsnummer, sisteDag) }

    internal fun memento() = SykepengegrunnlagMemento(
        arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.memento() },
        deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.memento() },
        vurdertInfotrygd = this.vurdertInfotrygd,
        sammenligningsgrunnlag = this.sammenligningsgrunnlag.memento(),
        `6G` = this.`6G`.memento()
    )
}

