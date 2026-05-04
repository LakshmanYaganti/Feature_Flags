import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket(onFlagUpdate) {
  const clientRef = useRef(null)

  const connect = useCallback(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/flags', (msg) => {
          try { onFlagUpdate(JSON.parse(msg.body)) } catch (e) { console.error(e) }
        })
      },
    })
    client.activate()
    clientRef.current = client
  }, [onFlagUpdate])

  useEffect(() => {
    connect()
    return () => clientRef.current?.deactivate()
  }, [connect])
}
