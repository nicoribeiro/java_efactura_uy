# logrotate

`java_efactura_uy`: rota el log de consola de la API (`java_efactura_uy.log`, salida estandar del proceso), que no
rota solo. El log de la aplicacion (`java-efactura-uy-app.log`) lo rota logback (`conf/logback.xml`, 90 dias).

Instalar en cada servidor (necesita sudo):

    scp scripts/logrotate/java_efactura_uy <usuario>@<servidor>:/tmp/
    ssh -t <usuario>@<servidor> "sudo install -o root -g root -m 644 /tmp/java_efactura_uy /etc/logrotate.d/java_efactura_uy && sudo logrotate -d /etc/logrotate.d/java_efactura_uy"

`logrotate -d` es una prueba en seco: tiene que decir `considering log .../java_efactura_uy.log` y ningun `error`.

## Logs de la aplicacion anteriores a maxHistory

Logback 1.1.x, al arrancar, solo borra los archivos de unos 32 dias por detras del limite de 90; los mas viejos que
ya estaban antes de configurar `maxHistory` quedan para siempre. Borrarlos una vez a mano en cada servidor:

    find /var/log/srv -maxdepth 2 -name 'java-efactura-uy-app.20*.log' -mtime +90 -print -delete
