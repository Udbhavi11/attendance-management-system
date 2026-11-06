
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   MODULE F: FIELD VISITS
   File: field-visit.js
   Role-specific field visit screens
   ========================================================= */

const FIELD_VISIT_API = "/api/field-visits";

let fieldVisits = [];
let activeFieldVisit = null;
let fieldVisitCoordinates = null;

/* =========================================================
   1. INITIALIZE FIELD VISIT MODULE
   ========================================================= */

async function loadFieldVisitSection() {

    const container = document.getElementById("visitSection");

    if (!container) {
        console.error("visitSection was not found.");
        return;
    }

    // ADMIN: Read-only organization-wide visit reporting.
    if (CURRENT_ROLE === "ADMIN") {
        await loadAdminFieldVisitScreen();
        return;
    }

    // MANAGER: Read-only visits belonging to direct reports.
    if (CURRENT_ROLE === "MANAGER") {
        await loadManagerFieldVisitScreen();
        return;
    }

    // AUDITOR: Field visit information is available through reports.
    if (CURRENT_ROLE === "AUDITOR") {
        container.replaceChildren();

        const message = document.createElement("p");
        message.textContent =
            "Field visit information is available in the Reports section.";

        container.appendChild(message);
        return;
    }

    // EMPLOYEE: Existing start, complete and personal history.
    if (!EMPLOYEE_ID) {
        container.textContent =
            "Employee information is unavailable.";
        return;
    }

    renderFieldVisitLayout();
    await loadFieldVisitHistory();
}

/* =========================================================
   2. EMPLOYEE FIELD VISIT LAYOUT
   ========================================================= */

function renderFieldVisitLayout() {

    const container = document.getElementById("visitSection");

    if (!container) return;

    container.innerHTML = `
        <div class="card">
            <h3>Start a Field Visit</h3>

            <form id="fieldVisitForm">

                <div class="form-group">
                    <label for="visitType">Visit Type</label>
                    <select id="visitType" required>
                        <option value="">Select visit type</option>
                        <option value="SALES">Sales Visit</option>
                        <option value="CUSTOMER_SERVICE">
                            Customer Service Visit
                        </option>
                        <option value="COLLECTION">Collection Visit</option>
                        <option value="OTHER">Other Business Visit</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="customerReference">
                        Customer Reference
                    </label>
                    <input
                        type="text"
                        id="customerReference"
                        maxlength="100"
                        required
                        placeholder="Enter customer reference"
                    >
                </div>

                <div class="form-group">
                    <label for="visitRemarks">Remarks</label>
                    <textarea
                        id="visitRemarks"
                        maxlength="500"
                        placeholder="Optional remarks"
                    ></textarea>
                </div>

                <p id="visitLocationStatus">
                    Location not requested.
                </p>

                <button
                    type="button"
                    id="visitLocationBtn"
                    class="secondary-btn">
                    Get Current Location
                </button>

                <button
                    type="submit"
                    id="startVisitBtn"
                    class="primary-btn">
                    Start Visit
                </button>

            </form>

            <p id="fieldVisitMessage"></p>
        </div>

        <div class="card">
            <h3>Active Field Visit</h3>

            <div id="activeVisitDetails">
                No active visit.
            </div>

            <button
                type="button"
                id="completeVisitBtn"
                class="danger-btn"
                hidden>
                Complete Visit
            </button>
        </div>

        <div class="card">
            <h3>My Field Visit History</h3>

            <div class="form-group">
                <label for="visitHistoryDate">Select Date</label>
                <input type="date" id="visitHistoryDate">
            </div>

            <button
                type="button"
                id="refreshVisitHistoryBtn"
                class="secondary-btn">
                Refresh History
            </button>

            <div id="fieldVisitHistory">
                Loading visit history...
            </div>
        </div>
    `;

    document.getElementById("visitHistoryDate").value =
        getFieldVisitToday();

    document.getElementById("fieldVisitForm")
        .addEventListener("submit", startFieldVisit);

    document.getElementById("visitLocationBtn")
        .addEventListener("click", getFieldVisitLocation);

    document.getElementById("completeVisitBtn")
        .addEventListener("click", completeFieldVisit);

    document.getElementById("refreshVisitHistoryBtn")
        .addEventListener("click", loadFieldVisitHistory);
}

