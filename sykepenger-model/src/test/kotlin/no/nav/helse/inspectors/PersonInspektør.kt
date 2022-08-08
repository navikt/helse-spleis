package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Prosent
import org.junit.jupiter.api.fail

internal val Person.inspektør get() = PersonInspektør(this)
internal val Person.personLogg get() = inspektør.aktivitetslogg

internal fun Person.søppelbøtte(hendelse: IAktivitetslogg, periode: Periode) =
    søppelbøtte(hendelse, Vedtaksperiode.TIDLIGERE_OG_ETTERGØLGENDE(periode))

internal class PersonInspektør(person: Person): PersonVisitor {
    internal val arbeidsgiverteller get() = arbeidsgivere.size
    internal lateinit var vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        private set

    internal lateinit var aktivitetslogg: Aktivitetslogg
    internal lateinit var fødselsnummer: Fødselsnummer
    internal lateinit var aktørId: String
    internal lateinit var fødselsdato: LocalDate
    internal var dødsdato: LocalDate? = null
    internal lateinit var alder: Alder
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private val infotrygdelementerLagretInntekt = mutableListOf<Boolean>()
    private val grunnlagsdata = mutableListOf<Pair<LocalDate,VilkårsgrunnlagHistorikk.Grunnlagsdata>>()
    private val vilkårsgrunnlagHistorikkInnslag: MutableList<TestArbeidsgiverInspektør.InnslagId> = mutableListOf()

    init {
        person.accept(this)
    }

    internal fun arbeidsgiver(orgnummer: String) = arbeidsgivere[orgnummer]
    internal fun harLagretInntekt(indeks: Int) = infotrygdelementerLagretInntekt[indeks]
    internal fun harArbeidsgiver(organisasjonsnummer: String) = organisasjonsnummer in arbeidsgivere.keys
    internal fun grunnlagsdata(indeks: Int) = grunnlagsdata[indeks].second
    internal fun grunnlagsdata(skjæringstidspunkt: LocalDate) = grunnlagsdata.firstOrNull { it.first == skjæringstidspunkt }?.second ?: fail("Fant ikke grunnlagsdata på skjæringstidspunkt $skjæringstidspunkt")
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
        this.fødselsnummer = fødselsnummer
        this.aktørId = aktørId
        this.dødsdato = dødsdato
        this.vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
    }

    override fun visitAlder(alder: Alder, fødselsdato: LocalDate) {
        this.alder = alder
        this.fødselsdato = fødselsdato
    }

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        this.aktivitetslogg = aktivitetslogg
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
        this.arbeidsgivere[organisasjonsnummer] = arbeidsgiver
    }
}
