
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   MANAGER MODULE
   File: manager.js
   ========================================================= */


/* =========================================================
   1. LOAD MANAGER DASHBOARD
   ========================================================= */

async function loadManagerDashboard() {

    if (!CURRENT_EMPLOYEE || !EMPLOYEE_ID) {

        throw new Error(
            "Manager information is unavailable."
        );
    }

    const manager = CURRENT_EMPLOYEE;
    ensureManagerDashboardContainers();

    const welcomeElement =
        document.getElementById("managerWelcomeMessage");

    if (welcomeElement) {

        welcomeElement.textContent =
            "Welcome, " + manager.name + "!";
    }

    renderManagerProfile(manager);

    await loadManagerTeam();
    await loadManagerFieldVisits();

    console.log("Manager dashboard loaded.");
}


/* =========================================================
   2. RENDER MANAGER PROFILE
   ========================================================= */

function renderManagerProfile(manager) {

    const profileContainer =
        document.getElementById("managerProfileSection");

    if (!profileContainer) {
        return;
    }

    profileContainer.replaceChildren();

    const card = document.createElement("div");

    card.className = "card";

    const heading = document.createElement("h3");

    heading.textContent = "Manager Profile";

    card.appendChild(heading);

    addManagerProfileDetail(
        card,
        "Employee ID",
        manager.id
    );

    addManagerProfileDetail(
        card,
        "Name",
        manager.name
    );

    addManagerProfileDetail(
        card,
        "Email",
        manager.email
    );

    addManagerProfileDetail(card, "Department", manager.department);
    addManagerProfileDetail(card, "Designation", manager.designation || manager.jobTitle || manager.position);
    addManagerProfileDetail(card, "Team", manager.teamName || manager.team?.name);

    profileContainer.appendChild(card);
}


/* =========================================================
   3. MANAGER PROFILE DETAIL
   ========================================================= */

function addManagerProfileDetail(container, label, value) {

    const paragraph = document.createElement("p");

    paragraph.textContent =
        label + ": " +
        (
            value !== null &&
            value !== undefined &&
            value !== ""
                ? value
                : "Not assigned"
        );

    container.appendChild(paragraph);
}


/* =========================================================
   4. LOAD MANAGER TEAM
   ========================================================= */

async function loadManagerTeam() {

    const teamContainer =
        document.getElementById("managerTeamSection");

    if (!teamContainer) {
        return;
    }

    teamContainer.textContent = "Loading team members...";

    try {

        const employees = await apiRequest(
            "/api/employees"
        );

        if (!Array.isArray(employees)) {

            throw new Error(
                "Invalid employee data received."
            );
        }

        const managerId = Number(EMPLOYEE_ID);

        const teamMembers = employees.filter(employee => {

            return (
                employee.active !== false &&
                Number(employee.managerId) === managerId
            );

        });

        renderManagerTeam(teamMembers);

    } catch (error) {

        console.error(
            "Unable to load manager team:",
            error
        );

        teamContainer.textContent =
            "Unable to load team members: " +
            error.message;
    }
}


/* =========================================================
   5. RENDER MANAGER TEAM
   ========================================================= */

function renderManagerTeam(teamMembers) {

    const teamContainer =
        document.getElementById("managerTeamSection");

    if (!teamContainer) {
        return;
    }

    teamContainer.replaceChildren();

    const heading = document.createElement("h3");

    heading.textContent = "My Team";

    teamContainer.appendChild(heading);

    const count = document.createElement("p");

    count.textContent =
        "Total team members: " + teamMembers.length;

    teamContainer.appendChild(count);

    if (teamMembers.length === 0) {

        const emptyMessage = document.createElement("p");

        emptyMessage.textContent =
            "No team members are currently assigned to you.";

        teamContainer.appendChild(emptyMessage);

        return;
    }

    const table = document.createElement("table");

    table.className = "table";

    const thead = document.createElement("thead");

    const headerRow = document.createElement("tr");

    const headers = [
        "Employee ID",
        "Employee Code",
        "Name",
        "Email",
        "Department",
        "Status"
    ];

    headers.forEach(headerText => {

        const th = document.createElement("th");

        th.textContent = headerText;

        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    teamMembers.forEach(employee => {

        const row = document.createElement("tr");

        appendManagerTableCell(row, employee.id);

        appendManagerTableCell(
            row,
            employee.employeeCode
        );

        appendManagerTableCell(
            row,
            employee.name
        );

        appendManagerTableCell(
            row,
            employee.email
        );

        appendManagerTableCell(
            row,
            employee.department
        );

        appendManagerTableCell(
            row,
            employee.active === false
                ? "Inactive"
                : "Active"
        );

        tbody.appendChild(row);
    });

    table.appendChild(thead);

    table.appendChild(tbody);

    teamContainer.appendChild(table);
}


/* =========================================================
   6. CREATE TABLE CELL
   ========================================================= */

function appendManagerTableCell(row, value) {

    const cell = document.createElement("td");

    cell.textContent =
        value !== null && value !== undefined
            ? String(value)
            : "-";

    row.appendChild(cell);
}


/* =========================================================
   7. REFRESH MANAGER DASHBOARD
   ========================================================= */

async function refreshManagerDashboard() {

    await refreshEmployeeProfile();

    await loadManagerTeam();
}


/* =========================================================
   8. MODULE INITIALIZATION
   ========================================================= */

console.log("Manager module loaded.");


function ensureManagerDashboardContainers() {
    const root = document.getElementById("managerDashboardSection") || document.getElementById("dashboard");
    if (!root) return;
    ["managerProfileSection", "managerTeamSection", "managerVisitSummary"].forEach(id => {
        if (!document.getElementById(id)) {
            const div = document.createElement("div"); div.id = id; root.appendChild(div);
        }
    });
}

async function loadManagerFieldVisits() {
    const container = document.getElementById("managerVisitSummary");
    if (!container || !EMPLOYEE_ID) return;
    container.replaceChildren();
    const heading = document.createElement("h3"); heading.textContent = "Team Field Visits";
    const note = document.createElement("p"); note.textContent = "Field visit activity for your assigned team.";
    container.append(heading,note);
    const today = new Date().toISOString().slice(0,10);
    try {
        const data = await apiRequest(`/api/reports/field-visits?startDate=${today}&endDate=${today}`);
        renderManagerVisitSummary(data, container);
    } catch (e) {
        const msg = document.createElement("p"); msg.textContent = "Unable to load team field-visit summary: " + e.message; container.appendChild(msg);
    }
}

function renderManagerVisitSummary(data, container) {
    const metrics = [
        ["Total visits", data.totalVisits], ["Completed", data.completedVisits],
        ["Incomplete", data.incompleteVisits], ["Duration (seconds)", data.totalVisitDurationSeconds]
    ];
    const row = document.createElement("div"); row.className = "sync-summary";
    metrics.forEach(([label,value]) => { const card=document.createElement("div"); card.className="summary-item"; const n=document.createElement("h3"); n.textContent=value ?? 0; const l=document.createElement("p"); l.textContent=label; card.append(n,l); row.appendChild(card); });
    container.appendChild(row);
}
