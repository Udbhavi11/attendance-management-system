
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   OFFICE LOCATION MANAGEMENT MODULE

   File: office-storage.js

   Backend:
   GET    /api/locations
   GET    /api/locations/{id}
   POST   /api/locations
   PUT    /api/locations/{id}
   ========================================================= */

const OFFICE_API = "/api/locations";

let adminOffices = [];
let editingOfficeId = null;


/* =========================================================
   1. HTML ESCAPING
   ========================================================= */

function escapeOfficeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}


/* =========================================================
   2. LOAD OFFICE LOCATIONS
   ========================================================= */

async function loadAdminOffices() {
    const container = document.getElementById("officeSection");

    if (!container) {
        console.error("Office section was not found.");
        return;
    }

    container.innerHTML = `
        <div class="card">
            <p>Loading office locations...</p>
        </div>
    `;

    try {
        const result = await apiRequest(OFFICE_API);

        if (!Array.isArray(result)) {
            throw new Error("Invalid office data received.");
        }

        adminOffices = result;

        renderAdminOffices();

    } catch (error) {
        console.error("Office loading failed:", error);

        container.innerHTML = `
            <div class="card">
                <p class="error-message">
                    Unable to load office locations:
                    ${escapeOfficeHtml(error.message)}
                </p>

                <button type="button"
                        class="secondary-btn"
                        id="retryOfficeBtn">
                    Retry
                </button>
            </div>
        `;

        document.getElementById("retryOfficeBtn")
            ?.addEventListener("click", loadAdminOffices);
    }
}


/* =========================================================
   3. RENDER OFFICE MANAGEMENT SCREEN
   ========================================================= */

