package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import java.util.UUID

internal class SendtSøknadBuilder : SøknadBuilder() {
    private val perioder = mutableListOf<Søknadsperiode>()
    private val merkander = mutableListOf<Merknad>()
    private val inntektskilder = mutableListOf<Inntektskilde>()
    private var korrigerer: UUID? = null

    internal fun build() = Søknad(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        fødselsdato = fødselsdato,
        orgnummer = organisasjonsnummer,
        perioder = perioder,
        andreInntektskilder = inntektskilder,
        sendtTilNAVEllerArbeidsgiver = innsendt!!,
        permittert = permittert,
        merknaderFraSykmelding = merkander,
        sykmeldingSkrevet = sykmeldingSkrevet,
        korrigerer = korrigerer
    )

    override fun inntektskilde(sykmeldt: Boolean, type: String) = apply {
        inntektskilder.add(Inntektskilde(sykmeldt = sykmeldt, type = type))
    }

    override fun utdanning(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Utdanning(fom, tom))
    }

    override fun permisjon(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Permisjon(fom, tom))
    }

    override fun ferie(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Ferie(fom, tom))
    }

    override fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Utlandsopphold(fom, tom))
    }

    override fun merknader(type: String, beskrivelse: String?) = apply {
        merkander.add(Merknad(type))
    }

    override fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Papirsykmelding(fom = fom, tom = tom))
    }

    override fun egenmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Egenmelding(fom = fom, tom = tom))
    }

    override fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Arbeid(fom, tom))
    }

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        perioder.add(
            Sykdom(
                fom = fom,
                tom = tom,
                sykmeldingsgrad = grad.prosent,
                arbeidshelse = arbeidshelse?.prosent
            )
        )
    }

    fun korrigerer(korrigerer: UUID) {
        this.korrigerer = korrigerer
    }
}
