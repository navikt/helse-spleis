Jobs
====


# Starte feriepenger

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
