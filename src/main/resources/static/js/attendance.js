
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   ATTENDANCE MODULE
   File: attendance.js
   ========================================================= */


/* =========================================================
   1. GENERATE IDEMPOTENCY KEY
   ========================================================= */

function generateIdempotencyKey() {

    if (
        typeof crypto !== "undefined" &&
        typeof crypto.randomUUID === "function"
    ) {
        return crypto.randomUUID();
    }

    if (
        typeof crypto !== "undefined" &&
        typeof crypto.getRandomValues === "function"
    ) {
        const bytes = new Uint8Array(16);

        crypto.getRandomValues(bytes);

        return Array.from(bytes)
            .map(byte => byte.toString(16).padStart(2, "0"))
            .join("");
    }

    return (
        Date.now().toString(36) +
        Math.random().toString(36).substring(2)
    );
}


/* =========================================================
   2. GET EMPLOYEE GPS LOCATION
   ========================================================= */

function getEmployeeLocation() {

    const locationStatus =
        document.getElementById("locationStatus");

    const locationButton =
        document.getElementById("getLocationBtn");

    if (!locationStatus) {
        return Promise.reject(
            new Error("Location status element was not found.")
        );
    }

    if (!navigator.geolocation) {

        const message =
            "Geolocation is not supported by this browser.";

        locationStatus.textContent = message;

        return Promise.reject(new Error(message));
    }

    // Show immediate feedback.
    locationStatus.textContent =
        "Getting your current location...";

    if (locationButton) {
        locationButton.disabled = true;
        locationButton.textContent = "Getting Location...";
    }

    return new Promise((resolve, reject) => {

        navigator.geolocation.getCurrentPosition(

            // GPS SUCCESS
            position => {

                currentLocation = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    locationAccuracyMeters: position.coords.accuracy
                };

                locationStatus.textContent =
                    "Location obtained successfully. Accuracy: " +
                    Math.round(position.coords.accuracy) +
                    " metres.";

                if (locationButton) {
                    locationButton.disabled = false;
                    locationButton.textContent = "Get My Location";
                }

                resolve(currentLocation);
            },

            // GPS ERROR
            error => {

                currentLocation = null;

                let message =
                    "Unable to retrieve your location.";

                switch (error.code) {

                    case error.PERMISSION_DENIED:
                        message =
                            "Location permission denied. Please allow location access.";
                        break;

                    case error.POSITION_UNAVAILABLE:
                        message =
                            "Your current location is unavailable. Check your device location services.";
                        break;

                    case error.TIMEOUT:
                        message =
                            "Location request timed out. Please try again.";
                        break;
                }

                locationStatus.textContent = message;

                console.error("GPS acquisition error:", error);

                if (locationButton) {
                    locationButton.disabled = false;
                    locationButton.textContent = "Get My Location";
                }

                reject(new Error(message));
            },

            // GPS OPTIONS
            {
                enableHighAccuracy: true,
                timeout: 30000,
                maximumAge: 0
            }
        );

    });
}


/* =========================================================
   3. HANDLE ATTENDANCE EVENT
   ========================================================= */

