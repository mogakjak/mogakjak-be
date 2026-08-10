import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
const wsUrl = __ENV.WS_URL || 'ws://localhost:8081';
const duration = __ENV.DURATION || '2m';
const connections = Number(__ENV.CONNECTIONS || 20);
const eventInterval = Number(__ENV.EVENT_INTERVAL || 1);
const tokens = (__ENV.ACCESS_TOKENS || __ENV.ACCESS_TOKEN || '')
  .split(',')
  .map((token) => token.trim())
  .filter(Boolean);

const realtimeLatency = new Trend('realtime_e2e_latency', true);
const realtimeMessages = new Counter('realtime_messages_received');

export const options = {
  scenarios: {
    subscribers: {
      executor: 'constant-vus',
      exec: 'subscriber',
      vus: connections,
      duration,
    },
    publisher: {
      executor: 'constant-vus',
      exec: 'publisher',
      vus: 1,
      duration,
      startTime: '5s',
    },
  },
  thresholds: {
    realtime_e2e_latency: ['p(95)<500', 'p(99)<1000'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  if (tokens.length === 0) {
    throw new Error('ACCESS_TOKEN 또는 ACCESS_TOKENS가 필요합니다.');
  }

  for (const token of tokens) {
    http.post(`${baseUrl}/api/lounge/enter`, null, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

export function subscriber() {
  const token = tokens[(__VU - 1) % tokens.length];
  const response = ws.connect(`${wsUrl}/connect-native`, {}, (socket) => {
    socket.on('open', () => {
      socket.send(stompFrame('CONNECT', {
        Authorization: `Bearer ${token}`,
        'accept-version': '1.2',
        'heart-beat': '0,0',
      }));
    });

    socket.on('message', (rawMessage) => {
      for (const frame of String(rawMessage).split('\0')) {
        if (frame.startsWith('CONNECTED')) {
          socket.send(stompFrame('SUBSCRIBE', {
            id: `lounge-${__VU}`,
            destination: '/topic/lounge/presence',
            ack: 'auto',
          }));
          continue;
        }

        if (!frame.startsWith('MESSAGE')) {
          continue;
        }

        const separator = frame.indexOf('\n\n');
        if (separator < 0) {
          continue;
        }

        const payload = JSON.parse(frame.slice(separator + 2));
        realtimeMessages.add(1);
        if (payload.publishedAt) {
          realtimeLatency.add(Date.now() - Date.parse(payload.publishedAt));
        }
      }
    });

    socket.on('error', (error) => {
      console.error(`WebSocket error: ${error.error()}`);
    });

    socket.setTimeout(() => socket.close(), parseDurationMillis(duration));
  });

  check(response, { 'WebSocket upgrade succeeded': (result) => result && result.status === 101 });
}

export function publisher() {
  const response = http.post(`${baseUrl}/api/lounge/enter`, null, {
    headers: { Authorization: `Bearer ${tokens[0]}` },
  });
  check(response, { 'presence event request succeeded': (result) => result.status === 200 });
  sleep(eventInterval);
}

function stompFrame(command, headers, body = '') {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return `${command}\n${headerLines.join('\n')}\n\n${body}\0`;
}

function parseDurationMillis(value) {
  const match = /^(\d+)(ms|s|m|h)$/.exec(value);
  if (!match) {
    throw new Error(`지원하지 않는 DURATION 형식입니다: ${value}`);
  }

  const amount = Number(match[1]);
  const multipliers = { ms: 1, s: 1000, m: 60_000, h: 3_600_000 };
  return amount * multipliers[match[2]];
}
