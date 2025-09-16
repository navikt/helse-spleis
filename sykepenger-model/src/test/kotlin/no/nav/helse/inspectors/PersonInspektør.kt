package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.person.Person

internal val Person.inspektør get() = PersonInspektør(this)

internal class PersonInspektør(person: Person) {
    internal val arbeidsgiverteller get() = arbeidsgivere.size
    internal val vilkårsgrunnlagHistorikk = person.vilkårsgrunnlagHistorikk.inspektør
    private val infotrygdhistorikk = person.infotrygdhistorikk

    internal val personidentifikator = person.personidentifikator
    internal val fødselsdato = person.alder.fødselsdato
    internal var dødsdato = person.alder.dødsdato

    private val arbeidsgivere = person.yrkesaktiviteter.associateBy { it.organisasjonsnummer() }

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
