package no.nav.helse.spleis

import com.apurebase.kgraphql.GraphQL
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import io.ktor.application.*
import io.ktor.auth.*
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.v2.Behandlingstype
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.Tidslinjeperiode
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.håndterPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.DataSource

data class GraphQLPerson(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<GraphQLArbeidsgiver>,
    val dodsdato: LocalDate?,
    val versjon: Int
)

data class GraphQLArbeidsgiver(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<GraphQLGenerasjon>
)

data class GraphQLGenerasjon(
    val id: UUID,
    val perioder: List<GraphQLTidslinjeperiode>
)

interface GraphQLTidslinjeperiode {
    val id: UUID
    val fom: LocalDate
    val tom: LocalDate

    //    val tidslinje: List<GraphQLDag>
    val behandlingstype: Behandlingstype
    val periodetype: Periodetype
    val inntektskilde: Inntektskilde
    val erForkastet: Boolean
    val opprettet: LocalDateTime
}

data class GraphQLUberegnetPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
//    override val tidslinje: List<GraphQLDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime
) : GraphQLTidslinjeperiode {
    override val id: UUID = UUID.randomUUID()
}

data class GraphQLBeregnetPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
//    override val tidslinje: List<GraphQLDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    val beregningId: UUID,
    val gjenstaendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val skjaeringstidspunkt: LocalDate,
    val maksdato: LocalDate,
//    val utbetaling:
//    val hendelser:
//    val simulering:
    val vilkarsgrunnlaghistorikkId: UUID,
//    val periodevilkår:
//    val aktivitetslogg:
) : GraphQLTidslinjeperiode {
    override val id: UUID = UUID.randomUUID()
}

data class GraphQLDag(
    val dato: LocalDate
)

fun mapTidslinjeperiode(periode: Tidslinjeperiode) =
    when (periode) {
        is BeregnetPeriode -> GraphQLBeregnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
//          tidslinje = emptyList(), // periode.sammenslåttTidslinje,
            behandlingstype = periode.behandlingstype,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet,
            beregningId = periode.beregningId,
            gjenstaendeSykedager = periode.gjenståendeSykedager,
            forbrukteSykedager = periode.forbrukteSykedager,
            skjaeringstidspunkt = periode.skjæringstidspunkt,
            maksdato = periode.maksdato,
            vilkarsgrunnlaghistorikkId = periode.vilkårsgrunnlagshistorikkId
            //    val utbetaling:
            //    val hendelser:
            //    val simulering:
            //    val periodevilkår:
            //    val aktivitetslogg:
        )
        else -> GraphQLUberegnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
//                                                    tidslinje = emptyList(), // periode.sammenslåttTidslinje,
            behandlingstype = periode.behandlingstype,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet
        )
    }

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
                            arbeidsgiver.generasjoner?.get(indeks)
                                ?.let { it.perioder }
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

    enum<Behandlingstype>()
    enum<Inntektskilde>()
    enum<Periodetype>()

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
