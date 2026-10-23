# Asennusohje (Windows)

*Laadittu 19.10.2026, päivitetty 23.10.2026.*

Tällä ohjeella AI Meal Plannerin saa käyntiin omalla Windows-koneella: PostgreSQL-tietokanta, backend
(Spring Boot) ja frontend (React + Vite). Ohje on tehty Windows 10:lle ja 11:lle. Linuxin ja macOS:n erot
on koottu loppuun.

Kaikki komennot ajetaan **PowerShellissä**. Komentokehote (cmd) ei käy, koska `.\`- ja `$env:`-merkinnät
eivät toimi siinä.

Ensimmäinen asennus vie noin tunnin. Siitä suurin osa kuluu ohjelmien asentamiseen.

## Sisällys

1. [Mitä koneelle tarvitaan](#1-mitä-koneelle-tarvitaan)
2. [Ohjelmien asennus](#2-ohjelmien-asennus)
3. [Repojen kloonaus](#3-repojen-kloonaus)
4. [Tietokanta](#4-tietokanta)
5. [Backend](#5-backend)
6. [Frontend](#6-frontend)
7. [Käynnistys seuraavilla kerroilla](#7-käynnistys-seuraavilla-kerroilla)
8. [Ongelmatilanteet](#8-ongelmatilanteet)
9. [Linux ja macOS](#9-linux-ja-macos)

---

## 1. Mitä koneelle tarvitaan

### Ohjelmat

| Ohjelma | Versio | Mihin tarvitaan |
|---|---|---|
| **Git** | mikä tahansa tuore | repojen kloonaus |
| **Java JDK** | 17 tai uudempi, **suositus 25** | backend. Rahti ja CI käyttävät Javaa 25, joten sama versio omalla koneella vähentää yllätyksiä. |
| **PostgreSQL** | 16 tai uudempi | tietokanta ja `psql`-komento |
| **Node.js** | 22 LTS tai uudempi (npm tulee mukana) | frontend. Vite 8 ei toimi vanhoilla Node-versioilla. |
| 7-Zip | valinnainen | dump-tiedoston purku (onnistuu myös PowerShellillä) |
| VS Code tai IntelliJ IDEA | valinnainen | koodin muokkaus |

**Mavenia ei tarvitse asentaa.** Backendin repossa on Maven-wrapper (`mvnw.cmd`), joka lataa oikean
Maven-version itse.

### Muut tarvittavat asiat

- **Tietokannan dump-tiedosto `mealplanner_dump.sql.gz`** (noin 21 Mt). Siinä on reseptit ja Finelin
  ravintoarvot. Tiedosto on liian iso gitiin, joten **pyydä se tiimiltä**.
- **Pääsy GitHub-organisaatioon `HH-DarkMode`**, jos repot eivät ole julkisia.
- Vapaat portit **5432** (PostgreSQL), **8080** (backend) ja **5173** (frontend).
- Levytilaa noin 2 Gt ohjelmille, riippuvuuksille ja tietokannalle.

---

## 2. Ohjelmien asennus

Asenna ohjelmat alla olevassa järjestyksessä. **Avaa jokaisen asennuksen jälkeen uusi PowerShell-ikkuna.**
Vanha ikkuna ei näe uusia ohjelmia, koska PATH-ympäristömuuttuja luetaan vain ikkunaa avattaessa.

### 2.1 Git

Lataa asennusohjelma osoitteesta <https://git-scm.com/download/win>. Oletusasetukset käyvät.

### 2.2 Java (JDK)

1. Lataa **Eclipse Temurin JDK 25** (Windows x64, `.msi`) osoitteesta <https://adoptium.net/>.
2. Valitse asennusohjelman kohdassa *Custom Setup* myös **Set JAVA_HOME variable**
   (oletuksena se ei ole valittuna). Ilman sitä `mvnw.cmd` ei löydä Javaa.

### 2.3 PostgreSQL

1. Lataa asennusohjelma osoitteesta <https://www.postgresql.org/download/windows/>
   (linkki *Download the installer*, EDB).
2. Asennuksen aikana:
   - **Kirjoita ylös `postgres`-käyttäjän salasana.** Tarvitset sitä kohdassa 4.
   - Pidä portti **5432**.
   - *Locale*: oletus käy, koska tietokanta luodaan myöhemmin erikseen UTF-8-merkistöllä.
   - Stack Builderia ei tarvita. Ohita se asennuksen lopussa.
3. PostgreSQL käynnistyy Windows-palveluna automaattisesti aina kun kone käynnistyy.

**Lisää `psql` PATHiin pysyvästi.** EDB:n asennusohjelma ei tee tätä itse.

1. Paina Windows-näppäintä ja kirjoita **ympäristömuuttujat**. Valitse
   *Muokkaa tilisi ympäristömuuttujia* (Edit environment variables for your account).
2. Valitse ylemmästä listasta **Path** ja paina **Muokkaa**.
3. Paina **Uusi** ja lisää kansio, jossa versionumero vastaa asentamaasi versiota:
   `C:\Program Files\PostgreSQL\17\bin`
4. Paina OK kaikissa ikkunoissa ja avaa uusi PowerShell-ikkuna.

Jos haluat lisätä kansion vain avoimeen ikkunaan, se onnistuu komennolla
`$env:Path += ";C:\Program Files\PostgreSQL\17\bin"`.

### 2.4 Node.js

Lataa **LTS**-version asennusohjelma osoitteesta <https://nodejs.org/>. Oletusasetukset käyvät, eikä
*Tools for Native Modules* -valintaa tarvita.

### 2.5 Tarkista asennukset

Avaa uusi PowerShell-ikkuna ja aja:

```powershell
git --version
java -version
psql --version
node --version
npm --version
```

Jokaisen komennon pitää tulostaa versionumero. Jos jokin komento ei ole tunnettu, katso kohta 8.

**Jos `npm --version` antaa virheen `npm.ps1 cannot be loaded because running scripts is disabled`**,
salli skriptit omalle käyttäjällesi. Tämä tehdään vain kerran:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

---

## 3. Repojen kloonaus

Backend ja frontend ovat eri repoissa. Kloonaa ne samaan kansioon, esimerkiksi `C:\projektit\mealplanner`:

```powershell
mkdir C:\projektit\mealplanner
cd C:\projektit\mealplanner
git clone https://github.com/HH-DarkMode/Meal-Planner-Backend.git
git clone https://github.com/HH-DarkMode/Meal-Planner-Frontend.git
```

Kopioi myös tiimiltä saatu `mealplanner_dump.sql.gz` kansioon `C:\projektit\mealplanner`.
Ohjeen loppuosa olettaa tämän kansiorakenteen:

```
C:\projektit\mealplanner\
├── Meal-Planner-Backend\
├── Meal-Planner-Frontend\
└── mealplanner_dump.sql.gz
```

---

## 4. Tietokanta

### 4.1 Luo käyttäjä ja tietokanta

Komennot kysyvät `postgres`-käyttäjän salasanan, jonka asetit asennuksessa. Vaihda `valitse-salasana`
omaan salasanaasi. Sitä tarvitaan kohdassa 5.1.

```powershell
psql -U postgres -h localhost -c "CREATE USER mealplanner WITH PASSWORD 'valitse-salasana';"
psql -U postgres -h localhost -c "CREATE DATABASE mealplanner OWNER mealplanner ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C';"
```

Tietokanta luodaan **UTF-8-merkistöllä**. Windowsissa Postgresin oletusmerkistö voi olla `WIN1252`, jolloin
osa resepteistä (erikoismerkit) ei lataudu.

### 4.2 Pura dump-tiedosto

**7-Zipillä:** klikkaa `mealplanner_dump.sql.gz`-tiedostoa hiiren oikealla ja valitse
*7-Zip → Extract Here*.

**Ilman 7-Zipiä** (aja kansiossa `C:\projektit\mealplanner`):

```powershell
$src = (Resolve-Path .\mealplanner_dump.sql.gz).Path
$dst = Join-Path (Split-Path $src) "mealplanner_dump.sql"
$in  = [IO.File]::OpenRead($src)
$gz  = New-Object IO.Compression.GZipStream($in, [IO.Compression.CompressionMode]::Decompress)
$out = [IO.File]::Create($dst)
$gz.CopyTo($out); $out.Close(); $gz.Close(); $in.Close()
```

Tuloksena syntyy tiedosto `mealplanner_dump.sql`, jonka koko on noin 86 Mt.

### 4.3 Lataa data tietokantaan

Dump on UTF-8-muotoinen, joten `psql`:n merkistö pakotetaan samaksi. Aja kansiossa `C:\projektit\mealplanner`:

```powershell
$env:PGCLIENTENCODING = "UTF8"
psql -U mealplanner -d mealplanner -h localhost -f mealplanner_dump.sql
```

Komento kysyy `mealplanner`-käyttäjän salasanan. Lataus kestää noin 10–30 sekuntia, ja sen pitää päättyä
sanaan `COMMIT` ilman virheitä.

### 4.4 Luo ateriasuunnitelman taulu

**Tämä vaihe on pakollinen.** Dumpissa ei ole ateriavalintojen taulua (`meal_plan_selections`), koska
ominaisuus tehtiin dumpin jälkeen. Ilman taulua backend ei käynnisty.

```powershell
cd C:\projektit\mealplanner\Meal-Planner-Backend
$env:PGCLIENTENCODING = "UTF8"
psql -U mealplanner -d mealplanner -h localhost -f db/meal-plan-schema.sql
```

### 4.5 Tarkista tietokanta

```powershell
psql -U mealplanner -d mealplanner -h localhost -c "\dt"
```

Listassa pitää olla 11 taulua: `food_names`, `food_specdiets`, `food_units`, `foods`, `ingredient_matches`,
`meal_plan_selections`, `profile_diets`, `profiles`, `recipe_ingredients`, `recipes` ja `specdiets`.

---

## 5. Backend

Aja tämän osion komennot kansiossa `C:\projektit\mealplanner\Meal-Planner-Backend`.

### 5.1 Aseta tietokannan tunnukset

Backend lukee tietokannan osoitteen ja salasanan tiedostosta `application-local.properties`. Tiedosto ei ole
gitissä, koska siinä on salasana. Luo se mallitiedostosta:

```powershell
Copy-Item src\main\resources\application-local.properties.example src\main\resources\application-local.properties
notepad src\main\resources\application-local.properties
```

Vaihda Muistiossa `VAIHDA_SALASANA` tilalle kohdassa 4.1 valitsemasi salasana ja tallenna:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mealplanner
spring.datasource.username=mealplanner
spring.datasource.password=valitse-salasana
```

