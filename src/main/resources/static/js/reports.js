
/* =========================================================
   AUDITOR REPORTS WORKSPACE
   Daily Attendance
   Monthly Attendance
   Field Visit Summary
   GPS Anomaly Report
   Audit History
   ========================================================= */

async function loadAuditorDashboard() {
    const root = document.getElementById("reportsContent");
    if (!root) return;

    root.replaceChildren();

    const title = document.getElementById("dashboardTitle");
    if (title) {
        title.textContent = "Auditor Portal";
    }

    await loadAuditorReports();
}

async function loadAuditorReports() {
    const root = document.getElementById("reportsContent");
    if (!root) return;

    root.replaceChildren();

    const today = getLocalDateValue();
    const month = today.slice(0, 7);

    const intro = document.createElement("div");
    intro.className = "card";
    intro.innerHTML = `
        <h2>Reports & Audit History</h2>
        <p>Read-only attendance, field visit, GPS anomaly and audit reports.</p>
    `;
    root.appendChild(intro);

    createDailyAttendanceSection(root, today);
    createMonthlyAttendanceSection(root, month);
    createFieldVisitSection(root, today);
    createGpsAnomalySection(root, today);
    createAuditHistorySection(root, today);
}

/* =========================================================
   DAILY ATTENDANCE REPORT
   Filter: attendance date
   ========================================================= */

function createDailyAttendanceSection(root, today) {
    const card = createReportCard(
        root,
        "Daily Attendance Report",
        "View attendance for a selected date."
    );

    const date = createInput("date", today);
    const load = createButton("Load report", "primary-btn");
    const result = createResultArea(card);

    card.querySelector(".report-controls").append(
        createLabeledControl("Attendance date", date),
        load
    );

    let page = 0;
    const size = 10;

    async function fetchReport() {
        if (!date.value) {
            showMessage(result, "Select an attendance date.", "error");
            return;
        }

        const url =
            `/api/reports/daily?date=${encodeURIComponent(date.value)}` +
            `&page=${page}&size=${size}`;

        await loadPagedReport(
            url,
            result,
            page,
            nextPage,
            previousPage,
            "Daily attendance"
        );
    }

    function nextPage() {
        page++;
        fetchReport();
    }

    function previousPage() {
        if (page > 0) {
            page--;
            fetchReport();
        }
    }

    load.addEventListener("click", () => {
        page = 0;
        fetchReport();
    });

    fetchReport();
}

/* =========================================================
   MONTHLY ATTENDANCE REPORT
   Filters: month and employee ID
   ========================================================= */

function createMonthlyAttendanceSection(root, defaultMonth) {
    const card = createReportCard(
        root,
        "Monthly Attendance Report",
        "View working days, attendance, late arrivals and working duration for an employee."
    );

    const month = createInput("month", defaultMonth);
    const employeeId = createInput("number", "");
    employeeId.min = "1";
    employeeId.placeholder = "Enter employee ID";

    const load = createButton("Load report", "primary-btn");
    const result = createResultArea(card);

    card.querySelector(".report-controls").append(
        createLabeledControl("Month", month),
        createLabeledControl("Employee ID", employeeId),
        load
    );

    async function fetchReport() {
        if (!month.value || !employeeId.value) {
            showMessage(
                result,
                "Select a month and enter an employee ID.",
                "error"
            );
            return;
        }

        if (Number(employeeId.value) <= 0) {
            showMessage(result, "Enter a valid employee ID.", "error");
            return;
        }

        const url =
            `/api/reports/monthly?employeeId=${encodeURIComponent(employeeId.value)}` +
            `&month=${encodeURIComponent(month.value)}`;

        await loadSimpleReport(url, result, "Monthly attendance");
    }

    load.addEventListener("click", fetchReport);
}

/* =========================================================
   FIELD VISIT SUMMARY
   Filters: start date, end date and optional employee ID
   ========================================================= */

