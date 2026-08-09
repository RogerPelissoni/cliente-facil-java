#!/usr/bin/env bash
#
# Publica um volume de mensagens direto no RabbitMQ (via API de management, sem passar pelo HTTP da
# aplicação) pra provar que o pipeline de retry+DLQ (Partes 6-9 de docs/guides/1_messaging-and-websocket.md)
# absorve um pico repentino sem perder mensagem nem travar.
#
# Propositalmente NÃO é benchmark de throughput/latência (p95/p99, msgs/seg sustentado) — não existe
# um número-alvo definido pro negócio ainda (ver "Teste de carga/performance" em
# docs/product/3_roadmap.md), então medir contra nada seria só teatro. O que este script confirma é
# mais concreto: sob N mensagens simultâneas, com uma fração falhando de propósito, TODA mensagem
# termina em exatamente um lugar — persistida com sucesso OU na dead-letter — nenhuma se perde, e a
# fila volta a zero sozinha.
#
# Uso:
#   ./scripts/rabbitmq-burst-test.sh [opções]
#
# Opções (todas com default pra rodar contra o docker-compose local sem argumento nenhum):
#   -n, --total N         Total de mensagens a publicar (default: 500)
#   -f, --failure-rate P  Percentual (0-100) que deve falhar de propósito e cair na DLQ (default: 20)
#   -c, --concurrency N   Publicações HTTP concorrentes (default: 20)
#   -t, --target TARGET   notification | email | both (default: notification)
#   -u, --user-id ID      userId usado nas notificações de teste (default: 1, o admin seedado)
#   -h, --help            Mostra isso aqui
#
# Requer: curl, python3 (só formata a saída), docker (lê contagem de notification_dead_letter direto
# no Postgres do docker-compose — ajuste get_db_count() se rodar fora desse setup).

set -euo pipefail

# Precisa ser EXECUTADO (./scripts/rabbitmq-burst-test.sh), não "source"ado — publica mensagens de
# verdade como efeito colateral assim que chega no fim do arquivo. `source` roda o script inteiro
# com os defaults, sem chance de revisar os argumentos antes (erro real cometido rodando este script
# a primeira vez — ver docs/guides/1_messaging-and-websocket.md, seção de teste de carga).
if (return 0 2>/dev/null); then
  echo "Este script publica mensagens de verdade — rode com ./scripts/rabbitmq-burst-test.sh, não 'source'." >&2
  return 1
fi

RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_MGMT_PORT="${RABBITMQ_MGMT_PORT:-15672}"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"
RABBITMQ_PASS="${RABBITMQ_PASS:-guest}"
MGMT_BASE="http://${RABBITMQ_HOST}:${RABBITMQ_MGMT_PORT}/api"

# 200 (não 500) de propósito: o listener de notificação roda com concorrência 1 por padrão (nenhum
# spring.rabbitmq.listener.simple.concurrency configurado) — cada mensagem que falha trava essa
# única thread pelos ~3s inteiros do backoff de retry (1s + 2s) antes de desistir. Com 20% de falha,
# 200 mensagens já levam ~2min pra drenar; 500 passa de 8min. Ver achado documentado no roadmap.
TOTAL=200
FAILURE_RATE=20
CONCURRENCY=20
TARGET="notification"
USER_ID=1

while [ $# -gt 0 ]; do
  case "$1" in
    -n|--total) TOTAL="$2"; shift 2 ;;
    -f|--failure-rate) FAILURE_RATE="$2"; shift 2 ;;
    -c|--concurrency) CONCURRENCY="$2"; shift 2 ;;
    -t|--target) TARGET="$2"; shift 2 ;;
    -u|--user-id) USER_ID="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^#//'; exit 0 ;;
    *) echo "Opção desconhecida: $1" >&2; exit 1 ;;
  esac
done

if [[ "$TARGET" != "notification" && "$TARGET" != "email" && "$TARGET" != "both" ]]; then
  echo "--target precisa ser notification, email ou both (recebido: $TARGET)" >&2
  exit 1
