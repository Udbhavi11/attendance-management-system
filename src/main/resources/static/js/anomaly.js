/* =========================================================
   MODULE J - ATTENDANCE ANOMALY MONITORING
   File: anomaly.js
   ========================================================= */

const ANOMALY_API = "/api/anomalies";

let anomalyRecords = [];

/* =========================================================
   INITIALIZE ANOMALY SECTION
   ========================================================= */

async function loadAnomalySection() {

    const container = document.getElementById("anomalySection");

    if (!container) {
        console.error("anomalySection not found.");
        return;
    }

    if (CURRENT_ROLE === "ADMIN") {

        renderAdminAnomalyLayout();
        await loadAllAnomalies();

    } else if (CURRENT_ROLE === "MANAGER") {

        if (!EMPLOYEE_ID) {
            container.textContent = "Manager profile is unavailable.";
            return;
        }

        renderManagerAnomalyLayout();
        await loadManagerAnomalies();

    } else {

        container.textContent =
            "You are not authorized to access anomaly monitoring.";
    }
}

/* =========================================================
   ADMIN LAYOUT
   ========================================================= */

function renderAdminAnomalyLayout() {

    const container = document.getElementById("anomalySection");

    container.innerHTML = `

        <div class="card">

            <h3>Attendance Anomaly Overview</h3>

            <p>
                Review suspicious or inconsistent attendance
                activity recorded by the system.
            </p>

            <button
                type="button"
                id="refreshAdminAnomaliesBtn"
                class="secondary-btn">

                Refresh Anomalies

            </button>

            <div id="adminAnomalySummary"></div>

            <div id="adminAnomalyList">
                Loading anomalies...
            </div>

        </div>

    `;

    document.getElementById("refreshAdminAnomaliesBtn")
        .addEventListener("click", loadAllAnomalies);
}

/* =========================================================
   MANAGER LAYOUT
   ========================================================= */

function renderManagerAnomalyLayout() {

    const container = document.getElementById("anomalySection");

    container.innerHTML = `

        <div class="card">

            <h3>Pending Team Anomalies</h3>

            <p>
                Review flagged attendance events belonging
                to your assigned employees.
            </p>

            <button
                type="button"
                id="refreshManagerAnomaliesBtn"
                class="secondary-btn">

                Refresh Anomalies

            </button>

            <div id="managerAnomalySummary"></div>

            <div id="managerAnomalyList">
                Loading pending anomalies...
            </div>

        </div>

    `;

    document.getElementById("refreshManagerAnomaliesBtn")
        .addEventListener("click", loadManagerAnomalies);
}

/* =========================================================
   LOAD ALL ANOMALIES - ADMIN
   GET /api/anomalies
   ========================================================= */

async function loadAllAnomalies() {

    const container = document.getElementById("adminAnomalyList");

    if (!container) return;

    container.textContent = "Loading anomalies...";

    try {

        const anomalies = await apiRequest(ANOMALY_API);

        if (!Array.isArray(anomalies)) {
            throw new Error("Invalid anomaly response received.");
        }

        anomalyRecords = anomalies;

        renderAnomalySummary("adminAnomalySummary");

        renderAnomalyTable(
            "adminAnomalyList",
            anomalyRecords,
            false
        );

    } catch (error) {

        console.error("Admin anomaly loading failed:", error);

        container.textContent =
            "Unable to load anomalies: " + error.message;
    }
}

/* =========================================================
   LOAD MANAGER PENDING ANOMALIES
   GET /api/anomalies/manager/{managerId}/pending
   ========================================================= */

async function loadManagerAnomalies() {

    const container = document.getElementById("managerAnomalyList");

    if (!container) return;

    container.textContent = "Loading pending anomalies...";

    try {

        const anomalies = await apiRequest(
            `${ANOMALY_API}/manager/${EMPLOYEE_ID}/pending`
        );

        if (!Array.isArray(anomalies)) {
            throw new Error("Invalid manager anomaly response.");
        }

        anomalyRecords = anomalies;

        renderAnomalySummary("managerAnomalySummary");

        renderAnomalyTable(
            "managerAnomalyList",
            anomalyRecords,
            true
        );

    } catch (error) {

        console.error("Manager anomaly loading failed:", error);

        container.textContent =
            "Unable to load pending anomalies: " + error.message;
    }
}

/* =========================================================
   ANOMALY SUMMARY
   ========================================================= */

function renderAnomalySummary(containerId) {

    const container = document.getElementById(containerId);

    if (!container) return;

    const highCount = anomalyRecords.filter(
        item => item.severity === "HIGH"
    ).length;

    const mediumCount = anomalyRecords.filter(
        item => item.severity === "MEDIUM"
    ).length;

    const lowCount = anomalyRecords.filter(
        item => item.severity === "LOW"
    ).length;

    const pendingCount = anomalyRecords.filter(
        item => item.reviewStatus === "PENDING"
    ).length;

    const reviewedCount = anomalyRecords.filter(
        item => item.reviewStatus === "REVIEWED"
    ).length;

    container.innerHTML = `

        <div class="sync-summary">

            <div class="summary-item">
                <h3>${anomalyRecords.length}</h3>
                <p>Total Flags</p>
            </div>

            <div class="summary-item">
                <h3>${highCount}</h3>
                <p>High Severity</p>
            </div>

            <div class="summary-item">
                <h3>${mediumCount}</h3>
                <p>Medium Severity</p>
            </div>

            <div class="summary-item">
                <h3>${lowCount}</h3>
                <p>Low Severity</p>
            </div>

            <div class="summary-item">
                <h3>${pendingCount}</h3>
                <p>Pending</p>
            </div>

            <div class="summary-item">
                <h3>${reviewedCount}</h3>
                <p>Reviewed</p>
            </div>

        </div>

    `;
}

