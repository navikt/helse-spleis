package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.Sykepengegrunnlag

class OverstyrArbeidsforhold(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()), OverstyrSykepengegrunnlag {

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    override fun dokumentsporing() = Dokumentsporing.overstyrArbeidsforhold(meldingsreferanseId())

    override fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(person: Person, jurist: MaskinellJurist) {
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(this, this.skjæringstidspunkt, jurist)
    }

    internal fun overstyr(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        return overstyrteArbeidsforhold.fold(sykepengegrunnlag, ) { acc, overstyring ->
            overstyring.overstyr(acc, subsumsjonObserver)
        }
    }

    internal fun overstyr(opptjening: Opptjening, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return overstyrteArbeidsforhold.fold(opptjening, ) { acc, overstyring ->
            overstyring.overstyr(acc, subsumsjonObserver)
        }
    }

    class ArbeidsforholdOverstyrt(
        internal val orgnummer: String,
        private val deaktivert: Boolean,
        private val forklaring: String
    ) {
        internal fun overstyr(sykepengegrunnlag: Sykepengegrunnlag, subsumsjonObserver: SubsumsjonObserver) = when (deaktivert) {
            true -> sykepengegrunnlag.deaktiver(orgnummer, forklaring, subsumsjonObserver)
            else -> sykepengegrunnlag.aktiver(orgnummer, forklaring, subsumsjonObserver)
        }

        internal fun overstyr(opptjening: Opptjening, subsumsjonObserver: SubsumsjonObserver) = when (deaktivert) {
            true -> opptjening.deaktiver(orgnummer, subsumsjonObserver)
            else -> opptjening.aktiver(orgnummer, subsumsjonObserver)
        }
    }
}