function renderAdminOffices() {
    const container = document.getElementById("officeSection");

    if (!container) {
        return;
    }

    const activeCount = adminOffices.filter(
        office => office.active === true
    ).length;

    const inactiveCount = adminOffices.length - activeCount;

    container.innerHTML = `
        <div class="card">

            <div class="section-header">
                <div>
                    <h2>Office Location Management</h2>
                    <p>Configure office locations and attendance geofences.</p>
                </div>

                <div class="form-actions">
                    <button type="button"
                            class="primary-btn"
                            id="addOfficeBtn">
                        + Add Office
                    </button>

                    <button type="button"
                            class="secondary-btn"
                            id="refreshOfficeBtn">
                        Refresh
                    </button>
                </div>
            </div>

            <div class="dashboard-cards">

                <div class="card">
                    <h3>Total Offices</h3>
                    <p>${adminOffices.length}</p>
                </div>

                <div class="card">
                    <h3>Active Offices</h3>
                    <p>${activeCount}</p>
                </div>

                <div class="card">
                    <h3>Inactive Offices</h3>
                    <p>${inactiveCount}</p>
                </div>

            </div>
        </div>

        <div class="card" id="officeFormCard" hidden>

            <h3 id="officeFormTitle">Create Office</h3>

            <form id="adminOfficeForm">

                <div class="form-group">
                    <label for="officeName">
                        Office Name *
                    </label>

                    <input
                        id="officeName"
                        name="name"
                        type="text"
                        maxlength="100"
                        required
                    >
                </div>

                <div class="form-group">
                    <label for="officeAddress">
                        Office Address *
                    </label>

                    <textarea
                        id="officeAddress"
                        name="address"
                        maxlength="255"
                        required
                    ></textarea>
                </div>

                <div class="form-group">
                    <label for="officeLatitude">
                        Latitude *
                    </label>

                    <input
                        id="officeLatitude"
                        name="latitude"
                        type="number"
                        min="-90"
                        max="90"
                        step="any"
                        required
                    >
                </div>

                <div class="form-group">
                    <label for="officeLongitude">
                        Longitude *
                    </label>

                    <input
                        id="officeLongitude"
                        name="longitude"
                        type="number"
                        min="-180"
                        max="180"
                        step="any"
                        required
                    >
                </div>

                <div class="form-group">

                    <button
                        type="button"
                        class="secondary-btn"
                        id="captureOfficeLocationBtn">
                        Capture Current Location
                    </button>

                    <p id="officeGpsAccuracy" role="status">
                        Use this option to capture the browser's current coordinates.
                    </p>

                </div>

                <div class="form-group">
                    <label for="officeRadius">
                        Permitted Attendance Radius (metres) *
                    </label>

                    <input
                        id="officeRadius"
                        name="allowedRadiusMeters"
                        type="number"
                        min="0.01"
                        step="any"
                        required
                    >
                </div>

                <div class="form-group">
                    <label for="officeActive">
                        Office Status
                    </label>

                    <select id="officeActive" name="active">
                        <option value="true">Active</option>
                        <option value="false">Inactive</option>
                    </select>
                </div>

                <p id="officeMessage" role="status"></p>

                <div class="form-actions">

                    <button
                        type="submit"
                        class="primary-btn"
                        id="saveOfficeBtn">
                        Save Office
                    </button>

                    <button
                        type="button"
                        class="secondary-btn"
                        id="cancelOfficeBtn">
                        Cancel
                    </button>

                </div>

            </form>
        </div>

        <div class="card">

            <h3>Registered Office Locations</h3>

            <div class="table-container">

                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Office Name</th>
                            <th>Address</th>
                            <th>Latitude</th>
                            <th>Longitude</th>
                            <th>Radius (m)</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody id="officeTableBody">

                        ${adminOffices.map(office => `
                            <tr>

                                <td>
                                    ${escapeOfficeHtml(office.id)}
                                </td>

                                <td>
                                    ${escapeOfficeHtml(office.name)}
                                </td>

                                <td>
                                    ${escapeOfficeHtml(office.address)}
                                </td>

                                <td>
                                    ${escapeOfficeHtml(office.latitude)}
                                </td>

                                <td>
                                    ${escapeOfficeHtml(office.longitude)}
                                </td>

                                <td>
                                    ${escapeOfficeHtml(
                                        office.allowedRadiusMeters
                                    )}
                                </td>

                                <td>
                                    ${office.active ? "Active" : "Inactive"}
                                </td>

                                <td>

                                    <button
                                        type="button"
                                        class="secondary-btn"
                                        data-edit-office="${escapeOfficeHtml(
                                            office.id
                                        )}">
                                        Edit
                                    </button>

                                    <button
                                        type="button"
                                        class="secondary-btn"
                                        data-toggle-office="${escapeOfficeHtml(
                                            office.id
                                        )}">
                                        ${office.active
                                            ? "Deactivate"
                                            : "Activate"}
                                    </button>

                                </td>

                            </tr>
                        `).join("")}

                    </tbody>
                </table>

            </div>

            ${adminOffices.length === 0
                ? "<p>No office locations registered.</p>"
                : ""}

        </div>
    `;

    bindOfficeEvents();
}


/* =========================================================
   4. BIND OFFICE EVENTS
   ========================================================= */

function bindOfficeEvents() {

    document.getElementById("addOfficeBtn")
        ?.addEventListener("click", openCreateOfficeForm);

    document.getElementById("refreshOfficeBtn")
        ?.addEventListener("click", loadAdminOffices);

    document.getElementById("cancelOfficeBtn")
        ?.addEventListener("click", closeOfficeForm);

    document.getElementById("adminOfficeForm")
        ?.addEventListener("submit", saveAdminOffice);

    document.getElementById("captureOfficeLocationBtn")
        ?.addEventListener("click", captureOfficeLocation);

    document.querySelectorAll("[data-edit-office]")
        .forEach(button => {

            button.addEventListener("click", () => {

                editAdminOffice(
                    Number(button.dataset.editOffice)
                );

            });

        });

    document.querySelectorAll("[data-toggle-office]")
        .forEach(button => {

            button.addEventListener("click", () => {

                toggleAdminOffice(
                    Number(button.dataset.toggleOffice)
                );

            });

        });
}


/* =========================================================
   5. CAPTURE CURRENT OFFICE COORDINATES
   ========================================================= */