/* =========================================================
   3. LOCATION CAPTURE - EMPLOYEE ONLY
   ========================================================= */

function getFieldVisitLocation() {

    const status = document.getElementById(
        "visitLocationStatus"
    );

    const button = document.getElementById(
        "visitLocationBtn"
    );

    if (!status || !button) return;

    if (!navigator.geolocation) {
        status.textContent =
            "Geolocation is not supported by this browser.";
        return;
    }

    status.textContent = "Getting your location...";
    button.disabled = true;

    navigator.geolocation.getCurrentPosition(
        position => {

            fieldVisitCoordinates = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
            };

            status.textContent =
                "Location captured successfully. Accuracy: " +
                Math.round(position.coords.accuracy) +
                " metres.";

            button.disabled = false;
        },

        error => {

            fieldVisitCoordinates = null;

            const messages = {
                1: "Location permission was denied.",
                2: "Your location is currently unavailable.",
                3: "Location request timed out."
            };

            status.textContent =
                messages[error.code] ||
                "Unable to retrieve your location.";

            button.disabled = false;
        },

        {
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 0
        }
    );
}

/* =========================================================
   4. START FIELD VISIT - EMPLOYEE ONLY
   ========================================================= */

async function startFieldVisit(event) {

    event.preventDefault();

    if (CURRENT_ROLE !== "EMPLOYEE") {
        alert("Only employees can start field visits.");
        return;
    }

    const message = document.getElementById(
        "fieldVisitMessage"
    );

    const button = document.getElementById(
        "startVisitBtn"
    );

    if (!EMPLOYEE_ID) {
        message.textContent =
            "Employee information is unavailable.";
        return;
    }

    if (!fieldVisitCoordinates) {
        message.textContent =
            "Please capture your current location first.";
        return;
    }

    const payload = {
        employeeId: Number(EMPLOYEE_ID),

        visitType: document.getElementById(
            "visitType"
        ).value,

        customerReference: document.getElementById(
            "customerReference"
        ).value.trim(),

        startLatitude: fieldVisitCoordinates.latitude,

        startLongitude: fieldVisitCoordinates.longitude,

        remarks: document.getElementById(
            "visitRemarks"
        ).value.trim() || null
    };

    if (!payload.visitType || !payload.customerReference) {
        message.textContent =
            "Please complete all required fields.";
        return;
    }

    button.disabled = true;
    message.textContent = "Starting field visit...";

    try {

        const result = await apiRequest(
            `${FIELD_VISIT_API}/start`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            }
        );

        message.textContent =
            "Field visit started successfully.";

        document.getElementById(
            "fieldVisitForm"
        ).reset();

        fieldVisitCoordinates = null;

        document.getElementById(
            "visitLocationStatus"
        ).textContent = "Location not requested.";

        activeFieldVisit = result;

        renderActiveFieldVisit();

        await loadFieldVisitHistory();

    } catch (error) {

        console.error("Unable to start field visit:", error);

        message.textContent =
            error.message || "Unable to start field visit.";

    } finally {

        button.disabled = false;
    }
}

/* =========================================================
   5. COMPLETE FIELD VISIT - EMPLOYEE ONLY
   ========================================================= */

