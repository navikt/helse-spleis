package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.VedtaksperiodeFilter

internal val Person.inspektør get() = PersonInspektør(this)
internal val Person.personLogg get() = inspektør.aktivitetslogg

internal fun Person.søppelbøtte(hendelse: Hendelse, periode: Periode) =
    søppelbøtte(hendelse) { it.periode().start >= periode.start }

internal fun Person.søppelbøtte(hendelse: Hendelse, filter: VedtaksperiodeFilter) =
    søppelbøtte(hendelse, filter)

internal class PersonInspektør(person: Person) {
    internal val arbeidsgiverteller get() = arbeidsgivere.size
    internal val vilkårsgrunnlagHistorikk = person.vilkårsgrunnlagHistorikk.inspektør
    private val infotrygdhistorikk = person.infotrygdhistorikk

    internal val aktivitetslogg = person.aktivitetslogg
    internal val personidentifikator = person.personidentifikator
    internal val aktørId = person.aktørId
    internal val fødselsdato = person.alder.fødselsdato
    internal var dødsdato = person.alder.dødsdato

    private val arbeidsgivere = person.arbeidsgivere.associateBy { it.organisasjonsnummer() }

    internal val utbetaltIInfotrygd get() = infotrygdhistorikk.betaltePerioder()

    internal fun vedtaksperioder() = arbeidsgivere.mapValues { it.value.view().aktiveVedtaksperioder }
    internal fun vedtaksperiode(vedtaksperiodeId: UUID) = arbeidsgivere.firstNotNullOf { (_, arbeidsgiver) ->
        arbeidsgiver.view().aktiveVedtaksperioder.firstOrNull { vedtaksperiode ->
            vedtaksperiode.inspektør.id == vedtaksperiodeId
        }
    }
    internal fun forkastetVedtaksperiode(vedtaksperiodeId: UUID) = arbeidsgivere.firstNotNullOf { (_, arbeidsgiver) ->
        arbeidsgiver.view().forkastetVedtaksperioder.firstOrNull { vedtaksperiode ->
            vedtaksperiode.inspektør.id == vedtaksperiodeId
        }
    }
    internal fun sisteVedtaksperiodeTilstander() = arbeidsgivere
        .flatMap { (_, arbeidsgiver) -> arbeidsgiver.view().aktiveVedtaksperioder.map { it.id to it.tilstand } }
        .toMap()

    internal fun arbeidsgivere() = arbeidsgivere.keys.toList()
    internal fun arbeidsgiver(orgnummer: String) = arbeidsgivere[orgnummer]
    internal fun harArbeidsgiver(organisasjonsnummer: String) = organisasjonsnummer in arbeidsgivere.keys
}