Tiedosto on `.gitignore`ssa. **Älä commitoi sitä.**

### 5.2 Käynnistä backend

```powershell
.\mvnw.cmd spring-boot:run
```

Ensimmäinen käynnistys lataa Mavenin ja kaikki riippuvuudet, joten siihen menee muutama minuutti.
Käynnistys on valmis, kun lokissa lukee `Started DemoApplication`. Backend toimii nyt portissa **8080**.

Jätä PowerShell-ikkuna auki. Backend sammuu, kun painat ikkunassa `Ctrl+C` tai suljet sen.

Backendin voi käynnistää myös VS Codesta tai IntelliJ:stä ajamalla `DemoApplication`-luokan.

### 5.3 Tarkista, että backend toimii

Avaa selaimessa:

| Osoite | Mitä pitäisi näkyä |
|---|---|
| <http://localhost:8080/> | `{"status":"ok", "message":"AI Meal Planner backend is running", ...}` |
| <http://localhost:8080/swagger-ui/index.html> | Swagger UI, jossa on kaikki endpointit |
| <http://localhost:8080/api/recipes/search?calories=2500&size=2> | kaksi reseptiä JSON-muodossa |

PowerShellissä sama tarkistus onnistuu `Invoke-RestMethod`illa. PowerShellin `curl` on eri komento kuin
Linuxin curl.

