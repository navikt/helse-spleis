package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.ArbeidsgiveropplysningerKorrigertEvent.KorrigerendeInntektektsopplysningstype.SAKSBEHANDLER
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Sykepengegrunnlag

class OverstyrArbeidsgiveropplysninger(
    private val meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg), OverstyrSykepengegrunnlag {
    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt
    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrArbeidsgiveropplysninger(meldingsreferanseId()))
    }

    override fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, jurist: MaskinellJurist) {
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
            this,
            this.skjæringstidspunkt,
            jurist
        )
    }

    internal fun overstyr(builder: Sykepengegrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger.forEach { builder.leggTilInntekt(it) }
    }

    internal fun arbeidsgiveropplysningerKorrigert(person: Person, orgnummer: String, hendelseId: UUID) {
        if(arbeidsgiveropplysninger.any { it.gjelder(orgnummer) }) {
            person.arbeidsgiveropplysningerKorrigert(
                PersonObserver.ArbeidsgiveropplysningerKorrigertEvent(
                    korrigertInntektsmeldingId = hendelseId,
                    korrigerendeInntektektsopplysningstype = SAKSBEHANDLER,
                    korrigerendeInntektsopplysningId = meldingsreferanseId
                )
            )
        }
    }
}