async function completeFieldVisit() {

    if (CURRENT_ROLE !== "EMPLOYEE") {
        alert("Only employees can complete their field visits.");
        return;
    }

    if (!activeFieldVisit) {
        alert("There is no active field visit.");
        return;
    }

    if (!fieldVisitCoordinates) {
        alert(
            "Please capture your current location before completing the visit."
        );
        return;
    }

    const button = document.getElementById(
        "completeVisitBtn"
    );

    button.disabled = true;

    try {

        const payload = {
            endLatitude: fieldVisitCoordinates.latitude,
            endLongitude: fieldVisitCoordinates.longitude
        };

        await apiRequest(
            `${FIELD_VISIT_API}/${activeFieldVisit.id}/complete`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            }
        );

        alert("Field visit completed successfully.");

        activeFieldVisit = null;
        fieldVisitCoordinates = null;

        renderActiveFieldVisit();

        await loadFieldVisitHistory();

    } catch (error) {

        console.error("Unable to complete field visit:", error);

        alert(
            error.message || "Unable to complete field visit."
        );

    } finally {

        button.disabled = false;
    }
}

/* =========================================================
   6. LOAD EMPLOYEE / MANAGER VISIT HISTORY
   ========================================================= */

async function loadFieldVisitHistory() {

    const historyContainer = document.getElementById(
        "fieldVisitHistory"
    );

    if (!historyContainer) return;

    const date = document.getElementById(
        "visitHistoryDate"
    )?.value || getFieldVisitToday();

    historyContainer.textContent =
        "Loading field visit history...";

    try {

        let endpoint;

        if (CURRENT_ROLE === "MANAGER") {

            endpoint =
                `${FIELD_VISIT_API}/manager/${EMPLOYEE_ID}/team?date=${encodeURIComponent(date)}`;

        } else if (CURRENT_ROLE === "EMPLOYEE") {

            endpoint =
                `${FIELD_VISIT_API}/employee/${EMPLOYEE_ID}/daily?date=${encodeURIComponent(date)}`;

        } else {
            throw new Error("This role cannot access this history screen.");
        }

        const result = await apiRequest(endpoint);

        if (!Array.isArray(result)) {
            throw new Error("Invalid field visit response.");
        }

        fieldVisits = result;

        renderFieldVisitHistory(fieldVisits);

        if (CURRENT_ROLE === "EMPLOYEE") {

            activeFieldVisit = fieldVisits.find(
                visit => visit.status === "IN_PROGRESS"
            ) || null;

            renderActiveFieldVisit();
        }

    } catch (error) {

        console.error(
            "Unable to load field visit history:",
            error
        );

        historyContainer.textContent =
            "Unable to load field visit history: " +
            error.message;
    }
}

/* =========================================================
   7. RENDER ACTIVE EMPLOYEE VISIT
   ========================================================= */

function renderActiveFieldVisit() {

    const container = document.getElementById(
        "activeVisitDetails"
    );

    const completeButton = document.getElementById(
        "completeVisitBtn"
    );

    if (!container || !completeButton) return;

    if (!activeFieldVisit) {

        container.textContent = "No active visit.";

        completeButton.hidden = true;

        return;
    }

    container.innerHTML = `
        <p><strong>Visit ID:</strong>
            ${escapeVisitHTML(activeFieldVisit.id)}
        </p>

        <p><strong>Customer:</strong>
            ${escapeVisitHTML(activeFieldVisit.customerReference)}
        </p>

        <p><strong>Type:</strong>
            ${formatVisitType(activeFieldVisit.visitType)}
        </p>

        <p><strong>Started:</strong>
            ${formatVisitDate(activeFieldVisit.startTime)}
        </p>

        <p><strong>Status:</strong>
            ${escapeVisitHTML(activeFieldVisit.status)}
        </p>
    `;

    completeButton.hidden = false;
}

/* =========================================================
   8. RENDER VISIT HISTORY TABLE
   ========================================================= */

