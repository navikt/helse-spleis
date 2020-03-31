package no.nav.helse.serde.api

import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.JsonBuilderTest.Companion.ingenBetalingsperson
import no.nav.helse.serde.JsonBuilderTest.Companion.inntektsmelding
import no.nav.helse.serde.JsonBuilderTest.Companion.manuellSaksbehandling
import no.nav.helse.serde.JsonBuilderTest.Companion.person
import no.nav.helse.serde.JsonBuilderTest.Companion.simulering
import no.nav.helse.serde.JsonBuilderTest.Companion.sykmelding
import no.nav.helse.serde.JsonBuilderTest.Companion.søknad
import no.nav.helse.serde.JsonBuilderTest.Companion.utbetalt
import no.nav.helse.serde.JsonBuilderTest.Companion.vilkårsgrunnlag
import no.nav.helse.serde.JsonBuilderTest.Companion.ytelser
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*


internal class SpeilBuilderTest {
    private val aktørId = "1234"
    private val fnr = "12020052345"

    private fun hendelser(): Triple<List<UUID>, List<UUID>, List<HendelseDTO>> {
        val hendelser1 = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val hendelser2 = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        return Triple(
            hendelser1, hendelser2, listOf(
                SykmeldingDTO(
                    id = hendelser1[0].toString(),
                    type = "NY_SØKNAD",
                    fom = 1.januar,
                    tom = 31.januar,
                    rapportertdato = 1.februar.atStartOfDay()
                ),
                SøknadDTO(
                    id = hendelser1[1].toString(),
                    type = "SENDT_SØKNAD_NAV",
                    fom = 1.januar,
                    tom = 31.januar,
                    rapportertdato = 1.februar.atStartOfDay(),
                    sendtNav = 1.februar.atStartOfDay()
                ),
                InntektsmeldingDTO(
                    id = hendelser1[2].toString(),
                    type = "INNTEKTSMELDING",
                    mottattDato = 1.januar.atStartOfDay(),
                    beregnetInntekt = 25000.0
                ),
                SykmeldingDTO(
                    id = hendelser2[0].toString(),
                    type = "NY_SØKNAD",
                    fom = 1.februar,
                    tom = 14.februar,
                    rapportertdato = 1.februar.atStartOfDay()
                ),
                SøknadDTO(
                    id = hendelser2[1].toString(),
                    type = "SENDT_SØKNAD_NAV",
                    fom = 1.februar,
                    tom = 14.februar,
                    rapportertdato = 15.februar.atStartOfDay(),
                    sendtNav = 15.februar.atStartOfDay()
                ),
                InntektsmeldingDTO(
                    id = hendelser2[2].toString(),
                    type = "INNTEKTSMELDING",
                    mottattDato = 1.februar.atStartOfDay(),
                    beregnetInntekt = 25000.0
                )
            )
        )
    }

