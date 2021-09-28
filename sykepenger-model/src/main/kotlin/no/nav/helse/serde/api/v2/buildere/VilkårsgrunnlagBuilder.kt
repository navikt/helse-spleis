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
import no.nav.helse.serde.api.v2.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
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

    internal fun toDTO() = innslag.mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.toDTO() }
}

internal class ISykepengegrunnlag(
    val inntekterPerArbeidsgiver: List<IArbeidsgiverinntekt>,
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

internal enum class IMedlemskapstatus {
    JA, NEI, VET_IKKE;

    internal fun toDTO() = when (this) {
        JA -> MedlemskapstatusDTO.JA
        NEI -> MedlemskapstatusDTO.NEI
        VET_IKKE -> MedlemskapstatusDTO.VET_IKKE
    }
}

internal interface IVilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val omregnetÅrsinntekt: Double?
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<IArbeidsgiverinntekt>
    fun toDTO(): Vilkårsgrunnlag
}

internal class ISpleisGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double?,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double,
    val avviksprosent: Double?,
    val oppfyllerKravOmMinstelønn: Boolean?,
    val grunnbeløp: Int,
    val medlemskapstatus: IMedlemskapstatus,
    val meldingsreferanseId: UUID?
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return SpleisVilkårsgrunnlag(
            skjæringstidspunkt,
            omregnetÅrsinntekt,
            sammenligningsgrunnlag,
            sykepengegrunnlag,
            inntekter.map { it.toDTO() },
            avviksprosent,
            oppfyllerKravOmMinstelønn,
            grunnbeløp,
            medlemskapstatus.toDTO()
        )
    }
}

internal class IInfotrygdGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double?,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return no.nav.helse.serde.api.v2.InfotrygdVilkårsgrunnlag(
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
        historikk[vilkårsgrunnlagHistorikkId]?.vilkårsgrunnlag(skjæringstidspunkt) as? ISpleisGrunnlag

    internal fun toDTO() = historikk.mapValues { (_, innslag) -> innslag.toDTO() }.toMap()
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

    internal fun build() = historikk

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

        internal fun build() = IInnslag(vilkårsgrunnlag.toMap())

        override fun preVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: Grunnlagsdata) {
            val sykepengegrunnlag = SykepengegrunnlagBuilder(grunnlagsdata.sykepengegrunnlag, sammenligningsgrunnlagBuilder).build()
            val sammenligningsgrunnlag = InntektBuilder(grunnlagsdata.sammenligningsgrunnlag).build()
            val grunnbeløp = InntektBuilder(Grunnbeløp.`1G`.beløp(skjæringstidspunkt)).build()
            val avviksprosent = grunnlagsdata.avviksprosent?.prosent()
            val medlemskapstatus = when (grunnlagsdata.medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> IMedlemskapstatus.JA
                Medlemskapsvurdering.Medlemskapstatus.Nei -> IMedlemskapstatus.NEI
                else -> IMedlemskapstatus.VET_IKKE
            }

            vilkårsgrunnlag.putIfAbsent(
                skjæringstidspunkt, ISpleisGrunnlag(
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
            private val sammenligningsgrunnlagBuilder: OppsamletSammenligningsgrunnlagBuilder
        ) :
            VilkårsgrunnlagHistorikkVisitor {
            private val inntekterPerArbeidsgiver = mutableListOf<IArbeidsgiverinntekt>()
            private lateinit var sykepengegrunnlag: IInntekt
            private var omregnetÅrsinntekt by Delegates.notNull<Double>()

            init {
                sykepengegrunnlag.accept(this)
            }

            internal fun build() = ISykepengegrunnlag(inntekterPerArbeidsgiver.toList(), sykepengegrunnlag.årlig, sykepengegrunnlag.daglig, omregnetÅrsinntekt)

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
            private lateinit var inntekt: IArbeidsgiverinntekt

            init {
                inntektsopplysning.accept(this)
            }

            internal fun build() = inntekt

            private fun nyArbeidsgiverInntekt(
                kilde: IInntektkilde,
                inntekt: IInntekt,
                skjæringstidspunkt: LocalDate,
                inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null,
            ) = IArbeidsgiverinntekt(
                organisasjonsnummer,
                IOmregnetÅrsinntekt(kilde, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen),
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder.sammenligningsgrunnlag(organisasjonsnummer, skjæringstidspunkt)
            )

            override fun visitInfotrygd(infotrygd: Infotrygd, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Infotrygd, inntekt, dato)
            }

            override fun visitSaksbehandler(
                saksbehandler: Saksbehandler,
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
                dato: LocalDate,
                hendelseId: UUID,
                beløp: Inntekt,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Inntektsmelding, inntekt, dato)
            }

            override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
                val (inntekt, inntekterFraAOrdningen) = SkattBuilder(skattComposite).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.AOrdningen, inntekt, dato, inntekterFraAOrdningen)
            }

            private class SkattBuilder(skattComposite: Inntektshistorikk.SkattComposite) : VilkårsgrunnlagHistorikkVisitor {
                val inntekt = InntektBuilder(skattComposite.grunnlagForSykepengegrunnlag()).build()
                val inntekterFraAOrdningen = mutableListOf<IInntekterFraAOrdningen>()

                init {
                    skattComposite.accept(this)
                }

                internal fun build() = inntekt to inntekterFraAOrdningen.toList()

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
                    inntekterFraAOrdningen.add(IInntekterFraAOrdningen(måned, inntekt.månedlig))
                }
            }
        }
    }
}
