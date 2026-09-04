Jobs
====

# Introduksjon
Spleis består av flere jobs. Dette dokumentet omhandler hvordan man kjører feriepenger.

# Forberedelser til kjøring av feriepenger
Kjøring av feriepenger skaper så mye aktivitet i systemet at det ikke lar seg gjøre å jobbe i Speil samtidig. Derfor skal NAY varsles i forkant, og man bør legge ut en melding i Speil. Team utbetaling bør også varsles.

Det kreves også bittelitt teknisk prepp av to av våre apper.

Før man starter selve kjøringen kan man gjøre dry-runs. Eksempelvis hvis man har annonsert at Speil blir utilgjengelig kl. 16, kan man starte med dry-run kl. 15 eller 15.30.

Kjøringen tar en del timer.

# Om flagg
- Selve jobb-kjøringen har et flagg `dryrun`, som styrer om jobben sender ut behov `SykepengehistorikkForFeriepenger`. Det vil si at i dry-run-modus vil jobben bare finne aktuelle personer i databasen og logge litt.
- Spleis har en bryter `SEND_FERIEPENGEOPPDRAG`, den styrer om spleis **faktisk** sender oppdrag til spenn (som sender til oppdragssystemet) etter at den har beregnet feriepenger for en person.

# Ting å tenke på
Har det blitt innført nye kategorier av sakstyper i spleis/Speil siden forrige feriepengekjøring, og skal disse ha feriepenger eller ikke? Vil feriepenger-relatert kode håndtere disse sakstypene riktig?

