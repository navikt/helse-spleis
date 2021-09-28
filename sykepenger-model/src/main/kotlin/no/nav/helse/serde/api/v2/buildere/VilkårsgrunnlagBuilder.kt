package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.*
import no.nav.helse.person.Inntektshistorikk.Infotrygd
import no.nav.helse.person.Inntektshistorikk.Inntektsmelding
import no.nav.helse.person.Inntektshistorikk.Saksbehandler
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.serde.api.MedlemskapstatusDTO
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.properties.Delegates

private typealias VilkårsgrunnlagHistorikkId = UUID

internal class IInnslag(
    private val innslag: Map<LocalDate, IVilkårsgrunnlag>) {

    fun vilkårsgrunnlag(skjæringstidspunkt: LocalDate) = innslag[skjæringstidspunkt]
}


internal class ISykepengegrunnlag(
    val inntekterPerArbeidsgiver: List<Arbeidsgiverinntekt>,
    val sykepengegrunnlag: Double,
    val maksUtbetalingPerDag: Double,
    val omregnetÅrsinntekt: Double
)

internal class IInntekt(
    val årlig: Double,
    val månedlig: Double,
    val daglig: Double,
    val dagligAvrundet: Int
)

internal interface IVilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val omregnetÅrsinntekt: Double?
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<Arbeidsgiverinntekt>
}

internal class SpleisGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double?,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double,
    val avviksprosent: Double?,
    val oppfyllerKravOmMinstelønn: Boolean?,
    val grunnbeløp: Int,
    val medlemskapstatus: MedlemskapstatusDTO,
    val meldingsreferanseId: UUID?

) : IVilkårsgrunnlag

internal class InfotrygdGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double?,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double
) : IVilkårsgrunnlag

internal class InntektBuilder(private val inntekt: Inntekt) {
    fun build(): IInntekt {
        return inntekt.reflection { årlig, månedlig, daglig, dagligInt ->
            IInntekt(årlig, månedlig, daglig, dagligInt)
        }
    }
}

internal class IVilkårsgrunnlagHistorikk {
    private val historikk = mutableMapOf<VilkårsgrunnlagHistorikkId, IInnslag>()

    internal fun leggTil(vilkårsgrunnlagHistorikkId: UUID, innslag: IInnslag) {
        historikk.putIfAbsent(vilkårsgrunnlagHistorikkId, innslag)
    }

    internal fun spleisgrunnlag(vilkårsgrunnlagHistorikkId: UUID, skjæringstidspunkt: LocalDate) =
        historikk[vilkårsgrunnlagHistorikkId]?.vilkårsgrunnlag(skjæringstidspunkt) as? SpleisGrunnlag

    internal fun toMap() = historikk.toMap() // TODO: DTO
}

