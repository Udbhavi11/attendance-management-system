/* =========================================================
   MODULE I - ATTENDANCE CORRECTIONS

   File: correction.js

   ========================================================= */

const CORRECTION_API = "/api/corrections";
const CORRECTION_ATTENDANCE_API = "/api/attendance";

let employeeCorrectionRequests = [];
let managerCorrectionRequests = [];

/* =========================================================
   INITIALIZE SECTION
   ========================================================= */

async function loadCorrectionSection() {

    const container = document.getElementById("correctionSection");

    if (!container) {
        console.error("correctionSection not found.");
        return;
    }

    if (!EMPLOYEE_ID && CURRENT_ROLE !== "ADMIN") {
        container.textContent = "User profile is unavailable.";
        return;
    }

    if (CURRENT_ROLE === "EMPLOYEE") {
        renderEmployeeCorrectionLayout();
        await loadEmployeeCorrectionData();

    } else if (CURRENT_ROLE === "MANAGER") {
        renderManagerCorrectionLayout();
        await loadManagerCorrectionRequests();

    } else {
        container.innerHTML =
            "<p>Attendance corrections are handled by employees and their assigned managers.</p>";
    }
}

/* =========================================================
   EMPLOYEE LAYOUT
   ========================================================= */

function renderEmployeeCorrectionLayout() {

    const container = document.getElementById("correctionSection");

    container.innerHTML = `

        <div class="card">

            <h3>Submit Attendance Correction</h3>

            <form id="correctionForm">

                <label for="correctionSession">
                    Attendance Session
                </label>

                <select id="correctionSession" required>
                    <option value="">Select attendance session</option>
                </select>

                <div id="originalAttendanceDetails"></div>

                <label for="requestedCheckIn">
                    Requested Check-in Time
                </label>

                <input
                    type="datetime-local"
                    id="requestedCheckIn"
                >

                <label for="requestedCheckOut">
                    Requested Check-out Time
                </label>

                <input
                    type="datetime-local"
                    id="requestedCheckOut"
                >

                <label for="correctionReason">
                    Reason for Correction
                </label>

                <textarea
                    id="correctionReason"
                    maxlength="500"
                    required
                    placeholder="Explain why attendance needs correction"
                ></textarea>

                <button
                    type="submit"
                    id="submitCorrectionBtn"
                    class="primary-btn">

                    Submit Correction Request

                </button>

            </form>

            <p id="correctionMessage"></p>

        </div>

        <div class="card">

            <h3>My Correction Requests</h3>

            <button
                type="button"
                id="refreshCorrectionBtn"
                class="secondary-btn">

                Refresh Requests

            </button>

            <div id="employeeCorrectionHistory">
                Loading requests...
            </div>

        </div>

    `;

    document.getElementById("correctionForm")
        .addEventListener("submit", submitEmployeeCorrection);

    document.getElementById("correctionSession")
        .addEventListener("change", showSelectedSessionDetails);

    document.getElementById("refreshCorrectionBtn")
        .addEventListener("click", loadEmployeeCorrectionData);
}

/* =========================================================
   LOAD EMPLOYEE ATTENDANCE AND CORRECTION HISTORY
   ========================================================= */

let employeeCorrectionSessions = [];

async function loadEmployeeCorrectionData() {

    const sessionSelect = document.getElementById("correctionSession");

    const historyContainer = document.getElementById(
        "employeeCorrectionHistory"
    );

    if (!sessionSelect || !historyContainer) return;

    try {

        const [sessions, requests] = await Promise.all([

            apiRequest(
                `${CORRECTION_ATTENDANCE_API}/employee/${EMPLOYEE_ID}`
            ),

            apiRequest(
                `${CORRECTION_API}/employee/${EMPLOYEE_ID}`
            )

        ]);

        if (!Array.isArray(sessions) || !Array.isArray(requests)) {
            throw new Error("Invalid correction data received.");
        }

        employeeCorrectionSessions = sessions;
        employeeCorrectionRequests = requests;

        populateCorrectionSessions();
        renderEmployeeCorrectionHistory();

    } catch (error) {

        console.error("Correction data loading failed:", error);

        historyContainer.textContent =
            "Unable to load correction data: " + error.message;
    }
}

/* =========================================================
   POPULATE ATTENDANCE SESSION DROPDOWN
   ========================================================= */

function populateCorrectionSessions() {

    const select = document.getElementById("correctionSession");

    if (!select) return;

    const pendingSessionIds = new Set(

        employeeCorrectionRequests
            .filter(request => request.status === "PENDING")
            .map(request => Number(request.attendanceSessionId))

    );

    const eligibleSessions = employeeCorrectionSessions.filter(
        session => !pendingSessionIds.has(Number(session.id))
    );

    select.innerHTML = `

        <option value="">Select attendance session</option>

        ${eligibleSessions.map(session => `

            <option value="${Number(session.id)}">

                Session #${Number(session.id)} -
                ${formatCorrectionDate(session.checkInTime)}

            </option>

        `).join("")}

    `;

    if (eligibleSessions.length === 0) {

        select.innerHTML = `
            <option value="">No eligible sessions available</option>
        `;
    }
}

