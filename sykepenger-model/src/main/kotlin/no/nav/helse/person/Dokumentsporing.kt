package no.nav.helse.person

import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.hendelser.MeldingsreferanseId

data class Dokumentsporing(
    val id: MeldingsreferanseId,
    val dokumentType: DokumentType
) {

    companion object {
        internal fun søknad(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.Søknad)
        internal fun inntektsmeldingInntekt(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.InntektsmeldingInntekt)
        internal fun inntektsmeldingRefusjon(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.InntektsmeldingRefusjon)
        internal fun inntektsmeldingDager(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.InntektsmeldingDager)
        internal fun inntektFraAOrdingen(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.InntektFraAOrdningen)
        internal fun overstyrTidslinje(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.OverstyrTidslinje)
        internal fun overstyrArbeidsgiveropplysninger(id: MeldingsreferanseId) = Dokumentsporing(id, DokumentType.OverstyrArbeidsgiveropplysninger)

        internal fun Iterable<Dokumentsporing>.eksterneIder() = filter { it.dokumentType.ekstern }.map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.ider() = eksterneIder().map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.søknadIder() = filter { it.dokumentType == DokumentType.Søknad }.map { it.id }.toSet()

        internal fun gjenopprett(dto: DokumentsporingDto): Dokumentsporing {
            return Dokumentsporing(
                id = MeldingsreferanseId.gjenopprett(dto.id),
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
                }
            )
        }
    }

    override fun toString() = "$dokumentType ($id)"

    internal fun dto() = DokumentsporingDto(
        id = this.id.dto(),
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
        }
    )
}

