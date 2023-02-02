package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Opptjening
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.økonomi.Prosent
import org.junit.jupiter.api.fail
import kotlin.properties.Delegates

internal val VilkårsgrunnlagHistorikk.inspektør get() = Vilkårgrunnlagsinspektør(this)
internal val VilkårsgrunnlagHistorikk.Innslag.inspektør get() = VilkårgrunnlagInnslagInspektør(this)

internal class Vilkårgrunnlagsinspektør(historikk: VilkårsgrunnlagHistorikk) : VilkårsgrunnlagHistorikkVisitor {
    val vilkårsgrunnlagTeller = mutableMapOf<Int, Int>()
    private var innslag = -1
    internal val aktiveSpleisSkjæringstidspunkt = mutableSetOf<LocalDate>()
    private val grunnlagsdata = mutableListOf<Pair<LocalDate,VilkårsgrunnlagHistorikk.Grunnlagsdata>>()

    init {
        historikk.accept(this)
    }

    internal fun antallGrunnlagsdata() = vilkårsgrunnlagTeller.map(Map.Entry<*, Int>::value).sum()

    internal fun grunnlagsdata(indeks: Int) = grunnlagsdata[indeks].second
    internal fun grunnlagsdata(skjæringstidspunkt: LocalDate) = grunnlagsdata.firstOrNull { it.first == skjæringstidspunkt }?.second ?: fail("Fant ikke grunnlagsdata på skjæringstidspunkt $skjæringstidspunkt")


    override fun preVisitVilkårsgrunnlagHistorikk() {
        vilkårsgrunnlagTeller.clear()
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        this.innslag += 1
        vilkårsgrunnlagTeller[this.innslag] = 0
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        avviksprosent: Prosent?,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        val teller = vilkårsgrunnlagTeller.getValue(innslag)
        if (innslag == 0) aktiveSpleisSkjæringstidspunkt.add(skjæringstidspunkt)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
        this.grunnlagsdata.add(skjæringstidspunkt to grunnlagsdata)
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }
}

internal class VilkårgrunnlagInnslagInspektør(innslag: VilkårsgrunnlagHistorikk.Innslag) : VilkårsgrunnlagHistorikkVisitor {
    internal lateinit var id: UUID
        private set
    internal val elementer = mutableListOf<VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>()

    init {
        innslag.accept(this)
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        this.id = id
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        avviksprosent: Prosent?,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        elementer.add(grunnlagsdata)
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        elementer.add(infotrygdVilkårsgrunnlag)
    }
}

internal val VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.inspektør get() = GrunnlagsdataInspektør(this)

internal class GrunnlagsdataInspektør(grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement) : VilkårsgrunnlagHistorikkVisitor {
    internal lateinit var sykepengegrunnlag: Sykepengegrunnlag
        private set
    internal lateinit var sammenligningsgrunnlag: Sammenligningsgrunnlag
        private set
    internal var avviksprosent: Prosent? = null
        private set
    internal var antallOpptjeningsdagerErMinst by Delegates.notNull<Int>()
        private set
    internal var harOpptjening by Delegates.notNull<Boolean>()
        private set
    internal lateinit var opptjening: Opptjening
        private set
    internal var meldingsreferanseId: UUID? = null
        private set
    internal var harMinimumInntekt: Boolean? = null
        private set
    internal var vurdertOk by Delegates.notNull<Boolean>()
        private set
    internal lateinit var inntektskilde: Inntektskilde
        private set
    internal lateinit var vilkårsgrunnlagId: UUID
        private set
    private lateinit var type: String
    internal val infotrygd get() = type == "Infotrygd"

    init {
        grunnlagsdata.accept(this)
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        avviksprosent: Prosent?,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
        this.sammenligningsgrunnlag = sammenligningsgrunnlag
        this.avviksprosent = avviksprosent
        this.opptjening = opptjening
        this.meldingsreferanseId = meldingsreferanseId
        this.harMinimumInntekt = harMinimumInntekt
        this.vurdertOk = vurdertOk
        this.opptjening = opptjening
        this.inntektskilde = sykepengegrunnlag.inntektskilde()
        this.vilkårsgrunnlagId = vilkårsgrunnlagId
        this.type = "Spleis"
    }

    override fun postVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
        this.vilkårsgrunnlagId = vilkårsgrunnlagId
        this.type = "Infotrygd"
    }

    override fun preVisitOpptjening(opptjening: Opptjening, arbeidsforhold: List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag>, opptjeningsperiode: Periode) {
        this.antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager()
        this.harOpptjening = opptjening.erOppfylt()
    }
}
