package no.nav.helse.person

import java.util.Objects
import java.util.UUID

internal class InntektsmeldingInfo(
    private val id: UUID,
    private val arbeidsforholdId: String?
) {

    internal fun leggTil(hendelser: MutableSet<Dokumentsporing>) {
        hendelser.add(Dokumentsporing.inntektsmelding(id))
    }

    internal fun accept(visitor: InntektsmeldingInfoVisitor) {
        visitor.visitInntektsmeldinginfo(id, arbeidsforholdId)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is InntektsmeldingInfo) return false
        return this.id == other.id && this.arbeidsforholdId == other.arbeidsforholdId
    }

    override fun hashCode() = Objects.hash(id, arbeidsforholdId)

    internal companion object {
        fun List<InntektsmeldingInfo>.ider() = map { it.id }
    }
}