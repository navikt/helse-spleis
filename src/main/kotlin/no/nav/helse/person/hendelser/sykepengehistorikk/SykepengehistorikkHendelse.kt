package no.nav.helse.person.hendelser.sykepengehistorikk

import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.ArbeidstakerHendelse

class SykepengehistorikkHendelse(private val sykepengehistorikk: Sykepengehistorikk): ArbeidstakerHendelse, SakskompleksHendelse {

    override fun sakskompleksId() =
        sykepengehistorikk.sakskompleksId

    override fun aktørId() =
        sykepengehistorikk.aktørId

    fun sisteFraværsdag() =
            sykepengehistorikk.perioder.maxBy { it.tom }?.tom

    override fun organisasjonsnummer(): String =
            sykepengehistorikk.organisasjonsnummer
}