async function submitAttendanceEvent(eventType) {

    if (CURRENT_ROLE !== "EMPLOYEE" || !EMPLOYEE_ID) {

        alert(
            "Please log in as an employee to mark attendance."
        );

        return;
    }

    const allowedEvents = [
        "CHECK_IN",
        "CHECK_OUT",
        "BREAK_START",
        "BREAK_END"
    ];

    if (!allowedEvents.includes(eventType)) {

        alert("Invalid attendance event.");

        return;
    }

    const buttons = [
        "checkInBtn",
        "checkOutBtn",
        "startBreakBtn",
        "endBreakBtn"
    ];

    buttons.forEach(id => {

        const button = document.getElementById(id);

        if (button) {
            button.disabled = true;
        }

    });

    try {

        // Obtain a fresh GPS location for every event.
        const location = await getEmployeeLocation();

        const clientReportedAt =
            new Date().toISOString();

        const payload = {

            employeeId: Number(EMPLOYEE_ID),

            eventType: eventType,

            clientReportedAt: clientReportedAt,

            latitude: location.latitude,

            longitude: location.longitude,

            locationAccuracyMeters:
                location.locationAccuracyMeters
        };

        const localRequest = {

            idempotencyKey: generateIdempotencyKey(),

            payload: payload,

            status: "PENDING",

            createdAt: clientReportedAt,

            lastAttemptAt: null,

            serverReceivedAt: null,

            eventId: null,

            message: "Waiting for synchronization",

            lastError: null
        };

        // Save locally before attempting network synchronization.
        await saveAttendanceRequest(localRequest);

        await refreshAttendanceDashboard();

        if (!navigator.onLine) {

            alert(
                "You are offline. Your attendance request has been saved and will synchronize when the connection is restored."
            );

            return;
        }

        // Synchronize the saved request with the backend.
        await synchronizePendingRequests();

        await refreshAttendanceDashboard();

        const savedRequest =
            await getAttendanceRequest(
                localRequest.idempotencyKey
            );

        if (savedRequest) {

            alert(
                savedRequest.message ||
                "Attendance request processed."
            );
        }

    } catch (error) {

        console.error(
            "Attendance submission failed:",
            error
        );

        alert(error.message || "Unable to submit attendance.");

    } finally {

        updateAttendanceButtons();
    }

}


/* =========================================================
   4. REFRESH ATTENDANCE DASHBOARD
   ========================================================= */

async function refreshAttendanceDashboard() {

    if (!isOfflineStorageAvailable()) {

        console.warn("Offline storage is unavailable.");

        return;
    }

    try {

        const requests = await getAllAttendanceRequests();

        const employeeRequests = requests

            .filter(request => {

                return (
                    request.payload &&
                    Number(request.payload.employeeId) ===
                    Number(EMPLOYEE_ID)
                );

            })

            .sort((a, b) => {

                return (
                    new Date(b.createdAt) -
                    new Date(a.createdAt)
                );

            });

        updateAttendanceCounts(employeeRequests);

        renderAttendanceRequests(employeeRequests);

        updateAttendanceStatusFromRequests(employeeRequests);

        updateAttendanceButtons();

    } catch (error) {

        console.error(
            "Unable to refresh attendance dashboard:",
            error
        );

        const list = document.getElementById("requestList");

        if (list) {

            list.textContent =
                "Unable to load attendance history.";
        }
    }

}


/* =========================================================
   5. UPDATE ATTENDANCE COUNTS
   ========================================================= */

function updateAttendanceCounts(requests) {

    const counts = {

        PENDING: 0,

        ACCEPTED: 0,

        REJECTED: 0,

        REQUIRES_REVIEW: 0
    };

    requests.forEach(request => {

        if (Object.prototype.hasOwnProperty.call(
            counts,
            request.status
        )) {

            counts[request.status]++;
        }
    });

    setAttendanceCount("pendingCount", counts.PENDING);

    setAttendanceCount("acceptedCount", counts.ACCEPTED);

    setAttendanceCount("rejectedCount", counts.REJECTED);

    setAttendanceCount("reviewCount", counts.REQUIRES_REVIEW);

}


/* =========================================================
   6. UPDATE INDIVIDUAL COUNT ELEMENT
   ========================================================= */

function setAttendanceCount(elementId, value) {

    const element = document.getElementById(elementId);

    if (element) {
        element.textContent = value;
    }

}


/* =========================================================
   7. RENDER ATTENDANCE REQUEST HISTORY
   ========================================================= */

function renderAttendanceRequests(requests) {

    const requestList =
        document.getElementById("requestList");

    if (!requestList) {
        return;
    }

    requestList.replaceChildren();

    if (!requests || requests.length === 0) {

        const emptyMessage = document.createElement("p");

        emptyMessage.textContent =
            "No attendance requests found.";

        requestList.appendChild(emptyMessage);

        return;
    }

    requests.forEach(request => {

        const card = document.createElement("div");

        card.className = "card attendance-request";

        const eventHeading = document.createElement("h4");

        eventHeading.textContent =
            formatAttendanceEvent(
                request.payload.eventType
            );

        const status = document.createElement("p");

        status.textContent =
            "Status: " + request.status;

        const date = document.createElement("p");

        date.textContent =
            "Requested at: " +
            formatAttendanceDate(request.createdAt);

        const message = document.createElement("p");

        message.textContent =
            "Message: " +
            (
                request.message ||
                request.lastError ||
                "No additional information"
            );

        card.appendChild(eventHeading);

        card.appendChild(status);

        card.appendChild(date);

        card.appendChild(message);

        if (
            request.eventId !== null &&
            request.eventId !== undefined
        ) {

            const eventId = document.createElement("p");

            eventId.textContent =
                "Server Event ID: " + request.eventId;

            card.appendChild(eventId);
        }

        requestList.appendChild(card);

    });

}