# Hvordan kjøre feriepenger
1. Skaff deg en liten (virtuell) notatblokk - der kan du notere underveis, det dukker nemlig alltid opp noe rart
1. Skaler opp [sykepengeperioder](https://github.com/navikt/helse-sparkelapper/commit/08e07c375ceb57f87f9f2d380456b3f9536cb08b)
1. Skaler opp [databasen til spleis](https://github.com/navikt/helse-spleis/commit/a38fea8749076bc566da3ca837bfbba80d9dabea) og skru på [toggle i spleis](https://github.com/navikt/helse-spleis/commit/9b97446caa7648fb31f017d73733029c8605d62a)
1. Kjør dry run av jobben før du kjører på ekte. Bruk en ny ArbeidId. Se [starte feriepenger](#starte-feriepenger) for guide.
   1. Nå kommer du til å få en jobb som heter `spleis-migrate-1` som feiler, hvor loggene fra poddene vil inneholde feilmeldingen `Something unusual has occurred` - og det er helt OK 🙆‍
      1. Dette er bare fordi etter at du slettet jobben forrige gang du kjørte feriepenger forsvant også tilgangene jobben trenger for å kjøre. Denne første kjøringen får på plass tilgangene jobben trenger til neste punkt.
1. ️Kjør dry run av jobben på ny med samme parametre (ikke minst samme **ArbeidId**)
   1. Nå kommer du til å få en jobb som heter `spleis-migrate-2`, som ikke feiler.
1. Verifiser dry run-kjøringen. Se [Hva gjør dryrun?](#hva-gjør-dryrun) for hva som faktisk skjer og hva du bør sjekke.
1. Avslutt dry-run-kjøringen når du er klar til å kjøre jobben på ekte. Se [Avslutte dry run](#avslutte-dry-run).
1. Kjør på ekte, med `dryrun=false`. **Du må bruke en ny ArbeidId** — dry run-kjøringen har allerede markert alle personene i `arbeidstabell` som ferdige for den forrige ArbeidId-en, så en ekte kjøring med samme ID vil ikke gjøre noe som helst.
1. La jobben kjøre ferdig. Det tar en del timer.
   1. Du kan følge med på consumer lag [her](https://grafana.nav.cloud.nais.io/d/ayeT9XyGk/kafka-aiven?orgId=1&from=now-1h&to=now&timezone=browser&var-datasource=000000011&var-apps=$__all&var-Persentil=0.90&var-event_name=$__all) - når den har gått til normalt nivå for både spleis og sparkel-sykepengeperioder er jobben ferdig. Consumer lag kan gå opp og ned som en jojo under kjøringen.
   1. For å finne de vi reelt sender til oppdrag kan du søke på dette:
    ```
    jsonPayload.message:"Skal sende arbeidsgiveroppdrag til OS: true" OR jsonPayload.message:"Skal sende personoppdrag til OS: true"
    resource.labels.container_name="spleis"
    ```
   1. For å finne alle spleis har begynt å håndtere:
    ```
    jsonPayload.message:""Behandler utbetalingshistorikk for feriepenger""
    resource.labels.container_name="spleis"
    ```
   1. For å se hvor mange behov jobben har sendt ut
   ```
    jsonPayload.message:"sender behov om SykepengehistorikkForFeriepenger for fødselsnummer"
    resource.labels.container_name="spleis-migrate"
   ```   
   1. Du kan også følge progresjonen direkte i `arbeidstabell` i spleis-databasen. Jobben legger inn én rad per person når den starter, setter `arbeid_startet` når en pod plukker personen, og `arbeid_ferdig` når personen er ferdig behandlet (behovet er publisert eller personen er filtrert bort). Bytt ut `arbeid_id` med den du kjørte med.
   ```sql
    -- hvor mange som gjenstår
    select count(*) from arbeidstabell where arbeid_id = 'fp-2026-08-24-wet' and arbeid_ferdig is null;

    -- hvor mange poddene har behandlet ferdig
    select count(*) from arbeidstabell where arbeid_id = 'fp-2026-08-24-wet' and arbeid_ferdig is not null;

    -- totalt antall personer i kjøringen
    select count(*) from arbeidstabell where arbeid_id = 'fp-2026-08-24-wet';
   ```
   Kjør spørringene mot spleis-databasen, for eksempel via `nais postgres proxy spleis`. Så lenge antallet som gjenstår synker, går jobben som den skal — når det treffer 0 er poddene ferdige med sin del (svarene fra sparkel-sykepengeperioder kan fortsatt være under behandling, se consumer lag over).
   1. Stopper antallet som gjenstår å synke, kan poddene ha gått ned. Se [Hvis poddene dør underveis](#hvis-poddene-dør-underveis).

1. Slett naisjob `spleis-migrate` (fun fact - visste du at i august 2024 startet jobben av seg selv?)
1. Skaler ned sykepengeperioder og spleis, og skru av toggle, ved å reverte det du gjorde i steg 1 og 2.
1. Gratulerer, du har kjørt feriepenger! 🎉
1. En fagperson vil ha uttrekk. Etter kjøringen er det disse to uttrekkene som gjelder, og de kjøres i Logs Explorer:

    1. **Overlapp mot Infotrygd** — saker hvor vi faktisk sender feriepengeoppdrag til Oppdragssystemet (OS), og hvor Spleis har beregnet at Infotrygd ville utbetalt et beløp ulikt 0 «i en verden uten Spleis». Dette er personene hvor Spleis og Infotrygd overlapper, og som fagpersonen må se nærmere på.
   ```
    resource.labels.container_name="spleis"
    jsonPayload.message=~"Nøkkelverdier om feriepengeberegning"
    jsonPayload.message=~"til OS: true"
    -jsonPayload.message=~"til RAPID"
    jsonPayload.message=~"i en verden uten Spleis: [1-9-]"
   ```

    1. **Korrigeringsbeløp mot Infotrygd** — alle saker hvor vi sender feriepengeoppdrag til OS, med hele oppsummeringen inkludert linjen `Infotrygd-utbetalingen må korrigeres med`. Dette gir fagpersonen beløpene Infotrygd-utbetalingen avviker med per person og arbeidsgiver.
   ```
    resource.labels.container_name="spleis"
    jsonPayload.message=~"Nøkkelverdier om feriepengeberegning"
    jsonPayload.message=~"Infotrygd-utbetalingen må korrigeres med"
    jsonPayload.message=~"til OS: true"
    -jsonPayload.message=~"til RAPID"
   ```

   Noen forklaringer til filtrene:
   - `Nøkkelverdier om feriepengeberegning` er oppsummeringsloggen Spleis skriver per person og arbeidsgiver, med både Infotrygd- og Spleis-beløp, datoer og oppdragsdetaljer.
   - `til OS: true` treffer `Skal sende arbeidsgiveroppdrag/personoppdrag til OS: true`, altså de sakene hvor det faktisk sendes et oppdrag.
   - `-jsonPayload.message=~"til RAPID"` fjerner dublettene fra `tjenestekall`-loggingen, hvor hele aktivitetsloggen skrives ut på nytt når den publiseres på rapiden. Uten dette får du hver sak flere ganger.
   - `i en verden uten Spleis: [1-9-]` treffer beløp som starter med 1–9 eller minus, altså alt som ikke er 0.

## Hva gjør dryrun?

`dryrun` settes som env-variabelen `DRYRUN` på naisjob'en og leses i `App.kt`. Den påvirker kun ett eneste sted i koden: om jobben faktisk publiserer behovet på Kafka.

Med `dryrun=true` gjør jobben alt det andre helt som vanlig:

- fyller `arbeidstabell` med én rad per person i `person`-tabellen (hvis ikke `arbeid_id`-en finnes fra før)
- plukker personer i batcher, deserialiserer hver person og vurderer om hen er aktuell for feriepenger i opptjeningsåret
- logger `sender behov om SykepengehistorikkForFeriepenger for fødselsnummer=...` til sikkerlogg for hver person som *ville* fått et behov
- markerer raden som ferdig i `arbeidstabell`

Det eneste som ikke skjer er selve `producer.send(...)` mot `tbd.teknisk.v1`. Ingen behov havner på rapiden, spleis beregner ingen feriepenger, og ingenting sendes til Oppdragssystemet.

Dry run er derfor en test av at jobben starter, har databasetilgang, klarer å lese og deserialisere personene, og hvor mange personer som er aktuelle — det er *ikke* en test av selve feriepengeberegningen.

Vær oppmerksom på at dry run bruker opp `arbeid_id`-en: alle radene markeres som ferdige, så den ekte kjøringen må ha en ny `arbeid_id`.

### Slik verifiserer du dry run-kjøringen

1. **Jobben kjører uten å kaste exceptions.** Sjekk at det ikke ligger `Uncaught exception` i loggene:
   ```
    resource.labels.container_name="spleis-migrate"
    jsonPayload.message=~"Uncaught exception"
   ```
1. **Jobben plukker arbeid.** Antallet som gjenstår i `arbeidstabell` skal synke jevnt — bruk spørringene i steget «La jobben kjøre ferdig» over.
1. **Antall personer som ville fått behov.** Tell treffene på:
   ```
    resource.labels.container_name="spleis-migrate"
    jsonPayload.message:"sender behov om SykepengehistorikkForFeriepenger for fødselsnummer"
   ```
   Sammenlign størrelsesordenen med forrige feriepengekjøring. Store avvik er verdt å grave i før du kjører på ekte.
1. **Ingen personer feiler på deserialisering.** Sjekk om det er uventet mange treff på:
   ```
    resource.labels.container_name="spleis-migrate"
    jsonPayload.message=~"person lar seg ikke serialisere"
   ```
### Avslutte dry run

Jobben avslutter seg selv når alle personene er behandlet — da logger poddene `Fant ikke noe arbeid, avslutter` og terminerer.

Vil du stoppe tidligere, eller er du ferdig med å verifisere og klar for ekte kjøring, sletter du naisjob'en:

```bash
kubectl delete naisjob spleis-migrate
kubectl get pods -l app=spleis-migrate
```

Vent til det ikke er noen podder igjen før du starter den ekte kjøringen. Start den så med `dryrun=false` og **ny** `arbeid_id`.

## Hvis poddene dør underveis

Feriepengepoddene kan dø sporadisk under kjøring. En vanlig årsak er GKE autoscaler: når en node er lite utnyttet, kan autoscaleren skalere ned, evakuere alle podder på noden med `SIGTERM` og slette noden. Det kan skje når som helst, også midt på dagen, og feriepengejobben kjører i mange timer og er derfor eksponert for det. Det er altså ikke nødvendigvis en feil i koden — men sjekk loggene, for poddene kan også ha gått ned av andre grunner.

Hvis poddene har gått ned, kan noen personer bli liggende «stale» i `arbeidstabell`: `arbeid_startet` er satt, men `arbeid_ferdig` er `null`. Disse plukkes ikke opp igjen automatisk, fordi jobben kun henter rader hvor `arbeid_startet is null`. De må resettes manuelt før du starter jobben på nytt.

Fremgangsmåten:

1. **Sjekk at alle poddene er borte**, slik at ingen podder holder på med rader du er i ferd med å resette:
   ```bash
   kubectl get pods -l app=spleis-migrate
   ```
   Vent til kommandoen ikke returnerer noen kjørende podder.

1. **Se hvor mange som er stale** — altså plukket, men aldri fullført:
   ```sql
   select count(*) from arbeidstabell
   where arbeid_id = 'fp-2026-08-24-wet'
     and arbeid_startet is not null
     and arbeid_ferdig is null;
   ```

1. **Reset dem**, slik at de plukkes opp igjen ved neste kjøring:
   ```sql
   update arbeidstabell
   set arbeid_startet = null
   where arbeid_id = 'fp-2026-08-24-wet'
     and arbeid_startet is not null
     and arbeid_ferdig is null;
   ```
   Antallet rader du endrer skal stemme med tellingen i forrige steg.

1. **Start jobben på nytt med nøyaktig samme parametere**, ikke minst samme `arbeid_id`. Jobben ser at `arbeidstabell` allerede er fylt for denne `arbeid_id`-en, og fortsetter der den slapp i stedet for å begynne forfra.

Det er trygt å kjøre personer på nytt: downstream-logikken er idempotent, så en person som allerede er behandlet vil ikke få dobbel utbetaling om hen skulle bli plukket opp igjen.

## Starte feriepenger

Feriepenger-jobben tar inn to parametere:
- Dato for når det sist ble kjørt feriepenger i Infotrygd (`datoForSisteFeriepengekjøringIInfotrygd`)
- Hvilket år det skal beregnes feriepenger for (`opptjeningsår`, brukes [her](https://github.com/navikt/helse-spleis/blob/9b72f3b3e1549886a64aacd262dc0f4bb1853e93/jobs/src/main/kotlin/no/nav/helse/spleis/jobs/Feriepenger.kt#L57))

I eksemplene under er `datoForSisteFeriepengekjøringIInfotrygd` satt til `2025-05-10` og `opptjeningsåret` til `2024`.

### Start jobben fra GitHub Actions

Jobben startes fra workflowen [Kjør feriepenger](https://github.com/navikt/helse-spleis/actions/workflows/spleis-kjor-feriepenger.yml).

1. Åpne workflowen i lenken over.
2. Trykk **Run workflow**.
3. Fyll ut feltene og trykk **Run workflow** for å starte kjøringen.

| Felt | Beskrivelse |
|------|-------------|
| `image` | Fullt image-navn med tag. Se [Hvor kan jeg finne image-navn for jobben?](#hvor-kan-jeg-finne-image-navn-for-jobben) |
| `cluster` | `dev-gcp` eller `prod-gcp` |
| `arbeid_id` | ID-en på batchen, for eksempel `fp2025` |
| `dryrun` | `true` for testkjøring, `false` for ekte kjøring |
| `dato_for_siste_feriepengekjøring_i_infotrygd` | Dato på formatet `YYYY-MM-DD`, for eksempel `2025-05-10` |
| `opptjeningsår` | Året det beregnes feriepenger for, for eksempel `2024` |
| `parallelism` | Antall parallelle podder, standard `30` |
| `prod_bekreftelse` | Må være `START FERIEPENGER PROD` ved kjøring i `prod-gcp` |
| `bekreft_arbeid_id` | Må være identisk med `arbeid_id` ved kjøring i `prod-gcp` |

Workflowen validerer feltene før den deployer, og viser de valgte verdiene i workflow-summary.

Når jobben er ferdig, husk å slette naisjob'en:

```bash
kubectl delete naisjob spleis-migrate
```


### Nyttig info

#### Hvor kan jeg finne image-navn for jobben?
Under Actions i GitHub finner du siste bygg, og velger bygget av Spleis-JOBS(!!!). Under summary på den siden kan man 
expande `build inputs` og der vil du finne hele image-navnet til sist bygde versjon av Spleis-jobs under tags.

#### Hva er arbeidId og må jeg bry meg?
`arbeidId` er ID-en som brukes når spleis kopierer alle personer fra person-tabellen inn i arbeidstabellen i sin base.
ArbeidId er altså ID-en på batchen feriepengejobben skal tygge gjennom. Denne må være unik per feriepengekjøring.
Det er en constraint i arbeidstabellen at man ikke kan ha rader med samme kombinasjon av 
fødselsnummer og arbeidId. Det vil si at om jobben feiler kan den trygt kjøres med samme arbeidId uten at det
blir laget duplikate feriepengeoppdrag.


#### Feriepengejobben feiler med 'Something unusual has occurred to cause the driver to fail. Please report this exception.'
Dette er en synkronisering issue, hvor Nais ikke har provisjonert tilgang til basen for den nye Naisjob'en du
har laget. Det kan hende dette løses med med å deploye igjen.

Hvis ikke, og du er litt desperat, så kan du slette job'en (ikke naisjob'en), kjøre denne kommandoen 
```bash
kubectl patch naisjob spleis-migrate --type json -p='[{"op": "remove", "path": "/status/synchronizationHash"}]
```
og deploy jobben på nytt. Bruk samme arbeidId.

#### Feriepengejobben logger masse om at arbeid ikke er klart og du må vente
Det tar litt tid å kopiere alle personer fra person tabellen til arbeidstabellen. Hvis man ikke har skalert opp
databasen kan det ta typ 30 minutter. Skaler opp basen eller vær tålmodig.
