package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.GraphQL
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.application.*
import io.ktor.auth.*
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.serde.api.v2.*
import no.nav.helse.serde.api.v2.Sykdomstidslinjedag.SykdomstidslinjedagKilde
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.håndterPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.DataSource

internal fun SchemaBuilder.personSchema(personDao: PersonDao, hendelseDao: HendelseDao) {
    query("generasjon") {
        resolver { fnr: Long, orgnr: String, indeks: Int ->
            personDao.hentPersonFraFnr(fnr)
                ?.deserialize { hendelseDao.hentAlleHendelser(fnr) }
                ?.let { håndterPerson(it, hendelseDao) }
                ?.let { person ->
                    person.arbeidsgivere
                        .firstOrNull { it.organisasjonsnummer == orgnr }
                        ?.let { arbeidsgiver ->
                            arbeidsgiver.generasjoner?.get(indeks)?.perioder
                                ?.map { periode -> mapTidslinjeperiode(periode) }
                        }
                } ?: emptyList()
        }
    }

    query("person") {
        resolver { fnr: Long ->
            personDao.hentPersonFraFnr(fnr)
                ?.deserialize { hendelseDao.hentAlleHendelser(fnr) }
                ?.let { håndterPerson(it, hendelseDao) }
                ?.let {
                    GraphQLPerson(
                        aktorId = it.aktørId,
                        fodselsnummer = it.fødselsnummer,
                        arbeidsgivere = it.arbeidsgivere.map { arbeidsgiver ->
                            GraphQLArbeidsgiver(
                                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                                id = arbeidsgiver.id,
                                generasjoner = arbeidsgiver.generasjoner?.map { generasjon ->
                                    GraphQLGenerasjon(
                                        id = generasjon.id,
                                        perioder = generasjon.perioder.map { periode -> mapTidslinjeperiode(periode) }
                                    )
                                } ?: emptyList()
                            )
                        },
                        dodsdato = it.dødsdato,
                        versjon = it.versjon
                    )
                }
        }
    }

    type<GraphQLPerson>()
    type<GraphQLArbeidsgiver>()
    type<GraphQLTidslinjeperiode>()
    type<GraphQLBeregnetPeriode>()
    type<GraphQLUberegnetPeriode>()
    type<GraphQLUtbetaling>()

    enum<Behandlingstype>()
    enum<Inntektskilde>()
    enum<Periodetype>()
    enum<SykdomstidslinjedagType>()
    enum<UtbetalingstidslinjedagType>()
    enum<SykdomstidslinjedagKildetype>()
    enum<GraphQLHendelsetype>()
    enum<BegrunnelseDTO>()

    type<SykdomstidslinjedagKilde>()
    type<GraphQLHendelse>()
    type<GraphQLInntektsmelding>()
    type<GraphQLSoknadNav>()
    type<GraphQLSoknadArbeidsgiver>()
    type<GraphQLSykmelding>()
    type<GraphQLSimulering>()
    type<AktivitetDTO>()
    type<Utbetalingsinfo>()

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

fun Application.installGraphQLApi(dataSource: DataSource, authProviderName: String) {
    val personDao = PersonDao(dataSource)
    val hendelseDao = HendelseDao(dataSource)

    install(GraphQL) {
        endpoint = "/graphql"

        wrap {
            authenticate(authProviderName, build = it)
        }

        schema {
            personSchema(personDao, hendelseDao)
        }
    }
}
