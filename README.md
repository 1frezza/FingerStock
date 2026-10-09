# 🛹 FingerStock

CRUD de estoque de uma loja de skates de dedo (fingerboards) e acessórios.
Trabalho 1 — Infraestrutura Cloud AWS.

| Requisito do trabalho | Como é atendido |
|---|---|
| Aplicação CRUD | Spring Boot 3 + Thymeleaf + Spring Data JPA (cadastro, listagem/filtro, edição e exclusão de produtos) |
| VPS (EC2) com a stack configurada | Amazon Linux 2023 + Java 21 (Corretto) + Nginx + systemd — `deploy/setup-ec2.sh` |
| Repositório de código | Git/GitHub — a EC2 faz `git clone` / `git pull` |
| Copiar os dados e servir a aplicação | `deploy/deploy.sh` compila o jar na EC2 e reinicia o serviço; Nginx publica na porta 80 |
| Banco de dados em AWS RDS | MySQL no RDS, configurado por variáveis de ambiente (`/etc/fingerstock/app.env`) |
| Backup | Backups automáticos do RDS (snapshots) + dump diário via cron (`deploy/backup.sh`), opcionalmente enviado ao S3 |

## Arquitetura

```
Internet ──80──> [ EC2: Nginx ──> Spring Boot :8080 ] ──3306──> [ RDS MySQL ]
                   (subnet pública, sg-app)                      (privado, sg-db: só aceita sg-app)
                          │
                          └── cron 03:00 → mysqldump → /var/backups/fingerstock (→ S3 opcional)
```

## Rodando localmente

Requer JDK 17 ou superior. Sem perfil ativo, a aplicação usa um banco H2 em memória e já sobe com produtos de exemplo.

```bash
./mvnw spring-boot:run
```

Acesse http://localhost:8080.

## Estrutura

```
src/main/java/br/com/fingerstock/
  model/        Produto, Categoria
  repository/   ProdutoRepository
  controller/   ProdutoController
  config/       DadosIniciais (produtos de exemplo se a tabela estiver vazia)
src/main/resources/
  application.properties        padrão (H2, local)
  application-prod.properties   perfil prod (MySQL no RDS)
  templates/ static/            telas
deploy/
  setup-ec2.sh             prepara a EC2 (pacotes, usuário, systemd, nginx, cron)
  deploy.sh                git pull + build + restart
  backup.sh                dump do banco (+ S3 opcional)
  fingerstock.service      unidade systemd
  nginx-fingerstock.conf   proxy reverso 80 -> 8080
  app.env.example          modelo das variáveis de ambiente
```

---

# Passo a passo na AWS

Use a mesma região para tudo (ex.: `us-east-1`).

## 1. Subir o código para o GitHub

Crie um repositório vazio no GitHub e, na pasta do projeto:

```bash
git init -b main
git add .
git commit -m "FingerStock: CRUD de estoque"
git remote add origin https://github.com/1frezza/FingerStock.git
git push -u origin main
```

## 2. Security Groups (EC2 > Security Groups)

**`fingerstock-app-sg`** (para a EC2) — regras de entrada:

| Tipo | Porta | Origem |
|---|---|---|
| SSH | 22 | Meu IP |
| HTTP | 80 | 0.0.0.0/0 |

**`fingerstock-db-sg`** (para o RDS) — regra de entrada:

| Tipo | Porta | Origem |
|---|---|---|
| MySQL/Aurora | 3306 | o security group `fingerstock-app-sg` |

Assim o banco só aceita conexões vindas da EC2.

## 3. Criar o banco no RDS (RDS > Create database)

- **Standard create** → **MySQL** → template **Free tier**
- DB instance identifier: `fingerstock-db`
- Master username: `admin` — defina e anote a senha
- Instance class: `db.t3.micro` (ou `db.t4g.micro`), 20 GB gp3
- Connectivity: mesma VPC da EC2, **Public access: No**, security group `fingerstock-db-sg`
- Additional configuration:
  - **Initial database name: `fingerstock`**
  - **Backup: Enable automated backups**, retention **7 dias** (escolha uma janela de backup)

Depois de criado, copie o **Endpoint** (aba *Connectivity & security*).

## 4. Criar a EC2 (EC2 > Launch instance)

- Nome: `fingerstock-app`
- AMI: **Amazon Linux 2023**
- Tipo: `t3.micro` (ou `t2.micro`, free tier)
- Key pair: crie/baixe um `.pem`
- Network: **Auto-assign public IP: Enable**, security group `fingerstock-app-sg`
- Storage: 8–10 GB gp3

Conecte por SSH:

```bash
ssh -i sua-chave.pem ec2-user@IP_PUBLICO_DA_EC2
```

## 5. Configurar a stack na EC2

```bash
sudo dnf install -y git
git clone https://github.com/1frezza/FingerStock.git
cd FingerStock
sudo bash deploy/setup-ec2.sh
```

O script instala Java 21, Nginx, cliente MySQL e cron; cria o usuário de serviço `fingerstock`; registra o serviço systemd; configura o Nginx; e agenda o backup diário.

Preencha os dados do RDS:

```bash
sudo nano /etc/fingerstock/app.env
```

```
DB_HOST=<endpoint do RDS>
DB_USER=admin
DB_PASSWORD=<senha definida no passo 3>
```

(Opcional) teste a conexão EC2 → RDS:

```bash
mysql -h <endpoint do RDS> -u admin -p -e "show databases;"
```

## 6. Deploy

```bash
bash deploy/deploy.sh
```

Acesse `http://IP_PUBLICO_DA_EC2/`. As tabelas são criadas automaticamente no RDS na primeira execução.

Para publicar uma alteração: `git push` na sua máquina e `bash deploy/deploy.sh` na EC2.

Comandos úteis:

```bash
sudo systemctl status fingerstock
sudo journalctl -u fingerstock -f
curl http://127.0.0.1:8080/actuator/health
```

## 7. Backup

**a) Backup automático do RDS** (já habilitado no passo 3): snapshot diário + logs de transação, com retenção de 7 dias e restauração *point-in-time*. Para mostrar na apresentação: RDS > Databases > `fingerstock-db` > aba **Maintenance & backups**. Um snapshot manual pode ser tirado em **Actions > Take snapshot**.

**b) Dump lógico diário na EC2** (instalado pelo `setup-ec2.sh`): todo dia às 03:00 o cron roda `mysqldump` e guarda os últimos 7 dias em `/var/backups/fingerstock`. Para rodar na hora:

```bash
sudo /usr/local/bin/fingerstock-backup
sudo ls -lh /var/backups/fingerstock
```

**c) (Opcional) Enviar o dump para o S3:** crie um bucket, crie uma IAM Role para EC2 com permissão `s3:PutObject` nesse bucket, anexe-a à instância (EC2 > Actions > Security > Modify IAM role) e preencha `BACKUP_S3_BUCKET=nome-do-bucket` em `/etc/fingerstock/app.env`.

Restaurar um dump:

```bash
gunzip -c /var/backups/fingerstock/ARQUIVO.sql.gz | mysql -h <endpoint do RDS> -u admin -p fingerstock
```

## Ao terminar a apresentação

Para não gerar custos: termine a instância EC2, delete a instância RDS (e snapshots) e esvazie/remova o bucket S3, se criado.
