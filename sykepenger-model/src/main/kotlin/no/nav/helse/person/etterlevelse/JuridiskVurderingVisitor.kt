package no.nav.helse.person.etterlevelse

import java.time.LocalDate

interface GrupperbarVurderingVisitor {
    fun visitVurdering(fom: LocalDate, tom: LocalDate) {}
}
