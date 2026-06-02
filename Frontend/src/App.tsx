// src/App.tsx
import { AuthProvider } from "./context/AuthContext";
import { BalanceProvider } from "./components/BalanceBar/BalanceProvider";
import AppRouter from "./routes/AppRouter";
import GroupNotificationsListener from "./components/GroupNotifications/GroupNotificationsListener";

function App() {
  return (
    <AuthProvider>
      <BalanceProvider>
        <GroupNotificationsListener />
        <AppRouter />
      </BalanceProvider>
    </AuthProvider>
  );
}

export default App;
