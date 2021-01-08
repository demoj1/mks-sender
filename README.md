# MKS WiFi file uploader

Compatibilities with Marlin firmware.  
Tested on Ghost 5.

# Usage

## PrusaSlicer

Go to Print Settings => Output options => Post-processing scripts  
Insert next code:
```sh
java -jar <PATH_TO_RELEASE_FILE>.jar <PRINTER_IP>;
```

For example:
```sh
java -jar /Users/mac/Documents/mks-uploader/target/mks-uploader.jar 192.168.88.196;
```

Log file locate in temporary directory by name mks-sender.log.  
In Unix-like system is **/tmp**.

---

# MKS WiFi file uploader

Совместимо с Marlin  
Тестировалось на принтере Ghost 5

# Как использовать

## PrusaSlicer

Перейти в Настройки печати => Выходные параметры => Скрипты постобработки  
Добавить следующий код:
```sh
java -jar <ПУТЬ_ДО_СКАЧЕННОГО ФАЙЛА>.jar <IP_АДРЕС_ПРИНТЕРА>;
```

Для примера:
```sh
java -jar /Users/mac/Documents/mks-uploader/target/mks-uploader.jar 192.168.88.196;
```

Файл с логами находиться в временной директории под названием mks-sender.log.  
В Unix-like системах **/tmp**.