package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime
): PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()), OverstyrInntektsgrunnlag {

    override fun erRelevant(skjæringstidspunkt: LocalDate) =
        this.skjæringstidspunkt == skjæringstidspunkt

    override fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, jurist: BehandlingSubsumsjonslogg) {
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(this, skjæringstidspunkt, jurist)
    }

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