/* =========================================================
   DISPLAY ORIGINAL SESSION TIMES
   ========================================================= */

function showSelectedSessionDetails() {

    const sessionId = Number(
        document.getElementById("correctionSession").value
    );

    const session = employeeCorrectionSessions.find(
        item => Number(item.id) === sessionId
    );

    const container = document.getElementById(
        "originalAttendanceDetails"
    );

    if (!container) return;

    if (!session) {
        container.innerHTML = "";
        return;
    }

    container.innerHTML = `

        <div class="card">

            <h4>Original Attendance Details</h4>

            <p>
                <strong>Check-in:</strong>
                ${escapeCorrectionHTML(
                    formatCorrectionDate(session.checkInTime)
                )}
            </p>

            <p>
                <strong>Check-out:</strong>
                ${escapeCorrectionHTML(
                    formatCorrectionDate(session.checkOutTime)
                )}
            </p>

            <p>
                <strong>Status:</strong>
                ${escapeCorrectionHTML(session.status || "-")}
            </p>

        </div>

    `;

    // Pre-fill existing times for convenient editing.

    document.getElementById("requestedCheckIn").value =
        toDateTimeLocal(session.checkInTime);

    document.getElementById("requestedCheckOut").value =
        toDateTimeLocal(session.checkOutTime);
}

/* =========================================================
   SUBMIT EMPLOYEE CORRECTION
   ========================================================= */

async function submitEmployeeCorrection(event) {

    event.preventDefault();

    const message = document.getElementById("correctionMessage");

    const button = document.getElementById("submitCorrectionBtn");

    const sessionId = Number(
        document.getElementById("correctionSession").value
    );

    const session = employeeCorrectionSessions.find(
        item => Number(item.id) === sessionId
    );

    if (!session) {
        message.textContent = "Please select an attendance session.";
        return;
    }

    const requestedCheckInValue =
        document.getElementById("requestedCheckIn").value;

    const requestedCheckOutValue =
        document.getElementById("requestedCheckOut").value;

    const reason = document.getElementById(
        "correctionReason"
    ).value.trim();

    if (!reason) {
        message.textContent = "Correction reason is required.";
        return;
    }

    const requestedCheckInTime = requestedCheckInValue
        ? new Date(requestedCheckInValue).toISOString()
        : null;

    const requestedCheckOutTime = requestedCheckOutValue
        ? new Date(requestedCheckOutValue).toISOString()
        : null;

    if (!requestedCheckInTime && !requestedCheckOutTime) {

        message.textContent =
            "Enter at least one requested attendance time.";

        return;
    }

    const payload = {

        attendanceSessionId: sessionId,

        employeeId: Number(EMPLOYEE_ID),

        reason: reason,

        requestedCheckInTime: requestedCheckInTime,

        requestedCheckOutTime: requestedCheckOutTime

    };

    button.disabled = true;

    message.textContent = "Submitting correction request...";

    try {

        await apiRequest(CORRECTION_API, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(payload)

        });

        message.textContent =
            "Correction request submitted successfully.";

        document.getElementById("correctionForm").reset();

        document.getElementById(
            "originalAttendanceDetails"
        ).innerHTML = "";

        await loadEmployeeCorrectionData();

    } catch (error) {

        console.error("Correction submission failed:", error);

        message.textContent =
            error.message || "Unable to submit correction request.";

    } finally {

        button.disabled = false;
    }
}

/* =========================================================
   EMPLOYEE REQUEST HISTORY
   ========================================================= */

function renderEmployeeCorrectionHistory() {

    const container = document.getElementById(
        "employeeCorrectionHistory"
    );

    if (!container) return;

    if (employeeCorrectionRequests.length === 0) {

        container.innerHTML =
            "<p>No correction requests submitted yet.</p>";

        return;
    }

    container.innerHTML = `

        <div class="table-responsive">

            <table>

                <thead>

                    <tr>

                        <th>Request ID</th>

                        <th>Session ID</th>

                        <th>Reason</th>

                        <th>Original Check-in</th>

                        <th>Original Check-out</th>

                        <th>Requested Check-in</th>

                        <th>Requested Check-out</th>

                        <th>Status</th>

                        <th>Reviewed At</th>

                    </tr>

                </thead>

                <tbody>

                    ${employeeCorrectionRequests.map(request => `

                        <tr>

                            <td>${Number(request.id)}</td>

                            <td>${Number(request.attendanceSessionId)}</td>

                            <td>${escapeCorrectionHTML(request.reason)}</td>

                            <td>${escapeCorrectionHTML(formatCorrectionDate(request.originalCheckInTime))}</td>

                            <td>${escapeCorrectionHTML(formatCorrectionDate(request.originalCheckOutTime))}</td>

                            <td>${escapeCorrectionHTML(formatCorrectionDate(request.requestedCheckInTime))}</td>

                            <td>${escapeCorrectionHTML(formatCorrectionDate(request.requestedCheckOutTime))}</td>

                            <td>${escapeCorrectionHTML(request.status)}</td>

                            <td>${escapeCorrectionHTML(formatCorrectionDate(request.reviewedAt))}</td>

                        </tr>

                    `).join("")}

                </tbody>

            </table>

        </div>

    `;
}

