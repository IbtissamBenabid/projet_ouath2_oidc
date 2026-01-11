import ReactDOM from "react-dom/client";
import App from "./App";
import keycloak from "./keycloak";

keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false,
}).then((authenticated) => {

    if (!authenticated) {
        window.location.reload();
    }

    const root = ReactDOM.createRoot(document.getElementById("root"));
    root.render(<App keycloak={keycloak} />);
});
