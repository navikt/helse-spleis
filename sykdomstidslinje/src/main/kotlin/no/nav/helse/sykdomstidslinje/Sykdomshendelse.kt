package no.nav.helse.sykdomstidslinje

import java.time.LocalDateTime

interface Sykdomshendelse : Comparable<Sykdomshendelse>{
    fun rappertertDato(): LocalDateTime
}
