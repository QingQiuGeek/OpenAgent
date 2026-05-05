import { createRoot } from "react-dom/client";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import App from "./App.tsx";
import { ThemeProvider } from "./contexts/ThemeContext.tsx";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <ConfigProvider locale={zhCN}>
    <ThemeProvider>
      <App />
    </ThemeProvider>
  </ConfigProvider>
);