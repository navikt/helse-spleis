package no.nav.helse.serde

import java.time.LocalDate
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.AKTØRID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FDATO_2018
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.memento.ArbeidsgiverMemento
import no.nav.helse.memento.AvsenderMemento
import no.nav.helse.memento.DagMemento
import no.nav.helse.memento.DokumenttypeMemento
import no.nav.helse.memento.VedtaksperiodetilstandMemento
import no.nav.helse.memento.VilkårsgrunnlaghistorikkMemento
import no.nav.helse.oktober
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class PersonDataBuilderTest : AbstractDslTest() {

    @Test
    fun `serialisering av person`() {
        a1 {
            håndterSøknad(Sykdom(5.januar, 17.januar, 100.prosent))
            håndterInntektsmeldingPortal(listOf(1.januar til 16.januar), inntektsdato = 1.januar, førsteFraværsdag = 1.januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(
                    a1 to INNTEKT
                ), 1.januar),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterAnnullering(inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())

            håndterSykmelding(Sykmeldingsperiode(1.august, 5.august))
        }
        a2 {
            håndterSøknad(Sykdom(3.februar, 28.februar, 100.prosent), Arbeid(21.februar, 28.februar), egenmeldinger = listOf(
                Søknad.Søknadsperiode.Arbeidsgiverdag(1.februar, 2.februar)
            ), sendtTilNAVEllerArbeidsgiver = 1.juni)
            håndterInntektsmeldingPortal(listOf(1.februar til 16.februar), inntektsdato = 1.februar, førsteFraværsdag = 1.februar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
        }
        a3 {
            håndterSøknad(Sykdom(1.juni, 16.juni, 100.prosent))
            håndterInntektsmeldingPortal(listOf(1.juni til 16.juni),
                inntektsdato = 1.juni,
                førsteFraværsdag = 1.juni,
                beregnetInntekt = INNTEKT,
                begrunnelseForReduksjonEllerIkkeUtbetalt = "IngenOpptjening",
                refusjon = Inntektsmelding.Refusjon(INGEN, null)
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(
                a3 to INNTEKT
            ), 1.juni))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        håndterDødsmelding(1.oktober)

        val memento = memento()

        assertEquals(UNG_PERSON_FNR_2018.toString(), memento.fødselsnummer)
        assertEquals(AKTØRID, memento.aktørId)
        assertEquals(UNG_PERSON_FDATO_2018, memento.alder.fødselsdato)
        assertEquals(1.oktober, memento.alder.dødsdato)

        assertArbeidsgivere(memento.arbeidsgivere)
        assertVilkårsgrunnlaghistorikk(memento.vilkårsgrunnlagHistorikk)
        assertGjenoppbygget(memento)
    }

    private fun assertArbeidsgivere(arbeidsgivere: List<ArbeidsgiverMemento>) {
        assertEquals(3, arbeidsgivere.size)

        arbeidsgivere[0].also { arbeidsgiver ->
            assertEquals(a1, arbeidsgiver.organisasjonsnummer)
            assertEquals(1, arbeidsgiver.inntektshistorikk.historikk.size)
            arbeidsgiver.inntektshistorikk.historikk[0].also { inntektsmelding ->
                assertEquals(1.januar, inntektsmelding.dato)
                val forventetInntekt = INNTEKT
                assertEquals(forventetInntekt.reflection { årlig, _, _, _ -> årlig }, inntektsmelding.beløp.årlig)
                assertEquals(forventetInntekt.reflection { _, månedligDouble, _, _ -> månedligDouble }, inntektsmelding.beløp.månedligDouble)
                assertEquals(forventetInntekt.reflection { _, _, dagligDouble, _ -> dagligDouble }, inntektsmelding.beløp.dagligDouble)
                assertEquals(forventetInntekt.reflection { _, _, _, dagligInt -> dagligInt }, inntektsmelding.beløp.dagligInt)
            }
            assertEquals(3, arbeidsgiver.sykdomshistorikk.elementer.size)
            arbeidsgiver.sykdomshistorikk.elementer[2].also { sykdomshistorikkElement ->
                assertEquals(13, sykdomshistorikkElement.hendelseSykdomstidslinje.dager.size)
                sykdomshistorikkElement.hendelseSykdomstidslinje.dager.also { dager ->
                    val forventetPeriode = 5.januar til 17.januar
                    forventetPeriode.forEach { dato ->
                        val dagen = dager.single { it.dato == dato }
                        assertEquals("Søknad", dagen.kilde.type)
                        if (dato.erHelg()) assertInstanceOf(DagMemento.SykHelgedagMemento::class.java, dagen)
                        else assertInstanceOf(DagMemento.SykedagMemento::class.java, dagen)
                    }
                }
                assertEquals(13, sykdomshistorikkElement.beregnetSykdomstidslinje.dager.size)
            }
            assertEquals(1, arbeidsgiver.sykmeldingsperioder.perioder.size)
            arbeidsgiver.sykmeldingsperioder.perioder[0].also { periode ->
                assertEquals(1.august, periode.fom)
                assertEquals(5.august, periode.tom)
            }

            assertEquals(2, arbeidsgiver.utbetalinger.size)
            assertEquals(0, arbeidsgiver.vedtaksperioder.size)
            assertEquals(1, arbeidsgiver.forkastede.size)
            arbeidsgiver.forkastede[0].vedtaksperiode.also { vedtaksperiode ->
                assertEquals(VedtaksperiodetilstandMemento.TIL_INFOTRYGD, vedtaksperiode.tilstand)
                assertEquals(3, vedtaksperiode.generasjoner.generasjoner.size)
                vedtaksperiode.generasjoner.generasjoner[0].also { generasjon ->
                    assertEquals(AvsenderMemento.SYKMELDT, generasjon.kilde.avsender)
                    assertNull(generasjon.vedtakFattet)
                    assertNotNull(generasjon.avsluttet)
                    assertEquals(1, generasjon.endringer.size)
                    generasjon.endringer[0].also { endring ->
                        assertEquals(5.januar, endring.periode.fom)
                        assertEquals(17.januar, endring.periode.tom)
                        assertEquals(5.januar, endring.sykmeldingsperiode.fom)
                        assertEquals(17.januar, endring.sykmeldingsperiode.tom)
                        assertEquals(DokumenttypeMemento.Søknad, endring.dokumentsporing.type)
                        assertEquals(13, endring.sykdomstidslinje.dager.size)
                        assertEquals(5.januar, endring.sykdomstidslinje.periode?.fom)
                        assertEquals(17.januar, endring.sykdomstidslinje.periode?.tom)
                    }
                }
            }
        }
    }
    private fun assertVilkårsgrunnlaghistorikk(historikk: VilkårsgrunnlaghistorikkMemento) {
        assertEquals(5, historikk.historikk.size)
    }
}