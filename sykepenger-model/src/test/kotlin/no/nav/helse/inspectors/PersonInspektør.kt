package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.VedtaksperiodeFilter
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel

internal val Person.inspektør get() = PersonInspektør(this)
internal val Person.personLogg get() = inspektør.aktivitetslogg

internal fun Person.søppelbøtte(hendelse: Hendelse, periode: Periode) =
    søppelbøtte(hendelse) { it.periode().start >= periode.start }

internal fun Person.søppelbøtte(hendelse: Hendelse, filter: VedtaksperiodeFilter) =
    søppelbøtte(hendelse, filter)

internal class PersonInspektør(person: Person): PersonVisitor {
    internal val arbeidsgiverteller get() = arbeidsgivere.size
    internal lateinit var vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        private set

    internal lateinit var aktivitetslogg: Aktivitetslogg
    internal lateinit var personidentifikator: Personidentifikator
    internal lateinit var aktørId: String
    internal lateinit var fødselsdato: LocalDate
    internal var dødsdato: LocalDate? = null
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private val vilkårsgrunnlagHistorikkInnslag: MutableList<VilkårsgrunnlagHistorikk.Innslag> = mutableListOf()

    private var infotrygdInnslag = 0
    private val infotrygdPerioder = mutableListOf<Periode>()
    internal val utbetaltIInfotrygd get() = infotrygdPerioder.toList()

    init {
        person.accept(this)
    }

    internal fun vedtaksperioder() = arbeidsgivere.mapValues { it.value.inspektør.aktiveVedtaksperioder() }
    internal fun vedtaksperiode(vedtaksperiodeId: UUID) = arbeidsgivere.firstNotNullOf { (_, arbeidsgiver) ->
        arbeidsgiver.inspektør.aktiveVedtaksperioder().firstOrNull { vedtaksperiode ->
            vedtaksperiode.inspektør.id == vedtaksperiodeId
        }
    }
    internal fun forkastetVedtaksperiode(vedtaksperiodeId: UUID) = arbeidsgivere.firstNotNullOf { (_, arbeidsgiver) ->
        arbeidsgiver.inspektør.forkastedeVedtaksperioder().firstOrNull { vedtaksperiode ->
            vedtaksperiode.inspektør.id == vedtaksperiodeId
        }
    }
    internal fun sisteVedtaksperiodeTilstander() = mutableMapOf<UUID, TilstandType>().apply {
        arbeidsgivere.forEach { (_, arbeidsgiver) ->
            putAll(arbeidsgiver.inspektør.sisteVedtaksperiodeTilstander())
        }
    }
    internal fun arbeidsgivere() = arbeidsgivere.keys.toList()
    internal fun arbeidsgiver(orgnummer: String) = arbeidsgivere[orgnummer]
    internal fun harArbeidsgiver(organisasjonsnummer: String) = organisasjonsnummer in arbeidsgivere.keys
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlagHistorikkInnslag.toList()

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        personidentifikator: Personidentifikator,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        this.personidentifikator = personidentifikator
        this.aktørId = aktørId
        this.vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
    }

    override fun visitAlder(alder: Alder, fødselsdato: LocalDate, dødsdato: LocalDate?) {
        this.fødselsdato = fødselsdato
        this.dødsdato = dødsdato
    }

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        this.aktivitetslogg = aktivitetslogg
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        vilkårsgrunnlagHistorikkInnslag.add(innslag)
    }

    override fun preVisitInfotrygdhistorikkPerioder() {
        infotrygdInnslag++
    }

    override fun visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(
        periode: Utbetalingsperiode,
        orgnr: String,
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        inntekt: Inntekt
    ) {
        if (infotrygdInnslag == 1) infotrygdPerioder.add(fom til tom)
    }

    override fun visitInfotrygdhistorikkPersonUtbetalingsperiode(
        periode: Utbetalingsperiode,
        orgnr: String,
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        inntekt: Inntekt
    ) {
        if (infotrygdInnslag == 1) infotrygdPerioder.add(fom til tom)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        this.arbeidsgivere[organisasjonsnummer] = arbeidsgiver
    }
}
