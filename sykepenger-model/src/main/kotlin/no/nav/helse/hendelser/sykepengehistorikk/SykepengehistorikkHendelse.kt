package no.nav.helse.hendelser.sykepengehistorikk

import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.sak.ArbeidstakerHendelse

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