function renderFieldVisitHistory(visits) {

    const container = document.getElementById(
        "fieldVisitHistory"
    );

    if (!container) return;

    if (!visits.length) {
        container.textContent =
            "No field visits found for the selected date.";
        return;
    }

    const rows = visits.map(visit => {

        const duration = visit.durationInSeconds != null
            ? formatVisitDuration(visit.durationInSeconds)
            : "In progress";

        return `
            <tr>
                <td>${escapeVisitHTML(visit.id)}</td>

                <td>
                    ${escapeVisitHTML(visit.employeeName || "-")}
                </td>

                <td>
                    ${formatVisitType(visit.visitType)}
                </td>

                <td>
                    ${escapeVisitHTML(visit.customerReference)}
                </td>

                <td>
                    ${formatVisitDate(visit.startTime)}
                </td>

                <td>
                    ${visit.endTime
                        ? formatVisitDate(visit.endTime)
                        : "-"}
                </td>

                <td>
                    ${escapeVisitHTML(visit.status)}
                </td>

                <td>${duration}</td>
            </tr>
        `;
    }).join("");

    container.innerHTML = `
        <div class="table-responsive">
            <table>
                <thead>
                    <tr>
                        <th>Visit ID</th>
                        <th>Employee</th>
                        <th>Visit Type</th>
                        <th>Customer</th>
                        <th>Start Time</th>
                        <th>End Time</th>
                        <th>Status</th>
                        <th>Duration</th>
                    </tr>
                </thead>

                <tbody>
                    ${rows}
                </tbody>
            </table>
        </div>
    `;
}

/* =========================================================
   9. ADMIN FIELD VISIT SCREEN
   Read-only summary and date-range history.
   ========================================================= */

async function loadAdminFieldVisitScreen() {

    const container = document.getElementById("visitSection");

    if (!container) return;

    container.replaceChildren();

    const card = document.createElement("div");
    card.className = "card";

    const heading = document.createElement("h3");
    heading.textContent = "Field Visit Summary";

    const description = document.createElement("p");
    description.textContent =
        "View organization-wide field visit activity.";

    const filters = document.createElement("div");
    filters.className = "form-grid";

    const fromGroup = document.createElement("div");
    fromGroup.className = "form-group";

    const fromLabel = document.createElement("label");
    fromLabel.htmlFor = "adminVisitStartDate";
    fromLabel.textContent = "From Date";

    const fromDate = document.createElement("input");
    fromDate.type = "date";
    fromDate.id = "adminVisitStartDate";
    fromDate.value = getFieldVisitToday();

    fromGroup.append(fromLabel, fromDate);

    const toGroup = document.createElement("div");
    toGroup.className = "form-group";

    const toLabel = document.createElement("label");
    toLabel.htmlFor = "adminVisitEndDate";
    toLabel.textContent = "To Date";

    const toDate = document.createElement("input");
    toDate.type = "date";
    toDate.id = "adminVisitEndDate";
    toDate.value = getFieldVisitToday();

    toGroup.append(toLabel, toDate);

    const buttonGroup = document.createElement("div");
    buttonGroup.className = "form-group";

    const searchButton = document.createElement("button");
    searchButton.type = "button";
    searchButton.className = "primary-btn";
    searchButton.textContent = "View Report";

    searchButton.addEventListener(
        "click",
        loadAdminFieldVisitReport
    );

    buttonGroup.appendChild(searchButton);

    filters.append(fromGroup, toGroup, buttonGroup);

    const summary = document.createElement("div");
    summary.id = "adminVisitSummary";

    const breakdownHeading = document.createElement("h3");
    breakdownHeading.textContent = "Visit Type Breakdown";

    const breakdown = document.createElement("div");
    breakdown.id = "adminVisitBreakdown";

    const historyHeading = document.createElement("h3");
    historyHeading.textContent = "Field Visit History";

    historyHeading.style.marginTop = "25px";

    const history = document.createElement("div");
    history.id = "adminVisitHistory";
    history.className = "table-container";

    card.append(
        heading,
        description,
        filters,
        summary,
        breakdownHeading,
        breakdown,
        historyHeading,
        history
    );

    container.appendChild(card);

    await loadAdminFieldVisitReport();
}

