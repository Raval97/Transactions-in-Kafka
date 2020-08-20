/**  OBSERWACJE
 *
 * 1. DLa tych samych groupId nie ma duplikacji wiadomosci zarówno w transakcji i bez niej
 * 2. Dla różnych groupId jest duplikacja w obu przypadkach
 * 3. Czasami za pierwszym uruchomieniem przy pustym topicu uruchamia sie tylko producent (pierwszy blok)
 *
 * @@@@@@@@@@@@@@@@@@@@@@@@@@
 *
 * 4. Alpakka wysypuje błąd tych samych transactionalId, który nie przechwytuje decider, a restartSource
 * 5. Zombie dla bez transakcyjnosci działą dobrze, bo nie ma błędu tego samego transactional ID
 * 6. Dla bez transakcyjnosci podczas rzucenia błędu ConnectException traconyvh jest kilka pierwszych wiadomosci  z commita
 * 7  Dla bez transakcyjnosci wiadomosci odebrane z commita zapisywane sa w losowej kolejnosci
 * 8. Dla transakcyjnosci wysypanie bledu na RWP działa a na Consumencie sa duplikaty
 *
 * @@@@@@@@@@@@@@@@@@@@@@@@@@
 *
 * at.8 Po przetestowaniu, widać ze drugi blok transakcyjny (tzn. pierwsza transakcje odbiera z tematu A
 *      i przesyłą na temat B gdzie nasłuchuje druga transakcja) powoduje duplikacje
 * 9. Samo wywołanie RestartSource na pojedynńczym sourcie np na transakcji na której jest wyrzucony
 *      błąd ConnectException nie powoduje pobrania wiadomości jeszcze raz, trzeba je ponownie przesłać, dlatego
 *      musi sie wykonać RestartSource całego zestawu sourców złączonych przy pomocy Merga
 *
 * ########################
 *
 * Założenie: Pierwszy uruchomi się wątek (Future) transakcji zawartej w zestawie sourców podpietuch do RestartSourca
 *            a po nim uruchomi się Zombie, który jest pojedyńczą instancją bez RestartSourca
 * Obserwacje: Oba wątki (transakcja i Zombie_transakcja) odbiorą commit i zapiszą go do topicu kafki,
 *             ale tylko wiadomości przesłane przez ten drugi czyli Zombie będę mogły zostać zczytane
 *             (ustawiane jest cos w rodzaju flagi). Podczas tego procesu alpakka wyspie błąd pojawienia się
 *             transakcji z tym samymy ID, błąd ten nie może zostać przechwycony przez Decider, lecz powoduje on
 *             uruchomienie RestartSource i cały blok uruchamia się jeszcze raz. Producent produkuje te same wiadomośc
 *             ktore przechwyci wciąż dzialąjacy Zombie i zapisze je do tematu wyjsciowego, a następnie uruchomi się
 *             blok transkcji, który spowoduje zamianę głowneg procesu transakcyjnego o tym samym ID i prześle
 *             ten commit ponownie, powodując ostatecznie przesłanie tego samego commita 3-krotnie
 *
 * Aktualny stan projektu:
 * Bez transakcyjnośći
 *  - zombie: Działa, przesyłąne wiadomości są duplikowane
 *  - wyrzucenie błedu, Działa Połowicznie, po RestartSourcie wiadomości przesyłąne są ponownie
 *                     co powoduje ich duplikacje, ale tylko tych wiadomości które dal rade zapamietać,
 *                     ponieważ pierwsze wiadomości są gubione
 * Transakcja
 * - zombie: Nie Działa (chyba że pierwszy uruchomi się watęk Zombie a potem docelowa transakcja)
 * - wyrzucenie błędu: Działą Połowicznie, wyrzucenie blędu na drugim bloku (pierwsza transakcja powoduje
 *                     pomyślny RestartSource, ale na drugim występują duplikaty)
 */


Polecenia:
sudo -s
docker-compose -f docker-compose.yml up -d
docker exec -it kafka /bin/sh
cd opt/kafka/bin

./kafka-topics.sh --delete --topic inputTopic1,inputTopic2,outputTopic,ProductPrice,FinalPrice --zookeeper zookeeper:2181
./kafka-consumer-groups.sh  --bootstrap-server localhost:9092 --describe --group group1

./kafka-topics.sh --list --zookeeper zookeeper:2181
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic

./kafka-topics.sh --delete --topic topic --zookeeper zookeeper:2181 && \
./kafka-topics.sh --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 3 --topic topic && \
./kafka-topics.sh --describe --zookeeper zookeeper:2181 --topic topic

*** ctrl + shift + backapace