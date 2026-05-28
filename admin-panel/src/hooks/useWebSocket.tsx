import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '@/auth/AuthContext';
import { WS_BASE } from '@/api/client';

type Handler = (payload: unknown) => void;

interface WsContextValue {
  connected: boolean;
  /** Suscríbete a /topic/local/{localId}/playlists para tu local. Devuelve unsubscribe. */
  subscribePlaylists: (handler: Handler) => () => void;
}

const WsContext = createContext<WsContextValue | null>(null);

/**
 * Provider STOMP/SockJS para el local autenticado.
 * Reconexión automática con backoff. Se conecta sólo si hay sesión.
 */
export function WebSocketProvider({ children }: { children: ReactNode }) {
  const { token, local } = useAuth();
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef<Set<Handler>>(new Set());

  useEffect(() => {
    if (!token || !local) {
      // limpia conexión existente si la había
      clientRef.current?.deactivate();
      clientRef.current = null;
      setConnected(false);
      return;
    }

    const wsUrl = `${WS_BASE}/ws`;
    const client = new Client({
      // SockJS factory — STOMP correrá por encima de SockJS
      webSocketFactory: () => new SockJS(wsUrl) as unknown as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: () => {
        /* silenciar logs en producción */
      },
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(
        `/topic/local/${local.id}/playlists`,
        (msg: IMessage) => {
          let payload: unknown = msg.body;
          try {
            payload = JSON.parse(msg.body);
          } catch {
            /* no era JSON, deja el string */
          }
          handlersRef.current.forEach((h) => {
            try {
              h(payload);
            } catch (err) {
              // eslint-disable-next-line no-console
              console.error('WS handler error', err);
            }
          });
        },
      );
    };

    client.onWebSocketClose = () => setConnected(false);
    client.onDisconnect = () => setConnected(false);
    client.onStompError = () => setConnected(false);

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token, local]);

  const value = useMemo<WsContextValue>(
    () => ({
      connected,
      subscribePlaylists: (handler: Handler) => {
        handlersRef.current.add(handler);
        return () => {
          handlersRef.current.delete(handler);
        };
      },
    }),
    [connected],
  );

  return <WsContext.Provider value={value}>{children}</WsContext.Provider>;
}

/** Hook genérico: estado de conexión + suscripción manual. */
export function useLocalWebSocket(): WsContextValue {
  const ctx = useContext(WsContext);
  if (!ctx) {
    return {
      connected: false,
      subscribePlaylists: () => () => undefined,
    };
  }
  return ctx;
}

/**
 * Atajo: registra un handler que se ejecutará cuando llegue cualquier evento
 * de playlists/items/schedules del local. Útil para refetch automático.
 */
export function usePlaylistChannel(handler: () => void) {
  const ws = useLocalWebSocket();
  useEffect(() => {
    return ws.subscribePlaylists(handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ws.subscribePlaylists]);
}
