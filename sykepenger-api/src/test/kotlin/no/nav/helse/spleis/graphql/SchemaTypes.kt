package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.dto.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.dto.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLDag
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLHendelse
import no.nav.helse.spleis.graphql.dto.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.dto.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.dto.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.dto.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.dto.GraphQLInntektstype
import no.nav.helse.spleis.graphql.dto.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.graphql.dto.GraphQLSimulering
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsdetaljer
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsperiode
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsutbetaling
import no.nav.helse.spleis.graphql.dto.GraphQLSkjonnsmessigFastsatt
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadFrilans
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadSelvstendig
import no.nav.helse.spleis.graphql.dto.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagkilde
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.dto.GraphQLSykmelding
import no.nav.helse.spleis.graphql.dto.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.dto.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLUberegnetVilkarsprovdPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingsinfo
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.dto.GraphQLVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLVilkarsgrunnlaghistorikk
import no.nav.helse.spleis.graphql.dto.GraphQLVurdering

internal fun SchemaBuilder.personSchema(personResolver: (fnr: String) -> GraphQLPerson?) {
    query("person") {
        resolver { fnr: String -> personResolver(fnr) }
    }

    personTypes()
    arbeidsgiverTypes()
    hendelseTypes()
    inntektsgrunnlagTypes()
    simuleringTypes()
    tidslinjeperiodeTypes()
    vilkarsgrunnlagTypes()

    stringScalar<UUID> {
        deserialize = { uuid: String -> UUID.fromString(uuid) }
        serialize = { uuid: UUID -> uuid.toString() }
    }
    stringScalar<LocalDate> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        deserialize = { date: String -> LocalDate.parse(date, formatter) }
        serialize = { date: LocalDate -> date.format(formatter) }
    }
    stringScalar<LocalDateTime> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        deserialize = { date: String -> LocalDateTime.parse(date, formatter) }
        serialize = { date: LocalDateTime -> date.format(formatter) }
    }
    stringScalar<YearMonth> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        deserialize = { date: String -> YearMonth.parse(date, formatter) }
        serialize = { date: YearMonth -> date.format(formatter) }
    }
}

private fun SchemaBuilder.arbeidsgiverTypes() {
    type<GraphQLGenerasjon>()
    type<GraphQLArbeidsgiver>()
}

private fun SchemaBuilder.hendelseTypes() {
    enum<GraphQLHendelsetype>()
    type<GraphQLHendelse>()
    type<GraphQLInntektsmelding>()
    type<GraphQLSoknadNav>()
    type<GraphQLSoknadArbeidsgiver>()
    type<GraphQLSoknadArbeidsledig>()
    type<GraphQLSoknadFrilans>()
    type<GraphQLSoknadSelvstendig>()
    type<GraphQLSykmelding>()
}

private fun SchemaBuilder.inntektsgrunnlagTypes() {
    enum<GraphQLInntektskilde>()
    type<GraphQLInntekterFraAOrdningen>()
    type<GraphQLSkjonnsmessigFastsatt>()
    type<GraphQLOmregnetArsinntekt>()
    type<GraphQLArbeidsgiverinntekt>()
}

private fun SchemaBuilder.personTypes() {
    type<GraphQLPerson>()
}

private fun SchemaBuilder.simuleringTypes() {
    type<GraphQLSimuleringsdetaljer>()
    type<GraphQLSimuleringsutbetaling>()
    type<GraphQLSimuleringsperiode>()
    type<GraphQLSimulering>()
}

private fun SchemaBuilder.tidslinjeperiodeTypes() {
    enum<GraphQLInntektstype>()
    enum<GraphQLPeriodetype>()
    enum<GraphQLSykdomsdagtype>()
    enum<GraphQLUtbetalingsdagType>()
    enum<GraphQLSykdomsdagkildetype>()
    enum<GraphQLBegrunnelse>()
    enum<GraphQLPeriodetilstand>()
    enum<Utbetalingtype>()
    enum<GraphQLUtbetalingstatus>()

    type<GraphQLSykdomsdagkilde>()
    type<GraphQLUtbetalingsinfo>()
    type<GraphQLVurdering>()
    type<GraphQLUtbetaling> {
        property(GraphQLUtbetaling::type) {
            deprecate("Burde bruke enum \"typeEnum\"")
        }
        property(GraphQLUtbetaling::status) {
            deprecate("Burde bruke enum \"statusEnum\"")
        }
    }
    type<GraphQLDag>()
    type<GraphQLTidslinjeperiode>()
    type<GraphQLUberegnetPeriode>()
    type<GraphQLUberegnetVilkarsprovdPeriode>()
    type<GraphQLPeriodevilkar>()
    type<GraphQLPeriodevilkar.Sykepengedager>()
    type<GraphQLPeriodevilkar.Alder>()
    type<GraphQLBeregnetPeriode>()
}

private fun SchemaBuilder.vilkarsgrunnlagTypes() {
    type<GraphQLVilkarsgrunnlag>()
    type<GraphQLSpleisVilkarsgrunnlag>()
    type<GraphQLInfotrygdVilkarsgrunnlag>()
    type<GraphQLVilkarsgrunnlaghistorikk>()
}
