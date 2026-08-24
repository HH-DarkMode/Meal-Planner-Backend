# AI Meal Planner

## Projektin kuvaus

**AI Meal Planner** on verkkosovellus, jonka tarkoituksena on helpottaa käyttäjän ruokien ja aterioiden suunnittelua. Sovelluksessa hyödynnetään Finelin elintarviketietoja sekä tekoälyä, jonka avulla käyttäjälle voidaan muodostaa hänen tavoitteisiinsa sopivia aterioita ja reseptejä.

Sovelluksen tavoitteena on yhdistää ravintoarvotiedot ja tekoäly käytännölliseksi meal planner -sovellukseksi. Käyttäjä voi esimerkiksi määrittää päivittäisen kalorimäärän, proteiinitavoitteen sekä ruokavalioon liittyviä rajoitteita. Näiden tietojen perusteella sovellus voi muodostaa aterioita ja kokonaisia ruokasuunnitelmia.

## Tärkeimmät ominaisuudet

Sovelluksen alustavia ominaisuuksia ovat:

* Käyttäjä voi määrittää omat ravitsemustavoitteensa, kuten päivittäisen kalorimäärän ja proteiinitavoitteen.
* Käyttäjä voi määrittää ruokavalioon liittyviä rajoitteita ja mieltymyksiä.
* Sovellus hyödyntää Finelin elintarviketietoja ruoka-aineiden ravintoarvojen määrittämiseen.
* Tekoäly voi yhdistää eri raaka-aineita ja muodostaa niistä reseptejä.
* Sovellus voi muodostaa käyttäjälle päivä- tai viikkokohtaisen meal planin.
* Käyttäjä voi tarkastella aterioiden ravintoarvoja.
* Käyttäjä voi vaihtaa yksittäisen aterian tai reseptin toiseen.
* Sovellus voi muodostaa meal planin perusteella ostoslistan.
* Käyttäjä voi ilmoittaa käytettävissä olevia raaka-aineita, joita tekoäly voi hyödyntää resepteissä.

Projektin edetessä ominaisuuksia ja niiden toteutustapaa voidaan tarkentaa.

## Toteutusteknologiat

Projektin alustaviksi toteutusteknologioiksi on suunniteltu:

### Backend

* **Java**
* **Spring Boot**
* Spring Web REST API:n toteuttamiseen
* Spring Data JPA tietokantakäsittelyyn
* **PostgreSQL** tietokantana
* **Maven** projektinhallintaan

### Frontend

Frontendin toteutuksessa tullaan alustavasti käyttämään:

* **React**
* **TypeScript**
* HTML
* CSS

Frontend kommunikoi Spring Boot -backendin kanssa REST-rajapinnan avulla.

### Ulkoiset palvelut ja data

* **Fineli** elintarvikkeiden ja ravintoarvojen tietolähteenä
* Tekoälypalvelun API reseptien ja ruokasuunnitelmien muodostamiseen

Teknologiat ja käytettävät palvelut voivat muuttua projektin edetessä.

## Alustava arkkitehtuuri

Sovelluksen alustava rakenne on seuraava:



Spring Boot vastaa sovelluksen liiketoimintalogiikasta, ravintoarvojen laskennasta ja ulkoisten palveluiden kanssa kommunikoinnista. Tekoälyä hyödynnetään erityisesti reseptien ja ruokasuunnitelmien muodostamisessa.

## Tiimi

* **ghostinth3w1re95-cyber** — [GitHub-profiili](https://github.com/ghostinth3w1re95-cyber)
* **[GitHub-käyttäjätunnus]** — [GitHub-profiili](https://github.com/USERNAME)
* **[GitHub-käyttäjätunnus]** — [GitHub-profiili](https://github.com/USERNAME)

> Korvatkaa yllä olevat käyttäjätunnukset ja profiililinkit projektitiimin oikeilla GitHub-tiedoilla.

## Projektin tila

Projekti on tällä hetkellä suunnitteluvaiheessa. Projektin edetessä README-tiedostoa tullaan päivittämään vastaamaan toteutettua sovellusta ja käytettyjä teknologioita.