/* =========================================================
   10. ADMIN FIELD VISIT SUMMARY AND HISTORY
   ========================================================= */

async function loadAdminFieldVisitReport() {

    if (CURRENT_ROLE !== "ADMIN") return;

    const summary = document.getElementById(
        "adminVisitSummary"
    );

    const breakdown = document.getElementById(
        "adminVisitBreakdown"
    );

    const history = document.getElementById(
        "adminVisitHistory"
    );

    if (!summary || !breakdown || !history) return;

    const startDate = document.getElementById(
        "adminVisitStartDate"
    )?.value;

    const endDate = document.getElementById(
        "adminVisitEndDate"
    )?.value;

    if (!startDate || !endDate || startDate > endDate) {
        summary.textContent =
            "Please select a valid date range.";
        return;
    }

    summary.textContent = "Loading summary...";
    breakdown.textContent = "Loading visit type breakdown...";
    history.textContent = "Loading field visit history...";

    try {

        const report = await apiRequest(
            `/api/reports/field-visits?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`
        );

        renderAdminVisitSummary(report);
        renderAdminVisitBreakdown(report.visitTypeBreakdown || []);

        await loadAdminVisitHistory(startDate, endDate);

    } catch (error) {

        console.error("Unable to load admin visit report:", error);

        summary.textContent =
            "Unable to load summary: " + error.message;

        breakdown.textContent = "";
        history.textContent = "";
    }
}

/* =========================================================
   11. RENDER ADMIN SUMMARY CARDS
   ========================================================= */

function renderAdminVisitSummary(report) {

    const container = document.getElementById(
        "adminVisitSummary"
    );

    if (!container) return;

    container.replaceChildren();

    const metrics = [
        ["Total Visits", report.totalVisits],
        ["Completed Visits", report.completedVisits],
        ["Incomplete Visits", report.incompleteVisits],
        [
            "Total Duration",
            formatVisitDuration(report.totalVisitDurationSeconds || 0)
        ]
    ];

    const row = document.createElement("div");
    row.className = "sync-summary";

    metrics.forEach(([label, value]) => {

        const item = document.createElement("div");
        item.className = "summary-item";

        const number = document.createElement("h3");
        number.textContent = value ?? 0;

        const caption = document.createElement("p");
        caption.textContent = label;

        item.append(number, caption);
        row.appendChild(item);
    });

    container.appendChild(row);
}

/* =========================================================
   12. RENDER ADMIN VISIT TYPE BREAKDOWN
   ========================================================= */

