package no.nav.helse.person

import java.util.Objects
import java.util.UUID
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.memento.DokumentsporingMemento
import no.nav.helse.memento.DokumenttypeMemento

class Dokumentsporing private constructor(private val id: UUID, private val dokumentType: DokumentType) {

    companion object {
        internal fun sykmelding(id: UUID) = Dokumentsporing(id, DokumentType.Sykmelding)
        internal fun søknad(id: UUID) = Dokumentsporing(id, DokumentType.Søknad)
        internal fun inntektsmeldingInntekt(id: UUID) = Dokumentsporing(id, DokumentType.InntektsmeldingInntekt)
        internal fun inntektsmeldingDager(id: UUID) = Dokumentsporing(id, DokumentType.InntektsmeldingDager)
        internal fun overstyrTidslinje(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrTidslinje)
        internal fun overstyrInntekt(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrInntekt)
        internal fun overstyrRefusjon(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrRefusjon)
        internal fun overstyrArbeidsgiveropplysninger(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrArbeidsgiveropplysninger)
        internal fun overstyrArbeidsforhold(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrArbeidsforhold)
        internal fun skjønnsmessigFastsettelse(id: UUID) = Dokumentsporing(id, DokumentType.SkjønnsmessigFastsettelse)
        internal fun grunnbeløpendring(id: UUID) = Dokumentsporing(id, DokumentType.SkjønnsmessigFastsettelse) // TODO: bytte DokumentType

        internal fun Iterable<Dokumentsporing>.toJsonList() = map { it.id to it.dokumentType }
        internal fun Iterable<Dokumentsporing>.ider() = map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.søknadIder() = filter { it.dokumentType == DokumentType.Søknad }.map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.sisteInntektsmeldingId() = lastOrNull { it.dokumentType == DokumentType.InntektsmeldingDager }?.id

        internal fun Iterable<Dokumentsporing>.tilSubsumsjonsformat() = associate {
            it.id to when (it.dokumentType) {
                DokumentType.Sykmelding -> KontekstType.Sykmelding
                DokumentType.Søknad -> KontekstType.Søknad
                DokumentType.InntektsmeldingDager -> KontekstType.Inntektsmelding
                DokumentType.InntektsmeldingInntekt -> KontekstType.Inntektsmelding
                DokumentType.OverstyrTidslinje -> KontekstType.OverstyrTidslinje
                DokumentType.OverstyrInntekt -> KontekstType.OverstyrInntekt
                DokumentType.OverstyrRefusjon -> KontekstType.OverstyrRefusjon
                DokumentType.OverstyrArbeidsgiveropplysninger -> KontekstType.OverstyrArbeidsgiveropplysninger
                DokumentType.OverstyrArbeidsforhold -> KontekstType.OverstyrArbeidsforhold
                DokumentType.SkjønnsmessigFastsettelse -> KontekstType.SkjønnsmessigFastsettelse
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Dokumentsporing) return false
        if (other === this) return true
        return this.id == other.id && this.dokumentType == other.dokumentType
    }
    override fun hashCode(): Int {
        return Objects.hash(id, dokumentType)
    }
    override fun toString() = "$dokumentType ($id)"

    internal fun memento() = DokumentsporingMemento(
        id = this.id,
        type = when (dokumentType) {
            DokumentType.Sykmelding -> DokumenttypeMemento.Sykmelding
            DokumentType.Søknad -> DokumenttypeMemento.Søknad
            DokumentType.InntektsmeldingInntekt -> DokumenttypeMemento.InntektsmeldingInntekt
            DokumentType.InntektsmeldingDager -> DokumenttypeMemento.InntektsmeldingDager
            DokumentType.OverstyrTidslinje -> DokumenttypeMemento.OverstyrTidslinje
            DokumentType.OverstyrInntekt -> DokumenttypeMemento.OverstyrInntekt
            DokumentType.OverstyrRefusjon -> DokumenttypeMemento.OverstyrRefusjon
            DokumentType.OverstyrArbeidsgiveropplysninger -> DokumenttypeMemento.OverstyrArbeidsgiveropplysninger
            DokumentType.OverstyrArbeidsforhold -> DokumenttypeMemento.OverstyrArbeidsforhold
            DokumentType.SkjønnsmessigFastsettelse -> DokumenttypeMemento.SkjønnsmessigFastsettelse
        }
    )
}