    @Test
    internal fun `dager før førsteFraværsdag og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        val (hendelseIder, _, hendelser) = hendelser()
        val person = person(søknadhendelseId = hendelseIder[1])
        val personDTO = serializePersonForSpeil(person, hendelser)!!
        assertEquals(
            1.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.first().dato
        )
        assertEquals(
            31.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.last().dato
        )
    }

    @Test
    internal fun `person uten utbetalingsdager`() {
        val (hendelseIder, _, hendelser) = hendelser()
        val person = ingenBetalingsperson(søknadhendelseId = hendelseIder[1])
        val personDTO = serializePersonForSpeil(person, hendelser)!!

        assertEquals(
            1.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.first().dato
        )
        assertEquals(
            9.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.last().dato
        )
    }

    @Test
    internal fun `person med foreldet dager`() {
        val (hendelseIder, _, hendelser) = hendelser()
        val person = person(sendtSøknad = 1.juni, søknadhendelseId = hendelseIder[1])
        val personDTO = serializePersonForSpeil(person, hendelser)!!

        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)
        val utbetalingstidslinje =
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje[15].type)
        assertEquals(TypeDataDTO.ForeldetDag, utbetalingstidslinje[16].type)
        assertEquals(TypeDataDTO.ForeldetDag, utbetalingstidslinje.last().type)
    }

    @Test
    fun `ufullstendig vedtaksperiode når tilstand er Venter`() {
        val (hendelseIder, _, hendelser) = hendelser()
        val person = Person(aktørId, fnr).apply {
            håndter(sykmelding(hendelseId = hendelseIder[1], fom = 1.januar, tom = 31.januar))
        }
        val personDTO = serializePersonForSpeil(person, hendelser)!!

        val arbeidsgiver = personDTO.arbeidsgivere[0]
        val vedtaksperioder = arbeidsgiver.vedtaksperioder

        assertFalse(vedtaksperioder.first().fullstendig)
    }

    @Test
    fun `passer på at vedtakene har alle hendelsene`() {
        val (hendelseIder, hendelseIder2, hendelser) = hendelser()

        var vedtaksperiodeIder: Set<String>

        val person = Person(aktørId, fnr).apply {

            håndter(sykmelding(hendelseId = hendelseIder[0], fom = 1.januar, tom = 31.januar))
            håndter(søknad(hendelseId = hendelseIder[1], fom = 1.januar, tom = 31.januar))
            håndter(inntektsmelding(hendelseId = hendelseIder[2], fom = 1.januar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeIder.last()))

            håndter(sykmelding(hendelseId = hendelseIder2[0], fom = 1.februar, tom = 14.februar))
            håndter(søknad(hendelseId = hendelseIder2[1], fom = 1.februar, tom = 14.februar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
        }

        val personDTO = serializePersonForSpeil(person, hendelser)!!

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder as List<VedtaksperiodeDTO>

        assertEquals(2, vedtaksperioder.size)
        assertEquals(3, vedtaksperioder.first().hendelser.size)
        assertEquals(2, vedtaksperioder.last().hendelser.size) // Skal ikke ha inntektsmelding
        assertEquals(
            hendelseIder.map { it.toString() }.sorted(),
            vedtaksperioder.first().hendelser.map { it.id }.sorted()
        )
        assertEquals(
            hendelseIder2.subList(0, 2).map { it.toString() }.sorted(),
            vedtaksperioder[1].hendelser.map { it.id }.sorted()
        )
    }

    @Test
    fun `passer på at alle vedtak får fellesdata for sykefraværet`() {
        val (hendelseIder, hendelseIder2, hendelser) = hendelser()

        var vedtaksperiodeIder: Set<String>

        val person = Person(aktørId, fnr).apply {

            håndter(sykmelding(hendelseId = hendelseIder[0], fom = 1.januar, tom = 31.januar))
            håndter(søknad(hendelseId = hendelseIder[1], fom = 1.januar, tom = 31.januar))
            håndter(inntektsmelding(hendelseId = hendelseIder[2], fom = 1.januar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeIder.last()))

            håndter(sykmelding(hendelseId = hendelseIder2[0], fom = 1.februar, tom = 14.februar))
            håndter(søknad(hendelseId = hendelseIder2[1], fom = 1.februar, tom = 14.februar))

            vedtaksperiodeIder = collectVedtaksperiodeIder()

            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
        }

        val personDTO = serializePersonForSpeil(person, hendelser)!!

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder as List<VedtaksperiodeDTO>

        assertEquals(2, vedtaksperioder.size)
        assertEquals(vedtaksperioder.first().gruppeId, vedtaksperioder.last().gruppeId)
        assertEquals(vedtaksperioder.first().dataForVilkårsvurdering, vedtaksperioder.last().dataForVilkårsvurdering)
        assertEquals(vedtaksperioder.first().vilkår.opptjening, vedtaksperioder.last().vilkår.opptjening)
    }

    /**
     * Test for å verifisere at kontrakten mellom Spleis og Speil opprettholdes.
     * Hvis du trenger å gjøre endringer i denne testen må du sannsynligvis også gjøre endringer i Speil.
     */
    @Test
    fun `personDTO-en inneholder de feltene Speil forventer`() {
        val fom = 1.januar
        val tom = 31.januar
        val (hendelseIder, _, hendelser) = hendelser()

        val person = person(fom = fom, tom = tom, søknadhendelseId = hendelseIder[1])
        val personDTO = serializePersonForSpeil(person, hendelser)!!

        assertEquals("12020052345", personDTO.fødselsnummer)
        assertEquals("12345", personDTO.aktørId)
        assertEquals(1, personDTO.arbeidsgivere.size)

        val arbeidsgiver = personDTO.arbeidsgivere.first()
        assertEquals("987654321", arbeidsgiver.organisasjonsnummer)
        assertEquals(1, arbeidsgiver.vedtaksperioder.size)

        val vedtaksperiode = arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiode.fom)
        assertEquals(31.januar, vedtaksperiode.tom)
        assertEquals(TilstandstypeDTO.Utbetalt, vedtaksperiode.tilstand)
        assertTrue(vedtaksperiode.fullstendig)

        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertEquals(31, utbetalingstidslinje.size)
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(TypeDataDTO.NavDag, utbetalingstidslinje.last().type)
        assertEquals(100.0, (utbetalingstidslinje.last() as NavDagDTO).grad)

        assertEquals(15741, vedtaksperiode.totalbeløpArbeidstaker)

        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(31, sykdomstidslinje.size)
        assertEquals(JsonDagType.SYKEDAG_SØKNAD, sykdomstidslinje.first().type)
        assertEquals(1.januar, sykdomstidslinje.first().dagen)

        assertEquals("en_saksbehandler_ident", vedtaksperiode.godkjentAv)

        val vilkår = vedtaksperiode.vilkår
        val sykepengegrunnlag = vilkår.sykepengegrunnlag
        assertEquals(`1G`.beløp(fom).toInt(), sykepengegrunnlag.grunnbeløp)
        assertTrue(sykepengegrunnlag.oppfylt!!)
        assertEquals(31000.0 * 12, sykepengegrunnlag.sykepengegrunnlag)

        val sykepengedager = vilkår.sykepengedager
        assertEquals(11, sykepengedager.forbrukteSykedager)
        assertEquals(fom, sykepengedager.førsteFraværsdag)
        assertEquals(fom.plusDays(16), sykepengedager.førsteSykepengedag)
        assertEquals(28.desember, sykepengedager.maksdato)

        val alder = vilkår.alder
        assertEquals(17, alder.alderSisteSykedag)
        assertTrue(alder.oppfylt!!)

        val opptjening = vilkår.opptjening
        assertEquals(365, opptjening?.antallKjenteOpptjeningsdager)
        assertEquals(1.januar(2017), opptjening?.fom)
        assertTrue(opptjening?.oppfylt!!)

        val søknadsfrist = vilkår.søknadsfrist
        assertEquals(tom.plusDays(1).atStartOfDay(), søknadsfrist.sendtNav)
        assertEquals(fom, søknadsfrist.søknadFom)
        assertEquals(tom, søknadsfrist.søknadTom)
        assertTrue(søknadsfrist.oppfylt!!)

        assertEquals(31000.0, vedtaksperiode.inntektFraInntektsmelding)
        assertEquals(1, vedtaksperiode.hendelser.size) // Sender kun inn søknadId til person i denne testen

        val utbetalingslinjer = vedtaksperiode.utbetalingslinjer
        assertEquals(1, utbetalingslinjer.size)
        assertEquals(fom.plusDays(16), utbetalingslinjer.first().fom)
        assertEquals(tom, utbetalingslinjer.first().tom)

        assertEquals(372000.0, vedtaksperiode.dataForVilkårsvurdering?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(0.0, vedtaksperiode.dataForVilkårsvurdering?.avviksprosent)
    }


    private fun Person.collectVedtaksperiodeIder() = mutableSetOf<String>().apply {
        accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                gruppeId: UUID
            ) {
                add(id.toString())
            }
        })
    }
}

