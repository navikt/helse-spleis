package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk.Infotrygd
import no.nav.helse.person.Inntektshistorikk.Inntektsmelding
import no.nav.helse.person.Inntektshistorikk.Saksbehandler
import no.nav.helse.person.Inntektshistorikk.Skatt
import no.nav.helse.person.Inntektshistorikk.SkattComposite
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt
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
    val maksUtbetalingPerDag: Double,
    val omregnetÅrsinntekt: Double
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
        fun fra6GBegrensning(grunnbeløpgrense: Inntekt): SykepengegrunnlagsgrenseDTO {
            val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløpgrense / 6)
            return SykepengegrunnlagsgrenseDTO(
                Grunnbeløp.`1G`.beløp(virkningstidspunkt).reflection { årlig, _, _, _ -> årlig.toInt() },
                grunnbeløpgrense.reflection { årlig, _, _, _ -> årlig }.toInt(),
                virkningstidspunkt
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

internal class VilkårsgrunnlagBuilder(
    person: Person,
    private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder,
    private val inntektshistorikkForAordningenBuilder: InntektshistorikkForAOrdningenBuilder
) : PersonVisitor {
    private val historikk = IVilkårsgrunnlagHistorikk()

    init {
        person.accept(this)
    }

    internal fun build() = historikk

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        historikk.leggTil(id, InnslagBuilder(innslag, sammenligningsgrunnlagBuilder, inntektshistorikkForAordningenBuilder).build())
    }

    internal class InnslagBuilder(
        innslag: VilkårsgrunnlagHistorikk.Innslag,
        private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder,
        private val inntektshistorikkForAordningenBuilder: InntektshistorikkForAOrdningenBuilder
    ) : VilkårsgrunnlagHistorikkVisitor {
        private val vilkårsgrunnlag = mutableMapOf<LocalDate, IVilkårsgrunnlag>()

        init {
            innslag.accept(this)
        }

        internal fun build() = IInnslag(vilkårsgrunnlag.toMap())

        override fun preVisitGrunnlagsdata(
            skjæringstidspunkt: LocalDate,
            grunnlagsdata: Grunnlagsdata,
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Inntekt,
            avviksprosent: Prosent?,
            opptjening: Opptjening,
            vurdertOk: Boolean,
            meldingsreferanseId: UUID?,
            vilkårsgrunnlagId: UUID,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
        ) {
            val compositeSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, sammenligningsgrunnlagBuilder, skjæringstidspunkt, inntektshistorikkForAordningenBuilder).build()
            val grunnbeløp = InntektBuilder(Grunnbeløp.`1G`.beløp(skjæringstidspunkt)).build()
            val oppfyllerKravOmMedlemskap = when (medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> true
                Medlemskapsvurdering.Medlemskapstatus.Nei -> false
                else -> null
            }

            vilkårsgrunnlag.putIfAbsent(
                skjæringstidspunkt, ISpleisGrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    omregnetÅrsinntekt = compositeSykepengegrunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = InntektBuilder(sammenligningsgrunnlag).build().årlig,
                    inntekter = compositeSykepengegrunnlag.inntekterPerArbeidsgiver,
                    sykepengegrunnlag = compositeSykepengegrunnlag.sykepengegrunnlag,
                    avviksprosent = avviksprosent?.prosent(),
                    grunnbeløp = grunnbeløp.årlig.toInt(),
                    sykepengegrunnlagsgrense = SykepengegrunnlagsgrenseDTO.fra6GBegrensning(Grunnbeløp.`6G`.beløp(skjæringstidspunkt)),
                    meldingsreferanseId = meldingsreferanseId,
                    antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager(),
                    oppfyllerKravOmMinstelønn = compositeSykepengegrunnlag.oppfyllerMinsteinntektskrav,
                    oppfyllerKravOmOpptjening = opptjening.erOppfylt(),
                    oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap
                )
            )
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag,
            vilkårsgrunnlagId: UUID
        ) {
            val byggetSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, sammenligningsgrunnlagBuilder, skjæringstidspunkt, inntektshistorikkForAordningenBuilder).build()
            vilkårsgrunnlag.putIfAbsent(
                skjæringstidspunkt, IInfotrygdGrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    omregnetÅrsinntekt = byggetSykepengegrunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = null,
                    inntekter = byggetSykepengegrunnlag.inntekterPerArbeidsgiver,
                    sykepengegrunnlag = byggetSykepengegrunnlag.sykepengegrunnlag
                )
            )
        }

        private class SykepengegrunnlagBuilder(
            sykepengegrunnlag: Sykepengegrunnlag,
            private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder,
            private val skjæringstidspunkt: LocalDate,
            private val inntektshistorikkForAordningenBuilder: InntektshistorikkForAOrdningenBuilder
        ) :
            VilkårsgrunnlagHistorikkVisitor {
            private val inntekterPerArbeidsgiver = mutableListOf<IArbeidsgiverinntekt>()
            private lateinit var sykepengegrunnlag: IInntekt
            private var omregnetÅrsinntekt by Delegates.notNull<Double>()
            private lateinit var deaktiverteArbeidsforhold: List<String>
            private var oppfyllerMinsteinntektskrav by Delegates.notNull<Boolean>()

            init {
                sykepengegrunnlag.accept(this)
            }

            fun build() = ISykepengegrunnlag(
                inntekterPerArbeidsgiver = inntekterPerArbeidsgiver.toList() + sammenligningsgrunnlagForArbeidsgivereUtenSykepengegrunnlag(),
                sykepengegrunnlag = sykepengegrunnlag.årlig,
                oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav,
                maksUtbetalingPerDag = sykepengegrunnlag.daglig,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )

            private fun sammenligningsgrunnlagForArbeidsgivereUtenSykepengegrunnlag() = sammenligningsgrunnlagBuilder.orgnumre()
                .filter { it !in inntekterPerArbeidsgiver.map { inntekt -> inntekt.arbeidsgiver } }
                .filter { sammenligningsgrunnlagBuilder.sammenligningsgrunnlag(it, skjæringstidspunkt) != null}
                .map { orgnummer ->
                    IArbeidsgiverinntekt(
                        arbeidsgiver = orgnummer,
                        omregnetÅrsinntekt = inntektshistorikkForAordningenBuilder.hentInntekt(orgnummer, skjæringstidspunkt),
                        sammenligningsgrunnlag = sammenligningsgrunnlagBuilder.sammenligningsgrunnlag(orgnummer, skjæringstidspunkt),
                        deaktivert = deaktiverteArbeidsforhold.contains(orgnummer)
                    )
                }

            override fun preVisitSykepengegrunnlag(
                sykepengegrunnlag1: Sykepengegrunnlag,
                skjæringstidspunkt: LocalDate,
                sykepengegrunnlag: Inntekt,
                overstyrtGrunnlagForSykepengegrunnlag: Inntekt?,
                grunnlagForSykepengegrunnlag: Inntekt,
                maksimalDagsats: Inntekt,
                `6G`: Inntekt,
                begrensning: Sykepengegrunnlag.Begrensning,
                deaktiverteArbeidsforhold: List<String>,
                vurdertInfotrygd: Boolean,
                minsteinntekt: Inntekt,
                oppfyllerMinsteinntektskrav: Boolean
            ) {

                this.sykepengegrunnlag = InntektBuilder(sykepengegrunnlag).build()
                this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
                this.omregnetÅrsinntekt = InntektBuilder(grunnlagForSykepengegrunnlag).build().årlig
                this.deaktiverteArbeidsforhold = deaktiverteArbeidsforhold
            }

            override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
                inntekterPerArbeidsgiver.add(InntektsopplysningBuilder(orgnummer, arbeidsgiverInntektsopplysning, sammenligningsgrunnlagBuilder).build())
            }
        }


        private class InntektsopplysningBuilder(
            private val organisasjonsnummer: String,
            inntektsopplysning: ArbeidsgiverInntektsopplysning,
            private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder,
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var inntekt: IArbeidsgiverinntekt

            init {
                inntektsopplysning.accept(this)
            }

            fun build() = inntekt

            private fun nyArbeidsgiverInntekt(
                kilde: IInntektkilde,
                inntekt: IInntekt,
                skjæringstidspunkt: LocalDate,
                inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null,
            ) = IArbeidsgiverinntekt(
                organisasjonsnummer,
                IOmregnetÅrsinntekt(kilde, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen),
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder.sammenligningsgrunnlag(organisasjonsnummer, skjæringstidspunkt),
                deaktivert = false
            )

            override fun visitInfotrygd(infotrygd: Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Infotrygd, inntekt, dato)
            }

            override fun visitSaksbehandler(
                saksbehandler: Saksbehandler,
                id: UUID,
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Saksbehandler, inntekt, dato)
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
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Inntektsmelding, inntekt, dato)
            }

            override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
                val inntekt = IInntekt(0.0, 0.0, 0.0)
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.IkkeRapportert, inntekt, dato)
            }

            override fun preVisitSkatt(skattComposite: SkattComposite, id: UUID, dato: LocalDate) {
                val (inntekt, inntekterFraAOrdningen) = SkattBuilder(skattComposite).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.AOrdningen, inntekt, dato, inntekterFraAOrdningen)
            }

            class SkattBuilder(skattComposite: SkattComposite) : InntekthistorikkVisitor {
                private val inntekt = InntektBuilder(skattComposite.grunnlagForSykepengegrunnlag()).build()
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