/* =========================================================
   MANAGER LAYOUT
   ========================================================= */

function renderManagerCorrectionLayout() {

    const container = document.getElementById("correctionSection");

    container.innerHTML = `

        <div class="card">

            <h3>Pending Team Correction Requests</h3>

            <button
                type="button"
                id="refreshManagerCorrectionsBtn"
                class="secondary-btn">

                Refresh Requests

            </button>

            <div id="managerCorrectionList">
                Loading pending requests...
            </div>

        </div>

    `;

    document.getElementById("refreshManagerCorrectionsBtn")
        .addEventListener("click", loadManagerCorrectionRequests);
}

/* =========================================================
   LOAD MANAGER PENDING REQUESTS
   ========================================================= */

async function loadManagerCorrectionRequests() {

    const container = document.getElementById(
        "managerCorrectionList"
    );

    if (!container) return;

    try {

        const requests = await apiRequest(
            `${CORRECTION_API}/manager/${EMPLOYEE_ID}/pending`
        );

        if (!Array.isArray(requests)) {
            throw new Error("Invalid manager correction response.");
        }

        managerCorrectionRequests = requests;

        renderManagerCorrectionRequests();

    } catch (error) {

        console.error("Manager correction loading failed:", error);

        container.textContent =
            "Unable to load pending requests: " + error.message;
    }
}

/* =========================================================
   MANAGER REQUEST LIST
   ========================================================= */

function renderManagerCorrectionRequests() {

    const container = document.getElementById(
        "managerCorrectionList"
    );

    if (!container) return;

    if (managerCorrectionRequests.length === 0) {

        container.innerHTML =
            "<p>No pending correction requests from your team.</p>";

        return;
    }

    container.innerHTML = managerCorrectionRequests.map(request => `

        <div class="card">

            <h4>Correction Request #${Number(request.id)}</h4>

            <p>
                <strong>Employee ID:</strong>
                ${Number(request.employeeId)}
            </p>

            <p>
                <strong>Attendance Session:</strong>
                ${Number(request.attendanceSessionId)}
            </p>

            <p>
                <strong>Reason:</strong>
                ${escapeCorrectionHTML(request.reason)}
            </p>

            <p>
                <strong>Original Check-in:</strong>
                ${escapeCorrectionHTML(formatCorrectionDate(request.originalCheckInTime))}
            </p>

            <p>
                <strong>Original Check-out:</strong>
                ${escapeCorrectionHTML(formatCorrectionDate(request.originalCheckOutTime))}
            </p>

            <p>
                <strong>Requested Check-in:</strong>
                ${escapeCorrectionHTML(formatCorrectionDate(request.requestedCheckInTime))}
            </p>

            <p>
                <strong>Requested Check-out:</strong>
                ${escapeCorrectionHTML(formatCorrectionDate(request.requestedCheckOutTime))}
            </p>

            <p>
                <strong>Submitted:</strong>
                ${escapeCorrectionHTML(formatCorrectionDate(request.requestedTime))}
            </p>

            <button
                class="primary-btn"
                type="button"
                onclick="reviewCorrection(${Number(request.id)}, 'approve')">

                Approve

            </button>

            <button
                class="danger-btn"
                type="button"
                onclick="reviewCorrection(${Number(request.id)}, 'reject')">

                Reject

            </button>

        </div>

    `).join("");
}

/* =========================================================
   MANAGER APPROVE / REJECT
   ========================================================= */

async function reviewCorrection(requestId, action) {

    const actionText = action === "approve"
        ? "approve"
        : "reject";

    if (!confirm(`Are you sure you want to ${actionText} this correction?`)) {
        return;
    }

    try {

        await apiRequest(
            `${CORRECTION_API}/${requestId}/${action}/${EMPLOYEE_ID}`,
            {
                method: "PUT"
            }
        );

        alert(`Correction request ${actionText}d successfully.`);

        await loadManagerCorrectionRequests();

    } catch (error) {

        console.error("Correction review failed:", error);

        alert(
            error.message ||
            `Unable to ${actionText} correction request.`
        );
    }
}

/* =========================================================
   FORMATTERS
   ========================================================= */

function formatCorrectionDate(value) {

    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "-";

    return date.toLocaleString();
}

function toDateTimeLocal(value) {

    if (!value) return "";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "";

    const localDate = new Date(
        date.getTime() - date.getTimezoneOffset() * 60000
    );

    return localDate.toISOString().slice(0, 16);
}

function escapeCorrectionHTML(value) {

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
   MODULE INITIALIZATION
   ========================================================= */

console.log("Attendance correction module loaded.");

/* =========================================================
   NAVIGATION INTEGRATION
   ========================================================= */

document.addEventListener("DOMContentLoaded", function () {

    const correctionButton = document.querySelector(
        '.nav-btn[data-section="corrections"]'
    );

    if (correctionButton) {

        correctionButton.addEventListener("click", function () {
            loadCorrectionSection();
        });

    }

});