function renderAdminVisitBreakdown(items) {

    const container = document.getElementById(
        "adminVisitBreakdown"
    );

    if (!container) return;

    container.replaceChildren();

    if (!items.length) {
        container.textContent =
            "No visit type data available for this period.";
        return;
    }

    const table = document.createElement("table");

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    ["Visit Type", "Number of Visits"].forEach(label => {
        const th = document.createElement("th");
        th.textContent = label;
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    items.forEach(item => {

        const row = document.createElement("tr");

        const typeCell = document.createElement("td");
        typeCell.textContent =
            formatVisitType(item.visitType);

        const countCell = document.createElement("td");
        countCell.textContent = item.count ?? 0;

        row.append(typeCell, countCell);
        tbody.appendChild(row);
    });

    table.append(thead, tbody);

    const wrapper = document.createElement("div");
    wrapper.className = "table-container";
    wrapper.appendChild(table);

    container.appendChild(wrapper);
}

/* =========================================================
   13. ADMIN FIELD VISIT HISTORY
   ========================================================= */

async function loadAdminVisitHistory(startDate, endDate) {

    const container = document.getElementById(
        "adminVisitHistory"
    );

    if (!container) return;

    try {

        // Admin's backend endpoint returns organization-wide visits.
        const result = await apiRequest(FIELD_VISIT_API);

        if (!Array.isArray(result)) {
            throw new Error("Invalid field visit response.");
        }

        // Filter by the selected date range.
        const filtered = result.filter(visit => {

            if (!visit.startTime) return false;

            const visitDate = getVisitLocalDate(visit.startTime);

            return visitDate >= startDate &&
                   visitDate <= endDate;
        });

        fieldVisits = filtered;

        renderAdminVisitHistory(filtered);

    } catch (error) {

        console.error("Unable to load visit history:", error);

        container.textContent =
            "Unable to load field visit history: " +
            error.message;
    }
}

/* =========================================================
   14. RENDER ADMIN HISTORY TABLE
   ========================================================= */

function renderAdminVisitHistory(visits) {

    const container = document.getElementById(
        "adminVisitHistory"
    );

    if (!container) return;

    container.replaceChildren();

    if (!visits.length) {
        container.textContent =
            "No field visits found for the selected date range.";
        return;
    }

    const table = document.createElement("table");

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    [
        "Visit ID",
        "Employee",
        "Visit Type",
        "Customer Reference",
        "Start Time",
        "End Time",
        "Status",
        "Duration"
    ].forEach(label => {

        const th = document.createElement("th");
        th.textContent = label;
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    visits.forEach(visit => {

        const row = document.createElement("tr");

        const duration = visit.durationSeconds != null
            ? formatVisitDuration(visit.durationSeconds)
            : "In progress";

        const values = [
            visit.id,
            visit.employeeName || visit.employeeId || "-",
            formatVisitType(visit.visitType),
            visit.customerReference,
            formatVisitDate(visit.startTime),
            formatVisitDate(visit.endTime),
            visit.status,
            duration
        ];

        values.forEach(value => {

            const cell = document.createElement("td");
            cell.textContent = value ?? "-";

            row.appendChild(cell);
        });

        tbody.appendChild(row);
    });

    table.append(thead, tbody);

    const wrapper = document.createElement("div");
    wrapper.className = "table-responsive";
    wrapper.appendChild(table);

    container.appendChild(wrapper);
}

/* =========================================================
   15. MANAGER FIELD VISIT SCREEN
   ========================================================= */

async function loadManagerFieldVisitScreen() {

    const container = document.getElementById("visitSection");

    if (!container) return;

    if (!EMPLOYEE_ID) {
        container.textContent =
            "Manager employee information is unavailable.";
        return;
    }

    container.replaceChildren();

    const card = document.createElement("div");
    card.className = "card";

    const heading = document.createElement("h3");
    heading.textContent = "Team Field Visit History";

    const description = document.createElement("p");
    description.textContent =
        "View field visits recorded by employees assigned to your team.";

    const dateGroup = document.createElement("div");
    dateGroup.className = "form-group";

    const label = document.createElement("label");
    label.htmlFor = "managerVisitDate";
    label.textContent = "Select Date";

    const dateInput = document.createElement("input");
    dateInput.type = "date";
    dateInput.id = "managerVisitDate";
    dateInput.value = getFieldVisitToday();

    dateGroup.append(label, dateInput);

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.className = "primary-btn";
    refreshButton.textContent = "View Team Visits";

    refreshButton.addEventListener(
        "click",
        loadManagerFieldVisitHistory
    );

    const historyHeading = document.createElement("h3");
    historyHeading.textContent = "Team Visit Records";
    historyHeading.style.marginTop = "20px";

    const history = document.createElement("div");
    history.id = "managerFieldVisitHistory";
    history.className = "table-container";

    card.append(
        heading,
        description,
        dateGroup,
        refreshButton,
        historyHeading,
        history
    );

    container.appendChild(card);

    await loadManagerFieldVisitHistory();
}

/* =========================================================
   16. LOAD MANAGER TEAM VISITS
   ========================================================= */

async function loadManagerFieldVisitHistory() {

    if (CURRENT_ROLE !== "MANAGER") return;

    const container = document.getElementById(
        "managerFieldVisitHistory"
    );

    if (!container) return;

    const date = document.getElementById(
        "managerVisitDate"
    )?.value || getFieldVisitToday();

    container.textContent = "Loading team field visits...";

    try {

        const result = await apiRequest(
            `${FIELD_VISIT_API}/manager/${EMPLOYEE_ID}/team?date=${encodeURIComponent(date)}`
        );

        if (!Array.isArray(result)) {
            throw new Error("Invalid team field visit response.");
        }

        fieldVisits = result;

        renderManagerFieldVisitHistory(result);

    } catch (error) {

        console.error("Unable to load manager field visits:", error);

        container.textContent =
            "Unable to load team field visits: " +
            error.message;
    }
}

/* =========================================================
   17. RENDER MANAGER TEAM VISIT HISTORY
   ========================================================= */

function renderManagerFieldVisitHistory(visits) {

    const container = document.getElementById(
        "managerFieldVisitHistory"
    );

    if (!container) return;

    container.replaceChildren();

    if (!visits.length) {
        container.textContent =
            "No team field visits found for this date.";
        return;
    }

    const table = document.createElement("table");

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    [
        "Visit ID",
        "Employee",
        "Visit Type",
        "Customer Reference",
        "Start Time",
        "End Time",
        "Status",
        "Duration"
    ].forEach(label => {

        const th = document.createElement("th");
        th.textContent = label;
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    visits.forEach(visit => {

        const row = document.createElement("tr");

        const duration = visit.durationSeconds != null
            ? formatVisitDuration(visit.durationSeconds)
            : "In progress";

        const values = [
            visit.id,
            visit.employeeName || visit.employeeId || "-",
            formatVisitType(visit.visitType),
            visit.customerReference,
            formatVisitDate(visit.startTime),
            formatVisitDate(visit.endTime),
            visit.status,
            duration
        ];

        values.forEach(value => {

            const cell = document.createElement("td");
            cell.textContent = value ?? "-";

            row.appendChild(cell);
        });

        tbody.appendChild(row);
    });

    table.append(thead, tbody);

    const wrapper = document.createElement("div");
    wrapper.className = "table-responsive";
    wrapper.appendChild(table);

    container.appendChild(wrapper);
}

/* =========================================================
   18. DATE AND DISPLAY HELPERS
   ========================================================= */

function getFieldVisitToday() {

    const now = new Date();

    const year = now.getFullYear();

    const month = String(
        now.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        now.getDate()
    ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function getVisitLocalDate(value) {

    if (!value) return "";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value).slice(0, 10);
    }

    const year = date.getFullYear();

    const month = String(
        date.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        date.getDate()
    ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function formatVisitDate(value) {

    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "-";

    return date.toLocaleString();
}

function formatVisitType(value) {

    const labels = {
        SALES: "Sales Visit",
        CUSTOMER_SERVICE: "Customer Service Visit",
        COLLECTION: "Collection Visit",
        OTHER: "Other Business Visit"
    };

    return labels[value] || value || "-";
}

function formatVisitDuration(seconds) {

    const totalMinutes = Math.floor(seconds / 60);

    const hours = Math.floor(totalMinutes / 60);

    const minutes = totalMinutes % 60;

    return `${hours}h ${minutes}m`;
}

function escapeVisitHTML(value) {

    return String(value ?? "").replace(
        /[&<>"']/g,
        character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]
    );
}

/* =========================================================
   19. NAVIGATION INTEGRATION
   ========================================================= */

document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll(
        '.nav-btn[data-section="visits"]'
    ).forEach(button => {

        button.addEventListener("click", () => {
            loadFieldVisitSection();
        });

    });

});

console.log("Field Visit module loaded.");
