# MOTDManagerPlugin
Prosty plugin pozwalający na zmiane dowolnych wyświetlanych wartości na MOTD serwera

komendy:
/motdmanager reload - Przeładowuje config
/motdmanager check - Wyświetla załadowane wartości z configu
/motdmanager help - Wyświetla pomoc dot. komend

skript:
  events:
    server pinging
  effects:
    set motd fake player amount to %integer%");
    set motd hover to %string%");
    set motd line %integer% to %integer%");
    set motd max player amount to %integer%");
    set motd version to %string%");
  expressions:
    address of pinging person
    hover
    motd line %integer%
    max player(|s) amount
    online player(|s) amount
    motd edited version
    motd original version

config:
#Czy MOTD ma byc zmieniane
enable: true

#Czy MOTD ma być zmieniane według configu, czy skriptem
skript-mode: false

#Linia 1 i 2 MOTD
line1: '&aSampleLine1'
line2: '&bSampleLine2'

#Customowa wersja
version: '&cCustomVersion'

#Customowa ilosc graczy
online: 10
max: 20
#Fake players zawyża ilość graczy online o podaną ilość, gdy = 0 customowa ilość działa spowrotem
fakeplayers: 0

#Customowa lista graczy po najechaniu na ilonke z sygnalem
hover:
  - '&4line 1'
  - '&5line 2'
  - '&6line 3'

#Wazne notki:
#1) Gdy jest ustawiona customowa wersja to nie pokaze sie customowa ilosc graczy
#2) Przy zmianie ilosci graczy do listy hover dopisywana jest linijka z tekstem "...and N more..."
