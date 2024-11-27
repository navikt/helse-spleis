package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.PersonObserver
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime,
) : Hendelse,
    OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver

    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = opprettet,
            registrert = LocalDateTime.now(),
            automatiskBehandling = true,
        )

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun sykefraværstilfelleIkkeFunnet(observer: PersonObserver) {
        observer.sykefraværstilfelleIkkeFunnet(
            PersonObserver.SykefraværstilfelleIkkeFunnet(
                skjæringstidspunkt = skjæringstidspunkt,
            ),
        )
    }
}
