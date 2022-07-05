package no.nav.helse.dsl

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode

internal class Hendelsefabrikk(
    private val aktørId: String,
    private val fødselsnummer: Fødselsnummer,
    private val organisasjonsnummer: String
) {

    private val sykmeldinger = mutableListOf<Sykmelding>()

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID(),
        sykmeldingSkrevet: LocalDateTime = Sykmeldingsperiode.periode(sykeperioder.toList())!!.start.atStartOfDay(),
        mottatt: LocalDateTime? = null,
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = fødselsnummer.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            sykeperioder = listOf(*sykeperioder),
            sykmeldingSkrevet = sykmeldingSkrevet,
            mottatt = mottatt ?: sykmeldingSkrevet
        ).apply {
            sykmeldinger.add(this)
        }
    }

}