/* =========================================================
   8. FORMAT ATTENDANCE EVENT
   ========================================================= */

function formatAttendanceEvent(eventType) {

    const labels = {

        CHECK_IN: "Check In",

        CHECK_OUT: "Check Out",

        BREAK_START: "Break Start",

        BREAK_END: "Break End"
    };

    return labels[eventType] || eventType || "Attendance Event";

}


/* =========================================================
   9. FORMAT DATE
   ========================================================= */

function formatAttendanceDate(dateValue) {

    if (!dateValue) {
        return "Not available";
    }

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
        return "Invalid date";
    }

    return date.toLocaleString();

}


/* =========================================================
   10. DERIVE ATTENDANCE STATUS
   ========================================================= */

function updateAttendanceStatusFromRequests(requests) {

    const acceptedRequests = requests

        .filter(request => {

            return request.status === "ACCEPTED";

        })

        .sort((a, b) => {

            return (
                new Date(a.createdAt) -
                new Date(b.createdAt)
            );

        });

    let status = "NOT_CHECKED_IN";

    acceptedRequests.forEach(request => {

        switch (request.payload.eventType) {

            case "CHECK_IN":
                status = "CHECKED_IN";
                break;

            case "CHECK_OUT":
                status = "CHECKED_OUT";
                break;

            case "BREAK_START":
                status = "ON_BREAK";
                break;

            case "BREAK_END":
                status = "CHECKED_IN";
                break;

        }

    });

    currentAttendanceStatus = status;

    updateAttendanceStatusDisplay(status);

}


/* =========================================================
   11. DISPLAY CURRENT ATTENDANCE STATUS
   ========================================================= */

function updateAttendanceStatusDisplay(status) {

    const statusElement =
        document.getElementById("attendanceStatus");

    if (!statusElement) {
        return;
    }

    const labels = {

        NOT_CHECKED_IN: "Not Checked In",

        CHECKED_IN: "Checked In",

        CHECKED_OUT: "Checked Out",

        ON_BREAK: "On Break"
    };

    statusElement.textContent =
        labels[status] || status;

}


/* =========================================================
   12. UPDATE ATTENDANCE BUTTONS
   ========================================================= */

function updateAttendanceButtons() {

    const isEmployee =
        CURRENT_ROLE === "EMPLOYEE" &&
        EMPLOYEE_ID !== null;

    const buttonRules = {

        checkInBtn:
            isEmployee &&
            (
                currentAttendanceStatus === "NOT_CHECKED_IN" ||
                currentAttendanceStatus === "CHECKED_OUT"
            ),

        checkOutBtn:
            isEmployee &&
            currentAttendanceStatus === "CHECKED_IN",

        startBreakBtn:
            isEmployee &&
            currentAttendanceStatus === "CHECKED_IN",

        endBreakBtn:
            isEmployee &&
            currentAttendanceStatus === "ON_BREAK"
    };

    Object.entries(buttonRules).forEach(([id, enabled]) => {

        const button = document.getElementById(id);

        if (button) {
            button.disabled = !enabled;
        }

    });

}


/* =========================================================
   13. INITIALIZE ATTENDANCE MODULE
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {

    updateAttendanceButtons();

    if (CURRENT_ROLE === "EMPLOYEE" && EMPLOYEE_ID) {

        refreshAttendanceDashboard().catch(error => {

            console.error(
                "Attendance initialization failed:",
                error
            );

        });

    }

});


/* =========================================================
   14. MODULE INITIALIZATION
   ========================================================= */

console.log("Attendance module loaded.");