```powershell
Invoke-RestMethod "http://localhost:8080/api/recipes/search?calories=2500&size=2"
```

### 5.4 Testit (valinnainen)

```powershell
.\mvnw.cmd test
```

Testit eivät tarvitse tietokantaa. CI ajaa samat testit GitHubissa jokaisessa PR:ssä.

---

## 6. Frontend

Avaa **uusi** PowerShell-ikkuna, koska backendin ikkunan pitää jäädä auki. Aja komennot kansiossa
`C:\projektit\mealplanner\Meal-Planner-Frontend`.

### 6.1 Backendin osoite

Frontend lukee backendin osoitteen muuttujasta `VITE_BACKEND_URL`. Repossa on valmiina kaksi tiedostoa,
joten mitään ei tarvitse luoda:

| Tiedosto | Arvo | Milloin käytetään |
|---|---|---|
| `.env` | `http://localhost:8080` | `npm run dev` (paikallinen kehitys) |
| `.env.production` | Rahdin backend | `npm run build` (julkaisu) |

Muuta `.env`-tiedostoa vain, jos ajat backendiä muussa portissa kuin 8080. Älä commitoi omia muutoksiasi
siihen, koska tiedosto on yhteinen koko tiimille.

### 6.2 Asenna riippuvuudet ja käynnistä

```powershell
npm install
npm run dev
```

`npm install` tarvitaan vain ensimmäisellä kerralla ja aina kun `package.json` on muuttunut.

Avaa selaimessa <http://localhost:5173>.

### 6.3 Tarkista, että kokonaisuus toimii

1. Avaa **Profile**-sivu, täytä tiedot ja tallenna.
2. Avaa **Recipes**-sivu. Siinä pitäisi näkyä reseptiehdotukset aamiaiselle, lounaalle, välipalalle ja
   päivälliselle.
3. Valitse resepti napilla **Add to meal plan** ja tarkista, että se näkyy **Meal plan** -sivulla.

