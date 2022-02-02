package no.nav.helse.inspectors

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal val Person.inspektør get() = PersonInspektør(this)
internal val Person.personLogg get() = inspektør.aktivitetslogg

internal class PersonInspektør(person: Person): PersonVisitor {
    internal lateinit var vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        private set

    internal lateinit var aktivitetslogg: Aktivitetslogg
    private val arbeidsgivere = mutableSetOf<String>()
    private val infotrygdelementerLagretInntekt = mutableListOf<Boolean>()
    private val grunnlagsdata = mutableListOf<Pair<LocalDate,VilkårsgrunnlagHistorikk.Grunnlagsdata>>()
    private val vilkårsgrunnlagHistorikkInnslag: MutableList<TestArbeidsgiverInspektør.InnslagId> = mutableListOf()

    init {
        person.accept(this)
    }

    internal fun harLagretInntekt(indeks: Int) = infotrygdelementerLagretInntekt[indeks]
    internal fun harArbeidsgiver(organisasjonsnummer: String) = organisasjonsnummer in arbeidsgivere
    internal fun grunnlagsdata(indeks: Int) = grunnlagsdata[indeks].second
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlagHistorikkInnslag.sortedByDescending { it.timestamp }.toList()
    internal fun antallGrunnlagsdata() = grunnlagsdata.size

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        this.vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
    }

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        this.aktivitetslogg = aktivitetslogg
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        antallOpptjeningsdagerErMinst: Int,
        harOpptjening: Boolean,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        harMinimumInntekt: Boolean?,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID
    ) {
        this.grunnlagsdata.add(skjæringstidspunkt to grunnlagsdata)
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        vilkårsgrunnlagHistorikkInnslag.add(TestArbeidsgiverInspektør.InnslagId(id, opprettet))
    }

    override fun preVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
        infotrygdelementerLagretInntekt.add(lagretInntekter)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        this.arbeidsgivere.add(organisasjonsnummer)
    }
}
