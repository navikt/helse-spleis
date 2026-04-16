package no.nav.helse.dsl

import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.single
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.somOrganisasjonsnummer
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.assertNotNull

class Behovshåndterer(private val behovsoppsamler: Behovsoppsamler): EventSubscription {
    private val tilstander = mutableMapOf<UUID, TilstandType>()
    private val uhåndterteInntektsmeldinger = mutableMapOf<UUID, Inntektsmeldingdetaljer>()

    fun utbetalingsdetaljer(orgnummer: String): List<Behovsoppsamler.Behovsdetaljer.Utbetaling> {
        return behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Utbetaling>()
            .filter { it.organisasjonsnummer == orgnummer }
            .groupBy { "${it.utbetalingId}-${it.fagsystemId}" }
            // velger bare siste behov per utbetalingId-fagsystemId-kombinasjon for å håndtere at vedtaksperioden kan ha blitt påminnet og produsert behovet flere ganger
            .mapValues { (_, utbetalingsdetaljer) -> utbetalingsdetaljer.last() }
            .values
            .toList()
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om utbetaling, men det var det ikke!") }
    }

    fun simuleringsdetaljer(vedtaksperiodeId: UUID) =
        behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Simulering>().filter { it.vedtaksperiodeId == vedtaksperiodeId }
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om simulering, men det var det ikke!") }

    fun godkjenningsdetaljer(vedtaksperiodeId: UUID): Behovsoppsamler.Behovsdetaljer.Godkjenning {
        val godkjenningsdetaljer = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Godkjenning>().filter { it.vedtaksperiodeId == vedtaksperiodeId }
        assert(godkjenningsdetaljer.size == 1) { "Forventet at det skulle være forspurt nøyaktig én godkjenning. Fant ${godkjenningsdetaljer.size}"}
        return godkjenningsdetaljer.single()
    }

    fun feriepengerutbetalingsdetaljer() =
        behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Feriepengeutbetaling>()
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om feriepengerutbetaling, men det var det ikke!") }

