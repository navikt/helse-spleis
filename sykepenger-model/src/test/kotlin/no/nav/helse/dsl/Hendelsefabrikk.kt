package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad

internal class Hendelsefabrikk(
    private val aktørId: String,
    private val fødselsnummer: Fødselsnummer,
    private val organisasjonsnummer: String
) {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()

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

    internal fun lagSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        id: UUID = UUID.randomUUID(),
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNAVEllerArbeidsgiver: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        sykmeldingSkrevet: LocalDateTime? = null
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = fødselsnummer.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay()
        ).apply {
            søknader.add(this)
        }
    }

}