fi

NOTIFICATION_EXCHANGE="clientefacil.notification.exchange"
NOTIFICATION_ROUTING_KEY="clientefacil.notification"
NOTIFICATION_QUEUE="clientefacil.notification.queue"
NOTIFICATION_DLQ="clientefacil.notification.queue.dlq"
NOTIFICATION_SIMULATE_SENTINEL="__SIMULATE_DLQ_FAILURE__"

EMAIL_EXCHANGE="clientefacil.email.exchange"
EMAIL_ROUTING_KEY="clientefacil.email"
EMAIL_QUEUE="clientefacil.email.queue"
EMAIL_DLQ="clientefacil.email.queue.dlq"

mgmt_get() {
  curl -sf -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" "${MGMT_BASE}/$1"
}

queue_depth() {
  # Nunca reporta 0 numa falha de consulta (curl/API/parse) — um erro transitório vinha sendo
  # disfarçado de "fila vazia" pelo `|| echo 0` original, fazendo o loop de espera concluir "drenou"
  # prematuramente enquanto a fila ainda tinha centenas de mensagens (bug real encontrado rodando
  # este script a primeira vez). Em erro, devolve um número absurdo — pior caso, o loop só espera
  # até o teto e avisa, nunca mente que terminou.
  local response
  if ! response=$(mgmt_get "queues/%2f/$1" 2>/dev/null); then
    echo 999999999
    return
  fi

  echo "$response" | python3 -c "import sys,json; print(json.load(sys.stdin).get('messages', 0))" 2>/dev/null || echo 999999999
}

get_db_count() {
  docker exec db psql -U postgres -d clientefacil -t -c "$1" 2>/dev/null | tr -d '[:space:]'
}

