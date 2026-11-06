
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   APPLICATION CORE
   File: app.js
   ========================================================= */

let EMPLOYEE_ID = null;
let CURRENT_ROLE = null;
let CURRENT_USERNAME = null;
let CURRENT_EMPLOYEE = null;

let currentAttendanceStatus = "NOT_CHECKED_IN";
let currentLocation = null;

document.addEventListener("DOMContentLoaded", async function () {
    console.log("Initializing Workforce Attendance application...");

    history.replaceState(
        { screen: "home" },
        "",
        window.location.pathname
    );

    updateCurrentDate();
    updateConnectionStatus();

    try {
        await initializeOfflineStorage();
        console.log("Offline storage initialized successfully.");
    } catch (error) {
        console.error("Offline storage initialization failed:", error);
        alert("Unable to initialize offline storage: " + error.message);
    }

    initializeCommonEventListeners();

    console.log("Workforce application initialized.");
});

function initializeCommonEventListeners() {
    document.getElementById("syncBtn")
        ?.addEventListener("click", () => manualSync());

    document.getElementById("getLocationBtn")
        ?.addEventListener("click", async () => {
            try {
                await getEmployeeLocation();
            } catch (error) {
                console.error("Location acquisition failed:", error);
            }
        });

    document.getElementById("checkInBtn")
        ?.addEventListener("click", () => submitAttendanceEvent("CHECK_IN"));

    document.getElementById("checkOutBtn")
        ?.addEventListener("click", () => submitAttendanceEvent("CHECK_OUT"));

    document.getElementById("startBreakBtn")
        ?.addEventListener("click", () => submitAttendanceEvent("BREAK_START"));

    document.getElementById("endBreakBtn")
        ?.addEventListener("click", () => submitAttendanceEvent("BREAK_END"));

    window.addEventListener("online", updateConnectionStatus);
    window.addEventListener("offline", updateConnectionStatus);
}

async function apiRequest(url, options = {}) {
    const requestOptions = {
        ...options,
        credentials: "same-origin",
        headers: {
            ...(options.headers || {})
        }
    };

    if (options.body !== undefined) {
        requestOptions.headers["Content-Type"] = "application/json";
    }

    const response = await fetch(url, requestOptions);

    if (!response.ok) {
        let errorMessage = "Request failed with status " + response.status;

        try {
            const errorData = await response.json();
            errorMessage =
                errorData.message ||
                errorData.error ||
                errorMessage;
        } catch (error) {
            // Use the default HTTP error message.
        }

        throw new Error(errorMessage);
    }

    if (response.status === 204) {
        return null;
    }

    const responseText = await response.text();

    if (!responseText) {
        return null;
    }

    try {
        return JSON.parse(responseText);
    } catch (error) {
        throw new Error("Server returned an invalid JSON response.");
    }
}

function updateCurrentDate() {
    const dateElement = document.getElementById("currentDate");

    if (!dateElement) return;

    dateElement.textContent = new Date().toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "long",
        year: "numeric"
    });
}

function updateConnectionStatus() {
    const statusElement = document.getElementById("connectionStatus");

    if (!statusElement) return;

    if (navigator.onLine) {
        statusElement.textContent = "Online";
        statusElement.className = "connection-status online";
    } else {
        statusElement.textContent = "Offline";
        statusElement.className = "connection-status offline";
    }
}

function resetApplicationState() {
    EMPLOYEE_ID = null;
    CURRENT_ROLE = null;
    CURRENT_USERNAME = null;
    CURRENT_EMPLOYEE = null;

    currentAttendanceStatus = "NOT_CHECKED_IN";
    currentLocation = null;
}

console.log("Application core module loaded.");