function createFieldVisitSection(root, today) {
    const card = createReportCard(
        root,
        "Field Visit Summary",
        "Review completed and incomplete field visits for a selected period."
    );

    const startDate = createInput("date", today);
    const endDate = createInput("date", today);

    const employeeId = createInput("number", "");
    employeeId.min = "1";
    employeeId.placeholder = "All employees";

    const load = createButton("Load report", "primary-btn");
    const result = createResultArea(card);

    card.querySelector(".report-controls").append(
        createLabeledControl("Start date", startDate),
        createLabeledControl("End date", endDate),
        createLabeledControl("Employee ID (optional)", employeeId),
        load
    );

    async function fetchReport() {
        if (!validateDateRange(startDate.value, endDate.value, result)) {
            return;
        }

        if (employeeId.value && Number(employeeId.value) <= 0) {
            showMessage(result, "Enter a valid employee ID.", "error");
            return;
        }

        const params = new URLSearchParams({
            startDate: startDate.value,
            endDate: endDate.value
        });

        if (employeeId.value.trim()) {
            params.set("employeeId", employeeId.value.trim());
        }

        await loadSimpleReport(
            "/api/reports/field-visits?" + params.toString(),
            result,
            "Field visit summary"
        );
    }

    load.addEventListener("click", fetchReport);
    fetchReport();
}

/* =========================================================
   GPS ANOMALY REPORT
   Filters: date range and severity
   ========================================================= */

function createGpsAnomalySection(root, today) {
    const card = createReportCard(
        root,
        "GPS Anomaly Report",
        "Review GPS-related anomalies and attendance irregularities."
    );

    const startDate = createInput("date", today);
    const endDate = createInput("date", today);

    const severity = document.createElement("select");

    addOption(severity, "", "All severities");
    addOption(severity, "LOW", "Low");
    addOption(severity, "MEDIUM", "Medium");
    addOption(severity, "HIGH", "High");

    const load = createButton("Load report", "primary-btn");
    const result = createResultArea(card);

    card.querySelector(".report-controls").append(
        createLabeledControl("Start date", startDate),
        createLabeledControl("End date", endDate),
        createLabeledControl("Severity", severity),
        load
    );

    let page = 0;
    const size = 10;

    async function fetchReport() {
        if (!validateDateRange(startDate.value, endDate.value, result)) {
            return;
        }

        const params = new URLSearchParams({
            startDate: startDate.value,
            endDate: endDate.value,
            page: String(page),
            size: String(size)
        });

        if (severity.value) {
            params.set("severity", severity.value);
        }

        const url = "/api/reports/gps-anomalies?" + params.toString();

        await loadPagedReport(
            url,
            result,
            page,
            nextPage,
            previousPage,
            "GPS anomaly report"
        );
    }

    function nextPage() {
        page++;
        fetchReport();
    }

    function previousPage() {
        if (page > 0) {
            page--;
            fetchReport();
        }
    }

    load.addEventListener("click", () => {
        page = 0;
        fetchReport();
    });

    fetchReport();
}

/* =========================================================
   AUDIT HISTORY
   Filters: date range, action and performed-by username
   ========================================================= */

function createAuditHistorySection(root, today) {
    const card = createReportCard(
        root,
        "Audit History",
        "Read-only history of important actions performed in the system."
    );

    const startDate = createInput("date", today);
    const endDate = createInput("date", today);

    const action = document.createElement("select");

    addOption(action, "", "All actions");
    addOption(action, "CORRECTION_SUBMITTED", "Correction submitted");
    addOption(action, "CORRECTION_APPROVED", "Correction approved");
    addOption(action, "CORRECTION_REJECTED", "Correction rejected");
    addOption(action, "ANOMALY_REVIEWED", "Anomaly reviewed");
    addOption(action, "FIELD_VISIT_CORRECTED", "Field visit corrected");

    const performedBy = createInput("text", "");
    performedBy.placeholder = "Enter username";

    const load = createButton("Search audit history", "primary-btn");
    const reset = createButton("Reset filters", "secondary-btn");

    const result = createResultArea(card);

    card.querySelector(".report-controls").append(
        createLabeledControl("Start date", startDate),
        createLabeledControl("End date", endDate),
        createLabeledControl("Action type", action),
        createLabeledControl("Performed by", performedBy),
        load,
        reset
    );

    let page = 0;
    const size = 10;

    async function fetchAuditHistory() {
        if (!validateDateRange(startDate.value, endDate.value, result)) {
            return;
        }

        const params = new URLSearchParams({
            startDate: startDate.value,
            endDate: endDate.value,
            page: String(page),
            size: String(size)
        });

        if (action.value) {
            params.set("action", action.value);
        }

        if (performedBy.value.trim()) {
            params.set("performedBy", performedBy.value.trim());
        }

        await loadAuditPage(
            "/api/audit-records?" + params.toString(),
            result,
            page,
            nextPage,
            previousPage
        );
    }

    function nextPage() {
        page++;
        fetchAuditHistory();
    }

    function previousPage() {
        if (page > 0) {
            page--;
            fetchAuditHistory();
        }
    }

    load.addEventListener("click", () => {
        page = 0;
        fetchAuditHistory();
    });

    reset.addEventListener("click", () => {
        startDate.value = today;
        endDate.value = today;
        action.value = "";
        performedBy.value = "";
        page = 0;
        fetchAuditHistory();
    });

    fetchAuditHistory();
}

