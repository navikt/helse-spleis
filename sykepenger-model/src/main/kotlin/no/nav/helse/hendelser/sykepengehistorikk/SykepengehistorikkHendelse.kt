package no.nav.helse.hendelser.sykepengehistorikk

import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse

class SykepengehistorikkHendelse(private val sykepengehistorikk: Sykepengehistorikk): ArbeidstakerHendelse,
    VedtaksperiodeHendelse {

    override fun vedtaksperiodeId() =
        sykepengehistorikk.vedtaksperiodeId

    override fun aktørId() =
        sykepengehistorikk.aktørId

    override fun fødselsnummer() =
        sykepengehistorikk.fødselsnummer

    fun sisteFraværsdag() =
            sykepengehistorikk.perioder.maxBy { it.tom }?.tom

    override fun organisasjonsnummer(): String =
            sykepengehistorikk.organisasjonsnummer
}
