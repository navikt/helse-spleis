package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnslagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlaghistorikkInnDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.EndretInntektsgrunnlag
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.Inntektsgrunnlag.Companion.harUlikeGrunnbeløp
import no.nav.helse.person.inntekt.InntektsgrunnlagView

internal class VilkårsgrunnlagHistorikk private constructor(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    private fun sisteInnlag() = historikk.firstOrNull()

    private fun forrigeInnslag() = historikk.elementAtOrNull(1)

    internal fun view() = VilkårsgrunnlagHistorikkView(innslag = historikk.map { it.view() })

    internal fun lagre(vilkårsgrunnlag: VilkårsgrunnlagElement) {
        val siste = sisteInnlag()
        val nytt = Innslag(siste, vilkårsgrunnlag)
        historikk.add(0, nytt)
    }

    internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, sykefraværstilfeller: Set<LocalDate>) {
        val nyttInnslag = sisteInnlag()?.oppdaterHistorikk(aktivitetslogg, sykefraværstilfeller) ?: return
        historikk.add(0, nyttInnslag)
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
        sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun blitt6GBegrensetSidenSist(skjæringstidspunkt: LocalDate): Boolean {
        if (sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)?.er6GBegrenset() == false) return false
        return forrigeInnslag()?.vilkårsgrunnlagFor(skjæringstidspunkt)?.er6GBegrenset() == false
    }

    internal class Innslag private constructor(
        internal val id: UUID,
        private val opprettet: LocalDateTime,
        private val vilkårsgrunnlag: Map<LocalDate, VilkårsgrunnlagElement>
    ) {
        internal constructor(vilkårsgrunnlag: Map<LocalDate, VilkårsgrunnlagElement>) : this(UUID.randomUUID(), LocalDateTime.now(), vilkårsgrunnlag)

        internal constructor(other: Innslag?, nyttElement: VilkårsgrunnlagElement) : this((other?.vilkårsgrunnlag ?: emptyMap()) + mapOf(nyttElement.skjæringstidspunkt to nyttElement))

        internal fun view() = VilkårsgrunnlagInnslagView(vilkårsgrunnlag = vilkårsgrunnlag.map { it.value.view() })

        internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
            vilkårsgrunnlag[skjæringstidspunkt]

        internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, sykefraværstilfeller: Set<LocalDate>): Innslag? {
            val gyldigeVilkårsgrunnlag = beholdAktiveSkjæringstidspunkter(sykefraværstilfeller)
            val diff = this.vilkårsgrunnlag.size - gyldigeVilkårsgrunnlag.size
            if (diff == 0) return null
            aktivitetslogg.info("Fjernet $diff vilkårsgrunnlagselementer")
            return Innslag(gyldigeVilkårsgrunnlag)
        }

        private fun beholdAktiveSkjæringstidspunkter(sykefraværstilfeller: Set<LocalDate>): Map<LocalDate, VilkårsgrunnlagElement> {
            return vilkårsgrunnlag.filter { (dato, _) -> dato in sykefraværstilfeller }
        }

        internal companion object {
            fun gjenopprett(
                id: UUID,
                opprettet: LocalDateTime,
                elementer: Map<LocalDate, VilkårsgrunnlagElement>
            ): Innslag = Innslag(id, opprettet, elementer.toMutableMap())

            fun gjenopprett(dto: VilkårsgrunnlagInnslagInnDto, grunnlagsdata: MutableMap<UUID, VilkårsgrunnlagElement>): Innslag {
                return Innslag(
                    id = dto.id,
                    opprettet = dto.opprettet,
                    vilkårsgrunnlag = dto.vilkårsgrunnlag.associate {
                        it.skjæringstidspunkt to grunnlagsdata.getOrPut(it.vilkårsgrunnlagId) {
                            VilkårsgrunnlagElement.gjenopprett(it)
                        }
                    }.toMutableMap()
                )
            }
        }

        internal fun dto() = VilkårsgrunnlagInnslagUtDto(
            id = this.id,
            opprettet = this.opprettet,
            vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.value.dto() }
        )
    }

    internal sealed class VilkårsgrunnlagElement(
        val vilkårsgrunnlagId: UUID,
        val skjæringstidspunkt: LocalDate,
        val inntektsgrunnlag: Inntektsgrunnlag,
        val opptjening: Opptjening?
    ) : Aktivitetskontekst {
        internal open fun valider(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String) = true

        internal fun view() = VilkårsgrunnlagView(
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            vurdertOk = when (this) {
                is Grunnlagsdata -> vurdertOk
                is InfotrygdVilkårsgrunnlag -> true
            },
            type = when (this) {
                is Grunnlagsdata -> VilkårsgrunnlagView.VilkårsgrunnlagTypeView.SPLEIS
                is InfotrygdVilkårsgrunnlag -> VilkårsgrunnlagView.VilkårsgrunnlagTypeView.INFOTRYGD
            },
            meldingsreferanseId = when (this) {
                is Grunnlagsdata -> this.meldingsreferanseId
                is InfotrygdVilkårsgrunnlag -> null
            },
            inntektsgrunnlag = inntektsgrunnlag.view(),
            opptjening = opptjening?.view()
        )

        internal fun erArbeidsgiverRelevant(organisasjonsnummer: String) = inntektsgrunnlag.erArbeidsgiverRelevant(organisasjonsnummer)

        final override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to vilkårsgrunnlagtype()
            )
        )

        internal fun overstyrArbeidsgiveropplysninger(hendelse: OverstyrArbeidsgiveropplysninger, subsumsjonslogg: Subsumsjonslogg): Pair<VilkårsgrunnlagElement, EndretInntektsgrunnlag>? {
            if (this is InfotrygdVilkårsgrunnlag) return null
            val endretInntektsgrunnlag = inntektsgrunnlag.overstyrArbeidsgiveropplysninger(hendelse) ?: return null
            return kopierMed(endretInntektsgrunnlag.inntektsgrunnlagEtter, opptjening, subsumsjonslogg) to endretInntektsgrunnlag
        }

        internal fun skjønnsmessigFastsettelse(hendelse: SkjønnsmessigFastsettelse, subsumsjonslogg: Subsumsjonslogg): VilkårsgrunnlagElement? {
            if (this is InfotrygdVilkårsgrunnlag) return null
            val endretInntektsgrunnlag = inntektsgrunnlag.skjønnsmessigFastsettelse(hendelse) ?: return null
            return kopierMed(endretInntektsgrunnlag.inntektsgrunnlagEtter, opptjening, subsumsjonslogg)
        }

        protected abstract fun kopierMed(
            inntektsgrunnlag: Inntektsgrunnlag,
            opptjening: Opptjening?,
            subsumsjonslogg: Subsumsjonslogg,
            nyttSkjæringstidspunkt: LocalDate? = null
        ): VilkårsgrunnlagElement

        abstract fun overstyrArbeidsforhold(
            hendelse: OverstyrArbeidsforhold,
            subsumsjonslogg: Subsumsjonslogg
        ): VilkårsgrunnlagElement

        internal fun grunnbeløpsregulering(
            subsumsjonslogg: Subsumsjonslogg
        ): VilkårsgrunnlagElement? {
            val nyttSykepengegrunnlag = inntektsgrunnlag.grunnbeløpsregulering() ?: return null
            return kopierMed(nyttSykepengegrunnlag, opptjening, subsumsjonslogg)
        }

        internal fun nyeArbeidsgiverInntektsopplysninger(
            organisasjonsnummer: String,
            inntekt: ArbeidstakerFaktaavklartInntekt
        ): Pair<VilkårsgrunnlagElement, EndretInntektsgrunnlag>? {
            val endretInntektsgrunnlag = inntektsgrunnlag.nyeArbeidsgiverInntektsopplysninger(organisasjonsnummer, inntekt) ?: return null
            val grunnlag = kopierMed(endretInntektsgrunnlag.inntektsgrunnlagEtter, opptjening, EmptyLog)
            return grunnlag to endretInntektsgrunnlag
        }

        protected abstract fun vilkårsgrunnlagtype(): String

        internal fun er6GBegrenset() = inntektsgrunnlag.er6GBegrenset()

        internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
            inntektsgrunnlag.harNødvendigInntektForVilkårsprøving(organisasjonsnummer)

        internal fun harGjenbrukbarInntekt(organisasjonsnummer: String) =
            inntektsgrunnlag.harGjenbrukbarInntekt(organisasjonsnummer)

        internal fun lagreTidsnæreInntekter(
            skjæringstidspunkt: LocalDate,
            yrkesaktivitet: Yrkesaktivitet,
            aktivitetslogg: IAktivitetslogg,
            nyArbeidsgiverperiode: Boolean
        ) {
            inntektsgrunnlag.lagreTidsnæreInntekter(skjæringstidspunkt, yrkesaktivitet, aktivitetslogg, nyArbeidsgiverperiode)
        }

        internal fun berik(builder: UtkastTilVedtakBuilder) {
            builder.vilkårsgrunnlagId(vilkårsgrunnlagId)
            inntektsgrunnlag.berik(builder)
        }

        internal companion object {
            internal fun skjæringstidspunktperioder(elementer: Collection<VilkårsgrunnlagElement>): List<Periode> {
                val skjæringstidspunkter = elementer
                    .map { it.skjæringstidspunkt }
                    .sorted()
                return skjæringstidspunkter
                    .mapIndexed { index, skjæringstidspunkt ->
                        val sisteDag = skjæringstidspunkter.elementAtOrNull(index + 1)?.forrigeDag ?: LocalDate.MAX
                        skjæringstidspunkt til sisteDag
                    }
            }

            internal fun List<VilkårsgrunnlagElement>.harUlikeGrunnbeløp(): Boolean {
                return map { it.inntektsgrunnlag }.harUlikeGrunnbeløp()
            }

            internal fun gjenopprett(dto: VilkårsgrunnlagInnDto): VilkårsgrunnlagElement {
                return when (dto) {
                    is VilkårsgrunnlagInnDto.Infotrygd -> InfotrygdVilkårsgrunnlag.gjenopprett(dto)
                    is VilkårsgrunnlagInnDto.Spleis -> Grunnlagsdata.gjenopprett(dto)
                }
            }
        }

        internal fun dto(): VilkårsgrunnlagUtDto =
            dto(vilkårsgrunnlagId, skjæringstidspunkt, inntektsgrunnlag.dto())

        protected abstract fun dto(
            vilkårsgrunnlagId: UUID,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: InntektsgrunnlagUtDto
        ): VilkårsgrunnlagUtDto
    }

    internal class Grunnlagsdata(
        skjæringstidspunkt: LocalDate,
        inntektsgrunnlag: Inntektsgrunnlag,
        opptjening: Opptjening,
        val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        val vurdertOk: Boolean,
        val meldingsreferanseId: MeldingsreferanseId?,
        vilkårsgrunnlagId: UUID
    ) : VilkårsgrunnlagElement(vilkårsgrunnlagId, skjæringstidspunkt, inntektsgrunnlag, opptjening) {
        internal fun validerFørstegangsvurdering(aktivitetslogg: IAktivitetslogg) {
            when (opptjening) {
                is ArbeidstakerOpptjening -> inntektsgrunnlag.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjening)
                is SelvstendigNæringsdrivendeOpptjening -> {}
                null -> {}
            }

        }

        override fun valider(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String): Boolean {
            inntektsgrunnlag.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, organisasjonsnummer)
            return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
        }

        override fun vilkårsgrunnlagtype() = "Spleis"

        override fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonslogg: Subsumsjonslogg) = kopierMed(
            inntektsgrunnlag = inntektsgrunnlag.overstyrArbeidsforhold(hendelse, subsumsjonslogg),
            opptjening = opptjening!!.overstyrArbeidsforhold(hendelse).also {
                subsumsjonslogg.logg(it.subsumsjon)
            },
            subsumsjonslogg = subsumsjonslogg,
        )

        override fun kopierMed(
            inntektsgrunnlag: Inntektsgrunnlag,
            opptjening: Opptjening?,
            subsumsjonslogg: Subsumsjonslogg,
            nyttSkjæringstidspunkt: LocalDate?
        ): VilkårsgrunnlagElement {
            return Grunnlagsdata(
                skjæringstidspunkt = nyttSkjæringstidspunkt ?: skjæringstidspunkt,
                inntektsgrunnlag = inntektsgrunnlag,
                opptjening = opptjening ?: this.opptjening!!,
                medlemskapstatus = medlemskapstatus,
                vurdertOk = vurdertOk,
                meldingsreferanseId = meldingsreferanseId,
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        }

        override fun dto(
            vilkårsgrunnlagId: UUID,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: InntektsgrunnlagUtDto
        ) = VilkårsgrunnlagUtDto.Spleis(
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = skjæringstidspunkt,
            inntektsgrunnlag = sykepengegrunnlag,
            opptjening = this.opptjening!!.dto(),
            medlemskapstatus = when (medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> MedlemskapsvurderingDto.Ja
                Medlemskapsvurdering.Medlemskapstatus.Nei -> MedlemskapsvurderingDto.Nei
                Medlemskapsvurdering.Medlemskapstatus.VetIkke -> MedlemskapsvurderingDto.VetIkke
                Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål -> MedlemskapsvurderingDto.UavklartMedBrukerspørsmål
            },
            vurdertOk = vurdertOk,
            meldingsreferanseId = meldingsreferanseId?.dto()
        )

        internal companion object {
            fun gjenopprett(dto: VilkårsgrunnlagInnDto.Spleis): Grunnlagsdata {
                return Grunnlagsdata(
                    skjæringstidspunkt = dto.skjæringstidspunkt,
                    inntektsgrunnlag = Inntektsgrunnlag.gjenopprett(dto.skjæringstidspunkt, dto.inntektsgrunnlag),
                    opptjening = Opptjening.gjenopprett(dto.skjæringstidspunkt, dto.opptjening),
                    vilkårsgrunnlagId = dto.vilkårsgrunnlagId,
                    medlemskapstatus = when (dto.medlemskapstatus) {
                        MedlemskapsvurderingDto.Ja -> Medlemskapsvurdering.Medlemskapstatus.Ja
                        MedlemskapsvurderingDto.Nei -> Medlemskapsvurdering.Medlemskapstatus.Nei
                        MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål
                        MedlemskapsvurderingDto.VetIkke -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
                    },
                    vurdertOk = dto.vurdertOk,
                    meldingsreferanseId = dto.meldingsreferanseId?.let { MeldingsreferanseId.gjenopprett(it) }
                )
            }
        }
    }

    internal class InfotrygdVilkårsgrunnlag(
        skjæringstidspunkt: LocalDate,
        inntektsgrunnlag: Inntektsgrunnlag,
        vilkårsgrunnlagId: UUID = UUID.randomUUID()
    ) : VilkårsgrunnlagElement(vilkårsgrunnlagId, skjæringstidspunkt, inntektsgrunnlag, null) {

        override fun overstyrArbeidsforhold(hendelse: OverstyrArbeidsforhold, subsumsjonslogg: Subsumsjonslogg) = kopierMed(
            inntektsgrunnlag = inntektsgrunnlag.overstyrArbeidsforhold(hendelse, subsumsjonslogg),
            opptjening = null,
            subsumsjonslogg = subsumsjonslogg
        )

        override fun kopierMed(
            inntektsgrunnlag: Inntektsgrunnlag,
            opptjening: Opptjening?,
            subsumsjonslogg: Subsumsjonslogg,
            nyttSkjæringstidspunkt: LocalDate?
        ): InfotrygdVilkårsgrunnlag {
            return InfotrygdVilkårsgrunnlag(
                skjæringstidspunkt = nyttSkjæringstidspunkt ?: skjæringstidspunkt,
                inntektsgrunnlag = inntektsgrunnlag,
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        }

        override fun vilkårsgrunnlagtype() = "Infotrygd"

        override fun dto(
            vilkårsgrunnlagId: UUID,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: InntektsgrunnlagUtDto
        ) = VilkårsgrunnlagUtDto.Infotrygd(vilkårsgrunnlagId, skjæringstidspunkt, sykepengegrunnlag)

        internal companion object {
            fun gjenopprett(dto: VilkårsgrunnlagInnDto.Infotrygd): InfotrygdVilkårsgrunnlag {
                return InfotrygdVilkårsgrunnlag(
                    skjæringstidspunkt = dto.skjæringstidspunkt,
                    inntektsgrunnlag = Inntektsgrunnlag.gjenopprett(dto.skjæringstidspunkt, dto.inntektsgrunnlag),
                    vilkårsgrunnlagId = dto.vilkårsgrunnlagId
                )
            }
        }
    }

    internal companion object {
        internal fun gjenopprett(dto: VilkårsgrunnlaghistorikkInnDto, grunnlagsdata: MutableMap<UUID, VilkårsgrunnlagElement>): VilkårsgrunnlagHistorikk {
            return VilkårsgrunnlagHistorikk(
                historikk = dto.historikk.map { Innslag.gjenopprett(it, grunnlagsdata) }.toMutableList()
            )
        }
    }

    fun dto() = VilkårsgrunnlaghistorikkUtDto(
        historikk = this.historikk.map { it.dto() }
    )
}

internal data class VilkårsgrunnlagHistorikkView(val innslag: List<VilkårsgrunnlagInnslagView>)
internal data class VilkårsgrunnlagInnslagView(val vilkårsgrunnlag: List<VilkårsgrunnlagView>)
internal data class VilkårsgrunnlagView(
    val vilkårsgrunnlagId: UUID,
    val skjæringstidspunkt: LocalDate,
    val vurdertOk: Boolean,
    val type: VilkårsgrunnlagTypeView,
    val meldingsreferanseId: MeldingsreferanseId?,
    val inntektsgrunnlag: InntektsgrunnlagView,
    val opptjening: OpptjeningView?
) {
    enum class VilkårsgrunnlagTypeView { INFOTRYGD, SPLEIS }
}
