package no.nav.helse.serde.api

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun hendelseReferanserForPerson(person: Person): MutableSet<UUID> {
    val hendelseVisitor = HendelseVisitor()
    person.accept(hendelseVisitor)
    return hendelseVisitor.hendelsereferanser
}

internal class HendelseVisitor : PersonVisitor {
    internal val hendelsereferanser = mutableSetOf<UUID>()

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) {
        if (id == null) return
        hendelsereferanser.add(id)
    }

    override fun visitInntekt(inntektsendring: Inntekthistorikk.Inntektsendring, id: UUID, kilde: Inntekthistorikk.Inntektsendring.Kilde, fom: LocalDate) {
        hendelsereferanser.add(id)
    }
}