function captureOfficeLocation() {

    const button = document.getElementById(
        "captureOfficeLocationBtn"
    );

    const message = document.getElementById(
        "officeGpsAccuracy"
    );

    const latitudeInput = document.getElementById(
        "officeLatitude"
    );

    const longitudeInput = document.getElementById(
        "officeLongitude"
    );

    if (!button || !message) {
        return;
    }

    if (!navigator.geolocation) {
        message.textContent =
            "Geolocation is not supported by this browser.";
        return;
    }

    button.disabled = true;
    button.textContent = "Capturing location...";
    message.textContent = "Waiting for location permission...";

    navigator.geolocation.getCurrentPosition(

        position => {

            const {
                latitude,
                longitude,
                accuracy
            } = position.coords;

            latitudeInput.value = latitude.toFixed(7);
            longitudeInput.value = longitude.toFixed(7);

            message.textContent =
                `Location captured. Reported accuracy: approximately ±${Math.round(
                    accuracy
                )} metres.`;

            button.disabled = false;
            button.textContent = "Capture Current Location";

        },

        error => {

            const messages = {
                1: "Location permission denied.",
                2: "Location is currently unavailable.",
                3: "Location request timed out."
            };

            message.textContent =
                messages[error.code] ||
                "Unable to capture location.";

            button.disabled = false;
            button.textContent = "Capture Current Location";

            console.error("Office GPS error:", error);

        },

        {
            enableHighAccuracy: true,
            timeout: 20000,
            maximumAge: 0
        }

    );
}


/* =========================================================
   6. OPEN CREATE OFFICE FORM
   ========================================================= */

function openCreateOfficeForm() {

    editingOfficeId = null;

    const form = document.getElementById("adminOfficeForm");

    if (!form) {
        return;
    }

    form.reset();

    document.getElementById("officeActive").value = "true";

    document.getElementById("officeFormTitle").textContent =
        "Create Office";

    document.getElementById("saveOfficeBtn").textContent =
        "Save Office";

    document.getElementById("officeMessage").textContent = "";

    document.getElementById("officeGpsAccuracy").textContent =
        "Use this option to capture the browser's current coordinates.";

    const formCard = document.getElementById("officeFormCard");

    formCard.hidden = false;

    formCard.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}


/* =========================================================
   7. EDIT OFFICE
   ========================================================= */

function editAdminOffice(id) {

    const office = adminOffices.find(
        item => Number(item.id) === Number(id)
    );

    if (!office) {
        alert("Office not found.");
        return;
    }

    editingOfficeId = Number(id);

    document.getElementById("officeName").value =
        office.name || "";

    document.getElementById("officeAddress").value =
        office.address || "";

    document.getElementById("officeLatitude").value =
        office.latitude ?? "";

    document.getElementById("officeLongitude").value =
        office.longitude ?? "";

    document.getElementById("officeRadius").value =
        office.allowedRadiusMeters ?? "";

    document.getElementById("officeActive").value =
        String(office.active === true);

    document.getElementById("officeFormTitle").textContent =
        "Update Office";

    document.getElementById("saveOfficeBtn").textContent =
        "Update Office";

    document.getElementById("officeMessage").textContent = "";

    document.getElementById("officeGpsAccuracy").textContent =
        "You can update the office coordinates manually or capture the current location.";

    const formCard = document.getElementById("officeFormCard");

    formCard.hidden = false;

    formCard.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}


/* =========================================================
   8. BUILD OFFICE REQUEST PAYLOAD
   ========================================================= */

function buildOfficePayload() {

    return {

        name: document.getElementById("officeName")
            .value.trim(),

        address: document.getElementById("officeAddress")
            .value.trim(),

        latitude: Number(
            document.getElementById("officeLatitude").value
        ),

        longitude: Number(
            document.getElementById("officeLongitude").value
        ),

        allowedRadiusMeters: Number(
            document.getElementById("officeRadius").value
        ),

        active:
            document.getElementById("officeActive").value === "true"

    };
}


/* =========================================================
   9. VALIDATE OFFICE FORM
   ========================================================= */