**Valintojen tyhjentäminen:** kun kaikki päivän ateriat on valittu, Recipes-sivu näyttää vain
"All meals chosen for today!". Valinnat tyhjennetään Swaggerissa: valitse
`DELETE /api/meal-plan/selections`, paina *Try it out* ja sitten *Execute*, ja lataa Recipes-sivu uudelleen.
Valinnat nollautuvat myös itsestään päivän vaihtuessa.

---

## 7. Käynnistys seuraavilla kerroilla

Kun asennus on tehty kerran, käynnistykseen riittää kaksi PowerShell-ikkunaa. PostgreSQL on jo käynnissä
Windows-palveluna.

**Ikkuna 1, backend:**

```powershell
cd C:\projektit\mealplanner\Meal-Planner-Backend
git pull
.\mvnw.cmd spring-boot:run
```

**Ikkuna 2, frontend:**

```powershell
cd C:\projektit\mealplanner\Meal-Planner-Frontend
git pull
npm install
npm run dev
```

Jos `git pull` toi uuden tiedoston `db/`-kansioon, aja se tietokantaan kohdan 4.4 tapaan ennen backendin
käynnistämistä.

---

## 8. Ongelmatilanteet

| Oire | Syy ja korjaus |
|---|---|
| `java`, `psql`, `node` tai `git` ei ole tunnettu komento | PowerShell-ikkuna on avattu ennen asennusta, joten avaa uusi ikkuna. `psql`:n kohdalla tarkista PATH (kohta 2.3). |
| `JAVA_HOME is not defined correctly` | JAVA_HOME-valinta jäi pois Javan asennuksessa. Aja asennusohjelma uudelleen, valitse *Set JAVA_HOME variable* ja avaa uusi ikkuna. |
| `npm.ps1 cannot be loaded because running scripts is disabled` | Aja `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (kohta 2.5) tai käytä komentoja `npm.cmd install` ja `npm.cmd run dev`. |
| `password authentication failed for user "mealplanner"` | Salasana on väärä `application-local.properties`-tiedostossa tai `psql`-komennossa. |
| Backend ei käynnisty, ja lokissa lukee `Schema-validation: missing table [meal_plan_selections]` | Kohta 4.4 on tekemättä. |
| `Schema-validation: missing column` | Tietokanta on luotu vanhemmalla dumpilla. Aja `db/profile-schema.sql` ja `db/recipe-nutrition.sql` kohdan 4.4 tapaan. Molemmat voi ajaa uudelleen turvallisesti. |
| `invalid byte sequence` tai `has no equivalent in encoding "WIN1252"` datan latauksessa | Tietokanta on luotu ilman UTF-8:aa tai `PGCLIENTENCODING` puuttuu. Poista kanta (`psql -U postgres -h localhost -c "DROP DATABASE mealplanner;"`), luo se uudelleen kohdan 4.1 komennolla ja lataa data uudelleen. |
| `Port 8080 was already in use` | Toinen backend on jo käynnissä. Katso alta, miten löydät ja sammutat sen. |
| Frontend näyttää "Could not load recipes. Is the backend running?" | Backend ei ole käynnissä portissa 8080, tai `.env`-tiedoston osoite on muutettu (kohta 6.1). Käynnistä `npm run dev` uudelleen aina kun `.env` muuttuu. |
| Selaimen konsolissa on `CORS`-virhe | Frontend toimii muussa portissa kuin 5173 tai 4173. Backend sallii vain nämä portit (asetus `app.cors.allowed-origins` tiedostossa `application.properties`). |

**Kuka käyttää porttia 8080?**

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

Vaihda `<pid>` ensimmäisen komennon tulostaman rivin viimeiseen numeroon.

---

## 9. Linux ja macOS

Vaiheet ovat samat, mutta muutama komento on erilainen:

| Vaihe | Windows (PowerShell) | Linux / macOS |
|---|---|---|
| Käyttäjän ja kannan luonti (4.1) | `psql -U postgres -h localhost -c "..."` | `sudo -u postgres psql -c "..."` (macOS + Homebrew: `psql postgres -c "..."`) |
| Dumpin purku (4.2) | 7-Zip tai PowerShell-skripti | `gunzip -k mealplanner_dump.sql.gz` |
| `PGCLIENTENCODING` (4.3, 4.4) | `$env:PGCLIENTENCODING = "UTF8"` | ei tarvita |
| Tunnusten kopiointi (5.1) | `Copy-Item ...` | `cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties` |
| Backendin käynnistys (5.2) | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Portin 8080 käyttäjä | `netstat -ano \| findstr :8080` | `lsof -i :8080` tai `ss -ltnp \| grep :8080` |
