package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.PersonObserver

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime
): Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.Person(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId
    )

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )

    override fun erRelevant(skjæringstidspunkt: LocalDate) =
        this.skjæringstidspunkt == skjæringstidspunkt

    internal fun sykefraværstilfelleIkkeFunnet(observer: PersonObserver) {
        observer.sykefraværstilfelleIkkeFunnet(
            PersonObserver.SykefraværstilfelleIkkeFunnet(
                fødselsnummer = behandlingsporing.fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }
}