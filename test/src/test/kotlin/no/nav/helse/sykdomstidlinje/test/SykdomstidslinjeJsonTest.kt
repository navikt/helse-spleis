package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.dag.JsonDagType
import no.nav.helse.sykdomstidslinje.objectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykdomstidslinjeJsonTest {
    val inntektsmelding = Inntektsmelding(objectMapper.readTree(SykdomstidslinjeJsonTest::class.java.getResourceAsStream("/inntektsmelding.json")))
    val søknadSendt = SendtSykepengesøknad(objectMapper.readTree(SykdomstidslinjeJsonTest::class.java.getResourceAsStream("/søknad_arbeidstaker_sendt_nav.json")))
    val expectedJsonPayload = "[{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-01\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-02\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-03\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-04\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"HELGEDAG\",\"dato\":\"2019-10-05\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"HELGEDAG\",\"dato\":\"2019-10-06\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-07\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-07\",\"hendelse\":{\"type\":\"SendtSykepengesøknad\",\"json\":{\"id\":\"68da259c-ff7f-47cf-8fa0-c348ae95e220\",\"type\":\"ARBEIDSTAKERE\",\"status\":\"SENDT\",\"aktorId\":\"1234567890123\",\"sykmeldingId\":\"71bd853d-36a1-49df-a34c-6e02cf727cfa\",\"arbeidsgiver\":{\"navn\":\"Nærbutikken AS\",\"orgnummer\":\"987654321\"},\"arbeidssituasjon\":\"ARBEIDSTAKER\",\"korrigerer\":null,\"korrigertAv\":null,\"soktUtenlandsopphold\":null,\"arbeidsgiverForskutterer\":null,\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"startSyketilfelle\":\"2019-06-01\",\"arbeidGjenopptatt\":null,\"sykmeldingSkrevet\":\"2019-06-01T00:00:00\",\"opprettet\":\"2019-06-01T00:00:00.000\",\"sendtNav\":\"2019-06-14T00:00:00.000\",\"sendtArbeidsgiver\":\"2019-06-14T00:00:00.000\",\"egenmeldinger\":[],\"papirsykmeldinger\":null,\"fravar\":[],\"andreInntektskilder\":[],\"soknadsperioder\":[{\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"sykmeldingsgrad\":100,\"faktiskGrad\":null,\"avtaltTimer\":null,\"faktiskTimer\":null,\"sykmeldingstype\":null}],\"sporsmal\":null}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-08\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-08\",\"hendelse\":{\"type\":\"SendtSykepengesøknad\",\"json\":{\"id\":\"68da259c-ff7f-47cf-8fa0-c348ae95e220\",\"type\":\"ARBEIDSTAKERE\",\"status\":\"SENDT\",\"aktorId\":\"1234567890123\",\"sykmeldingId\":\"71bd853d-36a1-49df-a34c-6e02cf727cfa\",\"arbeidsgiver\":{\"navn\":\"Nærbutikken AS\",\"orgnummer\":\"987654321\"},\"arbeidssituasjon\":\"ARBEIDSTAKER\",\"korrigerer\":null,\"korrigertAv\":null,\"soktUtenlandsopphold\":null,\"arbeidsgiverForskutterer\":null,\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"startSyketilfelle\":\"2019-06-01\",\"arbeidGjenopptatt\":null,\"sykmeldingSkrevet\":\"2019-06-01T00:00:00\",\"opprettet\":\"2019-06-01T00:00:00.000\",\"sendtNav\":\"2019-06-14T00:00:00.000\",\"sendtArbeidsgiver\":\"2019-06-14T00:00:00.000\",\"egenmeldinger\":[],\"papirsykmeldinger\":null,\"fravar\":[],\"andreInntektskilder\":[],\"soknadsperioder\":[{\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"sykmeldingsgrad\":100,\"faktiskGrad\":null,\"avtaltTimer\":null,\"faktiskTimer\":null,\"sykmeldingstype\":null}],\"sporsmal\":null}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-09\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-09\",\"hendelse\":{\"type\":\"SendtSykepengesøknad\",\"json\":{\"id\":\"68da259c-ff7f-47cf-8fa0-c348ae95e220\",\"type\":\"ARBEIDSTAKERE\",\"status\":\"SENDT\",\"aktorId\":\"1234567890123\",\"sykmeldingId\":\"71bd853d-36a1-49df-a34c-6e02cf727cfa\",\"arbeidsgiver\":{\"navn\":\"Nærbutikken AS\",\"orgnummer\":\"987654321\"},\"arbeidssituasjon\":\"ARBEIDSTAKER\",\"korrigerer\":null,\"korrigertAv\":null,\"soktUtenlandsopphold\":null,\"arbeidsgiverForskutterer\":null,\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"startSyketilfelle\":\"2019-06-01\",\"arbeidGjenopptatt\":null,\"sykmeldingSkrevet\":\"2019-06-01T00:00:00\",\"opprettet\":\"2019-06-01T00:00:00.000\",\"sendtNav\":\"2019-06-14T00:00:00.000\",\"sendtArbeidsgiver\":\"2019-06-14T00:00:00.000\",\"egenmeldinger\":[],\"papirsykmeldinger\":null,\"fravar\":[],\"andreInntektskilder\":[],\"soknadsperioder\":[{\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"sykmeldingsgrad\":100,\"faktiskGrad\":null,\"avtaltTimer\":null,\"faktiskTimer\":null,\"sykmeldingstype\":null}],\"sporsmal\":null}},\"erstatter\":[]}]},{\"type\":\"ARBEIDSDAG\",\"dato\":\"2019-10-10\",\"hendelse\":{\"type\":\"Inntektsmelding\",\"json\":{\"inntektsmeldingId\":\"1\",\"arbeidstakerFnr\":\"12345678910\",\"arbeidstakerAktorId\":\"1234567890123\",\"virksomhetsnummer\":\"123456789\",\"arbeidsgiverFnr\":\"10987654321\",\"arbeidsgiverAktorId\":\"1110987654321\",\"arbeidsgivertype\":\"VIRKSOMHET\",\"arbeidsforholdId\":\"42\",\"beregnetInntekt\":\"10000.01\",\"rapportertDato\":\"2019-05-01T16:00:00\",\"refusjon\":{\"beloepPrMnd\":null,\"opphoersdato\":null},\"endringIRefusjoner\":[],\"opphoerAvNaturalytelser\":[],\"gjenopptakelseNaturalytelser\":[],\"arbeidsgiverperioder\":[{\"fom\":\"2019-05-01\",\"tom\":\"2019-06-01\"}],\"status\":\"GYLDIG\",\"arkivreferanse\":\"ENARKIVREFERANSE\"}},\"erstatter\":[{\"type\":\"SYKEDAG\",\"dato\":\"2019-10-10\",\"hendelse\":{\"type\":\"SendtSykepengesøknad\",\"json\":{\"id\":\"68da259c-ff7f-47cf-8fa0-c348ae95e220\",\"type\":\"ARBEIDSTAKERE\",\"status\":\"SENDT\",\"aktorId\":\"1234567890123\",\"sykmeldingId\":\"71bd853d-36a1-49df-a34c-6e02cf727cfa\",\"arbeidsgiver\":{\"navn\":\"Nærbutikken AS\",\"orgnummer\":\"987654321\"},\"arbeidssituasjon\":\"ARBEIDSTAKER\",\"korrigerer\":null,\"korrigertAv\":null,\"soktUtenlandsopphold\":null,\"arbeidsgiverForskutterer\":null,\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"startSyketilfelle\":\"2019-06-01\",\"arbeidGjenopptatt\":null,\"sykmeldingSkrevet\":\"2019-06-01T00:00:00\",\"opprettet\":\"2019-06-01T00:00:00.000\",\"sendtNav\":\"2019-06-14T00:00:00.000\",\"sendtArbeidsgiver\":\"2019-06-14T00:00:00.000\",\"egenmeldinger\":[],\"papirsykmeldinger\":null,\"fravar\":[],\"andreInntektskilder\":[],\"soknadsperioder\":[{\"fom\":\"2019-06-01\",\"tom\":\"2019-06-14\",\"sykmeldingsgrad\":100,\"faktiskGrad\":null,\"avtaltTimer\":null,\"faktiskTimer\":null,\"sykmeldingstype\":null}],\"sporsmal\":null}},\"erstatter\":[]}]}]"

    @Test
    fun `lagring og restoring av en sykdomstidslinje med kun arbeidsdag returnerer like objekter`() {
        val tidslinjeA = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 1),
            LocalDate.of(2019, 10, 3), inntektsmelding)
        val tidslinjeB = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding)
        val tidslinjeC = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding)

        val combined = tidslinjeA + tidslinjeB + tidslinjeC
        val json = combined.toJson()

        val restored = Sykdomstidslinje.fromJson(json)
        assertEquals(combined, restored)
    }

    @Test
    fun `lagring og restoring av en sykdomstidslinje med søknader og inntektsmeldinger returnerer like objekter`() {
        val egenmelding = Sykdomstidslinje.egenmeldingsdager(LocalDate.of(2019, 9, 30), LocalDate.of(2019, 10, 1), søknadSendt)
        val sykedagerA = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 2),
            LocalDate.of(2019, 10, 4), søknadSendt)
        val ikkeSykedager = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding)
        val sykedagerB = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding)

        val combined = egenmelding + sykedagerA + ikkeSykedager + sykedagerB
        val json = combined.toJson()

        val restored = Sykdomstidslinje.fromJson(json)
        assertEquals(combined, restored)
    }

    @Disabled("Valg av dag er ikke stabil nok, gir feil resultat.")
    @Test
    fun `konvertering til json returnerer forventet json resultat`() {
        val tidslinjeA = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 1),
            LocalDate.of(2019, 10, 3), inntektsmelding)
        val tidslinjeB = Sykdomstidslinje.ikkeSykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), inntektsmelding)
        val tidslinjeC = Sykdomstidslinje.sykedager(
            LocalDate.of(2019, 10, 7),
            LocalDate.of(2019, 10, 10), søknadSendt)

        val json = (tidslinjeA + tidslinjeB + tidslinjeC).toJson()
        assertEquals(expectedJsonPayload, json)
    }

    @Test
    fun `sykdomstidslinje med alle typer dager blir serialisert riktig`() {
        val egenmeldingsdag = Sykdomstidslinje.egenmeldingsdag(LocalDate.of(2019, 10, 7), inntektsmelding)
        val sykedag = Sykdomstidslinje.sykedag(LocalDate.of(2019, 10, 8), søknadSendt)
        val feriedag = ferie(LocalDate.of(2019, 10, 9), søknadSendt)
        val permisjonsdager = Sykdomstidslinje.permisjonsdager(LocalDate.of(2019, 10, 11), LocalDate.of(2019, 10, 12), søknadSendt)
        val sykedager = Sykdomstidslinje.sykedager(LocalDate.of(2019, 10, 13), LocalDate.of(2019, 10, 15), søknadSendt)

        val permisjonsdagForUbestemt = Sykdomstidslinje.permisjonsdag(LocalDate.of(2019, 10, 16), inntektsmelding)
        val sykedagForUbestemt = Sykdomstidslinje.sykedag(LocalDate.of(2019, 10, 16), søknadSendt)
        val ubestemtdag = permisjonsdagForUbestemt + sykedagForUbestemt
        val studiedag = Sykdomstidslinje.studiedag(LocalDate.of(2019, 10, 17), søknadSendt)
        val utenlandsdag = Sykdomstidslinje.utenlandsdag(LocalDate.of(2019, 10, 22), søknadSendt)
        val utdanningsdag = Sykdomstidslinje.utdanningsdag(LocalDate.of(2019, 10, 23), søknadSendt)

        val tidslinje =  egenmeldingsdag + sykedag + feriedag + permisjonsdager + sykedager + ubestemtdag + studiedag + utenlandsdag + utdanningsdag

        val json = tidslinje.toJson()

        val restored = Sykdomstidslinje.fromJson(json)

        assertEquals(tidslinje, restored)

        JsonDagType.values().forEach {
            assertTrue(json.contains("\"${it.name}\""), "Tidslinje inneholder ikke dag-type $it")
        }
    }
}
