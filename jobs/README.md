Jobs
====

# Hvordan kjøre feriepenger
1. Skalerer opp [sykepengeperioder](https://github.com/navikt/helse-sparkelapper/commit/08e07c375ceb57f87f9f2d380456b3f9536cb08b)
1.Skaler opp [databasen til spleis](https://github.com/navikt/helse-spleis/commit/a38fea8749076bc566da3ca837bfbba80d9dabea)
1. Skru på [toggle i spleis](https://github.com/navikt/helse-spleis/commit/9b97446caa7648fb31f017d73733029c8605d62a)
1. Kjør dry run av jobben under før du kjører på ekte. Bruk en ny unik ArbeidId. Se [starte feriepenger](#starte-feriepenger) for guide.
   1. Nå kommer du til å få en jobb som heter `spleis-migrate-1` som feiler med feilmeldingen `Something unusual has occurred` - og det er helt OK 🙆‍
      1. Dette er bare fordi etter at du slettet jobben forrige gang du kjørte feriepenger forsvant også tilgangene jobben trenger for å kjøre. Denne første kjøringen får på plass tilgangene jobben trenger til neste punkt.
1. ️Kjør dry run av jobben på ny med samme parametre (SAMME ArbeidId!!!)
   1. Nå kommer du til å få en jobb som heter `spleis-migrate-2` som ikke feiler.
1. Kjør på ekte. Samme som over, bare ikke dry run.
1. Vent til jobben er ferdig. 
   1. Dette kan du se ved at Kafka consumer lag [her](https://grafana.nav.cloud.nais.io/d/ayeT9XyGk/kafka-aiven?orgId=1&from=now-1h&to=now&timezone=browser&var-datasource=000000011&var-apps=$__all&var-Persentil=0.90&var-event_name=$__all) for både Spleis og sparkel-sykepengeperioder har gått til normalt nivå. Den kan gå opp og ned som en jojo.
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
1. Slett naisjob `spleis-migrate`
1. Skaler ned sykepengeperioder og spleis, og skru av toggle, ved å reverte det du gjorde i steg 1, 2 og 3.
1. Gratulerer, du har kjørt feriepenger! 🎉

## Starte feriepenger

Parametrene til feriepenger er:
- `datoForSisteFeriepengekjøringIInfotrygd`
- `opptjeningsår`

I eksempelet under er `datoForSisteFeriepengekjøringIInfotrygd` satt til `2025-05-10`.

Parametrene skilles med mellomrom.

```
% ./deploy_jobb.sh
🐳 Image: imagenavnet mitt (husk det er spleis-jobs-imaget man skal ha)
☸️ Cluster (1: dev-gcp | 2: prod-gcp): 2
🔑 API key: <hemmelig>
🔑 Parallelism: (30 default) 
🛠️ Hvilken jobb skal du kjøre? feriepenger
🪪 Hva skal arbeidId settes til? fp2025
🏜️ Dryrun? (Y/n): n
🎒 Eventuelt andre parametre til jobben? 2025-05-10 2024

Når jobben er ferdig, husk å kjøre
  kubectl delete naisjob spleis-migrate
```

API-key hentes i [Nais-konsollet](https://console.nav.cloud.nais.io/team/tbd/settings). 

Dryrun settes til Y om du ønsker å teste jobben. Dette forutsetter at jobben skjønner hva dryrun er.

### Fallgruver
#### Hvor ligger bilde ref jeg skal bruke?
Under Actions i GitHub finner du siste bygg, og velger bygget av Spleis-JOBS(!!!). Under summary på den siden kan man 
expande `build inputs` og der vil du finne hele bilde ref til siste bygg av Spleis-jobs under tags.

#### Hva er arbeidId og må jeg bry meg?
Arbeid ID er ID'en som brukes når Spleis kopierer alle personer fra person tabellen inn i arbeidstabellen i sin base.
Arbeid ID er da batch ID'en på batchen feriepengejobben skal tygge gjennom. Denne må være unik per feriepengekjøring.
Det er en constraint i arbeidstabellen at man ikke kan ha rader med samme kombinasjon av 
fødselsnummer og arbeidId. Det vil si at om jobben feiler kan den trygt kjøres med samme arbeidID uten at det
blir laget duplikate feriepengeoppdrag.

#### Jeg klarer ikke å deploye jobben, den sier noe rart om ARM og AMD platformer
Dette kommer av at du vil kjøre et bilde bygget på `amd64` arkitektur på en annen prosessor arkitektur/
Hvis du kjører Colima på Macbook med M chip, restart Colima med kommandoen
```bash
colima start --arch x86_64 --vz-rosetta
```
og prøv på nytt. Om du kjører Docker desktop er det innstillinger som må settes i der.


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