/* =========================================================
   REPORT LOADING
   ========================================================= */

async function loadSimpleReport(url, result, reportName) {
    setLoading(result);

    try {
        const data = await apiRequest(url);
        renderReportObject(data, result);
    } catch (error) {
        showMessage(
            result,
            `Unable to load ${reportName}: ${error.message}`,
            "error"
        );
    }
}

async function loadPagedReport(
    url,
    result,
    page,
    nextPage,
    previousPage,
    reportName
) {
    setLoading(result);

    try {
        const data = await apiRequest(url);
        renderReportObject(data, result);

        const totalPages = Number(data.totalPages || 0);
        const totalElements = Number(data.totalElements || 0);

        addPagination(
            result,
            page,
            totalPages,
            totalElements,
            previousPage,
            nextPage
        );
    } catch (error) {
        showMessage(
            result,
            `Unable to load ${reportName}: ${error.message}`,
            "error"
        );
    }
}

async function loadAuditPage(
    url,
    result,
    page,
    nextPage,
    previousPage
) {
    setLoading(result);

    try {
        const data = await apiRequest(url);
        renderAuditTable(data, result);

        addPagination(
            result,
            page,
            Number(data.totalPages || 0),
            Number(data.totalElements || 0),
            previousPage,
            nextPage
        );
    } catch (error) {
        showMessage(
            result,
            `Unable to load audit history: ${error.message}`,
            "error"
        );
    }
}

/* =========================================================
   AUDIT TABLE
   ========================================================= */

