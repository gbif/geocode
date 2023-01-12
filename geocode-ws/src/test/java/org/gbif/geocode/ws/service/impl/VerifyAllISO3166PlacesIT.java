package org.gbif.geocode.ws.service.impl;

import org.gbif.api.vocabulary.Country;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


import org.gbif.geocode.api.service.InternalGeocodeService;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.gbif.api.vocabulary.Country.*;

/**
 * Check we give the preferred response for all places in our Country enumeration,
 * i.e. everywhere with an ISO 3166-2 code, including dependent territories, overseas regions, disputed
 * regions etc. These can change in the underlying data sources, and it's undesirable that we could
 * "lose" a country due to a change in politics from a data source.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GeocoderIntegrationTestsConfiguration.class)
@TestPropertySource(value = "classpath:application-test.properties",
  properties = {"spring.shapefiles.enabled=PoliticalLayer", "spring.defaultLayers=Political"})
public class VerifyAllISO3166PlacesIT {
  private static final Logger LOG = LoggerFactory.getLogger(VerifyAllISO3166PlacesIT.class);

  private final InternalGeocodeService geocoder;
  private Set<Country> spottedCountries = new HashSet<>();

  @Autowired
  public VerifyAllISO3166PlacesIT(GeocodeServiceImpl geocodeServiceImpl) {
    this.geocoder = geocodeServiceImpl;
  }

  /**
   * Some basics with comments explaining how territories etc are handled.
   */
  @Test
  public void basicTest() {

    // Straightforward test — Zürich is in Switzerland.
    testCountry("Zürich", 47.37, 8.55, SWITZERLAND);

    // The UK has overseas territories, but the UK itself has an ISO code.  This is unlikely to
    // be a problem in the data sources.
    testCountry("London", 51.51, -0.13, UNITED_KINGDOM);
    // Simple EEZ check.
    testCountry("Off the coast of Brighton", 50.51, 0, UNITED_KINGDOM);

    // Norway is similar, but for some reason Norway doesn't have ISO code NO in the data source.
    testCountry("Tromsø, Norway", 69.68, 18.94, NORWAY);
    testCountry("Ocean off Norway", 69.00, 13.00, NORWAY);
    // Svalbard and Jan Mayen have their own ISO codes
    testCountry("Longyearbyen, Svalbard", 78.22, 15.65, SVALBARD_JAN_MAYEN);
    testCountry("Ocean off Svalbard", 83.00, 20.00, SVALBARD_JAN_MAYEN);
    testCountry("Airstrip on Jan Mayen", 70.961111, -8.575833, SVALBARD_JAN_MAYEN);
    testCountry("Sea around Jan Mayen", 71, -4, SVALBARD_JAN_MAYEN);
    // As does Bouvet Island
    testCountry("Bouvet Island", -54.43, 3.38, BOUVET_ISLAND);
    testCountry("Sea around Bouvet Island", -54.5, 3, BOUVET_ISLAND);

    // Similarly for Denmark, Greenland, Faroe Islands, Australia etc.

    // France has overseas departments, which are equal with France.
    // Metropolitan France (France in Europe) has ISO code FR, but the database comes
    // with a polygon for all of France, which doesn't (strictly) have an ISO code.
    // It's been overridden.
    testCountry("Paris, Metropolitain France", 48.86, 2.35, FRANCE);
    // These overseas departments have ISO codes, and we do not override them
    // in the database to use FR.
    testCountry("Forêt de Bébour, Réunion", -21.11, 55.54, RÉUNION);
    testCountry("Cadet, Guadeloupe", 16.27, -61.69, GUADELOUPE);
    testCountry("Certitude, French Guiana", 3.83, -53.3, FRENCH_GUIANA);
    testCountry("Les Trois-Îlets, Martinique", 14.53, -61.04, MARTINIQUE);
    testCountry("Mamoudzou, Mayotte", -12.78, 45.23, MAYOTTE, COMOROS);
    // The territorial waters of France may also need an ISO code override.
    testCountry("La Manche off France", 50.95, 1.60, FRANCE);
    testCountry("Ocean off Réunion", -21, 56, RÉUNION);
    testCountry("Ocean off Guadeloupe", 16.0, -61.4, GUADELOUPE);
    testCountry("Ocean off French Guiana", 5.29, -52.4, FRENCH_GUIANA);
    testCountry("Ocean off Martinique", 14.67, -60.82, MARTINIQUE);
    testCountry("Ocean off Mayotte", -12.64, 45.17, MAYOTTE, COMOROS);
    // The overseas collectivities and special collectivity of France have more independence,
    // and shouldn't be put under France.
    testCountry("Mont 'Orohena, Tahiti, French Polynesia", -17.62, -149.50, FRENCH_POLYNESIA);
    testCountry("Ocean surrounding French Polynesia", -15.64, -145.81, FRENCH_POLYNESIA);
    testCountry("Saint Pierre and Miquelon", 47.04, -56.32, SAINT_PIERRE_MIQUELON);
    testCountry("Ocean by Saint Pierre and Miquelon", 46.92, -56.37, SAINT_PIERRE_MIQUELON);
    testCountry("Wallis and Futuna", -14.28, -178.14, WALLIS_FUTUNA);
    testCountry("Ocean by Wallis and Futuna", -14.27, -178.05, WALLIS_FUTUNA);
    testCountry("Cul-de-Sac, Saint Martin", 18.10, -63.03, SAINT_MARTIN_FRENCH);
    testCountry("Ocean by Saint Martin", 18.10, -62.95, SAINT_MARTIN_FRENCH);
    testCountry("Saint Barthélemy", 17.90, -62.82, SAINT_BARTHÉLEMY);
    testCountry("Ocean by Saint Barthélemy", 18, -62.5, SAINT_BARTHÉLEMY);
    testCountry("New Caledonia", -20.98, 165.12, NEW_CALEDONIA);
    testCountry("Ocean by New Caledonia", -20.16, 165.36, NEW_CALEDONIA);
    // The French Southern and Antarctic Lands
    testCountry("Europa Island", -22.366667, 40.366667, FRENCH_SOUTHERN_TERRITORIES);
    testCountry("Bassas da India", -21.483333, 39.683333, FRENCH_SOUTHERN_TERRITORIES);
    testCountry("Juan de Nova Island", -17.055556, 42.725, FRENCH_SOUTHERN_TERRITORIES);
    testCountry("Tromelin Island", -15.892222, 54.524722, FRENCH_SOUTHERN_TERRITORIES, MADAGASCAR, MAURITIUS);
    // Possibly TF ought to be before MG here.
    testCountry("Glorioso Islands", -11.55, 47.333333, MADAGASCAR, FRENCH_SOUTHERN_TERRITORIES);

    // Taiwan / Chinese Taipei
    testCountry("臺北市, Taiwan", 25.05, 121.54, TAIWAN);
    testCountry("Ocean near Taiwan", 22.59, 121.14, TAIWAN);

    // Northern Cyprus
    testCountry("Northern Cyprus", 35.21, 33.66, CYPRUS);
    testCountry("Sea north of Cyprus", 35.43, 33.70, CYPRUS);

    // Gaza City, Palestine
    testCountry("Gaza City, Palestine", 31.52, 34.45, PALESTINIAN_TERRITORY, ISRAEL);
    testCountry("Sea NW of Gaza, Palestine", 31.46, 34.34, PALESTINIAN_TERRITORY, ISRAEL);

    // Kosovo.
    testCountry("Pristina, Kosovo", 42.67, 21.15, KOSOVO);

    // Somaliland isn't recognised.
    testCountry("Hargeisa, Somaliland, Somalia", 9.56, 44.07, SOMALIA);
    testCountry("Sea north of Somalia", 11.31, 47.33, SOMALIA);

    // Laâyoune, Western Sahara, Morocco
    testCountry("Laâyoune, Western Sahara, Morocco", 27.15, -13.20, WESTERN_SAHARA, MOROCCO);
    testCountry("Ocean west of Western Sahara, Morocco", 26.85, -13.65, WESTERN_SAHARA, MOROCCO);

    // See also, https://en.wikipedia.org/wiki/List_of_states_with_limited_recognition
  }

  @Test
  public void capitalCitiesTest() {
    spottedCountries.clear();

    // Mostly capital cities, doesn't necessarily include all ISO codes.
    testCountry("Kabul", 34.51666667, 69.183333, AFGHANISTAN);
    testCountry("Mariehamn", 60.116667, 19.9, ALAND_ISLANDS);
    testCountry("Tirana", 41.31666667, 19.816667, ALBANIA);
    testCountry("Algiers", 36.75, 3.05, ALGERIA);
    testCountry("Pago Pago", -14.26666667, -170.7, AMERICAN_SAMOA);
    testCountry("Andorra la Vella", 42.55, 1.516667, ANDORRA);
    testCountry("Luanda", -8.833333333, 13.216667, ANGOLA);
    testCountry("The Valley", 18.21666667, -63.05, ANGUILLA);
    testCountry("South Pole", -90, 0, ANTARCTICA);
    testCountry("Saint John's", 17.11666667, -61.85, ANTIGUA_BARBUDA);
    testCountry("Buenos Aires", -34.58333333, -58.666667, ARGENTINA);
    testCountry("Yerevan", 40.16666667, 44.5, ARMENIA);
    testCountry("Oranjestad", 12.51666667, -70.033333, ARUBA);
    testCountry("Canberra", -35.26666667, 149.133333, AUSTRALIA);
    testCountry("Vienna", 48.2, 16.366667, AUSTRIA);
    testCountry("Baku", 40.38333333, 49.866667, AZERBAIJAN);
    testCountry("Nassau", 25.08333333, -77.35, BAHAMAS);
    testCountry("Manama", 26.23333333, 50.566667, BAHRAIN);
    testCountry("Dhaka", 23.71666667, 90.4, BANGLADESH);
    testCountry("Bridgetown", 13.1, -59.616667, BARBADOS);
    testCountry("Minsk", 53.9, 27.566667, BELARUS);
    testCountry("Brussels", 50.83333333, 4.333333, BELGIUM);
    testCountry("Belmopan", 17.25, -88.766667, BELIZE);
    testCountry("Porto-Novo", 6.483333333, 2.616667, BENIN);
    testCountry("Hamilton", 32.28333333, -64.783333, BERMUDA);
    testCountry("Thimphu", 27.46666667, 89.633333, BHUTAN);
    testCountry("La Paz", -16.5, -68.15, BOLIVIA);
    testCountry("Bonaire", 12.18, -68.25, BONAIRE_SINT_EUSTATIUS_SABA);
    testCountry("Sarajevo", 43.86666667, 18.416667, BOSNIA_HERZEGOVINA);
    testCountry("Gaborone", -24.63333333, 25.9, BOTSWANA);
    testCountry("Bouvet Island", -54.43, 3.38, BOUVET_ISLAND);
    testCountry("Brasilia", -15.78333333, -47.916667, BRAZIL);
    testCountry("Diego Garcia", -7.3, 72.4, BRITISH_INDIAN_OCEAN_TERRITORY, MAURITIUS);
    testCountry("Road Town", 18.41666667, -64.616667, VIRGIN_ISLANDS_BRITISH);
    testCountry("Bandar Seri Begawan", 4.883333333, 114.933333, BRUNEI_DARUSSALAM);
    testCountry("Sofia", 42.68333333, 23.316667, BULGARIA);
    testCountry("Ouagadougou", 12.36666667, -1.516667, BURKINA_FASO);
    testCountry("Bujumbura", -3.366666667, 29.35, BURUNDI);
    testCountry("Phnom Penh", 11.58, 104.87, CAMBODIA);
    testCountry("Yaounde", 3.866666667, 11.516667, CAMEROON);
    testCountry("Ottawa", 45.41666667, -75.7, CANADA);
    testCountry("Praia", 14.91666667, -23.516667, CAPE_VERDE);
    testCountry("George Town", 19.3, -81.383333, CAYMAN_ISLANDS);
    testCountry("Mbrès", 6.67, 19.80, CENTRAL_AFRICAN_REPUBLIC); // Rather than Bangui
    testCountry("Moundou", 8.57, 16.07, CHAD); // Rather than N'Djamena
    testCountry("Santiago", -33.45, -70.666667, CHILE);
    testCountry("Beijing", 39.91666667, 116.383333, CHINA);
    testCountry("The Settlement", -10.41666667, 105.716667, CHRISTMAS_ISLAND);
    testCountry("West Island", -12.15, 96.82, COCOS_ISLANDS);
    testCountry("Bogota", 4.6, -74.083333, COLOMBIA);
    testCountry("Moroni", -11.7, 43.233333, COMOROS);
    testCountry("Avarua", -21.2, -159.766667, COOK_ISLANDS);
    testCountry("San Jose", 9.933333333, -84.083333, COSTA_RICA);
    testCountry("Yamoussoukro", 6.816666667, -5.266667, CÔTE_DIVOIRE);
    testCountry("Zagreb", 45.8, 16, CROATIA);
    testCountry("Havana", 23.11666667, -82.35, CUBA);
    testCountry("Willemstad", 12.1, -68.916667, CURAÇAO);
    testCountry("Nicosia", 35.16666667, 33.366667, CYPRUS);
    testCountry("Prague", 50.08333333, 14.466667, CZECH_REPUBLIC);
    testCountry("Mbanza-Ngungu", -5.25, 14.86, CONGO_DEMOCRATIC_REPUBLIC); // Rather than Kinshasa
    testCountry("Copenhagen", 55.66666667, 12.583333, DENMARK);
    testCountry("Djibouti", 11.58333333, 43.15, DJIBOUTI);
    testCountry("Roseau", 15.3, -61.4, DOMINICA);
    testCountry("Santo Domingo", 18.46666667, -69.9, DOMINICAN_REPUBLIC);
    testCountry("Quito", -0.216666667, -78.5, ECUADOR);
    testCountry("Cairo", 30.05, 31.25, EGYPT);
    testCountry("San Salvador", 13.7, -89.2, EL_SALVADOR);
    testCountry("Malabo", 3.75, 8.783333, EQUATORIAL_GUINEA);
    testCountry("Asmara", 15.33333333, 38.933333, ERITREA);
    testCountry("Tallinn", 59.43333333, 24.716667, ESTONIA);
    testCountry("Addis Ababa", 9.033333333, 38.7, ETHIOPIA);
    testCountry("Stanley", -51.7, -57.85, FALKLAND_ISLANDS, ARGENTINA);
    testCountry("Torshavn", 62, -6.766667, FAROE_ISLANDS);
    testCountry("Palikir", 6.916666667, 158.15, MICRONESIA);
    testCountry("Suva", -18.13333333, 178.416667, FIJI);
    testCountry("Helsinki", 60.16666667, 24.933333, FINLAND);
    testCountry("Paris", 48.86666667, 2.333333, FRANCE);
    testCountry("Certitude", 3.83, -53.3, FRENCH_GUIANA);
    testCountry("Papeete", -17.53333333, -149.566667, FRENCH_POLYNESIA);
    testCountry("Port-aux-Français", -49.35, 70.216667, FRENCH_SOUTHERN_TERRITORIES);
    testCountry("Libreville", 0.383333333, 9.45, GABON);
    testCountry("Tbilisi", 41.68333333, 44.833333, GEORGIA);
    testCountry("Berlin", 52.51666667, 13.4, GERMANY);
    testCountry("Accra", 5.55, -0.216667, GHANA);
    testCountry("Gibraltar", 36.13333333, -5.35, GIBRALTAR, SPAIN);
    testCountry("Athens", 37.98333333, 23.733333, GREECE);
    testCountry("Nuuk", 64.18333333, -51.75, GREENLAND);
    testCountry("Saint George's", 12.05, -61.75, GRENADA);
    testCountry("Cadet", 16.27, -61.69, GUADELOUPE);
    testCountry("Hagatna", 13.46666667, 144.733333, GUAM);
    testCountry("Guatemala City", 14.61666667, -90.516667, GUATEMALA);
    testCountry("Saint Peter Port", 49.45, -2.533333, GUERNSEY);
    testCountry("Conakry", 9.5, -13.7, GUINEA);
    testCountry("Bissau", 11.85, -15.583333, GUINEA_BISSAU);
    testCountry("Georgetown", 6.8, -58.15, GUYANA);
    testCountry("Port-au-Prince", 18.53333333, -72.333333, HAITI);
    testCountry("Heard & McDonald Islands", -53.1, 73.516667, HEARD_MCDONALD_ISLANDS);
    testCountry("Tegucigalpa", 14.1, -87.216667, HONDURAS);
    testCountry("Hong Kong", 22.25, 114.2, HONG_KONG);
    testCountry("Budapest", 47.5, 19.083333, HUNGARY);
    testCountry("Reykjavik", 64.15, -21.95, ICELAND);
    testCountry("New Delhi", 28.6, 77.2, INDIA);
    testCountry("Jakarta", -6.166666667, 106.816667, INDONESIA);
    testCountry("Tehran", 35.7, 51.416667, IRAN);
    testCountry("Baghdad", 33.33333333, 44.4, IRAQ);
    testCountry("Dublin", 53.31666667, -6.233333, IRELAND);
    testCountry("Douglas", 54.15, -4.483333, ISLE_OF_MAN);
    testCountry("Tel Aviv", 32.07, 34.78, ISRAEL);
    testCountry("Rome", 41.9, 12.483333, ITALY);
    testCountry("Kingston", 18, -76.8, JAMAICA);
    testCountry("Tokyo", 35.68333333, 139.75, JAPAN);
    testCountry("Saint Helier", 49.18333333, -2.1, JERSEY);
    testCountry("Amman", 31.95, 35.933333, JORDAN);
    testCountry("Astana", 51.16666667, 71.416667, KAZAKHSTAN);
    testCountry("Nairobi", -1.283333333, 36.816667, KENYA);
    testCountry("Tarawa", -0.883333333, 169.533333, KIRIBATI);
    testCountry("Kuwait City", 29.36666667, 47.966667, KUWAIT);
    testCountry("Bishkek", 42.86666667, 74.6, KYRGYZSTAN);
    testCountry("Vientiane", 19.15, 102.21, LAO); // Rather than Vientiane
    testCountry("Riga", 56.95, 24.1, LATVIA);
    testCountry("Beirut", 33.86666667, 35.5, LEBANON);
    testCountry("Maseru", -29.84, 28.05, LESOTHO); // Rather than Maseru
    testCountry("Monrovia", 6.3, -10.8, LIBERIA);
    testCountry("Tripoli", 32.88333333, 13.166667, LIBYA);
    testCountry("Vaduz", 47.13333333, 9.516667, LIECHTENSTEIN);
    testCountry("Vilnius", 54.68333333, 25.316667, LITHUANIA);
    testCountry("Luxembourg", 49.6, 6.116667, LUXEMBOURG);
    testCountry("Macao", 22.14, 113.57, MACAO);
    testCountry("Skopje", 42, 21.433333, MACEDONIA);
    testCountry("Antananarivo", -18.91666667, 47.516667, MADAGASCAR);
    testCountry("Lilongwe", -13.96666667, 33.783333, MALAWI);
    testCountry("Kuala Lumpur", 3.166666667, 101.7, MALAYSIA);
    testCountry("Male", 4.166666667, 73.5, MALDIVES);
    testCountry("Bamako", 12.65, -8, MALI);
    testCountry("Valletta", 35.88333333, 14.5, MALTA);
    testCountry("Majuro", 7.1, 171.383333, MARSHALL_ISLANDS);
    testCountry("Les Trois-Îlets", 14.53, -61.04, MARTINIQUE);
    testCountry("Nouakchott", 18.06666667, -15.966667, MAURITANIA);
    testCountry("Port Louis", -20.15, 57.483333, MAURITIUS);
    testCountry("Mamoudzou", -12.78, 45.23, MAYOTTE, COMOROS);
    testCountry("Mexico City", 19.43333333, -99.133333, MEXICO);
    testCountry("Chisinau", 47, 28.85, MOLDOVA);
    testCountry("Monaco", 43.73333333, 7.416667, MONACO);
    testCountry("Ulaanbaatar", 47.91666667, 106.916667, MONGOLIA);
    testCountry("Podgorica", 42.43333333, 19.266667, MONTENEGRO);
    testCountry("Plymouth", 16.7, -62.216667, MONTSERRAT);
    testCountry("Rabat", 34.01666667, -6.816667, MOROCCO);
    testCountry("Maputo", -25.95, 32.583333, MOZAMBIQUE);
    testCountry("Rangoon", 16.8, 96.15, MYANMAR);
    testCountry("Windhoek", -22.56666667, 17.083333, NAMIBIA);
    testCountry("Yaren", -0.5477, 166.920867, NAURU);
    testCountry("Kathmandu", 27.71666667, 85.316667, NEPAL);
    testCountry("Amsterdam", 52.35, 4.916667, NETHERLANDS);
    testCountry("Noumea", -22.26666667, 166.45, NEW_CALEDONIA);
    testCountry("Wellington", -41.3, 174.783333, NEW_ZEALAND);
    testCountry("Managua", 12.13333333, -86.25, NICARAGUA);
    testCountry("Niamey", 13.51666667, 2.116667, NIGER);
    testCountry("Abuja", 9.083333333, 7.533333, NIGERIA);
    testCountry("Alofi", -19.01666667, -169.916667, NIUE);
    testCountry("Kingston", -29.05, 167.966667, NORFOLK_ISLAND);
    testCountry("Pyongyang", 39.01666667, 125.75, KOREA_NORTH);
    testCountry("North Nicosia", 35.183333, 33.366667, CYPRUS);
    testCountry("Saipan", 15.2, 145.75, NORTHERN_MARIANA_ISLANDS);
    testCountry("Oslo", 59.91666667, 10.75, NORWAY);
    testCountry("Muscat", 23.61666667, 58.583333, OMAN);
    testCountry("Islamabad", 33.68333333, 73.05, PAKISTAN);
    testCountry("Melekeok", 7.483333333, 134.633333, PALAU);
    testCountry("Jerusalem", 31.76666667, 35.233333, PALESTINIAN_TERRITORY, ISRAEL);
    testCountry("Panama City", 8.966666667, -79.533333, PANAMA);
    testCountry("Port Moresby", -9.45, 147.183333, PAPUA_NEW_GUINEA);
    testCountry("Caaguazú", -25.47, -56.02, PARAGUAY); // Rather than Asuncion
    testCountry("Lima", -12.05, -77.05, PERU);
    testCountry("Manila", 14.6, 120.966667, PHILIPPINES);
    testCountry("Adamstown", -25.06666667, -130.083333, PITCAIRN);
    testCountry("Warsaw", 52.25, 21, POLAND);
    testCountry("Lisbon", 38.71666667, -9.133333, PORTUGAL);
    testCountry("San Juan", 18.46666667, -66.116667, PUERTO_RICO);
    testCountry("Doha", 25.28333333, 51.533333, QATAR);
    testCountry("Owando", -0.49, 15.90, CONGO); // Rather than Brazzaville
    testCountry("Forêt de Bébour", -21.11, 55.54, RÉUNION);
    testCountry("Bucharest", 44.43333333, 26.1, ROMANIA);
    testCountry("Moscow", 55.75, 37.6, RUSSIAN_FEDERATION);
    testCountry("Kigali", -1.95, 30.05, RWANDA);
    testCountry("Gustavia", 17.90, -62.82, SAINT_BARTHÉLEMY);
    testCountry("Jamestown", -15.93333333, -5.716667, SAINT_HELENA_ASCENSION_TRISTAN_DA_CUNHA);
    testCountry("Basseterre", 17.3, -62.716667, SAINT_KITTS_NEVIS);
    testCountry("Castries", 14, -61, SAINT_LUCIA);
    testCountry("Marigot", 18.0731, -63.0822, SAINT_MARTIN_FRENCH);
    testCountry("Saint-Pierre", 46.76666667, -56.183333, SAINT_PIERRE_MIQUELON);
    testCountry("Kingstown", 13.13333333, -61.216667, SAINT_VINCENT_GRENADINES);
    testCountry("Apia", -13.81666667, -171.766667, SAMOA);
    testCountry("San Marino", 43.93333333, 12.416667, SAN_MARINO);
    testCountry("Sao Tome", 0.333333333, 6.733333, SAO_TOME_PRINCIPE);
    testCountry("Riyadh", 24.65, 46.7, SAUDI_ARABIA);
    testCountry("Dakar", 14.73333333, -17.633333, SENEGAL);
    testCountry("Belgrade", 44.83333333, 20.5, SERBIA);
    testCountry("Victoria", -4.616666667, 55.45, SEYCHELLES);
    testCountry("Freetown", 8.483333333, -13.233333, SIERRA_LEONE);
    testCountry("Singapore", 1.283333333, 103.85, SINGAPORE);
    testCountry("Philipsburg", 18.01666667, -63.033333, SINT_MAARTEN);
    testCountry("Bratislava", 48.15, 17.116667, SLOVAKIA);
    testCountry("Ljubljana", 46.05, 14.516667, SLOVENIA);
    testCountry("Honiara", -9.433333333, 159.95, SOLOMON_ISLANDS);
    testCountry("Mogadishu", 2.066666667, 45.333333, SOMALIA);
    testCountry("Hargeisa", 9.55, 44.05, SOMALIA);
    testCountry("Pretoria", -25.7, 28.216667, SOUTH_AFRICA);
    testCountry("King Edward Point", -54.283333, -36.5, SOUTH_GEORGIA_SANDWICH_ISLANDS, ARGENTINA);
    testCountry("Seoul", 37.55, 126.983333, KOREA_SOUTH);
    testCountry("Juba", 4.85, 31.616667, SOUTH_SUDAN);
    testCountry("Madrid", 40.4, -3.683333, SPAIN);
    testCountry("Colombo", 6.916666667, 79.833333, SRI_LANKA);
    testCountry("Khartoum", 15.6, 32.533333, SUDAN);
    testCountry("Paramaribo", 5.833333333, -55.166667, SURINAME);
    testCountry("Longyearbyen", 78.21666667, 15.633333, SVALBARD_JAN_MAYEN);
    testCountry("Mbabane", -26.31666667, 31.133333, SWAZILAND);
    testCountry("Stockholm", 59.33333333, 18.05, SWEDEN);
    testCountry("Bern", 46.91666667, 7.466667, SWITZERLAND);
    testCountry("Damascus", 33.5, 36.3, SYRIA);
    testCountry("Taipei", 25.03333333, 121.516667, TAIWAN);
    testCountry("Dushanbe", 38.55, 68.766667, TAJIKISTAN);
    testCountry("Dar es Salaam", -6.8, 39.283333, TANZANIA);
    testCountry("Bangkok", 13.75, 100.516667, THAILAND);
    testCountry("Banjul", 13.45, -16.566667, GAMBIA);
    testCountry("Dili", -8.583333333, 125.6, TIMOR_LESTE);
    testCountry("Tchekpo Dedekpoe", 6.53, 1.36, TOGO); // Rather than Lomé
    testCountry("Atafu", -9.166667, -171.833333, TOKELAU);
    testCountry("Nuku'alofa", -21.13333333, -175.2, TONGA);
    testCountry("Port of Spain", 10.65, -61.516667, TRINIDAD_TOBAGO);
    testCountry("Tunis", 36.8, 10.183333, TUNISIA);
    testCountry("Ankara", 39.93333333, 32.866667, TURKEY);
    testCountry("Ashgabat", 37.95, 58.383333, TURKMENISTAN);
    testCountry("Grand Turk", 21.46666667, -71.133333, TURKS_CAICOS_ISLANDS);
    testCountry("Funafuti", -8.516666667, 179.216667, TUVALU);
    testCountry("Kampala", 0.316666667, 32.55, UGANDA);
    testCountry("Kyiv", 50.43333333, 30.516667, UKRAINE);
    testCountry("Abu Dhabi", 24.46666667, 54.366667, UNITED_ARAB_EMIRATES);
    testCountry("London", 51.5, -0.083333, UNITED_KINGDOM);
    testCountry("Washington, D.C.", 38.883333, -77, UNITED_STATES);
    testCountry("Montevideo", -34.85, -56.166667, URUGUAY);
    testCountry("Wake Island", 19.28, 166.64, UNITED_STATES_OUTLYING_ISLANDS);
    testCountry("Charlotte Amalie", 18.35, -64.933333, VIRGIN_ISLANDS);
    testCountry("Tashkent", 41.31666667, 69.25, UZBEKISTAN);
    testCountry("Port-Vila", -17.73333333, 168.316667, VANUATU);
    testCountry("Vatican City", 41.903, 12.453, VATICAN);
    testCountry("Caracas", 10.48333333, -66.866667, VENEZUELA);
    testCountry("Hanoi", 21.03333333, 105.85, VIETNAM);
    testCountry("Mata-Utu", -13.28, -176.18, WALLIS_FUTUNA);
    testCountry("Laayoune", 27.153611, -13.203333, WESTERN_SAHARA, MOROCCO);
    testCountry("Sanaa", 15.35, 44.2, YEMEN);
    testCountry("Lusaka", -15.41666667, 28.283333, ZAMBIA);
    testCountry("Harare", -17.81666667, 31.033333, ZIMBABWE);

    for (Country o : Country.OFFICIAL_COUNTRIES) {
      if (!spottedCountries.contains(o)) {
        LOG.error(o.getTitle() + " missing");
      }
    }
    for (Country o : Country.OFFICIAL_COUNTRIES) {
      MatcherAssert.assertThat(o.getTitle() + " checked", spottedCountries.contains(o));
    }
    Assertions.assertEquals(spottedCountries.size(), Country.OFFICIAL_COUNTRIES.size());

    // Kosovo is an unofficial code (XK)
    testCountry("Pristina", 42.66666667, 21.166667, KOSOVO);

    // The same countries, but in reverse, to check the bitmap cache is OK.
    // (grep testCountry VerifyAllISO3166PlacesIT.java | tac ...)
    testCountry("Pristina", 42.66666667, 21.166667, KOSOVO);
    testCountry("Harare", -17.81666667, 31.033333, ZIMBABWE);
    testCountry("Lusaka", -15.41666667, 28.283333, ZAMBIA);
    testCountry("Sanaa", 15.35, 44.2, YEMEN);
    testCountry("Laayoune", 27.153611, -13.203333, WESTERN_SAHARA, MOROCCO);
    testCountry("Mata-Utu", -13.28, -176.18, WALLIS_FUTUNA);
    testCountry("Hanoi", 21.03333333, 105.85, VIETNAM);
    testCountry("Caracas", 10.48333333, -66.866667, VENEZUELA);
    testCountry("Vatican City", 41.903, 12.453, VATICAN);
    testCountry("Port-Vila", -17.73333333, 168.316667, VANUATU);
    testCountry("Tashkent", 41.31666667, 69.25, UZBEKISTAN);
    testCountry("Charlotte Amalie", 18.35, -64.933333, VIRGIN_ISLANDS);
    testCountry("Wake Island", 19.28, 166.64, UNITED_STATES_OUTLYING_ISLANDS);
    testCountry("Montevideo", -34.85, -56.166667, URUGUAY);
    testCountry("Washington, D.C.", 38.883333, -77, UNITED_STATES);
    testCountry("London", 51.5, -0.083333, UNITED_KINGDOM);
    testCountry("Abu Dhabi", 24.46666667, 54.366667, UNITED_ARAB_EMIRATES);
    testCountry("Kyiv", 50.43333333, 30.516667, UKRAINE);
    testCountry("Kampala", 0.316666667, 32.55, UGANDA);
    testCountry("Funafuti", -8.516666667, 179.216667, TUVALU);
    testCountry("Grand Turk", 21.46666667, -71.133333, TURKS_CAICOS_ISLANDS);
    testCountry("Ashgabat", 37.95, 58.383333, TURKMENISTAN);
    testCountry("Ankara", 39.93333333, 32.866667, TURKEY);
    testCountry("Tunis", 36.8, 10.183333, TUNISIA);
    testCountry("Port of Spain", 10.65, -61.516667, TRINIDAD_TOBAGO);
    testCountry("Nuku'alofa", -21.13333333, -175.2, TONGA);
    testCountry("Atafu", -9.166667, -171.833333, TOKELAU);
    testCountry("Tchekpo Dedekpoe", 6.53, 1.36, TOGO); // Rather than Lomé
    testCountry("Dili", -8.583333333, 125.6, TIMOR_LESTE);
    testCountry("Banjul", 13.45, -16.566667, GAMBIA);
    testCountry("Bangkok", 13.75, 100.516667, THAILAND);
    testCountry("Dar es Salaam", -6.8, 39.283333, TANZANIA);
    testCountry("Dushanbe", 38.55, 68.766667, TAJIKISTAN);
    testCountry("Taipei", 25.03333333, 121.516667, TAIWAN);
    testCountry("Damascus", 33.5, 36.3, SYRIA);
    testCountry("Bern", 46.91666667, 7.466667, SWITZERLAND);
    testCountry("Stockholm", 59.33333333, 18.05, SWEDEN);
    testCountry("Mbabane", -26.31666667, 31.133333, SWAZILAND);
    testCountry("Longyearbyen", 78.21666667, 15.633333, SVALBARD_JAN_MAYEN);
    testCountry("Paramaribo", 5.833333333, -55.166667, SURINAME);
    testCountry("Khartoum", 15.6, 32.533333, SUDAN);
    testCountry("Colombo", 6.916666667, 79.833333, SRI_LANKA);
    testCountry("Madrid", 40.4, -3.683333, SPAIN);
    testCountry("Juba", 4.85, 31.616667, SOUTH_SUDAN);
    testCountry("Seoul", 37.55, 126.983333, KOREA_SOUTH);
    testCountry("King Edward Point", -54.283333, -36.5, SOUTH_GEORGIA_SANDWICH_ISLANDS, ARGENTINA);
    testCountry("Pretoria", -25.7, 28.216667, SOUTH_AFRICA);
    testCountry("Hargeisa", 9.55, 44.05, SOMALIA);
    testCountry("Mogadishu", 2.066666667, 45.333333, SOMALIA);
    testCountry("Honiara", -9.433333333, 159.95, SOLOMON_ISLANDS);
    testCountry("Ljubljana", 46.05, 14.516667, SLOVENIA);
    testCountry("Bratislava", 48.15, 17.116667, SLOVAKIA);
    testCountry("Philipsburg", 18.01666667, -63.033333, SINT_MAARTEN);
    testCountry("Singapore", 1.283333333, 103.85, SINGAPORE);
    testCountry("Freetown", 8.483333333, -13.233333, SIERRA_LEONE);
    testCountry("Victoria", -4.616666667, 55.45, SEYCHELLES);
    testCountry("Belgrade", 44.83333333, 20.5, SERBIA);
    testCountry("Dakar", 14.73333333, -17.633333, SENEGAL);
    testCountry("Riyadh", 24.65, 46.7, SAUDI_ARABIA);
    testCountry("Sao Tome", 0.333333333, 6.733333, SAO_TOME_PRINCIPE);
    testCountry("San Marino", 43.93333333, 12.416667, SAN_MARINO);
    testCountry("Apia", -13.81666667, -171.766667, SAMOA);
    testCountry("Kingstown", 13.13333333, -61.216667, SAINT_VINCENT_GRENADINES);
    testCountry("Saint-Pierre", 46.76666667, -56.183333, SAINT_PIERRE_MIQUELON);
    testCountry("Marigot", 18.0731, -63.0822, SAINT_MARTIN_FRENCH);
    testCountry("Castries", 14, -61, SAINT_LUCIA);
    testCountry("Basseterre", 17.3, -62.716667, SAINT_KITTS_NEVIS);
    testCountry("Jamestown", -15.93333333, -5.716667, SAINT_HELENA_ASCENSION_TRISTAN_DA_CUNHA);
    testCountry("Gustavia", 17.90, -62.82, SAINT_BARTHÉLEMY);
    testCountry("Kigali", -1.95, 30.05, RWANDA);
    testCountry("Moscow", 55.75, 37.6, RUSSIAN_FEDERATION);
    testCountry("Bucharest", 44.43333333, 26.1, ROMANIA);
    testCountry("Forêt de Bébour", -21.11, 55.54, RÉUNION);
    testCountry("Owando", -0.49, 15.90, CONGO); // Rather than Brazzaville
    testCountry("Doha", 25.28333333, 51.533333, QATAR);
    testCountry("San Juan", 18.46666667, -66.116667, PUERTO_RICO);
    testCountry("Lisbon", 38.71666667, -9.133333, PORTUGAL);
    testCountry("Warsaw", 52.25, 21, POLAND);
    testCountry("Adamstown", -25.06666667, -130.083333, PITCAIRN);
    testCountry("Manila", 14.6, 120.966667, PHILIPPINES);
    testCountry("Lima", -12.05, -77.05, PERU);
    testCountry("Caaguazú", -25.47, -56.02, PARAGUAY); // Rather than Asuncion
    testCountry("Port Moresby", -9.45, 147.183333, PAPUA_NEW_GUINEA);
    testCountry("Panama City", 8.966666667, -79.533333, PANAMA);
    testCountry("Jerusalem", 31.76666667, 35.233333, PALESTINIAN_TERRITORY, ISRAEL);
    testCountry("Melekeok", 7.483333333, 134.633333, PALAU);
    testCountry("Islamabad", 33.68333333, 73.05, PAKISTAN);
    testCountry("Muscat", 23.61666667, 58.583333, OMAN);
    testCountry("Oslo", 59.91666667, 10.75, NORWAY);
    testCountry("Saipan", 15.2, 145.75, NORTHERN_MARIANA_ISLANDS);
    testCountry("North Nicosia", 35.183333, 33.366667, CYPRUS);
    testCountry("Pyongyang", 39.01666667, 125.75, KOREA_NORTH);
    testCountry("Kingston", -29.05, 167.966667, NORFOLK_ISLAND);
    testCountry("Alofi", -19.01666667, -169.916667, NIUE);
    testCountry("Abuja", 9.083333333, 7.533333, NIGERIA);
    testCountry("Niamey", 13.51666667, 2.116667, NIGER);
    testCountry("Managua", 12.13333333, -86.25, NICARAGUA);
    testCountry("Wellington", -41.3, 174.783333, NEW_ZEALAND);
    testCountry("Noumea", -22.26666667, 166.45, NEW_CALEDONIA);
    testCountry("Amsterdam", 52.35, 4.916667, NETHERLANDS);
    testCountry("Kathmandu", 27.71666667, 85.316667, NEPAL);
    testCountry("Yaren", -0.5477, 166.920867, NAURU);
    testCountry("Windhoek", -22.56666667, 17.083333, NAMIBIA);
    testCountry("Rangoon", 16.8, 96.15, MYANMAR);
    testCountry("Maputo", -25.95, 32.583333, MOZAMBIQUE);
    testCountry("Rabat", 34.01666667, -6.816667, MOROCCO);
    testCountry("Plymouth", 16.7, -62.216667, MONTSERRAT);
    testCountry("Podgorica", 42.43333333, 19.266667, MONTENEGRO);
    testCountry("Ulaanbaatar", 47.91666667, 106.916667, MONGOLIA);
    testCountry("Monaco", 43.73333333, 7.416667, MONACO);
    testCountry("Chisinau", 47, 28.85, MOLDOVA);
    testCountry("Mexico City", 19.43333333, -99.133333, MEXICO);
    testCountry("Mamoudzou", -12.78, 45.23, MAYOTTE, COMOROS);
    testCountry("Port Louis", -20.15, 57.483333, MAURITIUS);
    testCountry("Nouakchott", 18.06666667, -15.966667, MAURITANIA);
    testCountry("Les Trois-Îlets", 14.53, -61.04, MARTINIQUE);
    testCountry("Majuro", 7.1, 171.383333, MARSHALL_ISLANDS);
    testCountry("Valletta", 35.88333333, 14.5, MALTA);
    testCountry("Bamako", 12.65, -8, MALI);
    testCountry("Male", 4.166666667, 73.5, MALDIVES);
    testCountry("Kuala Lumpur", 3.166666667, 101.7, MALAYSIA);
    testCountry("Lilongwe", -13.96666667, 33.783333, MALAWI);
    testCountry("Antananarivo", -18.91666667, 47.516667, MADAGASCAR);
    testCountry("Skopje", 42, 21.433333, MACEDONIA);
    testCountry("Macao", 22.14, 113.57, MACAO);
    testCountry("Luxembourg", 49.6, 6.116667, LUXEMBOURG);
    testCountry("Vilnius", 54.68333333, 25.316667, LITHUANIA);
    testCountry("Vaduz", 47.13333333, 9.516667, LIECHTENSTEIN);
    testCountry("Tripoli", 32.88333333, 13.166667, LIBYA);
    testCountry("Monrovia", 6.3, -10.8, LIBERIA);
    testCountry("Maseru", -29.84, 28.05, LESOTHO); // Rather than Maseru
    testCountry("Beirut", 33.86666667, 35.5, LEBANON);
    testCountry("Riga", 56.95, 24.1, LATVIA);
    testCountry("Vientiane", 19.15, 102.21, LAO); // Rather than Vientiane
    testCountry("Bishkek", 42.86666667, 74.6, KYRGYZSTAN);
    testCountry("Kuwait City", 29.36666667, 47.966667, KUWAIT);
    testCountry("Tarawa", -0.883333333, 169.533333, KIRIBATI);
    testCountry("Nairobi", -1.283333333, 36.816667, KENYA);
    testCountry("Astana", 51.16666667, 71.416667, KAZAKHSTAN);
    testCountry("Amman", 31.95, 35.933333, JORDAN);
    testCountry("Saint Helier", 49.18333333, -2.1, JERSEY);
    testCountry("Tokyo", 35.68333333, 139.75, JAPAN);
    testCountry("Kingston", 18, -76.8, JAMAICA);
    testCountry("Rome", 41.9, 12.483333, ITALY);
    testCountry("Tel Aviv", 32.07, 34.78, ISRAEL);
    testCountry("Douglas", 54.15, -4.483333, ISLE_OF_MAN);
    testCountry("Dublin", 53.31666667, -6.233333, IRELAND);
    testCountry("Baghdad", 33.33333333, 44.4, IRAQ);
    testCountry("Tehran", 35.7, 51.416667, IRAN);
    testCountry("Jakarta", -6.166666667, 106.816667, INDONESIA);
    testCountry("New Delhi", 28.6, 77.2, INDIA);
    testCountry("Reykjavik", 64.15, -21.95, ICELAND);
    testCountry("Budapest", 47.5, 19.083333, HUNGARY);
    testCountry("Hong Kong", 22.25, 114.2, HONG_KONG);
    testCountry("Tegucigalpa", 14.1, -87.216667, HONDURAS);
    testCountry("Heard & McDonald Islands", -53.1, 73.516667, HEARD_MCDONALD_ISLANDS);
    testCountry("Port-au-Prince", 18.53333333, -72.333333, HAITI);
    testCountry("Georgetown", 6.8, -58.15, GUYANA);
    testCountry("Bissau", 11.85, -15.583333, GUINEA_BISSAU);
    testCountry("Conakry", 9.5, -13.7, GUINEA);
    testCountry("Saint Peter Port", 49.45, -2.533333, GUERNSEY);
    testCountry("Guatemala City", 14.61666667, -90.516667, GUATEMALA);
    testCountry("Hagatna", 13.46666667, 144.733333, GUAM);
    testCountry("Cadet", 16.27, -61.69, GUADELOUPE);
    testCountry("Saint George's", 12.05, -61.75, GRENADA);
    testCountry("Nuuk", 64.18333333, -51.75, GREENLAND);
    testCountry("Athens", 37.98333333, 23.733333, GREECE);
    testCountry("Gibraltar", 36.13333333, -5.35, GIBRALTAR, SPAIN);
    testCountry("Accra", 5.55, -0.216667, GHANA);
    testCountry("Berlin", 52.51666667, 13.4, GERMANY);
    testCountry("Tbilisi", 41.68333333, 44.833333, GEORGIA);
    testCountry("Libreville", 0.383333333, 9.45, GABON);
    testCountry("Port-aux-Français", -49.35, 70.216667, FRENCH_SOUTHERN_TERRITORIES);
    testCountry("Papeete", -17.53333333, -149.566667, FRENCH_POLYNESIA);
    testCountry("Certitude", 3.83, -53.3, FRENCH_GUIANA);
    testCountry("Paris", 48.86666667, 2.333333, FRANCE);
    testCountry("Helsinki", 60.16666667, 24.933333, FINLAND);
    testCountry("Suva", -18.13333333, 178.416667, FIJI);
    testCountry("Palikir", 6.916666667, 158.15, MICRONESIA);
    testCountry("Torshavn", 62, -6.766667, FAROE_ISLANDS);
    testCountry("Stanley", -51.7, -57.85, FALKLAND_ISLANDS, ARGENTINA);
    testCountry("Addis Ababa", 9.033333333, 38.7, ETHIOPIA);
    testCountry("Tallinn", 59.43333333, 24.716667, ESTONIA);
    testCountry("Asmara", 15.33333333, 38.933333, ERITREA);
    testCountry("Malabo", 3.75, 8.783333, EQUATORIAL_GUINEA);
    testCountry("San Salvador", 13.7, -89.2, EL_SALVADOR);
    testCountry("Cairo", 30.05, 31.25, EGYPT);
    testCountry("Quito", -0.216666667, -78.5, ECUADOR);
    testCountry("Santo Domingo", 18.46666667, -69.9, DOMINICAN_REPUBLIC);
    testCountry("Roseau", 15.3, -61.4, DOMINICA);
    testCountry("Djibouti", 11.58333333, 43.15, DJIBOUTI);
    testCountry("Copenhagen", 55.66666667, 12.583333, DENMARK);
    testCountry("Mbanza-Ngungu", -5.25, 14.86, CONGO_DEMOCRATIC_REPUBLIC); // Rather than Kinshasa
    testCountry("Prague", 50.08333333, 14.466667, CZECH_REPUBLIC);
    testCountry("Nicosia", 35.16666667, 33.366667, CYPRUS);
    testCountry("Willemstad", 12.1, -68.916667, CURAÇAO);
    testCountry("Havana", 23.11666667, -82.35, CUBA);
    testCountry("Zagreb", 45.8, 16, CROATIA);
    testCountry("Yamoussoukro", 6.816666667, -5.266667, CÔTE_DIVOIRE);
    testCountry("San Jose", 9.933333333, -84.083333, COSTA_RICA);
    testCountry("Avarua", -21.2, -159.766667, COOK_ISLANDS);
    testCountry("Moroni", -11.7, 43.233333, COMOROS);
    testCountry("Bogota", 4.6, -74.083333, COLOMBIA);
    testCountry("West Island", -12.15, 96.82, COCOS_ISLANDS);
    testCountry("The Settlement", -10.41666667, 105.716667, CHRISTMAS_ISLAND);
    testCountry("Beijing", 39.91666667, 116.383333, CHINA);
    testCountry("Santiago", -33.45, -70.666667, CHILE);
    testCountry("Moundou", 8.57, 16.07, CHAD); // Rather than N'Djamena
    testCountry("Mbrès", 6.67, 19.80, CENTRAL_AFRICAN_REPUBLIC); // Rather than Bangui
    testCountry("George Town", 19.3, -81.383333, CAYMAN_ISLANDS);
    testCountry("Praia", 14.91666667, -23.516667, CAPE_VERDE);
    testCountry("Ottawa", 45.41666667, -75.7, CANADA);
    testCountry("Yaounde", 3.866666667, 11.516667, CAMEROON);
    testCountry("Phnom Penh", 11.58, 104.87, CAMBODIA);
    testCountry("Bujumbura", -3.366666667, 29.35, BURUNDI);
    testCountry("Ouagadougou", 12.36666667, -1.516667, BURKINA_FASO);
    testCountry("Sofia", 42.68333333, 23.316667, BULGARIA);
    testCountry("Bandar Seri Begawan", 4.883333333, 114.933333, BRUNEI_DARUSSALAM);
    testCountry("Road Town", 18.41666667, -64.616667, VIRGIN_ISLANDS_BRITISH);
    testCountry("Diego Garcia", -7.3, 72.4, BRITISH_INDIAN_OCEAN_TERRITORY, MAURITIUS);
    testCountry("Brasilia", -15.78333333, -47.916667, BRAZIL);
    testCountry("Bouvet Island", -54.43, 3.38, BOUVET_ISLAND);
    testCountry("Gaborone", -24.63333333, 25.9, BOTSWANA);
    testCountry("Sarajevo", 43.86666667, 18.416667, BOSNIA_HERZEGOVINA);
    testCountry("Bonaire", 12.18, -68.25, BONAIRE_SINT_EUSTATIUS_SABA);
    testCountry("La Paz", -16.5, -68.15, BOLIVIA);
    testCountry("Thimphu", 27.46666667, 89.633333, BHUTAN);
    testCountry("Hamilton", 32.28333333, -64.783333, BERMUDA);
    testCountry("Porto-Novo", 6.483333333, 2.616667, BENIN);
    testCountry("Belmopan", 17.25, -88.766667, BELIZE);
    testCountry("Brussels", 50.83333333, 4.333333, BELGIUM);
    testCountry("Minsk", 53.9, 27.566667, BELARUS);
    testCountry("Bridgetown", 13.1, -59.616667, BARBADOS);
    testCountry("Dhaka", 23.71666667, 90.4, BANGLADESH);
    testCountry("Manama", 26.23333333, 50.566667, BAHRAIN);
    testCountry("Nassau", 25.08333333, -77.35, BAHAMAS);
    testCountry("Baku", 40.38333333, 49.866667, AZERBAIJAN);
    testCountry("Vienna", 48.2, 16.366667, AUSTRIA);
    testCountry("Canberra", -35.26666667, 149.133333, AUSTRALIA);
    testCountry("Oranjestad", 12.51666667, -70.033333, ARUBA);
    testCountry("Yerevan", 40.16666667, 44.5, ARMENIA);
    testCountry("Buenos Aires", -34.58333333, -58.666667, ARGENTINA);
    testCountry("Saint John's", 17.11666667, -61.85, ANTIGUA_BARBUDA);
    testCountry("South Pole", -90, 0, ANTARCTICA);
    testCountry("The Valley", 18.21666667, -63.05, ANGUILLA);
    testCountry("Luanda", -8.833333333, 13.216667, ANGOLA);
    testCountry("Andorra la Vella", 42.55, 1.516667, ANDORRA);
    testCountry("Pago Pago", -14.26666667, -170.7, AMERICAN_SAMOA);
    testCountry("Algiers", 36.75, 3.05, ALGERIA);
    testCountry("Tirana", 41.31666667, 19.816667, ALBANIA);
    testCountry("Mariehamn", 60.116667, 19.9, ALAND_ISLANDS);
    testCountry("Kabul", 34.51666667, 69.183333, AFGHANISTAN);
  }

  static class LatLng {
    double lat;
    double lng;

    LatLng(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    public double getLat() {
      return lat;
    }

    public double getLng() {
      return lng;
    }
  }

  private List<Country> getCountryForLatLng(LatLng coord) {
    List<Country> countries = new ArrayList();

    Collection<Location> lookups = geocoder.get(coord.getLat(), coord.getLng(), null, null);
    boolean first = true;
    if (lookups != null && lookups.size() > 0) {
      for (Location loc : lookups) {
        if (loc.getIsoCountryCode2Digit() != null && loc.getDistance() == 0) {
          countries.add(Country.fromIsoCode(loc.getIsoCountryCode2Digit()));
        }
        if (first) {
          spottedCountries.add(Country.fromIsoCode(loc.getIsoCountryCode2Digit()));
          first = false;
        }
      }
      LOG.debug("Countries are {}", countries);
    }
    LOG.debug("[{}] locations for coord {}: {}", lookups.size(), coord, countries);

    return countries;
  }

  private void testCountry(String explanation, double lat, double lng, Country... countries) {
    LOG.info("Testing {} ({},{}); want {}", explanation, lat, lng, countries);
    Assertions.assertEquals(
        Arrays.asList(countries),
        Arrays.asList(getCountryForLatLng(new LatLng(lat, lng)).toArray(new Country[0])));
  }
}
