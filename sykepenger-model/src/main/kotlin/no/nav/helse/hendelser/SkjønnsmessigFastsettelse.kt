package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsgrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer

class SkjønnsmessigFastsettelse(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    private val opprettet: LocalDateTime
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg), OverstyrInntektsgrunnlag {

    internal fun overstyr(builder: ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger.forEach { builder.leggTilInntekt(it) }
    }

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    override fun dokumentsporing() = Dokumentsporing.skjønnsmessigFastsettelse(meldingsreferanseId())

    override fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, jurist: MaskinellJurist) {
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(this, skjæringstidspunkt, jurist)
    }

    override fun avsender() = Avsender.SAKSBEHANDLER

    override fun innsendt() = opprettet
}
