package no.nav.helse

import java.time.LocalDate

internal interface AlderVisitor {
    fun visitAlder(alder: Alder, fødselsdato: LocalDate) {}
}