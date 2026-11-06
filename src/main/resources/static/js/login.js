
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   AUTHENTICATION MODULE
   File: login.js

   Handles:
   - Username/password authentication
   - Role verification
   - Employee and Manager profile loading from login response
   - Session-based authentication
   ========================================================= */

document.addEventListener("DOMContentLoaded", function () {

    const loginButton = document.getElementById("loginBtn");
    const passwordInput = document.getElementById("loginPassword");
    const usernameInput = document.getElementById("loginUsername");

    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        loginForm.addEventListener("submit", event => { event.preventDefault(); handleLogin(); });
    } else if (loginButton) {
        loginButton.addEventListener("click", handleLogin);
    }

    [usernameInput, passwordInput].forEach(input => {

        input?.addEventListener("keydown", event => {

            if (event.key === "Enter") {
                handleLogin();
            }

        });

    });

    console.log("Login module initialized.");
});


/* =========================================================
   LOGIN
   ========================================================= */

async function handleLogin() {

    const usernameInput =
        document.getElementById("loginUsername");

    const passwordInput =
        document.getElementById("loginPassword");

    const loginButton =
        document.getElementById("loginBtn");

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username || !password) {

        showLoginMessage(
            "Please enter both username and password.",
            true
        );

        return;
    }

    if (!CURRENT_ROLE) {

        showLoginMessage(
            "Please select a portal first.",
            true
        );

        return;
    }

    loginButton.disabled = true;

    showLoginMessage("Authenticating...", false);

    try {

        const result = await apiRequest(
            "/api/auth/login",
            {
                method: "POST",
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            }
        );

        if (!result || !result.role || !result.username) {

            throw new Error(
                "Invalid login response from server."
            );
        }

        const authenticatedRole =
            result.role.toUpperCase();

        // Verify selected portal against authenticated role.
        if (authenticatedRole !== CURRENT_ROLE) {

            await apiRequest(
                "/api/auth/logout",
                {
                    method: "POST"
                }
            ).catch(() => null);

            throw new Error(
                "This account does not have access to the selected portal."
            );
        }

        // Store authenticated user details.
        CURRENT_USERNAME = result.username;
        CURRENT_ROLE = authenticatedRole;

        // Populate linked employee profile.
        if (
            authenticatedRole === "EMPLOYEE" ||
            authenticatedRole === "MANAGER"
        ) {

            if (
                result.employeeId === null ||
                result.employeeId === undefined
            ) {

                throw new Error(
                    "No employee profile is linked to this login account. Contact the administrator."
                );
            }

            if (!result.employeeName) {

                throw new Error(
                    "Employee profile information is incomplete."
                );
            }

            EMPLOYEE_ID = Number(result.employeeId);

            CURRENT_EMPLOYEE = {
                id: EMPLOYEE_ID,
                name: result.employeeName,
                username: result.username,
                role: authenticatedRole
            };

        } else {

            // Admin accounts do not require an employee profile.
            EMPLOYEE_ID = null;
            CURRENT_EMPLOYEE = null;

        }

        console.log("Login successful:", {
            username: CURRENT_USERNAME,
            role: CURRENT_ROLE,
            employeeId: EMPLOYEE_ID
        });

        showLoginMessage("", false);

        // Open the appropriate portal.
        await openPortal();

    } catch (error) {

        console.error("Login failed:", error);

        // Clear profile information after failed login.
        EMPLOYEE_ID = null;
        CURRENT_EMPLOYEE = null;
        CURRENT_USERNAME = null;

        showLoginMessage(
            error.message || "Login failed. Please try again.",
            true
        );

    } finally {

        loginButton.disabled = false;

    }
}


/* =========================================================
   LOGIN MESSAGE
   ========================================================= */

function showLoginMessage(message, isError) {

    const element =
        document.getElementById("loginMessage");

    if (!element) return;

    element.textContent = message;

    element.className = isError
        ? "error-message"
        : "";

}
