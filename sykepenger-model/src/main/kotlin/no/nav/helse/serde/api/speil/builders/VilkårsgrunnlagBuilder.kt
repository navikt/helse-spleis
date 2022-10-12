package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.ArbeidsgiverInntektsopplysningVisitor
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk.Infotrygd
import no.nav.helse.person.Inntektshistorikk.Inntektsmelding
import no.nav.helse.person.Inntektshistorikk.Saksbehandler
import no.nav.helse.person.Inntektshistorikk.Skatt
import no.nav.helse.person.Inntektshistorikk.SkattComposite
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosent
import kotlin.properties.Delegates

private typealias VilkårsgrunnlagHistorikkId = UUID

internal class IInnslag(
    private val innslag: Map<LocalDate, IVilkårsgrunnlag>
) {

    fun vilkårsgrunnlag(skjæringstidspunkt: LocalDate) = innslag[skjæringstidspunkt]

    internal fun toDTO() = innslag.mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.toDTO() }
}

internal class ISykepengegrunnlag(
    val inntekterPerArbeidsgiver: List<IArbeidsgiverinntekt>,
    val sykepengegrunnlag: Double,
    val oppfyllerMinsteinntektskrav: Boolean,
    val omregnetÅrsinntekt: Double,
    val begrensning: SykepengegrunnlagsgrenseDTO
)

internal class IInntekt(
    val årlig: Double,
    val månedlig: Double,
    val daglig: Double
)

internal interface IVilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val omregnetÅrsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<IArbeidsgiverinntekt>
    fun toDTO(): Vilkårsgrunnlag
}

internal class ISpleisGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double,
    val avviksprosent: Double?,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val meldingsreferanseId: UUID?,
    val antallOpptjeningsdagerErMinst: Int,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return SpleisVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            avviksprosent = avviksprosent,
            grunnbeløp = grunnbeløp,
            sykepengegrunnlagsgrense = sykepengegrunnlagsgrense,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            opptjeningFra = skjæringstidspunkt.minusDays(antallOpptjeningsdagerErMinst.toLong()),
            oppfyllerKravOmMinstelønn = oppfyllerKravOmMinstelønn,
            oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
            oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap
        )
    }
}

class SykepengegrunnlagsgrenseDTO(
    val grunnbeløp: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
) {
    companion object {
        fun fra6GBegrensning(`6G`: Inntekt): SykepengegrunnlagsgrenseDTO {
            val `1G` = `6G` / 6
            return SykepengegrunnlagsgrenseDTO(
                grunnbeløp = InntektBuilder(`1G`).build().årlig.toInt(),
                grense = InntektBuilder(`6G`).build().årlig.toInt(),
                virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(`1G`)
            )
        }
    }
}

internal class IInfotrygdGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return no.nav.helse.serde.api.dto.InfotrygdVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() }
        )
    }
}

internal class InntektBuilder(private val inntekt: Inntekt) {
    internal fun build(): IInntekt {
        return inntekt.reflection { årlig, månedlig, daglig, _ ->
            IInntekt(årlig, månedlig, daglig)
        }
    }
}

internal class IVilkårsgrunnlagHistorikk {
    private val historikk = mutableMapOf<VilkårsgrunnlagHistorikkId, IInnslag>()

    internal fun leggTil(vilkårsgrunnlagHistorikkId: UUID, innslag: IInnslag) {
        historikk.putIfAbsent(vilkårsgrunnlagHistorikkId, innslag)
    }

    internal fun toDTO() = historikk.mapValues { (_, innslag) -> innslag.toDTO() }.toMap()
}

internal class VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) : VilkårsgrunnlagHistorikkVisitor {
    private val historikk = IVilkårsgrunnlagHistorikk()

    init {
        vilkårsgrunnlagHistorikk.accept(this)
    }

