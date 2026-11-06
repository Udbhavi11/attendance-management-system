
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   OFFLINE SYNCHRONIZATION MODULE
   ========================================================= */

const OFFLINE_ATTENDANCE_API = "/api/offline-attendance/sync";

let syncInProgress = false;
let syncInitializationComplete = false;

/* =========================================================
   1. INITIALIZE SYNCHRONIZATION
   ========================================================= */

function initializeSynchronization() {

    syncInitializationComplete = true;

    console.log("Synchronization initialization completed.");

    if (navigator.onLine) {
        synchronizePendingRequests();
    }
}

/* =========================================================
   2. CHECK OFFLINE STORAGE
   ========================================================= */

function isSynchronizationStorageReady() {

    if (typeof getPendingAttendanceRequests !== "function") {
        console.error(
            "Offline storage is not loaded: " +
            "getPendingAttendanceRequests is missing."
        );

        return false;
    }

    // The application initializes IndexedDB before
    // employees can submit attendance requests.

    if (typeof isOfflineStorageAvailable === "function") {

        return isOfflineStorageAvailable();

    }

    // Backward compatibility if the availability
    // function is not present in offline-storage.js.

    return true;
}

/* =========================================================
   3. SUBMIT ONE ATTENDANCE REQUEST
   ========================================================= */

async function submitAttendanceRequest(localRequest) {

    try {

        const response = await fetch(OFFLINE_ATTENDANCE_API, {

            method: "POST",

            headers: {

                "Content-Type": "application/json",

                "Idempotency-Key": localRequest.idempotencyKey

            },

            body: JSON.stringify(localRequest.payload)

        });

        let serverResponse = {};

        try {

            const responseText = await response.text();

            if (responseText) {
                serverResponse = JSON.parse(responseText);
            }

        } catch (error) {

            throw new Error(
                "Server returned an invalid response."
            );

        }

        if (response.status >= 400 && response.status < 500) {

            localRequest.status = "REJECTED";

            localRequest.message =
                serverResponse.message ||
                serverResponse.error ||
                "Attendance request rejected by server.";

            localRequest.lastError = localRequest.message;

            localRequest.lastAttemptAt =
                new Date().toISOString();

            await updateAttendanceRequest(localRequest);

            return;

        }

        if (response.status >= 500) {

            throw new Error(
                serverResponse.message ||
                "Server temporarily unavailable."
            );

        }

        if (!response.ok) {

            throw new Error(
                serverResponse.message ||
                "Attendance synchronization failed."
            );

        }

        const validStatuses = [

            "ACCEPTED",
            "REJECTED",
            "REQUIRES_REVIEW",
            "PENDING"

        ];

        if (!validStatuses.includes(serverResponse.status)) {

            throw new Error(
                "Server returned an unknown attendance status."
            );

        }

        localRequest.status = serverResponse.status;

        localRequest.eventId =
            serverResponse.eventId ?? null;

        localRequest.serverReceivedAt =
            serverResponse.serverReceivedAt ?? null;

        localRequest.message =
            serverResponse.message ||
            "Attendance request processed.";

        localRequest.lastError =
            serverResponse.status === "REJECTED"
                ? localRequest.message
                : null;

        localRequest.lastAttemptAt =
            new Date().toISOString();

        await updateAttendanceRequest(localRequest);

        console.log(
            "Attendance request synchronized:",
            localRequest.idempotencyKey,
            localRequest.status
        );

    } catch (error) {

        console.error(
            "Attendance synchronization failed:",
            error
        );

        localRequest.status = "PENDING";

        localRequest.lastError = error.message;

        localRequest.lastAttemptAt =
            new Date().toISOString();

        await updateAttendanceRequest(localRequest);

    }

}

/* =========================================================
   4. SYNCHRONIZE PENDING REQUESTS
   ========================================================= */

async function synchronizePendingRequests() {

    // IMPORTANT:
    // Do not permanently block synchronization just because
    // initializeSynchronization() was not called by app.js.
    // Check whether IndexedDB is actually ready instead.

    if (!isSynchronizationStorageReady()) {

        console.log(
            "Synchronization waiting for offline storage initialization."
        );

        return;

    }

    if (syncInProgress || !navigator.onLine) {
        return;
    }

    syncInProgress = true;

    try {

        const pendingRequests =
            await getPendingAttendanceRequests();

        console.log(
            "Pending attendance requests:",
            pendingRequests.length
        );

        pendingRequests.sort((a, b) => {

            return (
                new Date(a.createdAt) -
                new Date(b.createdAt)
            );

        });

        for (const request of pendingRequests) {

            if (!navigator.onLine) {
                break;
            }

            await submitAttendanceRequest(request);

        }

    } catch (error) {

        console.error(
            "Unable to synchronize pending requests:",
            error
        );

    } finally {

        syncInProgress = false;

        if (
            typeof refreshAttendanceDashboard === "function"
        ) {

            try {

                await refreshAttendanceDashboard();

            } catch (error) {

                console.error(
                    "Unable to refresh attendance dashboard:",
                    error
                );

            }

        }

    }

}

/* =========================================================
   5. CONNECTION EVENTS
   ========================================================= */

window.addEventListener("online", function () {

    updateConnectionStatus();

    synchronizePendingRequests();

});

window.addEventListener("offline", function () {

    updateConnectionStatus();

});

/* =========================================================
   6. TAB VISIBILITY
   ========================================================= */

document.addEventListener("visibilitychange", function () {

    if (document.visibilityState === "visible") {

        synchronizePendingRequests();

    }

});

/* =========================================================
   7. WINDOW FOCUS
   ========================================================= */

window.addEventListener("focus", function () {

    synchronizePendingRequests();

});

/* =========================================================
   8. MANUAL SYNCHRONIZATION
   ========================================================= */

async function manualSync() {

    if (!navigator.onLine) {

        alert(
            "You are offline. Check your internet connection."
        );

        return;

    }

    if (syncInProgress) {
        return;
    }

    await synchronizePendingRequests();

}

/* =========================================================
   9. MODULE LOADED
   ========================================================= */

console.log("Synchronization module loaded.");
