/* ==========================================
   WORKFORCE ATTENDANCE
   OFFLINE STORAGE MODULE
========================================== */

const ATTENDANCE_DB_NAME = "WorkforceAttendanceDB";
const ATTENDANCE_DB_VERSION = 1;
const ATTENDANCE_STORE_NAME = "attendanceRequests";

let attendanceDatabase = null;
let attendanceDatabasePromise = null;

/* 1. Initialize IndexedDB */

function initializeOfflineStorage() {
    if (attendanceDatabase) {
        return Promise.resolve(attendanceDatabase);
    }

    if (attendanceDatabasePromise) {
        return attendanceDatabasePromise;
    }

    attendanceDatabasePromise = new Promise((resolve, reject) => {
        if (!window.indexedDB) {
            attendanceDatabasePromise = null;
            reject(new Error("IndexedDB is not supported."));
            return;
        }

        const request = window.indexedDB.open(
            ATTENDANCE_DB_NAME,
            ATTENDANCE_DB_VERSION
        );

        request.onupgradeneeded = function (event) {
            const database = event.target.result;

            if (!database.objectStoreNames.contains(ATTENDANCE_STORE_NAME)) {
                database.createObjectStore(ATTENDANCE_STORE_NAME, {
                    keyPath: "idempotencyKey"
                });
            }
        };

        request.onsuccess = function (event) {
            const database = event.target.result;

            if (!database.objectStoreNames.contains(ATTENDANCE_STORE_NAME)) {
                database.close();
                attendanceDatabasePromise = null;
                reject(new Error("Attendance object store is missing."));
                return;
            }

            const transaction = database.transaction(
                ATTENDANCE_STORE_NAME,
                "readonly"
            );

            const store = transaction.objectStore(ATTENDANCE_STORE_NAME);

            if (store.keyPath !== "idempotencyKey") {
                database.close();
                attendanceDatabasePromise = null;
                reject(new Error(
                    "Attendance object store has an incompatible keyPath."
                ));
                return;
            }

            database.onversionchange = function () {
                database.close();
                attendanceDatabase = null;
                attendanceDatabasePromise = null;
            };

            attendanceDatabase = database;
            console.log("Offline storage initialized.");
            resolve(database);
        };

        request.onerror = function () {
            attendanceDatabasePromise = null;
            reject(
                request.error ||
                new Error("Unable to open attendance database.")
            );
        };

        request.onblocked = function () {
            console.warn(
                "Attendance database upgrade is blocked by another tab."
            );
        };
    });

    return attendanceDatabasePromise;
}

/* 2. Check whether offline storage is ready */

function isOfflineStorageAvailable() {
    return attendanceDatabase !== null;
}

/* 3. Get database */

async function getAttendanceDatabase() {
    if (!attendanceDatabase) {
        await initializeOfflineStorage();
    }

    if (!attendanceDatabase) {
        throw new Error("Attendance database is unavailable.");
    }

    return attendanceDatabase;
}

/* 4. Validate attendance request */

function validateAttendanceRequest(request) {
    if (!request || typeof request !== "object") {
        throw new Error("Invalid attendance request.");
    }

    if (
        typeof request.idempotencyKey !== "string" ||
        request.idempotencyKey.trim() === ""
    ) {
        throw new Error(
            "Attendance request requires a valid idempotencyKey."
        );
    }

    return request;
}

/* 5. Save attendance request */

async function saveAttendanceRequest(request) {
    validateAttendanceRequest(request);

    const database = await getAttendanceDatabase();

    return new Promise((resolve, reject) => {
        const transaction = database.transaction(
            ATTENDANCE_STORE_NAME,
            "readwrite"
        );

        const store = transaction.objectStore(ATTENDANCE_STORE_NAME);
        const operation = store.put(request);

        operation.onsuccess = function () {
            resolve(operation.result);
        };

        operation.onerror = function () {
            reject(operation.error);
        };

        transaction.onabort = function () {
            reject(
                transaction.error ||
                new Error("Unable to save attendance request.")
            );
        };
    });
}

/* 6. Get all attendance requests */

async function getAllAttendanceRequests() {
    const database = await getAttendanceDatabase();

    return new Promise((resolve, reject) => {
        const transaction = database.transaction(
            ATTENDANCE_STORE_NAME,
            "readonly"
        );

        const store = transaction.objectStore(ATTENDANCE_STORE_NAME);
        const operation = store.getAll();

        operation.onsuccess = function () {
            resolve(operation.result || []);
        };

        operation.onerror = function () {
            reject(operation.error);
        };
    });
}

/* 7. Get request by idempotency key */

async function getAttendanceRequest(idempotencyKey) {
    if (
        typeof idempotencyKey !== "string" ||
        idempotencyKey.trim() === ""
    ) {
        throw new Error("A valid idempotency key is required.");
    }

    const database = await getAttendanceDatabase();

    return new Promise((resolve, reject) => {
        const transaction = database.transaction(
            ATTENDANCE_STORE_NAME,
            "readonly"
        );

        const store = transaction.objectStore(ATTENDANCE_STORE_NAME);
        const operation = store.get(idempotencyKey);

        operation.onsuccess = function () {
            resolve(operation.result || null);
        };

        operation.onerror = function () {
            reject(operation.error);
        };
    });
}

/* 8. Get pending attendance requests */

async function getPendingAttendanceRequests() {
    const requests = await getAllAttendanceRequests();

    return requests.filter(
        request => request.status === "PENDING"
    );
}

/* 9. Update attendance request */

async function updateAttendanceRequest(request) {
    validateAttendanceRequest(request);

    const database = await getAttendanceDatabase();

    return new Promise((resolve, reject) => {
        const transaction = database.transaction(
            ATTENDANCE_STORE_NAME,
            "readwrite"
        );

        const store = transaction.objectStore(ATTENDANCE_STORE_NAME);
        const operation = store.put(request);

        operation.onsuccess = function () {
            resolve(operation.result);
        };

        operation.onerror = function () {
            reject(operation.error);
        };

        transaction.onabort = function () {
            reject(
                transaction.error ||
                new Error("Unable to update attendance request.")
            );
        };
    });
}

/* 10. Delete one attendance request */

async function deleteAttendanceRequest(idempotencyKey) {
    if (
        typeof idempotencyKey !== "string" ||
        idempotencyKey.trim() === ""
    ) {
        throw new Error("A valid idempotency key is required.");
    }

    const database = await getAttendanceDatabase();

    return new Promise((resolve, reject) => {
        const transaction = database.transaction(
            ATTENDANCE_STORE_NAME,
            "readwrite"
        );

        const store = transaction.objectStore(ATTENDANCE_STORE_NAME);
        const operation = store.delete(idempotencyKey);

        operation.onsuccess = function () {
            resolve(true);
        };

        operation.onerror = function () {
            reject(operation.error);
        };
    });
}