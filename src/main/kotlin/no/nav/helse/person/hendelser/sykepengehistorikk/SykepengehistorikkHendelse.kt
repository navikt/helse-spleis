package no.nav.helse.person.hendelser.sykepengehistorikk

import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate

class SykepengehistorikkHendelse(private val sykepengehistorikk: Sykepengehistorikk): ArbeidstakerHendelse, SakskompleksHendelse {

    override fun sakskompleksId() =
        sykepengehistorikk.sakskompleksId

    override fun aktørId() =
        sykepengehistorikk.aktørId

    fun sisteFraværsdag(periodeTom: LocalDate? = null) =
            sykepengehistorikk.perioder
                    .filter { if (periodeTom != null) it.tom < periodeTom else true }
                    .maxBy { it.tom }?.tom

    override fun organisasjonsnummer(): String =
            sykepengehistorikk.organisasjonsnummer
}