publish_notification() {
  local i="$1" fail="$2"
  local message="Mensagem de teste #${i}"
  [ "$fail" = "1" ] && message="$NOTIFICATION_SIMULATE_SENTINEL"

  local payload
  payload=$(python3 -c "
import json
print(json.dumps({'userId': $USER_ID, 'type': 'INFO', 'title': 'Burst test #$i', 'message': '''$message'''}))
")

  local body
  body=$(python3 -c "
import json
print(json.dumps({
    'properties': {'content_type': 'application/json'},
    'routing_key': '$NOTIFICATION_ROUTING_KEY',
    'payload': '''$payload''',
    'payload_encoding': 'string',
}))
")

  curl -sf -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" \
    -H "Content-Type: application/json" \
    -d "$body" \
    "${MGMT_BASE}/exchanges/%2f/${NOTIFICATION_EXCHANGE}/publish" > /dev/null
}

publish_email() {
  local i="$1" fail="$2"
  local variables='{}'
  [ "$fail" = "1" ] && variables='{"simulateFailure": true}'

  local payload
  payload=$(python3 -c "
import json
print(json.dumps({
    'companyId': None,
    'to': ['burst-test-$i@example.com'],
    'subject': 'Burst test #$i',
    'template': 'test-email',
    'variables': $variables,
}))
")

  local body
  body=$(python3 -c "
import json
print(json.dumps({
    'properties': {'content_type': 'application/json'},
    'routing_key': '$EMAIL_ROUTING_KEY',
    'payload': '''$payload''',
    'payload_encoding': 'string',
}))
")

  curl -sf -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" \
    -H "Content-Type: application/json" \
    -d "$body" \
    "${MGMT_BASE}/exchanges/%2f/${EMAIL_EXCHANGE}/publish" > /dev/null
}

export -f publish_notification publish_email
export RABBITMQ_USER RABBITMQ_PASS MGMT_BASE NOTIFICATION_EXCHANGE NOTIFICATION_ROUTING_KEY
export EMAIL_EXCHANGE EMAIL_ROUTING_KEY NOTIFICATION_SIMULATE_SENTINEL USER_ID

echo "=== Burst test: $TOTAL mensagens, ${FAILURE_RATE}% propositalmente falhando, concorrência $CONCURRENCY, alvo: $TARGET ==="
echo

echo "--- snapshot ANTES ---"
if [[ "$TARGET" == "notification" || "$TARGET" == "both" ]]; then
  BEFORE_NOTIFICATIONS=$(get_db_count "SELECT count(*) FROM notification;")
  BEFORE_DLQ_NOTIF=$(get_db_count "SELECT count(*) FROM notification_dead_letter WHERE tp_origin = 'NOTIFICATION';")
  echo "notification: $BEFORE_NOTIFICATIONS linhas | notification_dead_letter (NOTIFICATION): $BEFORE_DLQ_NOTIF linhas | fila: $(queue_depth "$NOTIFICATION_QUEUE") | DLQ: $(queue_depth "$NOTIFICATION_DLQ")"
fi
if [[ "$TARGET" == "email" || "$TARGET" == "both" ]]; then
  BEFORE_DLQ_EMAIL=$(get_db_count "SELECT count(*) FROM notification_dead_letter WHERE tp_origin = 'EMAIL';")
  echo "notification_dead_letter (EMAIL): $BEFORE_DLQ_EMAIL linhas | fila: $(queue_depth "$EMAIL_QUEUE") | DLQ: $(queue_depth "$EMAIL_DLQ")"
fi
echo

echo "--- publicando $TOTAL mensagens ---"
START=$(date +%s)

accumulator=0
seq 1 "$TOTAL" | while read -r i; do
  fail=0
  # Distribui as falhas uniformemente ao longo do lote (tipo Bresenham), em vez de um bloco só de
  # falha seguido de um bloco só de sucesso — mais parecido com falha real intercalada em tráfego
  # normal, e correto pra qualquer TOTAL (uma fórmula de módulo simples degenera quando TOTAL é
  # pequeno ou múltiplo de 100).
  accumulator=$(( accumulator + FAILURE_RATE ))
  if [ "$accumulator" -ge 100 ]; then
    fail=1
    accumulator=$(( accumulator - 100 ))
  fi

  if [[ "$TARGET" == "notification" || "$TARGET" == "both" ]]; then
    echo "notification $i $fail"
  fi
  if [[ "$TARGET" == "email" || "$TARGET" == "both" ]]; then
    echo "email $i $fail"
  fi
done | xargs -P "$CONCURRENCY" -L 1 bash -c '
  case "$0" in
    notification) publish_notification "$1" "$2" ;;
    email) publish_email "$1" "$2" ;;
  esac
'

ELAPSED=$(( $(date +%s) - START ))
echo "Publicação concluída em ${ELAPSED}s."
echo

EXPECTED_FAILURES=$(( TOTAL * FAILURE_RATE / 100 ))
ETA=$(( EXPECTED_FAILURES * 3 ))
echo "--- aguardando a fila drenar ---"
echo "(estimativa: ~${EXPECTED_FAILURES} mensagem(ns) devem falhar, cada uma trava o consumer por ~3s de"
echo " retry backoff — sem concorrência configurada no listener, isso é ~${ETA}s só de backoff, fora o"
echo " tempo de processar as que têm sucesso. Progresso a cada 5s abaixo.)"
echo
# A API de publish do management do RabbitMQ NÃO é síncrona com a fila de verdade — {"routed":true}
# volta na hora, mas a mensagem pode demorar visivelmente mais que isso pra aparecer na contagem da
# fila (confirmado na prática: uma leitura isolada de "0" logo após publicar bateu com a fila ainda
# tendo dezenas de mensagens que só chegaram depois). Por isso: espera inicial maior, e só declara
# "drenou" depois de 3 leituras SEGUIDAS em zero (não uma só) — uma leitura isolada não distingue
# "realmente vazia" de "as mensagens ainda não chegaram".
sleep 5
MAX_CHECKS=240 # 240 * 5s = 20min de teto — generoso o bastante mesmo pra lotes grandes com concorrência 1.
CONSECUTIVE_ZERO_NEEDED=3
consecutive_zero=0
for check in $(seq 1 "$MAX_CHECKS"); do
  depth=0
  [[ "$TARGET" == "notification" || "$TARGET" == "both" ]] && depth=$((depth + $(queue_depth "$NOTIFICATION_QUEUE")))
  [[ "$TARGET" == "email" || "$TARGET" == "both" ]] && depth=$((depth + $(queue_depth "$EMAIL_QUEUE")))

  if [ "$depth" -eq 0 ]; then
    consecutive_zero=$((consecutive_zero + 1))
    [ "$consecutive_zero" -ge "$CONSECUTIVE_ZERO_NEEDED" ] && { echo "fila drenada e estável em 0 (check $check)"; break; }
  else
    consecutive_zero=0
  fi

  [ $(( check % 4 )) -eq 0 ] && echo "  ainda drenando: $depth mensagem(ns) na fila ($(( check * 5 ))s decorridos)"
  sleep 5
  if [ "$check" -eq "$MAX_CHECKS" ]; then
    echo "⚠️  não drenou dentro de $(( MAX_CHECKS * 5 / 60 ))min — rode o snapshot manualmente mais tarde."
  fi
done
echo

echo "--- snapshot DEPOIS ---"
if [[ "$TARGET" == "notification" || "$TARGET" == "both" ]]; then
  AFTER_NOTIFICATIONS=$(get_db_count "SELECT count(*) FROM notification;")
  AFTER_DLQ_NOTIF=$(get_db_count "SELECT count(*) FROM notification_dead_letter WHERE tp_origin = 'NOTIFICATION';")
  DELTA_OK=$(( AFTER_NOTIFICATIONS - BEFORE_NOTIFICATIONS ))
  DELTA_DLQ=$(( AFTER_DLQ_NOTIF - BEFORE_DLQ_NOTIF ))
  echo "notification: $AFTER_NOTIFICATIONS linhas (+${DELTA_OK}) | notification_dead_letter (NOTIFICATION): $AFTER_DLQ_NOTIF linhas (+${DELTA_DLQ}) | fila: $(queue_depth "$NOTIFICATION_QUEUE") | DLQ: $(queue_depth "$NOTIFICATION_DLQ")"
  echo "esperado: +$TOTAL linhas em notification (toda mensagem gera uma — sucesso de verdade OU o alerta"
  echo "  \"Falha no processamento de notificação\" que NotificationDeadLetterListener cria por falha, ver"
  echo "  docs/guides/1_messaging-and-websocket.md Parte 7), +$((TOTAL * FAILURE_RATE / 100)) em notification_dead_letter (aprox.)"
  # DELTA_DLQ NÃO soma com DELTA_OK aqui: notification já inclui uma linha por falha (o alerta), então
  # o dead-letter é um segundo registro do MESMO evento (auditoria), não um evento adicional a somar.
  if [ "$DELTA_OK" -eq "$TOTAL" ]; then
    echo "✅ todas as $TOTAL mensagens geraram exatamente uma linha em notification (sucesso ou alerta de falha) — nenhuma se perdeu"
  else
    echo "⚠️  $DELTA_OK linhas em notification pra $TOTAL mensagens publicadas — sobrou mensagem em algum lugar (fila ainda drenando? rodar de novo o snapshot em alguns segundos)"
  fi
fi
if [[ "$TARGET" == "email" || "$TARGET" == "both" ]]; then
  AFTER_DLQ_EMAIL=$(get_db_count "SELECT count(*) FROM notification_dead_letter WHERE tp_origin = 'EMAIL';")
  echo "notification_dead_letter (EMAIL): $AFTER_DLQ_EMAIL linhas (+$((AFTER_DLQ_EMAIL - BEFORE_DLQ_EMAIL))) | fila: $(queue_depth "$EMAIL_QUEUE") | DLQ: $(queue_depth "$EMAIL_DLQ")"
  echo "e-mails de sucesso foram parar no MailHog: http://localhost:8025"
fi
