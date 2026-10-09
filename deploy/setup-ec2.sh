#!/usr/bin/env bash
# Prepara uma EC2 Amazon Linux 2023 para rodar o FingerStock.
# Rodar UMA vez, de dentro do repositório clonado:  sudo bash deploy/setup-ec2.sh
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
    echo "Execute com sudo: sudo bash deploy/setup-ec2.sh" >&2
    exit 1
fi

DIR_DEPLOY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ">> Instalando pacotes (Java 21, Git, Nginx, cliente MySQL, cron)"
dnf install -y java-21-amazon-corretto-devel git nginx mariadb105 cronie

echo ">> Criando usuário e diretórios da aplicação"
id fingerstock &>/dev/null || useradd --system --home-dir /opt/fingerstock --shell /sbin/nologin fingerstock
mkdir -p /opt/fingerstock /etc/fingerstock /var/backups/fingerstock
chown fingerstock:fingerstock /opt/fingerstock

if [[ ! -f /etc/fingerstock/app.env ]]; then
    cp "$DIR_DEPLOY/app.env.example" /etc/fingerstock/app.env
    echo ">> Criado /etc/fingerstock/app.env - EDITE com os dados do RDS antes do deploy"
fi
chown root:fingerstock /etc/fingerstock/app.env
chmod 640 /etc/fingerstock/app.env

echo ">> Instalando serviço systemd"
cp "$DIR_DEPLOY/fingerstock.service" /etc/systemd/system/fingerstock.service
systemctl daemon-reload
systemctl enable fingerstock

echo ">> Configurando Nginx como proxy reverso (80 -> 8080)"
cp "$DIR_DEPLOY/nginx-fingerstock.conf" /etc/nginx/conf.d/fingerstock.conf
nginx -t
systemctl enable --now nginx
systemctl reload nginx

echo ">> Agendando backup diário do banco às 03:00"
install -m 750 "$DIR_DEPLOY/backup.sh" /usr/local/bin/fingerstock-backup
echo "0 3 * * * root /usr/local/bin/fingerstock-backup >> /var/log/fingerstock-backup.log 2>&1" > /etc/cron.d/fingerstock-backup
chmod 644 /etc/cron.d/fingerstock-backup
systemctl enable --now crond

echo
echo "Pronto. Próximos passos:"
echo "  1. sudo nano /etc/fingerstock/app.env     (endpoint, usuário e senha do RDS)"
echo "  2. bash deploy/deploy.sh                   (compila e sobe a aplicação)"
