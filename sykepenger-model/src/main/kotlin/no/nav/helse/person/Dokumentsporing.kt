package no.nav.helse.person

import java.util.*
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto

class Dokumentsporing private constructor(val id: UUID, val dokumentType: DokumentType) {

    companion object {
        internal fun søknad(id: UUID) = Dokumentsporing(id, DokumentType.Søknad)
        internal fun inntektsmeldingInntekt(id: UUID) = Dokumentsporing(id, DokumentType.InntektsmeldingInntekt)
        internal fun inntektsmeldingRefusjon(id: UUID) = Dokumentsporing(id, DokumentType.InntektsmeldingRefusjon)
        internal fun inntektsmeldingDager(id: UUID) = Dokumentsporing(id, DokumentType.InntektsmeldingDager)
        internal fun inntektFraAOrdingen(id: UUID) = Dokumentsporing(id, DokumentType.InntektFraAOrdningen)
        internal fun overstyrTidslinje(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrTidslinje)
        internal fun overstyrArbeidsgiveropplysninger(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrArbeidsgiveropplysninger)
        internal fun andreYtelser(id: UUID) = Dokumentsporing(id, DokumentType.AndreYtelser)
        internal fun tilkommenInntektFraSøknad(id: UUID) = Dokumentsporing(id, DokumentType.TilkommenInntektFraSøknad)

        internal fun Iterable<Dokumentsporing>.ider() = filter { it.dokumentType.ekstern }.map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.søknadIder() = filter { it.dokumentType == DokumentType.Søknad }.map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.sisteInntektsmeldingDagerId() = lastOrNull { it.dokumentType == DokumentType.InntektsmeldingDager }?.id
        internal fun Iterable<Dokumentsporing>.sisteInntektsmeldingInntektId() = lastOrNull { it.dokumentType == DokumentType.InntektsmeldingInntekt }?.id

        internal fun gjenopprett(dto: DokumentsporingDto): Dokumentsporing {
            return Dokumentsporing(
                id = dto.id,
                dokumentType = when (dto.type) {
                    DokumenttypeDto.InntektsmeldingDager -> DokumentType.InntektsmeldingDager
                    DokumenttypeDto.InntektsmeldingInntekt -> DokumentType.InntektsmeldingInntekt
                    DokumenttypeDto.InntektsmeldingRefusjon -> DokumentType.InntektsmeldingRefusjon
                    DokumenttypeDto.InntektFraAOrdningen -> DokumentType.InntektFraAOrdningen
                    DokumenttypeDto.OverstyrArbeidsforhold -> DokumentType.OverstyrArbeidsforhold
                    DokumenttypeDto.OverstyrArbeidsgiveropplysninger -> DokumentType.OverstyrArbeidsgiveropplysninger
                    DokumenttypeDto.OverstyrInntekt -> DokumentType.OverstyrInntekt
                    DokumenttypeDto.OverstyrRefusjon -> DokumentType.OverstyrRefusjon
                    DokumenttypeDto.OverstyrTidslinje -> DokumentType.OverstyrTidslinje
                    DokumenttypeDto.SkjønnsmessigFastsettelse -> DokumentType.SkjønnsmessigFastsettelse
                    DokumenttypeDto.Sykmelding -> DokumentType.Sykmelding
                    DokumenttypeDto.Søknad -> DokumentType.Søknad
                    DokumenttypeDto.AndreYtelser -> DokumentType.AndreYtelser
                    DokumenttypeDto.TilkommenInntektFraSøknad -> DokumentType.TilkommenInntektFraSøknad
                }
            )
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

    internal fun dto() = DokumentsporingDto(
        id = this.id,
        type = when (dokumentType) {
            DokumentType.Sykmelding -> DokumenttypeDto.Sykmelding
            DokumentType.Søknad -> DokumenttypeDto.Søknad
            DokumentType.InntektsmeldingInntekt -> DokumenttypeDto.InntektsmeldingInntekt
            DokumentType.InntektsmeldingRefusjon -> DokumenttypeDto.InntektsmeldingRefusjon
            DokumentType.InntektsmeldingDager -> DokumenttypeDto.InntektsmeldingDager
            DokumentType.InntektFraAOrdningen -> DokumenttypeDto.InntektFraAOrdningen
            DokumentType.OverstyrTidslinje -> DokumenttypeDto.OverstyrTidslinje
            DokumentType.OverstyrInntekt -> DokumenttypeDto.OverstyrInntekt
            DokumentType.OverstyrRefusjon -> DokumenttypeDto.OverstyrRefusjon
            DokumentType.OverstyrArbeidsgiveropplysninger -> DokumenttypeDto.OverstyrArbeidsgiveropplysninger
            DokumentType.OverstyrArbeidsforhold -> DokumenttypeDto.OverstyrArbeidsforhold
            DokumentType.SkjønnsmessigFastsettelse -> DokumenttypeDto.SkjønnsmessigFastsettelse
            DokumentType.AndreYtelser -> DokumenttypeDto.AndreYtelser
            DokumentType.TilkommenInntektFraSøknad -> DokumenttypeDto.TilkommenInntektFraSøknad
        }
    )
}