function validateOfficePayload(data) {

    if (!data.name) {
        return "Office name is required.";
    }

    if (data.name.length > 100) {
        return "Office name cannot exceed 100 characters.";
    }

    if (!data.address) {
        return "Office address is required.";
    }

    if (data.address.length > 255) {
        return "Office address cannot exceed 255 characters.";
    }

    if (
        !Number.isFinite(data.latitude) ||
        data.latitude < -90 ||
        data.latitude > 90
    ) {
        return "Latitude must be between -90 and 90.";
    }

    if (
        !Number.isFinite(data.longitude) ||
        data.longitude < -180 ||
        data.longitude > 180
    ) {
        return "Longitude must be between -180 and 180.";
    }

    if (
        !Number.isFinite(data.allowedRadiusMeters) ||
        data.allowedRadiusMeters <= 0
    ) {
        return "Allowed radius must be greater than zero.";
    }

    return null;
}


/* =========================================================
   10. CREATE / UPDATE OFFICE
   ========================================================= */

async function saveAdminOffice(event) {

    event.preventDefault();

    const message = document.getElementById("officeMessage");

    const saveButton = document.getElementById("saveOfficeBtn");

    const officeData = buildOfficePayload();

    const validationError = validateOfficePayload(officeData);

    if (validationError) {

        message.textContent = validationError;
        message.className = "error-message";

        return;
    }

    const isCreate = editingOfficeId === null;

    const url = isCreate
        ? OFFICE_API
        : `${OFFICE_API}/${editingOfficeId}`;

    saveButton.disabled = true;

    message.textContent = "Saving office...";
    message.className = "";

    try {

        await apiRequest(url, {

            method: isCreate ? "POST" : "PUT",

            body: JSON.stringify(officeData)

        });

        closeOfficeForm();

        await loadAdminOffices();

        alert(
            isCreate
                ? "Office created successfully."
                : "Office updated successfully."
        );

    } catch (error) {

        console.error("Office save failed:", error);

        message.textContent =
            error.message || "Unable to save office.";

        message.className = "error-message";

    } finally {

        saveButton.disabled = false;

    }
}


/* =========================================================
   11. ACTIVATE / DEACTIVATE OFFICE
   ========================================================= */

async function toggleAdminOffice(id) {

    const office = adminOffices.find(
        item => Number(item.id) === Number(id)
    );

    if (!office) {
        alert("Office not found.");
        return;
    }

    const nextActive = office.active !== true;

    const action = nextActive
        ? "activate"
        : "deactivate";

    const confirmed = confirm(
        `Are you sure you want to ${action} ${office.name}?`
    );

    if (!confirmed) {
        return;
    }

    // The existing backend supports status changes through PUT.
    const updatedOffice = {

        name: office.name,

        address: office.address,

        latitude: office.latitude,

        longitude: office.longitude,

        allowedRadiusMeters: office.allowedRadiusMeters,

        active: nextActive

    };

    try {

        await apiRequest(
            `${OFFICE_API}/${id}`,
            {
                method: "PUT",
                body: JSON.stringify(updatedOffice)
            }
        );

        await loadAdminOffices();

        alert(
            nextActive
                ? "Office activated successfully."
                : "Office deactivated successfully."
        );

    } catch (error) {

        console.error("Office status update failed:", error);

        alert(
            error.message ||
            "Unable to update office status."
        );

    }
}


/* =========================================================
   12. CLOSE OFFICE FORM
   ========================================================= */

function closeOfficeForm() {

    editingOfficeId = null;

    const formCard = document.getElementById("officeFormCard");

    const form = document.getElementById("adminOfficeForm");

    if (formCard) {
        formCard.hidden = true;
    }

    if (form) {
        form.reset();
    }

    const saveButton = document.getElementById("saveOfficeBtn");

    if (saveButton) {
        saveButton.disabled = false;
        saveButton.textContent = "Save Office";
    }
}


/* =========================================================
   13. MODULE INITIALIZATION
   ========================================================= */

console.log("Office location management module loaded.");
