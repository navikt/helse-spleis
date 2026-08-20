package no.nav.helse.spleis.opptjening

import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.AbstractApiTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OpptjeningsvurderingApiTest : AbstractApiTest() {

    companion object {
        private const val UNG_PERSON_FNR = "12029240046"
        @JvmStatic private val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        private const val ORGNUMMER = "987654322"
        @JvmStatic private val MELDINGSREFERANSE = UUID.randomUUID()
    }

    @Test
    fun `hent opptjeningsvurderinger for person`() = blackboxTestApplication(::opprettVåreTestdata) {
        "/api/opptjeningsvurderinger".httpPost(
            HttpStatusCode.OK, mapOf(
            "fødselsnummer" to UNG_PERSON_FNR
        )
        ) {
            val jsonNode = objectMapper.readTree(this)
            jsonNode.path("opptjeningsvurderinger").apply {
                assertTrue(this.isArray)
                assertEquals(1, this.size())
                this.first().apply {
                    assertEquals("2017-01-01", this.path("opptjeningsperiode").path("fom").asText())
                    assertEquals("2017-12-31", this.path("opptjeningsperiode").path("tom").asText())
                    assertEquals("2018-01-01", this.path("skjæringstidspunkt").asText())
                    assertDoesNotThrow {
                        UUID.fromString(this.path("opptjeningsvurderingId").asText())
                    }
                    assertEquals("SPLEIS", this.path("typeGrunnlag").asText())
                    assertTrue(this.path("opptjeningOk").asBoolean())
                }
            }
        }
    }

    private fun opprettVåreTestdata(testDataSource: TestDataSource) {
        val eventBus = EventBus()
        val fom = LocalDate.of(2018, 1, 1)
        val tom = fom.plusDays(30)

        val person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER))

        val søknad = fabrikk.lagSøknad(Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER)
        person.håndterSøknad(eventBus, søknad, Aktivitetslogg())

        val vedtaksperiodeId = eventBus.events.filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>().single().vedtaksperiodeId

        val utbetalingshistorikk = fabrikk.lagUtbetalingshistorikk(vedtaksperiodeId)

        person.håndterUtbetalingshistorikk(eventBus, utbetalingshistorikk, Aktivitetslogg())

        val arbeidsgiveropplysninger = fabrikk.lagArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 31000.månedlig,
            vedtaksperiodeId = vedtaksperiodeId
        )
        person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, Aktivitetslogg())

        val arbeidsgiverinntekt = ArbeidsgiverInntekt(
            arbeidsgiver = ORGNUMMER,
            inntekter = listOf(YearMonth.of(2017, 10), YearMonth.of(2017, 11), YearMonth.of(2017, 12)).map {
                ArbeidsgiverInntekt.MånedligInntekt(
                    yearMonth = it, inntekt = 31000.månedlig, type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, fordel = "a", beskrivelse = "b"
                )
            },
        )

        val vilkårsgrunnlag = fabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = fom,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    orgnummer = ORGNUMMER,
                    ansettelseperiode = 1.januar(2017) til LocalDate.MAX,
                    type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(arbeidsgiverinntekt)
            ),
            inntekterForOpptjeningsvurdering = InntekterForOpptjeningsvurdering(listOf(arbeidsgiverinntekt).map {
                it.copy(inntekter = listOf(it.inntekter.last()))
            }),
            forsikringsvurderingId = null,
        )
        person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, Aktivitetslogg())

        testDataSource.ds.lagrePerson(UNG_PERSON_FNR, person)
    }
}
