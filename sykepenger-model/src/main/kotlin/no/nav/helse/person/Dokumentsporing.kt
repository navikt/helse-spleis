package no.nav.helse.person

import java.util.Objects
import java.util.UUID

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

        // overskriver potensielt dersom en vedtaksperiode har håndtert en hendelse to ganger (les: InntektsmeldingDager/InntektsmeldingInntekt)
        internal fun Iterable<Dokumentsporing>.toMap() = associate { it.id to it.dokumentType }
        internal fun Iterable<Dokumentsporing>.toJsonList() = map { it.id to it.dokumentType }
        internal fun Iterable<Dokumentsporing>.ider() = map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.søknadIder() = filter { it.dokumentType == DokumentType.Søknad }.map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.sisteInntektsmeldingId() = lastOrNull { it.dokumentType == DokumentType.InntektsmeldingDager }?.id
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
}

