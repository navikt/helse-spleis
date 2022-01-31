package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.*
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class RefusjonsgjødslerTest {

    @BeforeEach
    fun før() {
        resetSeed()
    }

    @Test
    fun `Gjødsler utbetalingslinje med full refusjon i januar`() {
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.januar, 2862.daglig))
        val refusjonshistorikk = refusjonshistorikk(refusjon(1.januar, 1431.daglig))
        val refusjonsgjødsler = refusjonsgjødsler(utbetalingstidslinje, refusjonshistorikk)
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.januar til 26.januar)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1431.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje uten refusjon i januar`() {
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.januar, 1431.daglig))
        val refusjonsgjødsler = refusjonsgjødsler(utbetalingstidslinje, refusjonshistorikk(refusjon(1.januar, null)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.januar til 26.januar)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 0.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }


    @Test
    fun `Gjødsler utbetalingslinje med full refusjon i februar`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = refusjonsgjødsler(utbetalingstidslinje, refusjonshistorikk(refusjon(1.februar, 1154.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.februar til 26.februar)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1154.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje hvor vi ikke finner noe refusjon (Infotrygd⁉) - Legger på warning og antar full refusjon`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = refusjonsgjødsler(utbetalingstidslinje, refusjonshistorikk())
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.februar til 26.februar)
        // Helger i arbeidsgiverperioden har inntekt
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 16.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[17.februar til 18.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[19.februar til 23.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[24.februar til 25.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[26.februar til 26.februar], 2308.0)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje med flere sammenhengende sykdomsperioder henter riktig refusjon for perioden`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S + 2.A + 10.S).utbetalingstidslinje(
            inntektsopplysning(1.februar, 2308.daglig) + inntektsopplysning(1.mars, 2500.daglig)
        )
        val refusjonsgjødsler = refusjonsgjødsler(utbetalingstidslinje, refusjonshistorikk(refusjon(1.februar, 2308.daglig), refusjon(1.mars, 2500.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.mars til 10.mars)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 26.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[27.februar til 28.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.mars til 10.mars], 2500.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje med flere sammenhengende sykdomsperioder og oppdelt arbeidsgiverperiode henter riktig refusjon for perioden`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (8.U + 10.A + 8.U + 2.S).utbetalingstidslinje(
            inntektsopplysning(19.februar, 2308.daglig)
        )
        val refusjonsgjødsler = refusjonsgjødsler(
            utbetalingstidslinje, refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 19.februar,
                    beløp = 2308.daglig,
                    arbeidsgiverperioder = listOf(1.februar til 8.februar, 19.februar til 26.februar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg, 1.februar til 28.februar)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 18.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[19.februar til 28.februar], 2308.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje hvor dagen før en arbeidsgiverperiode er en fridag`() {
        val utbetalingstidslinje = (8.U + 26.opphold + 16.U + 2.S).utbetalingstidslinje(
            inntektsopplysning(4.februar, 2308.daglig)
        )
        val refusjonsgjødsler = refusjonsgjødsler(
            utbetalingstidslinje, refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 4.februar,
                    beløp = 2308.daglig,
                    arbeidsgiverperioder = listOf(4.februar til 19.februar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        assertDoesNotThrow { refusjonsgjødsler.gjødsle(aktivitetslogg, 4.februar til 21.februar) }
    }

    @Test
    fun `Logger i aktivitetsloggen når vi bruker '16 dagers hopp'`() {
        val utbetalingstidslinje = (31.opphold + 15.S).utbetalingstidslinje(
            inntektsopplysning(1.februar, 2308.daglig),
            strategi = { true }
        )
        val refusjonsgjødsler = refusjonsgjødsler(
            utbetalingstidslinje, refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 1.januar,
                    beløp = 2308.daglig,
                    arbeidsgiverperioder = listOf(1.januar til 16.januar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        assertDoesNotThrow { refusjonsgjødsler.gjødsle(aktivitetslogg, 1.februar til 15.februar) }
        assertEquals(listOf("Fant refusjon ved å gå 16 dager tilbake fra første utbetalingsdag i sammenhengende utbetaling"), aktivitetslogg.infoMeldinger())
    }

    @Test
    fun `Warning når vi mangler refusjon innenfor vedtaksperioden`() {
        val utbetaling = (59.S).utbetalingstidslinje(
            inntektsopplysning(1.januar, 690.daglig)
        )

        val gjødsler = refusjonsgjødsler(utbetaling, refusjonshistorikk())

        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.februar til 28.februar)
        assertEquals(listOf("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."), aktivitetslogg.warningMeldinger())
    }

    @Test
    fun `Ingen warnings når du mangler refusjon utenom vedtaksperioden`() {
        resetSeed(1.januar(2017))
        val infotrygdUtbetaling = (31.S).utbetalingstidslinje(
            inntektsopplysning(1.januar(2017), 690.daglig)
        )
        resetSeed()
        val spleisUtbetaling = (31.S).utbetalingstidslinje(
            inntektsopplysning(1.januar, 420.daglig)
        )

        val gjødsler = refusjonsgjødsler(
            infotrygdUtbetaling + spleisUtbetaling, refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 1.januar,
                    beløp = 420.daglig,
                    arbeidsgiverperioder = listOf(1.januar til 16.januar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.januar til 31.januar)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingsperiode inneholder dager før første dag i arbeidsgiverperioden fører til infomelding`() {
        val gjødsler = refusjonsgjødsler(
            (31.S).utbetalingstidslinje(
                inntektsopplysning(1.januar, 690.daglig)
            ), refusjonshistorikk(
                refusjon(
                    arbeidsgiverperioder = emptyList(),
                    førsteFraværsdag = 17.januar,
                    beløp = 690.daglig
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.januar til 31.januar)
        assertEquals(1, aktivitetslogg.infoMeldinger().count { it == "Refusjon gjelder ikke for hele utbetalingsperioden" })
    }

    @Test
    fun `Legger ikke til ny infomelding ved nye førstegangsbehandlinger`() {
        val gjødsler = refusjonsgjødsler(
            (31.S + 28.opphold + 31.S).utbetalingstidslinje(
                inntektsopplysning(1.januar, 690.daglig) + inntektsopplysning(1.mars, 690.daglig)
            ), refusjonshistorikk(
                refusjon(
                    arbeidsgiverperioder = emptyList(),
                    førsteFraværsdag = 17.januar,
                    beløp = 690.daglig
                ),
                refusjon(
                    arbeidsgiverperioder = emptyList(),
                    førsteFraværsdag = 1.mars,
                    beløp = 690.daglig
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.mars til 31.mars)
        assertEquals(0, aktivitetslogg.infoMeldinger().count { it == "Refusjon gjelder ikke for hele utbetalingsperioden" })
    }

    @Test
    fun `legger ikke på error når brukerutbetalinger i infotrygd ikke overlapper med perioden`() {
        val gjødsler = refusjonsgjødsler(
            utbetalingstidslinje = (31.S).utbetalingstidslinje(inntektsopplysning(1.januar, 690.daglig)),
            refusjonshistorikk = refusjonshistorikk(),
            infotrygdhistorikk = infotrygdhistorikk(listOf(PersonUtbetalingsperiode(ORGNUMMER, 1.desember(2017), 31.desember(2017), 100.prosent, 10000.månedlig))),
            organisasjonsnummer = ORGNUMMER
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.januar til 31.januar)
        assertEquals(
            0,
            aktivitetslogg.errorMeldinger().count { it == "Finner ikke informasjon om refusjon i inntektsmelding og personen har brukerutbetaling" })
    }

    @Test
    fun `legger ikke på error når brukerutbetalinger i infotrygd ikke er for samme arbeidsgiver`() {
        val gjødsler = refusjonsgjødsler(
            utbetalingstidslinje = (31.S).utbetalingstidslinje(inntektsopplysning(1.januar, 690.daglig)),
            refusjonshistorikk = refusjonshistorikk(),
            infotrygdhistorikk = infotrygdhistorikk(listOf(PersonUtbetalingsperiode("987654321", 1.januar, 10.januar, 100.prosent, 10000.månedlig))),
            organisasjonsnummer = ORGNUMMER
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.januar til 31.januar)
        assertEquals(
            0,
            aktivitetslogg.errorMeldinger().count { it == "Finner ikke informasjon om refusjon i inntektsmelding og personen har brukerutbetaling" })
    }

    @Test
    fun `legger på error når brukerutbetalinger i infotrygd overlapper med perioden`() {
        val gjødsler = refusjonsgjødsler(
            utbetalingstidslinje = (31.S).utbetalingstidslinje(inntektsopplysning(1.januar, 690.daglig)),
            refusjonshistorikk = refusjonshistorikk(),
            infotrygdhistorikk = infotrygdhistorikk(listOf(PersonUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 10000.månedlig))),
            organisasjonsnummer = ORGNUMMER
        )
        val aktivitetslogg = Aktivitetslogg()
        gjødsler.gjødsle(aktivitetslogg, 1.januar til 31.januar)
        assertEquals(
            1,
            aktivitetslogg.errorMeldinger().count { it == "Finner ikke informasjon om refusjon i inntektsmelding og personen har brukerutbetaling" })
    }

    private fun infotrygdhistorikk(utbetalingsperioder: List<Infotrygdperiode>) = Infotrygdhistorikk().apply {
        this.oppdaterHistorikk(
            InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = utbetalingsperioder,
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            )
        )
    }

    private fun refusjonsgjødsler(
        utbetalingstidslinje: Utbetalingstidslinje,
        refusjonshistorikk: Refusjonshistorikk,
        infotrygdhistorikk: Infotrygdhistorikk = Infotrygdhistorikk(),
        organisasjonsnummer: String = ORGNUMMER
    ) = Refusjonsgjødsler(
        tidslinje = utbetalingstidslinje,
        refusjonshistorikk = refusjonshistorikk,
        infotrygdhistorikk = infotrygdhistorikk,
        organisasjonsnummer = organisasjonsnummer
    )

    private companion object {

        private const val ORGNUMMER = "123456789"

        operator fun Iterable<Utbetalingstidslinje.Utbetalingsdag>.get(periode: Periode) = filter { it.dato in periode }

        private fun assertRefusjonArbeidsgiver(utbetalingstidslinje: Iterable<Utbetalingstidslinje.Utbetalingsdag>, forventetUkedagbeløp: Double?) {
            utbetalingstidslinje.forEach { utbetalingsdag ->
                assertEquals(forventetUkedagbeløp, utbetalingsdag.økonomi.arbeidsgiverRefusjonsbeløp())
            }
        }

        private fun refusjonshistorikk(vararg refusjoner: Refusjonshistorikk.Refusjon): Refusjonshistorikk {
            val refusjonshistorikk = Refusjonshistorikk()
            refusjoner.forEach { refusjonshistorikk.leggTilRefusjon(it) }
            return refusjonshistorikk
        }

        private fun refusjon(
            førsteFraværsdag: LocalDate,
            beløp: Inntekt?,
            arbeidsgiverperioder: List<Periode> = listOf(førsteFraværsdag til førsteFraværsdag.plusDays(15))
        ) = Refusjonshistorikk.Refusjon(
            meldingsreferanseId = UUID.randomUUID(),
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = arbeidsgiverperioder,
            beløp = beløp,
            sisteRefusjonsdag = null,
            endringerIRefusjon = emptyList()
        )

        private fun Økonomi.arbeidsgiverRefusjonsbeløp() = medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _, _ -> arbeidsgiverRefusjonsbeløp }

        private fun inntektsopplysning(skjæringstidspunkt: LocalDate, inntekt: Inntekt) = mapOf(
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)
        )

        private fun Sykdomstidslinje.utbetalingstidslinje(
            inntektsopplysning: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>,
            strategi: Forlengelsestrategi = Forlengelsestrategi.Ingen
        ): Utbetalingstidslinje {
            val tidslinje = UtbetalingstidslinjeBuilder(
                skjæringstidspunkter = inntektsopplysning.keys.toList(),
                inntektPerSkjæringstidspunkt = inntektsopplysning,
                subsumsjonObserver = MaskinellJurist()
            ).apply { forlengelsestrategi(strategi)}.also { it.build(this, periode()!!) }.result()
            verifiserRekkefølge(tidslinje)
            return tidslinje
        }

        private fun verifiserRekkefølge(tidslinje: Utbetalingstidslinje) {
            tidslinje.windowed(2).forEach { (forrige, neste) ->
                assertTrue(neste.dato > forrige.dato) { "Rekkefølgen er ikke riktig: ${neste.dato} skal være nyere enn ${forrige.dato}" }
            }
        }

        fun Aktivitetslogg.infoMeldinger(): List<String> {
            val meldinger = mutableListOf<String>()

            accept(object : AktivitetsloggVisitor {
                override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                    meldinger.add(melding)
                }
            })

            return meldinger
        }

        fun Aktivitetslogg.warningMeldinger(): List<String> {
            val meldinger = mutableListOf<String>()

            accept(object : AktivitetsloggVisitor {
                override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                    meldinger.add(melding)
                }
            })

            return meldinger
        }

        fun Aktivitetslogg.errorMeldinger(): List<String> {
            val meldinger = mutableListOf<String>()

            accept(object : AktivitetsloggVisitor {
                override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
                    meldinger.add(melding)
                }
            })

            return meldinger
        }
    }
}
