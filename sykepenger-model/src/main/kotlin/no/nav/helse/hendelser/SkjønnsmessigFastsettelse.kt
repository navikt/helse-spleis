package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Sykepengegrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer

class SkjønnsmessigFastsettelse(
    private val meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {
    internal fun håndter(vedtaksperioder: List<Vedtaksperiode>) {
        vedtaksperioder.forEach {
            it.skjønsmessigFastsettelse(skjæringstidspunkt, meldingsreferanseId)
        }
    }

    internal fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, subsumsjonObserver: SubsumsjonObserver) =
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(this, skjæringstidspunkt, subsumsjonObserver)

    internal fun overstyr(builder: ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger.forEach { builder.leggTilInntekt(it) }
    }
}