function renderAuditTable(data, container) {
    container.replaceChildren();

    const records = data && Array.isArray(data.content)
        ? data.content
        : [];

    if (records.length === 0) {
        showMessage(
            container,
            "No audit records found for the selected filters.",
            "info"
        );
        return;
    }

    const columns = [
        ["id", "Audit ID"],
        ["actorUsername", "Performed By"],
        ["action", "Action"],
        ["targetRecord", "Target"],
        ["timestamp", "Date & Time"],
        ["relevantChanges", "Changes"],
        ["requestCorrelationId", "Correlation ID"]
    ];

    const table = document.createElement("table");
    table.className = "data-table";

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    columns.forEach(([, label]) => {
        const th = document.createElement("th");
        th.textContent = label;
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");

    records.forEach(record => {
        const row = document.createElement("tr");

        columns.forEach(([key]) => {
            const cell = document.createElement("td");
            const value = record[key];

            if (key === "timestamp" && value) {
                cell.textContent = formatReportTimestamp(value);
            } else {
                cell.textContent =
                    value === null || value === undefined || value === ""
                        ? "—"
                        : String(value);
            }

            row.appendChild(cell);
        });

        tbody.appendChild(row);
    });

    table.appendChild(tbody);

    const wrapper = document.createElement("div");
    wrapper.className = "table-container";
    wrapper.appendChild(table);

    container.appendChild(wrapper);
}

/* =========================================================
   GENERIC REPORT DISPLAY
   ========================================================= */

function renderReportObject(data, container) {
    container.replaceChildren();

    if (!data || typeof data !== "object") {
        showMessage(container, "No report data available.", "info");
        return;
    }

    const scalarEntries = Object.entries(data).filter(
        ([, value]) =>
            !Array.isArray(value) &&
            value !== null &&
            typeof value !== "object"
    );

    if (scalarEntries.length > 0) {
        const summary = document.createElement("div");
        summary.className = "sync-summary";

        scalarEntries.forEach(([key, value]) => {
            const item = document.createElement("div");
            item.className = "summary-item";

            const number = document.createElement("h3");
            number.textContent = value ?? "—";

            const label = document.createElement("p");
            label.textContent = formatLabel(key);

            item.append(number, label);
            summary.appendChild(item);
        });

        container.appendChild(summary);
    }

    const arrays = Object.entries(data).filter(
        ([, value]) => Array.isArray(value)
    );

    arrays.forEach(([key, rows]) => {
        const heading = document.createElement("h4");
        heading.textContent = formatLabel(key);
        container.appendChild(heading);

        if (rows.length === 0) {
            showMessage(container, "No records found.", "info");
            return;
        }

        renderObjectTable(rows, container);
    });

    if (scalarEntries.length === 0 && arrays.length === 0) {
        showMessage(container, "No report data available.", "info");
    }
}

function renderObjectTable(rows, container) {
    const columns = Object.keys(rows[0]);

    const table = document.createElement("table");
    table.className = "data-table";

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    columns.forEach(column => {
        const th = document.createElement("th");
        th.textContent = formatLabel(column);
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");

    rows.forEach(row => {
        const tr = document.createElement("tr");

        columns.forEach(column => {
            const td = document.createElement("td");
            const value = row[column];

            td.textContent =
                value === null || value === undefined
                    ? "—"
                    : typeof value === "object"
                        ? JSON.stringify(value)
                        : String(value);

            tr.appendChild(td);
        });

        tbody.appendChild(tr);
    });

    table.appendChild(tbody);

    const wrapper = document.createElement("div");
    wrapper.className = "table-container";
    wrapper.appendChild(table);

    container.appendChild(wrapper);
}

/* =========================================================
   COMMON UI HELPERS
   ========================================================= */

function createReportCard(root, titleText, descriptionText) {
    const card = document.createElement("section");
    card.className = "card report-card";

    const heading = document.createElement("h3");
    heading.textContent = titleText;

    const description = document.createElement("p");
    description.className = "section-description";
    description.textContent = descriptionText;

    const controls = document.createElement("div");
    controls.className = "form-grid report-controls";

    card.append(heading, description, controls);
    root.appendChild(card);

    return card;
}

function createResultArea(card) {
    const result = document.createElement("div");
    result.className = "report-result";
    result.style.marginTop = "20px";
    card.appendChild(result);
    return result;
}

function createInput(type, value) {
    const input = document.createElement("input");
    input.type = type;
    input.value = value || "";
    return input;
}

function createButton(text, className) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.textContent = text;
    return button;
}

function createLabeledControl(labelText, control) {
    const wrapper = document.createElement("div");
    wrapper.className = "form-group";

    const label = document.createElement("label");
    label.textContent = labelText;

    wrapper.append(label, control);
    return wrapper;
}

function addOption(select, value, label) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    select.appendChild(option);
}

function setLoading(container) {
    container.replaceChildren();

    const message = document.createElement("p");
    message.className = "section-description";
    message.textContent = "Loading report...";

    container.appendChild(message);
}

function showMessage(container, message, type) {
    const paragraph = document.createElement("p");

    paragraph.textContent = message;
    paragraph.className =
        type === "error"
            ? "error-message"
            : "section-description";

    container.appendChild(paragraph);
}

function validateDateRange(start, end, result) {
    if (!start || !end) {
        showMessage(result, "Select both start and end dates.", "error");
        return false;
    }

    if (start > end) {
        showMessage(
            result,
            "Start date cannot be after end date.",
            "error"
        );
        return false;
    }

    return true;
}

function addPagination(
    container,
    page,
    totalPages,
    totalElements,
    previousPage,
    nextPage
) {
    const pagination = document.createElement("div");
    pagination.className = "report-pagination";

    const previous = createButton("Previous", "secondary-btn");
    const next = createButton("Next", "secondary-btn");

    previous.disabled = page <= 0;
    next.disabled = totalPages === 0 || page >= totalPages - 1;

    previous.addEventListener("click", previousPage);
    next.addEventListener("click", nextPage);

    const info = document.createElement("span");
    info.textContent =
        `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} ` +
        `• ${totalElements} record(s)`;

    pagination.append(previous, info, next);
    container.appendChild(pagination);
}

function getLocalDateValue() {
    const now = new Date();

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function formatLabel(value) {
    return String(value)
        .replace(/([A-Z])/g, " $1")
        .replace(/_/g, " ")
        .replace(/^./, character => character.toUpperCase());
}

function formatReportTimestamp(value) {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: true,
        timeZone: "Asia/Kolkata"
    });
}
