package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelvstendigEndaEnGodkjenningsbehovTest : AbstractDslTest() {
    @Test
    fun `SelvstendigFaktaavklartInntekt - enda en godkjenningsbehov`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertGodkjenningsbehov(
                tags = setOf("Førstegangsbehandling", "Personutbetaling", "Innvilget", "EnArbeidsgiver"),
                forbrukteSykedager = 11,
                gjenståendeSykedager = 237,
                foreløpigBeregnetSluttPåSykepenger = 28.desember,
                utbetalingsdager = listOf(
                    utbetalingsdag(1.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(2.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(3.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(4.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(5.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(6.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(7.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(8.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(9.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(10.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(11.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(12.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(13.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(14.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(15.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(16.januar, "Ventetidsdag", 0, 100),
                    utbetalingsdag(17.januar, "NavDag", 1417, 100),
                    utbetalingsdag(18.januar, "NavDag", 1417, 100),
                    utbetalingsdag(19.januar, "NavDag", 1417, 100),
                    utbetalingsdag(20.januar, "NavHelgDag", 0, 100),
                    utbetalingsdag(21.januar, "NavHelgDag", 0, 100),
                    utbetalingsdag(22.januar, "NavDag", 1417, 100),
                    utbetalingsdag(23.januar, "NavDag", 1417, 100),
                    utbetalingsdag(24.januar, "NavDag", 1417, 100),
                    utbetalingsdag(25.januar, "NavDag", 1417, 100),
                    utbetalingsdag(26.januar, "NavDag", 1417, 100),
                    utbetalingsdag(27.januar, "NavHelgDag", 0, 100),
                    utbetalingsdag(28.januar, "NavHelgDag", 0, 100),
                    utbetalingsdag(29.januar, "NavDag", 1417, 100),
                    utbetalingsdag(30.januar, "NavDag", 1417, 100),
                    utbetalingsdag(31.januar, "NavDag", 1417, 100)
                ),
                sykepengegrunnlagsfakta = mapOf(
                    "sykepengegrunnlag" to 460_589.0,
                    "6G" to 561_804.0,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to emptyList<Map<String, Any>>(),
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to listOf(
                            mapOf(
                                "årstall" to 2017,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2016,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2015,
                                "beløp" to 450_000.0
                            )
                        ),
                        "beregningsgrunnlag" to 460589.0,

                        ),
                ),
                inntektskilde = "EN_ARBEIDSGIVER"
            )

        }
    }

    private fun assertGodkjenningsbehov(
        tags: Set<String>,
        skjæringstidspunkt: LocalDate = 1.januar,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        vedtaksperiodeId: UUID = 1.vedtaksperiode(selvstendig),
        hendelser: Set<UUID>? = null,
        orgnummere: Set<String> = setOf(selvstendig),
        kanAvvises: Boolean = true,
        periodeType: String = "FØRSTEGANGSBEHANDLING",
        førstegangsbehandling: Boolean = true,
        utbetalingstype: String = "UTBETALING",
        inntektskilde: String,
        behandlingId: UUID = inspektør(selvstendig).vedtaksperioder(1.vedtaksperiode(selvstendig)).behandlinger.behandlinger.last().id,
        perioderMedSammeSkjæringstidspunkt: List<Map<String, String>> = listOf(
            mapOf("vedtaksperiodeId" to 1.vedtaksperiode(selvstendig).toString(), "behandlingId" to 1.vedtaksperiode(selvstendig).sisteBehandlingId().toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
        ),
        forbrukteSykedager: Int = 11,
        gjenståendeSykedager: Int = 237,
        foreløpigBeregnetSluttPåSykepenger: LocalDate = 28.desember,
        utbetalingsdager: List<Map<String, Any>>,
        sykepengegrunnlagsfakta: Map<String, Any>,
    ) {
        val actualtags = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "tags") ?: emptySet()
        val actualSkjæringstidspunkt = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "skjæringstidspunkt")!!
        val actualInntektskilde = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "inntektskilde")!!
        val actualPeriodetype = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodetype")!!
        val actualPeriodeFom = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodeFom")!!
        val actualPeriodeTom = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodeTom")!!
        val actualFørstegangsbehandling = hentFelt<Boolean>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "førstegangsbehandling")!!
        val actualUtbetalingtype = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "utbetalingtype")!!
        val actualOrgnummereMedRelevanteArbeidsforhold = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "orgnummereMedRelevanteArbeidsforhold")!!
        val actualKanAvises = hentFelt<Boolean>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "kanAvvises")!!
        val actualBehandlingId = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "behandlingId")!!
        val actualPerioderMedSammeSkjæringstidspunkt = hentFelt<Any>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "perioderMedSammeSkjæringstidspunkt")!!
        val actualForbrukteSykedager = hentFelt<Int>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "forbrukteSykedager")!!
        val actualGjenståendeSykedager = hentFelt<Int>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "gjenståendeSykedager")!!
        val actualForeløpigBeregnetSluttPåSykepenger = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "foreløpigBeregnetSluttPåSykepenger")!!
        val actualUtbetalingsdager = hentFelt<List<Map<String, Any>>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "utbetalingsdager")!!

        hendelser?.let { assertHendelser(it, vedtaksperiodeId) }
        assertSykepengegrunnlagsfakta(vedtaksperiodeId, sykepengegrunnlagsfakta)
        assertEquals(tags, actualtags)
        assertEquals(skjæringstidspunkt.toString(), actualSkjæringstidspunkt)
        assertEquals(inntektskilde, actualInntektskilde)
        assertEquals(periodeType, actualPeriodetype)
        assertEquals(periodeFom.toString(), actualPeriodeFom)
        assertEquals(periodeTom.toString(), actualPeriodeTom)
        assertEquals(førstegangsbehandling, actualFørstegangsbehandling)
        assertEquals(utbetalingstype, actualUtbetalingtype)
        assertEquals(orgnummere, actualOrgnummereMedRelevanteArbeidsforhold)
        assertEquals(kanAvvises, actualKanAvises)
        assertEquals(behandlingId.toString(), actualBehandlingId)
        assertEquals(perioderMedSammeSkjæringstidspunkt, actualPerioderMedSammeSkjæringstidspunkt)
        assertEquals(forbrukteSykedager, actualForbrukteSykedager)
        assertEquals(gjenståendeSykedager, actualGjenståendeSykedager)
        assertEquals(foreløpigBeregnetSluttPåSykepenger.toString(), actualForeløpigBeregnetSluttPåSykepenger)
        assertEquals(utbetalingsdager, actualUtbetalingsdager)

        val utkastTilVedtak = observatør.utkastTilVedtakEventer.last()
        assertEquals(actualtags, utkastTilVedtak.tags)
        assertEquals(actualBehandlingId, utkastTilVedtak.behandlingId.toString())
        assertEquals(vedtaksperiodeId, utkastTilVedtak.vedtaksperiodeId)
    }

    private inline fun <reified T> hentFelt(vedtaksperiodeId: UUID = 1.vedtaksperiode(selvstendig), feltNavn: String) =
        testperson.personlogg.hentFeltFraBehov<T>(
            vedtaksperiodeId = vedtaksperiodeId,
            behov = Aktivitet.Behov.Behovtype.Godkjenning,
            felt = feltNavn
        )

    private fun assertSykepengegrunnlagsfakta(
        vedtaksperiodeId: UUID = 1.vedtaksperiode(selvstendig),
        sykepengegrunnlagsfakta: Map<String, Any>
    ) {
        val actualSykepengegrunnlagsfakta = hentFelt<Any>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "sykepengegrunnlagsfakta")!!
        assertEquals(sykepengegrunnlagsfakta, actualSykepengegrunnlagsfakta)
    }

    private fun assertHendelser(hendelser: Set<UUID>, vedtaksperiodeId: UUID) {
        val actualHendelser = hentFelt<Set<UUID>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "hendelser")!!
        assertEquals(hendelser, actualHendelser)
    }

    private fun utbetalingsdag(dato: LocalDate, type: String, beløpTilBruker: Int, sykdomsgrad: Int, begrunnelser: List<String> = emptyList()) = mapOf(
        "dato" to dato.toString(),
        "type" to type,
        "beløpTilArbeidsgiver" to 0,
        "beløpTilBruker" to beløpTilBruker,
        "sykdomsgrad" to sykdomsgrad,
        "begrunnelser" to begrunnelser
    )

    private fun UUID.sisteBehandlingId() = inspektør(selvstendig).vedtaksperioder(this).inspektør.behandlinger.last().id
}