internal class VilkårsgrunnlagBuilder(
    vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder
) :
    VilkårsgrunnlagHistorikkVisitor {
    private val historikk = IVilkårsgrunnlagHistorikk()

    init {
        vilkårsgrunnlagHistorikk.accept(this)
    }

    fun build() = historikk

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        historikk.leggTil(id, InnslagBuilder(innslag, sammenligningsgrunnlagBuilder).build())
    }

    internal class InnslagBuilder(
        innslag: VilkårsgrunnlagHistorikk.Innslag,
        private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder
    ) : VilkårsgrunnlagHistorikkVisitor {
        private val vilkårsgrunnlag = mutableMapOf<LocalDate, IVilkårsgrunnlag>()

        init {
            innslag.accept(this)
        }

        fun build() = IInnslag(vilkårsgrunnlag.toMap())

        override fun preVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: Grunnlagsdata) {
            val sykepengegrunnlag = SykepengegrunnlagBuilder(grunnlagsdata.sykepengegrunnlag, sammenligningsgrunnlagBuilder).build()
            val sammenligningsgrunnlag = InntektBuilder(grunnlagsdata.sammenligningsgrunnlag).build()
            val grunnbeløp = InntektBuilder(Grunnbeløp.`1G`.beløp(skjæringstidspunkt)).build()
            val avviksprosent = grunnlagsdata.avviksprosent?.prosent()
            val medlemskapstatus = when (grunnlagsdata.medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapstatusDTO.JA
                Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapstatusDTO.NEI
                else -> MedlemskapstatusDTO.VET_IKKE
            }

            vilkårsgrunnlag.putIfAbsent(
                skjæringstidspunkt, SpleisGrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    omregnetÅrsinntekt = sykepengegrunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = sammenligningsgrunnlag.årlig,
                    inntekter = sykepengegrunnlag.inntekterPerArbeidsgiver,
                    avviksprosent = avviksprosent,
                    oppfyllerKravOmMinstelønn = grunnlagsdata.harOpptjening,
                    grunnbeløp = grunnbeløp.årlig.toInt(),
                    medlemskapstatus = medlemskapstatus,
                    sykepengegrunnlag = sykepengegrunnlag.sykepengegrunnlag,
                    meldingsreferanseId = grunnlagsdata.meldingsreferanseId
                )
            )
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag
        ) {
            val byggetSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, sammenligningsgrunnlagBuilder).build()
            vilkårsgrunnlag.putIfAbsent(
                skjæringstidspunkt, InfotrygdGrunnlag(
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
            private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder
        ) :
            VilkårsgrunnlagHistorikkVisitor {
            private val inntekterPerArbeidsgiver = mutableListOf<Arbeidsgiverinntekt>()
            private lateinit var sykepengegrunnlag: IInntekt
            private var omregnetÅrsinntekt by Delegates.notNull<Double>()

            init {
                sykepengegrunnlag.accept(this)
            }

            fun build() = ISykepengegrunnlag(inntekterPerArbeidsgiver.toList(), sykepengegrunnlag.årlig, sykepengegrunnlag.daglig, omregnetÅrsinntekt)

            override fun preVisitSykepengegrunnlag(sykepengegrunnlag1: Sykepengegrunnlag, sykepengegrunnlag: Inntekt, grunnlagForSykepengegrunnlag: Inntekt) {
                this.sykepengegrunnlag = InntektBuilder(sykepengegrunnlag).build()
                this.omregnetÅrsinntekt = InntektBuilder(grunnlagForSykepengegrunnlag).build().årlig
            }

            override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {

                inntekterPerArbeidsgiver.add(InntektsopplysningBuilder(orgnummer, arbeidsgiverInntektsopplysning, sammenligningsgrunnlagBuilder).build())
            }
        }


        private class InntektsopplysningBuilder(
            private val organisasjonsnummer: String,
            inntektsopplysning: ArbeidsgiverInntektsopplysning,
            private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var inntekt: Arbeidsgiverinntekt

            init {
                inntektsopplysning.accept(this)
            }

            fun build() = inntekt

            private fun nyArbeidsgiverInntekt(
                kilde: Inntektkilde,
                inntekt: IInntekt,
                skjæringstidspunkt: LocalDate,
                inntekterFraAOrdningen: List<InntekterFraAOrdningen>? = null,
            ) = Arbeidsgiverinntekt(
                organisasjonsnummer,
                OmregnetÅrsinntekt(kilde, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen),
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder.sammenligningsgrunnlag(organisasjonsnummer, skjæringstidspunkt)
            )

            override fun visitInfotrygd(infotrygd: Infotrygd, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(Inntektkilde.Infotrygd, inntekt, dato)
            }

            override fun visitSaksbehandler(
                saksbehandler: Saksbehandler,
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(Inntektkilde.Saksbehandler, inntekt, dato)
            }

            override fun visitInntektsmelding(
                inntektsmelding: Inntektsmelding,
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(Inntektkilde.Inntektsmelding, inntekt, dato)
            }

            override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
                val (inntekt, inntekterFraAOrdningen) = SkattBuilder(skattComposite).build()
                this.inntekt = nyArbeidsgiverInntekt(Inntektkilde.AOrdningen, inntekt, dato, inntekterFraAOrdningen)
            }

            private class SkattBuilder(skattComposite: Inntektshistorikk.SkattComposite) : VilkårsgrunnlagHistorikkVisitor {
                val inntekt = InntektBuilder(skattComposite.grunnlagForSykepengegrunnlag()).build()
                val inntekterFraAOrdningen = mutableListOf<InntekterFraAOrdningen>()

                init {
                    skattComposite.accept(this)
                }

                fun build() = inntekt to inntekterFraAOrdningen.toList()

                override fun visitSkattSykepengegrunnlag(
                    sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
                    dato: LocalDate,
                    hendelseId: UUID,
                    beløp: Inntekt,
                    måned: YearMonth,
                    type: Inntektshistorikk.Skatt.Inntekttype,
                    fordel: String,
                    beskrivelse: String,
                    tidsstempel: LocalDateTime
                ) {
                    val inntekt = InntektBuilder(beløp).build()
                    inntekterFraAOrdningen.add(InntekterFraAOrdningen(måned, inntekt.månedlig))
                }
            }
        }
    }
}
