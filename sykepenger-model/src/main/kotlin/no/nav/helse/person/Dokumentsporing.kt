package no.nav.helse.person

import java.util.UUID

data class Dokumentsporing private constructor(private val id: UUID, private val dokumentType: DokumentType) {

    companion object {
        internal fun sykmelding(id: UUID) = Dokumentsporing(id, DokumentType.Sykmelding)
        internal fun søknad(id: UUID) = Dokumentsporing(id, DokumentType.Søknad)
        internal fun inntektsmelding(id: UUID) = Dokumentsporing(id, DokumentType.Inntektsmelding)
        internal fun overstyrTidslinje(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrTidslinje)
        internal fun overstyrInntekt(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrInntekt)
        internal fun overstyrRefusjon(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrRefusjon)
        internal fun overstyrArbeidsgiveropplysninger(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrArbeidsgiveropplysninger)
        internal fun overstyrArbeidsforhold(id: UUID) = Dokumentsporing(id, DokumentType.OverstyrArbeidsforhold)

        internal fun Iterable<Dokumentsporing>.toMap() = associate { it.id to it.dokumentType }
        internal fun Iterable<Dokumentsporing>.ider() = map { it.id }.toSet()
        internal fun Iterable<Dokumentsporing>.søknadIder() = filter { it.dokumentType == DokumentType.Søknad }.map { it.id }.toSet()
        internal fun Map<UUID, DokumentType>.tilSporing() = map { Dokumentsporing(it.key, it.value) }.toSet()
    }

    internal enum class DokumentType {
        Sykmelding,
        Søknad,
        Inntektsmelding,
        OverstyrTidslinje,
        OverstyrInntekt,
        OverstyrRefusjon,
        OverstyrArbeidsgiveropplysninger,
        OverstyrArbeidsforhold,
    }

    internal fun toMap() = mapOf(id.toString() to dokumentType.name)
}

