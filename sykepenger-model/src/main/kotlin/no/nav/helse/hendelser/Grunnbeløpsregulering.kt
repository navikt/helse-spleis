package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime
): PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId), OverstyrInntektsgrunnlag {

    override fun erRelevant(skjæringstidspunkt: LocalDate) =
        this.skjæringstidspunkt == skjæringstidspunkt

    override fun innsendt() = opprettet

    internal fun sykefraværstilfelleIkkeFunnet(observer: PersonObserver) {
        observer.sykefraværstilfelleIkkeFunnet(
            PersonObserver.SykefraværstilfelleIkkeFunnet(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }
}