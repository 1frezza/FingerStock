#!/usr/bin/env bash
# Atualiza o código, compila e reinicia a aplicação na EC2.
# Rodar de dentro do repositório clonado, com o usuário ec2-user:  bash deploy/deploy.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

echo ">> Baixando a última versão do repositório"
git pull --ff-only

echo ">> Compilando"
chmod +x mvnw
./mvnw -q -DskipTests clean package

echo ">> Atualizando a configuração do Nginx"
sudo cp deploy/nginx-fingerstock.conf /etc/nginx/conf.d/fingerstock.conf
sudo nginx -t
sudo systemctl reload nginx

echo ">> Publicando o jar e reiniciando o serviço"
sudo install -o fingerstock -g fingerstock -m 640 target/fingerstock.jar /opt/fingerstock/fingerstock.jar
sudo systemctl restart fingerstock

echo ">> Aguardando a aplicação responder"
for _ in $(seq 1 30); do
    if curl -fs http://127.0.0.1:8080/actuator/health >/dev/null; then
        echo "OK - aplicação no ar: http://$(curl -s --max-time 2 https://checkip.amazonaws.com || echo '<IP público da EC2>')/"
        exit 0
    fi
    sleep 2
done

echo "A aplicação não respondeu. Veja os logs: sudo journalctl -u fingerstock -n 80 --no-pager" >&2
exit 1
