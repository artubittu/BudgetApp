import { useEffect, useRef } from "react";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";
import { useBalance } from "../BalanceBar/useBalance";

interface GroupExpenseAddedNotification {
  type: "GROUP_EXPENSE_ADDED";
  groupId: number;
  groupName: string;
  title: string;
  amount: number;
  userShare: number;
  createdByEmail: string;
  message: string;
}

const WS_URL = "ws://localhost:8080/ws/group-notifications";

const isGroupExpenseAddedNotification = (
  value: unknown
): value is GroupExpenseAddedNotification => {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const notification = value as Partial<GroupExpenseAddedNotification>;

  return (
    notification.type === "GROUP_EXPENSE_ADDED" &&
    typeof notification.message === "string"
  );
};

const GroupNotificationsListener = () => {
  const { isAuthenticated } = useAuth();
  const { refreshBalance } = useBalance();
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    const closeSocket = () => {
      if (reconnectTimeoutRef.current !== null) {
        window.clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }

      if (socketRef.current) {
        socketRef.current.close();
        socketRef.current = null;
      }
    };

    if (!isAuthenticated) {
      closeSocket();
      return closeSocket;
    }

    let shouldReconnect = true;

    const connect = () => {
      const token = localStorage.getItem("accessToken");

      if (!token) {
        return;
      }

      const socket = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`);
      socketRef.current = socket;

      socket.onmessage = (event: MessageEvent<string>) => {
        try {
          const parsedNotification: unknown = JSON.parse(event.data);

          if (isGroupExpenseAddedNotification(parsedNotification)) {
            toast.info(parsedNotification.message);
            refreshBalance(null);
          }
        } catch (error) {
          console.error("Nie udało się odczytać powiadomienia grupowego:", error);
        }
      };

      socket.onerror = () => {
        console.error("Błąd połączenia WebSocket powiadomień grupowych.");
      };

      socket.onclose = () => {
        socketRef.current = null;

        if (shouldReconnect) {
          reconnectTimeoutRef.current = window.setTimeout(connect, 3000);
        }
      };
    };

    connect();

    return () => {
      shouldReconnect = false;
      closeSocket();
    };
  }, [isAuthenticated, refreshBalance]);

  return null;
};

export default GroupNotificationsListener;
