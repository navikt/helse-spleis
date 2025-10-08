package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Merknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Papirsykmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class SendtSøknadBuilder(arbeidssituasjon: String) : SøknadBuilder() {
    private val perioder = mutableListOf<Søknadsperiode>()
    private val merkander = mutableListOf<Merknad>()
    private var inntekterFraNyeArbeidsforhold: Boolean = false
    private var opprinneligSendt: LocalDateTime? = null
    private var harAndreInntektskilder: Boolean = false
    private var ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean = false
    private var utenlandskSykmelding: Boolean = false
    private var sendTilGosys: Boolean = false
    private var fraværFørSykmelding: Boolean? = null
    private val arbeidssituasjon = when (arbeidssituasjon) {
        "SELVSTENDIG_NARINGSDRIVENDE" -> Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
        "BARNEPASSER" -> Søknad.Arbeidssituasjon.BARNEPASSER
        "FRILANSER" -> Søknad.Arbeidssituasjon.FRILANSER
        "ARBEIDSTAKER" -> Søknad.Arbeidssituasjon.ARBEIDSTAKER
        "ARBEIDSLEDIG" -> Søknad.Arbeidssituasjon.ARBEIDSLEDIG

        "FISKER" -> Søknad.Arbeidssituasjon.FISKER
        "JORDBRUKER" -> Søknad.Arbeidssituasjon.JORDBRUKER
        "ANNET" -> Søknad.Arbeidssituasjon.ANNET
         else -> error("støtter ikke $arbeidssituasjon")
    }
    internal fun build(meldingsporing: Meldingsporing) = Søknad(
        meldingsreferanseId = meldingsporing.id,
        behandlingsporing = behandlingsporing,
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
        arbeidssituasjon = arbeidssituasjon,
        registrert = registrert,
        inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
        pensjonsgivendeInntekter = pensjonsgivendeInntekter,
        fraværFørSykmelding = fraværFørSykmelding
    )

    internal fun pensjonsgivendeInntekter(pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>) = apply {
        this.pensjonsgivendeInntekter = pensjonsgivendeInntekter
    }

    internal fun fraværFørSykmelding(fraværFørSykmelding: Boolean?) = apply {
        this.fraværFørSykmelding = fraværFørSykmelding
    }

    internal fun ventetid(ventetid: Periode) = apply {
        perioder.add(Søknadsperiode.Ventetid(ventetid))
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


    fun inntekterFraNyeArbeidsforhold(inntekterFraNyeArbeidsforhold: Boolean) {
        this.inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold
    }
}
