package no.nav.helse.sykdomstidslinje

import java.time.LocalDateTime

interface KildeHendelse : Comparable<KildeHendelse>{
    fun rapportertdato(): LocalDateTime
}