    fun bekreftForespurtVilkårsprøving(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilVilkårsprøving>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til vilkårsprøving. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    fun bekreftForespurtBeregningAvSelvstendig(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilBeregningAvSelvstendig>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til beregning av selvstendig periode. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    fun bekreftForespurtBeregningAvArbeidstaker(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilBeregningAvSelvstendig>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til beregning av arbeidstaker-periode. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    override fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        tilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }

    override fun inntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent) {
        uhåndterteInntektsmeldinger.remove(event.meldingsreferanseId)
    }

    private fun inntektsmeldingReplayBehovAkkuratNå() = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InntektsmeldingReplay>().toSet()

    private fun initiellHistorikFraInfotrygdBehovAkkuratNå() = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InitiellHistorikFraInfotrygd>().toSet()

    fun håndterBehovSomOppstårAutomatisk(
        operasjon: () -> Unit,
        hendelse: Hendelse,
        håndterInntektsmeldingerReplay: (inntektsmeldingerReplay: InntektsmeldingerReplay) -> Unit,
        håndterInitiellHistorikkFraInfotrygd: (utbetalingshistorikk: Utbetalingshistorikk) -> Unit
    ) {
        val inntektsmeldingReplayBehovFør = inntektsmeldingReplayBehovAkkuratNå()
        val initiellHistorikFraInfotrygdBehovFør = initiellHistorikFraInfotrygdBehovAkkuratNå()

        (hendelse as? Inntektsmelding)?.let { inntektsmelding ->
            uhåndterteInntektsmeldinger[inntektsmelding.metadata.meldingsreferanseId.id] = Inntektsmeldingdetaljer(inntektsmelding)
        }
        operasjon()

        val nyeInntektsmeldingReplayBehov = inntektsmeldingReplayBehovAkkuratNå() - inntektsmeldingReplayBehovFør
        val nyeInitiellHistorikFraInfotrygdBehov =  initiellHistorikFraInfotrygdBehovAkkuratNå() - initiellHistorikFraInfotrygdBehovFør

        nyeInitiellHistorikFraInfotrygdBehov.forEach { initiellHistorikFraInfotrygdBehov ->
            val fabrikk = initiellHistorikFraInfotrygdBehov.yrkesaktivitetssporing.let { ArbeidsgiverHendelsefabrikk(it.somOrganisasjonsnummer, it) }
            val løsning = fabrikk.lagUtbetalingshistorikk(initiellHistorikFraInfotrygdBehov.vedtaksperiodeId)
            håndterInitiellHistorikkFraInfotrygd(løsning)
        }

        nyeInntektsmeldingReplayBehov.forEach { trengerInntektsmeldingReplay ->
            val fabrikk = trengerInntektsmeldingReplay.forespørsel.orgnr.let { ArbeidsgiverHendelsefabrikk(it, Behandlingsporing.Yrkesaktivitet.Arbeidstaker(it)) }
            val inntektsmeldinger = uhåndterteInntektsmeldinger.values
                .filter { inntektsmeldingdetaljer -> trengerInntektsmeldingReplay.forespørsel.orgnr == inntektsmeldingdetaljer.inntektsmelding.behandlingsporing.organisasjonsnummer}
                .filter { inntektsmeldingdetaljer -> trengerInntektsmeldingReplay.forespørsel.erInntektsmeldingRelevant(inntektsmeldingdetaljer.eksternKontrakt) }
                .map { it.inntektsmelding }
                .sortedBy { it.metadata.innsendt }

            val løsning = fabrikk.lagInntektsmeldingReplay(
                vedtaksperiodeId = trengerInntektsmeldingReplay.vedtaksperiodeId,
                inntektsmeldinger = inntektsmeldinger
            )
            behovsoppsamler.besvart(trengerInntektsmeldingReplay)
            håndterInntektsmeldingerReplay(løsning)
        }
    }

    private class Inntektsmeldingdetaljer private constructor(
        val inntektsmelding: Inntektsmelding,
        val eksternKontrakt: no.nav.inntektsmeldingkontrakt.Inntektsmelding
    ) {
        constructor(inntektsmelding: Inntektsmelding): this(inntektsmelding.kopier(), inntektsmelding.somEksternKontrakt())
        private companion object {
            // Må lage en kopi av inntektsmeldingen slik at Spleis får håndtere den helt på ny
            // .. er enkelte verdier rundt daghåndtering og refusjon som blir lagret i selve objektet, så blir feil å sende inn samme objektet på nytt
            private fun Inntektsmelding.kopier() = Inntektsmelding(
                meldingsreferanseId = metadata.meldingsreferanseId,
                refusjon = refusjon,
                behandlingsporing = behandlingsporing,
                beregnetInntekt = faktaavklartInntekt.inntektsdata.beløp,
                arbeidsgiverperioder = arbeidsgiverperioder,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                opphørAvNaturalytelser = opphørAvNaturalytelser,
                førsteFraværsdag = førsteFraværsdag,
                mottatt = metadata.innsendt,
                arbeidsforholdId = arbeidsforholdId
            )
            private fun Inntektsmelding.somEksternKontrakt(): no.nav.inntektsmeldingkontrakt.Inntektsmelding {
                val beregnetInntekt = faktaavklartInntekt.inntektsdata.beløp
                return no.nav.inntektsmeldingkontrakt.Inntektsmelding(
                    inntektsmeldingId = UUID.randomUUID().toString(),
                    arbeidstakerFnr = "fnr",
                    arbeidstakerAktorId = "aktør",
                    virksomhetsnummer = behandlingsporing.organisasjonsnummer,
                    arbeidsgiverFnr = null,
                    arbeidsgiverAktorId = null,
                    arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                    arbeidsforholdId = null,
                    beregnetInntekt = BigDecimal.valueOf(beregnetInntekt.månedlig),
                    refusjon = Refusjon(BigDecimal.valueOf(beregnetInntekt.månedlig), null),
                    endringIRefusjoner = emptyList(),
                    opphoerAvNaturalytelser = emptyList(),
                    gjenopptakelseNaturalytelser = emptyList(),
                    arbeidsgiverperioder = arbeidsgiverperioder.map {
                        no.nav.inntektsmeldingkontrakt.Periode(it.start, it.endInclusive)
                    },
                    status = Status.GYLDIG,
                    arkivreferanse = "",
                    ferieperioder = emptyList(),
                    foersteFravaersdag = førsteFraværsdag,
                    mottattDato = metadata.innsendt,
                    begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt?.toString(),
                    naerRelasjon = null,
                    avsenderSystem = AvsenderSystem("SpleisModell"),
                    innsenderTelefon = "tlfnr",
                    innsenderFulltNavn = "SPLEIS Modell"
                )
            }
        }
    }
}