    internal fun build() = historikk

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        historikk.leggTil(id, InnslagBuilder(innslag).build())
    }

    internal class InnslagBuilder(innslag: VilkårsgrunnlagHistorikk.Innslag) : VilkårsgrunnlagHistorikkVisitor {
        private val vilkårsgrunnlag = mutableMapOf<LocalDate, IVilkårsgrunnlag>()

        init {
            innslag.accept(this)
        }

        internal fun build() = IInnslag(vilkårsgrunnlag.toMap())

        override fun preVisitGrunnlagsdata(
            skjæringstidspunkt: LocalDate,
            grunnlagsdata: Grunnlagsdata,
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            avviksprosent: Prosent?,
            opptjening: Opptjening,
            vurdertOk: Boolean,
            meldingsreferanseId: UUID?,
            vilkårsgrunnlagId: UUID,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
        ) {
            val sammenligningsgrunnlagBuilder = SammenligningsgrunnlagBuilder(sammenligningsgrunnlag)

            val compositeSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, sammenligningsgrunnlagBuilder).build()
            val oppfyllerKravOmMedlemskap = when (medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> true
                Medlemskapsvurdering.Medlemskapstatus.Nei -> false
                else -> null
            }

            vilkårsgrunnlag[skjæringstidspunkt] = ISpleisGrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                omregnetÅrsinntekt = compositeSykepengegrunnlag.omregnetÅrsinntekt,
                sammenligningsgrunnlag = InntektBuilder(sammenligningsgrunnlag.sammenligningsgrunnlag).build().årlig,
                inntekter = compositeSykepengegrunnlag.inntekterPerArbeidsgiver,
                sykepengegrunnlag = compositeSykepengegrunnlag.sykepengegrunnlag,
                avviksprosent = avviksprosent?.prosent(),
                grunnbeløp = compositeSykepengegrunnlag.begrensning.grunnbeløp,
                sykepengegrunnlagsgrense = compositeSykepengegrunnlag.begrensning,
                meldingsreferanseId = meldingsreferanseId,
                antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager(),
                oppfyllerKravOmMinstelønn = compositeSykepengegrunnlag.oppfyllerMinsteinntektskrav,
                oppfyllerKravOmOpptjening = opptjening.erOppfylt(),
                oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap
            )
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag,
            vilkårsgrunnlagId: UUID
        ) {
            val byggetSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, null /* vi har ikke noe sammenligningsgrunnlag for Infotrygd-saker */)
                .build()
            vilkårsgrunnlag[skjæringstidspunkt] = IInfotrygdGrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                omregnetÅrsinntekt = byggetSykepengegrunnlag.omregnetÅrsinntekt,
                sammenligningsgrunnlag = null,
                inntekter = byggetSykepengegrunnlag.inntekterPerArbeidsgiver,
                sykepengegrunnlag = byggetSykepengegrunnlag.sykepengegrunnlag
            )
        }

        internal class SammenligningsgrunnlagBuilder(sammenligningsgrunnlag: Sammenligningsgrunnlag) : VilkårsgrunnlagHistorikkVisitor {
            private val beløp = mutableMapOf<String, Double>()
            init {
                sammenligningsgrunnlag.accept(this)
            }

            internal fun totalFor(orgnummer: String) = beløp[orgnummer]

            internal fun inntekter() = beløp.map { (orgnummer, beløp) ->
                IArbeidsgiverinntekt(
                    arbeidsgiver = orgnummer,
                    omregnetÅrsinntekt = null,
                    sammenligningsgrunnlag = beløp,
                    deaktivert = false
                )
            }

            override fun preVisitArbeidsgiverInntektsopplysning(
                arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning,
                orgnummer: String
            ) {
                beløp[orgnummer] = SkattBuilder(arbeidsgiverInntektsopplysning).total()
            }

            private class SkattBuilder(inntektsopplysning: ArbeidsgiverInntektsopplysning) : ArbeidsgiverInntektsopplysningVisitor {
                private var inntekter = INGEN

                init {
                    inntektsopplysning.accept(this)
                }

                fun total() = inntekter
                    .reflection { årlig, _, _, _ -> årlig }

                override fun preVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
                    inntekter = skattComposite.rapportertInntekt()
                }
            }
        }

        private class SykepengegrunnlagBuilder(
            sykepengegrunnlag: Sykepengegrunnlag,
            private val sammenligningsgrunnlagBuilder: SammenligningsgrunnlagBuilder?
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var `6G`: Inntekt
            private val inntekterPerArbeidsgiver = mutableListOf<IArbeidsgiverinntekt>()
            private lateinit var sykepengegrunnlag: IInntekt
            private var omregnetÅrsinntekt by Delegates.notNull<Double>()
            private var oppfyllerMinsteinntektskrav by Delegates.notNull<Boolean>()

            init {
                sykepengegrunnlag.accept(this)
            }

            fun build(): ISykepengegrunnlag {
                return ISykepengegrunnlag(
                    inntekterPerArbeidsgiver = inntekterPerArbeidsgiver.toList() + arbeidsgivereUtenSykepengegrunnlag(),
                    sykepengegrunnlag = sykepengegrunnlag.årlig,
                    oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav,
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    begrensning = SykepengegrunnlagsgrenseDTO.fra6GBegrensning(this.`6G`)
                )
            }

            private fun arbeidsgivereUtenSykepengegrunnlag() = sammenligningsgrunnlagBuilder?.inntekter()?.filterNot { inntekt ->
                inntekterPerArbeidsgiver.any { other -> other.arbeidsgiver == inntekt.arbeidsgiver }
            } ?: emptyList()

            override fun preVisitSykepengegrunnlag(
                sykepengegrunnlag1: Sykepengegrunnlag,
                skjæringstidspunkt: LocalDate,
                sykepengegrunnlag: Inntekt,
                skjønnsmessigFastsattÅrsinntekt: Inntekt?,
                beregningsgrunnlag: Inntekt,
                `6G`: Inntekt,
                begrensning: Sykepengegrunnlag.Begrensning,
                vurdertInfotrygd: Boolean,
                minsteinntekt: Inntekt,
                oppfyllerMinsteinntektskrav: Boolean
            ) {
                this.sykepengegrunnlag = InntektBuilder(sykepengegrunnlag).build()
                this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
                this.omregnetÅrsinntekt = InntektBuilder(beregningsgrunnlag).build().årlig
                this.`6G` = `6G`
            }

            private var deaktivert = false
            override fun preVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
                deaktivert = true
            }

            override fun postVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
                deaktivert = false
            }

            override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
                inntekterPerArbeidsgiver.add(InntektsopplysningBuilder(orgnummer, arbeidsgiverInntektsopplysning, sammenligningsgrunnlagBuilder, deaktivert).build())
            }
        }


        private class InntektsopplysningBuilder(
            private val organisasjonsnummer: String,
            inntektsopplysning: ArbeidsgiverInntektsopplysning,
            private val sammenligningsgrunnlagBuilder: SammenligningsgrunnlagBuilder?,
            private val deaktivert: Boolean,
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var inntekt: IArbeidsgiverinntekt

            init {
                inntektsopplysning.accept(this)
            }

            fun build() = inntekt

            private fun nyArbeidsgiverInntekt(
                kilde: IInntektkilde,
                inntekt: IInntekt,
                inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null,
            ) = IArbeidsgiverinntekt(
                organisasjonsnummer,
                IOmregnetÅrsinntekt(kilde, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen),
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder?.totalFor(organisasjonsnummer),
                deaktivert = deaktivert
            )

            override fun visitInfotrygd(infotrygd: Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Infotrygd, inntekt)
            }

            override fun visitSaksbehandler(
                saksbehandler: Saksbehandler,
                id: UUID,
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                forklaring: String?,
                subsumsjon: Subsumsjon?,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Saksbehandler, inntekt)
            }

            override fun visitInntektsmelding(
                inntektsmelding: Inntektsmelding,
                id: UUID,
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Inntektsmelding, inntekt)
            }

            override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
                val inntekt = IInntekt(0.0, 0.0, 0.0)
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.IkkeRapportert, inntekt)
            }

            override fun preVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
                val (inntekt, inntekterFraAOrdningen) = SkattBuilder(skattComposite).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.AOrdningen, inntekt, inntekterFraAOrdningen)
            }

            class SkattBuilder(skattComposite: SkattComposite) : InntekthistorikkVisitor {
                private val inntekt = InntektBuilder(skattComposite.omregnetÅrsinntekt()).build()
                private val inntekterFraAOrdningen = mutableMapOf<YearMonth, Double>()

                init {
                    skattComposite.accept(this)
                }

                fun build() = inntekt to inntekterFraAOrdningen.map { (måned, sum) -> IInntekterFraAOrdningen(måned, sum) }

                override fun visitSkattSykepengegrunnlag(
                    sykepengegrunnlag: Skatt.Sykepengegrunnlag,
                    dato: LocalDate,
                    hendelseId: UUID,
                    beløp: Inntekt,
                    måned: YearMonth,
                    type: Skatt.Inntekttype,
                    fordel: String,
                    beskrivelse: String,
                    tidsstempel: LocalDateTime
                ) {
                    val inntekt = InntektBuilder(beløp).build()
                    inntekterFraAOrdningen.merge(måned, inntekt.månedlig, Double::plus)
                }
            }
        }
    }
}
