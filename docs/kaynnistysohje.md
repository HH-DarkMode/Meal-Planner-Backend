# Käynnistysohje (backend ja tietokanta)

Ohje siihen, miten backendin saa käyntiin omalla koneella. Backend käyttää PostgreSQL-tietokantaa,
jossa on reseptit ja Finelin ravintoarvot. Reseptidataa ei ole gitissä (liian iso), vaan se ladataan
erillisestä dump-tiedostosta.

Komennot on annettu kahdelle ympäristölle:
- **Windows**: PowerShell (ei Komentokehote/cmd, jossa `.\` ja `$env:` eivät toimi)
- **Linux / macOS**: pääte (bash tai zsh)

Vaiheet, joissa komennot ovat samat, on annettu vain kerran.

## Tarvittavat ohjelmat

| Ohjelma | Versio | Huomio |
|---|---|---|
| Java (JDK) | 17 tai uudempi | Maven-wrapper tulee repon mukana, Mavenia ei tarvitse asentaa |
| PostgreSQL | 16 (muu tuore versio todennäköisesti käy) | tarvitaan `psql`-komento |
| Node.js | tuore LTS | vain frontendille |

Tarkista, että Java löytyy: `java -version`. Jos komentoa ei tunnisteta tai versio on alle 17, asenna JDK ja
avaa uusi PowerShell-ikkuna.

**Windows: PostgreSQL:n `psql`-komento.** Asennusohjelma (EDB) ei aina lisää `psql`:ää PATH:iin. Jos
PowerShell sanoo, ettei `psql` ole tunnettu komento, lisää sen kansio nykyiseen ikkunaan (vaihda versionumero
omaasi vastaavaksi):

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\16\bin"
```

Tämä pitää tehdä uudelleen jokaisessa uudessa PowerShell-ikkunassa.

## 1. Luo tietokanta ja käyttäjä

Kanta luodaan UTF-8-merkistöllä. Se on tärkeää Windowsissa, koska Postgresin oletusmerkistö voi olla
`WIN1252`, jolloin osa resepteistä (esim. erikoismerkit) ei lataudu.

**Windows (PowerShell).** Komennot kysyvät `postgres`-käyttäjän salasanaa, jonka asetit Postgresia asentaessa:

```powershell
psql -U postgres -h localhost -c "CREATE USER mealplanner WITH PASSWORD 'valitse-salasana';"
psql -U postgres -h localhost -c "CREATE DATABASE mealplanner OWNER mealplanner ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C';"
```

**Linux / macOS:**

```bash
sudo -u postgres psql -c "CREATE USER mealplanner WITH PASSWORD 'valitse-salasana';"
sudo -u postgres psql -c "CREATE DATABASE mealplanner OWNER mealplanner ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C';"
```

Vaihda salasana omaksesi.

## 2. Lataa data

Tarvitset tiedoston `mealplanner_dump.sql.gz` (noin 21 MB pakattuna, 86 MB purettuna). Sitä ei ole repossa,
joten pyydä se tiimiltä. Tallenna se esimerkiksi kansioon, josta ajat komennot.

### 2a. Pura tiedosto

**Windows.** Helpoin tapa on 7-Zip: klikkaa tiedostoa hiiren oikealla ja valitse *7-Zip → Extract Here*.
Ilman 7-Zipiä sama onnistuu PowerShellillä (aja kansiossa, jossa tiedosto on):

```powershell
$src = (Resolve-Path .\mealplanner_dump.sql.gz).Path
$dst = Join-Path (Split-Path $src) "mealplanner_dump.sql"
$in  = [IO.File]::OpenRead($src)
$gz  = New-Object IO.Compression.GZipStream($in, [IO.Compression.CompressionMode]::Decompress)
$out = [IO.File]::Create($dst)
$gz.CopyTo($out); $out.Close(); $gz.Close(); $in.Close()
```

**Linux / macOS:**

```bash
gunzip -k mealplanner_dump.sql.gz
```

### 2b. Lataa tietokantaan

Lataa tiedosto **tyhjään** tietokantaan. Dump on UTF-8-muotoinen, joten Windowsissa pakotetaan `psql`:n
merkistö sellaiseksi.

**Windows (PowerShell):**

```powershell
$env:PGCLIENTENCODING = "UTF8"
psql -U mealplanner -d mealplanner -h localhost -f mealplanner_dump.sql
```

**Linux / macOS:**

```bash
psql -U mealplanner -d mealplanner -h localhost -f mealplanner_dump.sql
```

Komento kysyy `mealplanner`-käyttäjän salasanan. Latauksen pitäisi loppua sanaan `COMMIT` ilman virheitä
(kestää noin 10 sekuntia). Dump sisältää kaiken: reseptit, ainesosat, Finelin ravintoarvot ja ruokavaliot,
profiilitaulut sekä reseptien per annos -ravintoarvot. Repon `db/`-kansion SQL-tiedostoja ei tarvitse ajaa erikseen.

### Jos sinulla on jo vanhempi tietokanta

Jos kantasi on tehty aiemmalla dumpilla ja sovellus valittaa puuttuvasta taulusta tai sarakkeesta,
aja `db/`-kansion tiedostot (molemmat voi ajaa uudelleen turvallisesti). Komennot ovat samat kummallakin
käyttöjärjestelmällä (Windowsissa aseta ensin `$env:PGCLIENTENCODING = "UTF8"`):

```
psql -U mealplanner -d mealplanner -h localhost -f db/profile-schema.sql
psql -U mealplanner -d mealplanner -h localhost -f db/recipe-nutrition.sql
```

## 3. Aseta paikalliset tunnukset

Tietokannan salasana **ei ole gitissä**. Kopioi mallitiedosto ja täytä omat tunnuksesi.

**Windows (PowerShell):**

```powershell
Copy-Item src\main\resources\application-local.properties.example src\main\resources\application-local.properties
```

**Linux / macOS:**

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Muokkaa tiedostoa (`spring.datasource.url`, `username`, `password`) millä tahansa tekstieditorilla. Jos Postgres on
muussa portissa kuin 5432, vaihda se osoitteeseen. Tiedosto `application-local.properties` on `.gitignore`ssa, eikä
sitä pidä commitoida. Ilman tätä tiedostoa sovellus ei käynnisty.

## 4. Käynnistä backend

Aja repon juuressa.

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
sh mvnw spring-boot:run
```

Backend käynnistyy porttiin **8080**. Käynnistys onnistui, kun lokissa lukee `Started DemoApplication`.
Ensimmäinen käynnistys lataa riippuvuudet ja kestää pidempään.

Voit käynnistää sovelluksen myös VS Codesta tai IntelliJ:stä (`DemoApplication`-luokka), kunhan mikään muu
ei käytä porttia 8080.

### Tarkista, että toimii

Osoite `http://localhost:8080/` antaa virheen `404`, koska backendillä ei ole etusivua. Se on odotettua.
Helpoin tarkistus on avata nämä selaimessa:

- `http://localhost:8080/api/recipes/search?calories=2500&size=2` (reseptiehdotuksia)
- `http://localhost:8080/api/profile` (profiili)
- `http://localhost:8080/swagger-ui.html` (kaikkien endpointien selattava luettelo)

Komentoriviltä (**Windows:** käytä `Invoke-RestMethod`, koska PowerShellin `curl` on eri komento; **Linux / macOS:** `curl`):

```powershell
Invoke-RestMethod "http://localhost:8080/api/recipes/search?calories=2500&size=2"
```

```bash
curl "http://localhost:8080/api/recipes/search?calories=2500&size=2"
```

### Reseptihaun demosivu

Backendin mukana tulee yksinkertainen demosivu, jolla reseptihaun ja reseptin tietojen toiminnan näkee ilman
frontendiä: `http://localhost:8080/demo.html`. Sivu lataa tavoitteet profiilista, hakee ateriaehdotuksia tai päivän
ateriat ja avaa reseptin ainekset ja ohjeet klikkaamalla. Se on tarkoitettu kehittäjille, ei käyttäjille.
(Jos backend oli käynnissä ennen sivun lisäämistä, käynnistä se uudelleen.)

## 5. Käynnistä frontend

Frontend on omassa repossaan (`HH-DarkMode/Meal-Planner-Frontend`), ja ajantasainen versio on haarassa `main`.
Komennot ovat samat kummallakin käyttöjärjestelmällä:

```
git switch main
npm install
npm run dev
```

Avaa `http://localhost:5173`. Frontend kutsuu backendiä suoraan osoitteessa `http://localhost:8080` (ei välityspalvelinta),
joten backendin pitää olla käynnissä samalla koneella portissa 8080. Selain sallii kutsut vain, koska backend sallii
frontendin osoitteet `http://localhost:5173` ja `http://localhost:4173` (asetus `CorsConfig`). Jos ajat frontendiä
muussa portissa, lisää se siihen.

**Windows:** jos PowerShell sanoo, että skriptien ajaminen on estetty (`npm.ps1 cannot be loaded`), sallii
skriptit omalle käyttäjällesi kerran: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`. Vaihtoehtoisesti
käytä `npm.cmd install` ja `npm.cmd run dev`.

## Endpointit

| Endpoint | Kuvaus |
|---|---|
| `GET /api/recipes/search` | reseptiehdotuksia yhdelle ateriale |
| `GET /api/recipes/plan` | päivän ateriat: aamiainen, lounas ja päivällinen |
| `GET /api/recipes/{id}` | yhden reseptin ainekset ja valmistusohjeet |
| `GET /api/profile`, `PUT /api/profile` | käyttäjän profiili (tällä hetkellä yksi jaettu profiili) |

Hakuparametrit: `calories`, `protein`, `fat`, `carbs` (päivätavoitteet), `diets`
(`vegetarian`, `vegan`, `gluten-free`, `dairy-free`, `high-protein`, `low-carb`), `tolerance`, `size`.

Profiiliin tallentuvat kentät: `username`, `weight`, `height`, `mealsPerDay`, `gender`, `goal`, `diets`, `calories`, `protein`, `fat`, `carbs`.

## Ongelmatilanteet

| Oire | Syy ja korjaus |
|---|---|
| Sovellus ei käynnisty, valittaa tietokannasta tai salasanasta | `application-local.properties` puuttuu tai siinä on väärä salasana (kohta 3) |
| `Schema-validation: missing table/column` | tietokanta on vanhempi kuin koodi, aja `db/`-tiedostot (kohta 2) |
| `Port 8080 was already in use` | jokin toinen backend on jo käynnissä, sammuta se (ks. alla) |
| Windows: `psql` ei ole tunnettu komento | lisää PostgreSQL:n `bin`-kansio PATH:iin (ks. "Tarvittavat ohjelmat") |
| Windows: `invalid byte sequence` tai `has no equivalent in encoding "WIN1252"` latauksessa | kanta on luotu väärällä merkistöllä tai `PGCLIENTENCODING` puuttuu. Luo kanta kohdan 1 komennolla ja aseta `$env:PGCLIENTENCODING = "UTF8"` |
| Windows: `JAVA_HOME is not defined correctly` | JDK ei ole asennettu tai `java -version` ei toimi. Asenna JDK 17+ ja avaa uusi PowerShell-ikkuna |
| `mvnw package` tai `mvnw test` epäonnistuu | `DemoApplicationTests` tarvitsee toimivan tietokantayhteyden. Rakentamiseen käy `-DskipTests` |
| Frontend ei saa yhteyttä backendiin, selaimen konsolissa lukee `CORS`-virhe | frontend ajetaan portissa, jota `CorsConfig` ei salli, tai backend ei ole käynnissä portissa 8080 |

**Portti 8080 varattu — kuka sen käyttää?**

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

```bash
ss -ltnp | grep :8080
kill <pid>
```

## Tietojen lähteet ja lisenssit

- Ravintoarvot: **Fineli** (Terveyden ja hyvinvoinnin laitos, THL), lisenssi CC BY 4.0.
- Reseptit: **Culinary Recipes Dataset** (Zenodo), lisenssi ODC-BY 1.0. Reseptit ovat englanniksi ja peräisin Allrecipesista.

Molemmat lisenssit vaativat lähdemerkinnän, joten lähteet tulee mainita sovelluksessa.