/* =========================================================
   RENDER ANOMALY TABLE
   ========================================================= */

function renderAnomalyTable(containerId, anomalies, allowReview) {

    const container = document.getElementById(containerId);

    if (!container) return;

    if (anomalies.length === 0) {

        container.innerHTML = `
            <p class="empty-message">
                No attendance anomalies found.
            </p>
        `;

        return;
    }

    container.innerHTML = `

        <div class="table-responsive">

            <table>

                <thead>

                    <tr>

                        <th>ID</th>

                        <th>Employee</th>

                        <th>Employee ID</th>

                        <th>Attendance Event</th>

                        <th>Break Record</th>

                        <th>Rule</th>

                        <th>Detected At</th>

                        <th>Severity</th>

                        <th>Review Status</th>

                        <th>Review Outcome</th>

                        ${allowReview ? "<th>Action</th>" : ""}

                    </tr>

                </thead>

                <tbody>

                    ${anomalies.map(anomaly => `

                        <tr>

                            <td>${Number(anomaly.id)}</td>

                            <td>
                                ${escapeAnomalyHTML(anomaly.employeeName)}
                            </td>

                            <td>
                                ${Number(anomaly.employeeId)}
                            </td>

                            <td>
                                ${anomaly.attendanceEventId == null
                                    ? "-"
                                    : Number(anomaly.attendanceEventId)}
                            </td>

                            <td>
                                ${anomaly.breakRecordId == null
                                    ? "-"
                                    : Number(anomaly.breakRecordId)}
                            </td>

                            <td>
                                ${escapeAnomalyHTML(anomaly.ruleIdentifier)}
                            </td>

                            <td>
                                ${escapeAnomalyHTML(
                                    formatAnomalyDate(anomaly.detectedAt)
                                )}
                            </td>

                            <td>
                                ${escapeAnomalyHTML(anomaly.severity)}
                            </td>

                            <td>
                                ${escapeAnomalyHTML(anomaly.reviewStatus)}
                            </td>

                            <td>
                                ${escapeAnomalyHTML(anomaly.reviewOutcome || "-")}
                            </td>

                            ${allowReview ? `

                                <td>

                                    ${anomaly.reviewStatus === "PENDING" ? `

                                        <button
                                            type="button"
                                            class="primary-btn"
                                            onclick="reviewAnomaly(${Number(anomaly.id)})">

                                            Review

                                        </button>

                                    ` : "Completed"}

                                </td>

                            ` : ""}

                        </tr>

                    `).join("")}

                </tbody>

            </table>

        </div>

    `;
}

/* =========================================================
   MANAGER REVIEW ANOMALY
   PUT /api/anomalies/{id}/review/{managerId}
   ========================================================= */

async function reviewAnomaly(anomalyId) {

    const anomaly = anomalyRecords.find(
        item => Number(item.id) === Number(anomalyId)
    );

    if (!anomaly || anomaly.reviewStatus !== "PENDING") {
        alert("This anomaly is not available for review.");
        return;
    }

    const reviewOutcome = prompt(
        "Enter your review outcome for this anomaly:"
    );

    if (reviewOutcome === null) {
        return;
    }

    const trimmedOutcome = reviewOutcome.trim();

    if (!trimmedOutcome) {
        alert("Review outcome is required.");
        return;
    }

    if (trimmedOutcome.length > 500) {
        alert("Review outcome cannot exceed 500 characters.");
        return;
    }

    try {

        await apiRequest(
            `${ANOMALY_API}/${anomalyId}/review/${EMPLOYEE_ID}` +
            `?reviewOutcome=${encodeURIComponent(trimmedOutcome)}`,
            {
                method: "PUT"
            }
        );

        alert("Anomaly review recorded successfully.");

        await loadManagerAnomalies();

    } catch (error) {

        console.error("Anomaly review failed:", error);

        alert(
            error.message || "Unable to record anomaly review."
        );
    }
}

/* =========================================================
   DATE FORMATTER
   ========================================================= */

function formatAnomalyDate(value) {

    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    return date.toLocaleString();
}

/* =========================================================
   HTML ESCAPING
   ========================================================= */

function escapeAnomalyHTML(value) {

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
   NAVIGATION INTEGRATION
   ========================================================= */

document.addEventListener("DOMContentLoaded", function () {

    const anomalyButton = document.querySelector(
        '.nav-btn[data-section="anomalies"]'
    );

    if (anomalyButton) {

        anomalyButton.addEventListener("click", function () {
            loadAnomalySection();
        });

    }

});

console.log("Attendance anomaly module loaded.");