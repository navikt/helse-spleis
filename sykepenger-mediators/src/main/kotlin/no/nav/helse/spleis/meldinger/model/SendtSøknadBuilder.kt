package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Merknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Papirsykmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class SendtSøknadBuilder : SøknadBuilder() {
    private val perioder = mutableListOf<Søknadsperiode>()
    private val merkander = mutableListOf<Merknad>()
    private val tilkomneInntekter = mutableListOf<TilkommenInntekt>()
    private var opprinneligSendt: LocalDateTime? = null
    private var harAndreInntektskilder: Boolean = false
    private var ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean = false
    private var utenlandskSykmelding: Boolean = false
    private var sendTilGosys: Boolean = false
    private var søknadstype = Søknad.Søknadstype.Arbeidstaker

    internal fun build() = Søknad(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        perioder = perioder,
        andreInntektskilder = harAndreInntektskilder,
        ikkeJobbetIDetSisteFraAnnetArbeidsforhold = ikkeJobbetIDetSisteFraAnnetArbeidsforhold,
        sendtTilNAVEllerArbeidsgiver = innsendt!!,
        permittert = permittert,
        merknaderFraSykmelding = merkander,
        sykmeldingSkrevet = sykmeldingSkrevet,
        opprinneligSendt = opprinneligSendt,
        utenlandskSykmelding = utenlandskSykmelding,
        arbeidUtenforNorge = arbeidUtenforNorge,
        sendTilGosys = sendTilGosys,
        yrkesskade = yrkesskade,
        egenmeldinger = egenmeldinger,
        registrert = registrert,
        søknadstype = søknadstype,
        tilkomneInntekter = tilkomneInntekter
    )

    fun arbeidsledigsøknad() {
        søknadstype = Søknad.Søknadstype.Arbeidsledig
    }

    override fun inntektskilde(andreInntektskilder: Boolean) = apply {
        harAndreInntektskilder = andreInntektskilder
    }

    internal fun ikkeJobbetIDetSisteFraAnnetArbeidsforhold(ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean) = apply {
        this.ikkeJobbetIDetSisteFraAnnetArbeidsforhold = ikkeJobbetIDetSisteFraAnnetArbeidsforhold
    }

    override fun utenlandskSykmelding(utenlandsk: Boolean) = apply {
        utenlandskSykmelding = utenlandsk
    }
    override fun sendTilGosys(tilGosys: Boolean) = apply {
        sendTilGosys = tilGosys
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

    fun opprinneligSendt(opprinneligSendt: LocalDateTime) {
        this.opprinneligSendt = opprinneligSendt
    }

    fun tilkommenInntekt(fom: LocalDate, tom: LocalDate, orgnummer: String, beløp: Int) {
        tilkomneInntekter.add(
            TilkommenInntekt(
                fom = fom,
                tom = tom,
                orgnummer = orgnummer,
                beløp = beløp.daglig
            )
        )
    }
}
