/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   MODULE D: SHIFTS AND EMPLOYEE ROSTERS
   File: shift.js

   Backend APIs:
   Shifts:   /api/shifts
   Rosters:  /api/rosters
   Employees (ADMIN): /api/v1/admin/employees
   ========================================================= */

const SHIFT_API = "/api/shifts";
const ROSTER_API = "/api/rosters";
const SHIFT_EMPLOYEE_API = "/api/v1/admin/employees";

let availableShifts = [];
let shiftEmployees = [];
let employeeRosters = [];

let editingShiftId = null;
let editingRosterId = null;

let shiftSectionInitialized = false;
let shiftSectionLoading = false;

/* ---------------------------------------------------------
   SECTION INITIALIZATION
   --------------------------------------------------------- */

document.addEventListener("DOMContentLoaded", () => {
    document
        .querySelectorAll('.nav-btn[data-section="shifts"]')
        .forEach(button => {
            button.addEventListener("click", () => {
                loadShiftSection();
            });
        });
});

async function loadShiftSection() {
    const container = document.getElementById("shiftSection");

    if (!container || shiftSectionLoading) {
        return;
    }

    shiftSectionLoading = true;

    try {
        if (!shiftSectionInitialized) {
            renderShiftSectionShell(container);
            shiftSectionInitialized = true;
        }

        await loadShifts();

        if (CURRENT_ROLE === "ADMIN") {
            await loadRosterManagementData();
        } else if (CURRENT_ROLE === "EMPLOYEE" && EMPLOYEE_ID) {
            await loadCurrentEmployeeRoster();
        } else {
            renderRosterAccessMessage();
        }

    } catch (error) {
        console.error("Unable to load Shifts & Rosters:", error);

        showShiftMessage(
            "Unable to load this section: " + error.message,
            true
        );

    } finally {
        shiftSectionLoading = false;
    }
}

function renderShiftSectionShell(container) {
    container.replaceChildren();

    /* SHIFT CONFIGURATION CARD */

    const shiftCard = document.createElement("section");
    shiftCard.className = "card";

    const shiftHeading = document.createElement("h3");
    shiftHeading.textContent = "Shift Configuration";

    shiftCard.appendChild(shiftHeading);

    const shiftMessage = document.createElement("p");
    shiftMessage.id = "shiftMessage";
    shiftMessage.setAttribute("role", "status");

    shiftCard.appendChild(shiftMessage);

    const shiftActions = document.createElement("div");
    shiftActions.className = "employee-toolbar";

    if (CURRENT_ROLE === "ADMIN") {
        shiftActions.appendChild(
            createShiftButton(
                "Add Shift",
                "primary-btn",
                openNewShiftForm
            )
        );
    }

    shiftActions.appendChild(
        createShiftButton(
            "Refresh",
            "secondary-btn",
            loadShiftSection
        )
    );

    shiftCard.appendChild(shiftActions);

    if (CURRENT_ROLE === "ADMIN") {
        const formCard = document.createElement("div");

        formCard.id = "shiftFormCard";
        formCard.className = "card";
        formCard.hidden = true;

        shiftCard.appendChild(formCard);
    }

    const shiftTableContainer = document.createElement("div");
    shiftTableContainer.id = "shiftTableContainer";

    shiftCard.appendChild(shiftTableContainer);

    container.appendChild(shiftCard);

    /* ROSTER CARD */

    const rosterCard = document.createElement("section");
    rosterCard.className = "card";

    const rosterHeading = document.createElement("h3");

    rosterHeading.textContent =
        CURRENT_ROLE === "ADMIN"
            ? "Employee Roster Management"
            : "My Roster";

    rosterCard.appendChild(rosterHeading);

    const rosterMessage = document.createElement("p");
    rosterMessage.id = "rosterMessage";
    rosterMessage.setAttribute("role", "status");

    rosterCard.appendChild(rosterMessage);

    if (CURRENT_ROLE === "ADMIN") {

        const rosterFormCard = document.createElement("div");

        rosterFormCard.id = "rosterFormCard";
        rosterFormCard.className = "card";
        rosterFormCard.hidden = true;

        rosterCard.appendChild(rosterFormCard);

        const rosterActions = document.createElement("div");
        rosterActions.className = "employee-toolbar";

        rosterActions.appendChild(
            createShiftButton(
                "Assign Roster",
                "primary-btn",
                openNewRosterForm
            )
        );

        rosterActions.appendChild(
            createShiftButton(
                "Refresh Rosters",
                "secondary-btn",
                loadRosterManagementData
            )
        );

        rosterCard.appendChild(rosterActions);
    }

    const rosterTableContainer = document.createElement("div");
    rosterTableContainer.id = "rosterTableContainer";

    rosterCard.appendChild(rosterTableContainer);

    container.appendChild(rosterCard);
}

/* ---------------------------------------------------------
   SHIFT CRUD
   --------------------------------------------------------- */

async function loadShifts() {

    const shifts = await apiRequest(SHIFT_API);

    if (!Array.isArray(shifts)) {
        throw new Error(
            "The server returned an invalid shift list."
        );
    }

    availableShifts = shifts;

    renderShifts();
}

function renderShifts() {

    const container =
        document.getElementById("shiftTableContainer");

    if (!container) {
        return;
    }

    container.replaceChildren();

    if (availableShifts.length === 0) {

        const empty = document.createElement("p");

        empty.textContent =
            "No shifts have been configured yet.";

        container.appendChild(empty);

        return;
    }

    const table = createShiftTable([
        "Shift ID",
        "Shift Name",
        "Start Time",
        "End Time",
        "Late Arrival Grace (minutes)",
        "Status",
        "Actions"
    ]);

    const tbody = table.querySelector("tbody");

    availableShifts.forEach(shift => {

        const row = document.createElement("tr");

        appendShiftCell(row, shift.id);
        appendShiftCell(row, shift.name);

        appendShiftCell(
            row,
            formatShiftTime(shift.startTime)
        );

        appendShiftCell(
            row,
            formatShiftTime(shift.endTime)
        );

        appendShiftCell(
            row,
            shift.lateArrivalGraceMinutes ?? 0
        );

        appendShiftCell(
            row,
            shift.active ? "Active" : "Inactive"
        );

        const actions = document.createElement("td");

        if (CURRENT_ROLE === "ADMIN") {

            actions.appendChild(
                createShiftButton(
                    "Edit",
                    "secondary-btn",
                    () => openEditShiftForm(shift.id)
                )
            );

            actions.appendChild(
                createShiftButton(
                    shift.active ? "Deactivate" : "Activate",
                    shift.active
                        ? "danger-btn"
                        : "secondary-btn",
                    () => toggleShiftStatus(shift)
                )
            );

        } else {

            actions.textContent = "View only";

        }

        row.appendChild(actions);
        tbody.appendChild(row);
    });

    container.appendChild(table);
}

/* CREATE SHIFT */

function openNewShiftForm() {

    editingShiftId = null;

    renderShiftForm({
        name: "",
        startTime: "09:00",
        endTime: "18:00",
        lateArrivalGraceMinutes: 0
    });
}

/* EDIT SHIFT */

function openEditShiftForm(shiftId) {

    const shift = availableShifts.find(
        item => Number(item.id) === Number(shiftId)
    );

    if (!shift) {

        showShiftMessage(
            "The selected shift could not be found.",
            true
        );

        return;
    }

    editingShiftId = shift.id;

    renderShiftForm(shift);
}

/* SHIFT FORM */

function renderShiftForm(shift) {

    const card = document.getElementById("shiftFormCard");

    if (!card) {
        return;
    }

    card.replaceChildren();
    card.hidden = false;

    const heading = document.createElement("h4");

    heading.textContent =
        editingShiftId
            ? "Update Shift"
            : "Create Shift";

    card.appendChild(heading);

    const form = document.createElement("form");
    form.id = "shiftForm";

    form.appendChild(
        createShiftInput(
            "shiftName",
            "Shift Name",
            "text",
            shift.name,
            true,
            100
        )
    );

    form.appendChild(
        createShiftInput(
            "shiftStartTime",
            "Start Time",
            "time",
            formatShiftTime(shift.startTime),
            true
        )
    );

    form.appendChild(
        createShiftInput(
            "shiftEndTime",
            "End Time",
            "time",
            formatShiftTime(shift.endTime),
            true
        )
    );

    form.appendChild(
        createShiftInput(
            "shiftGraceMinutes",
            "Late Arrival Grace (minutes)",
            "number",
            shift.lateArrivalGraceMinutes ?? 0,
            true,
            null,
            0
        )
    );

    const actions = document.createElement("div");
    actions.className = "employee-toolbar";

    actions.appendChild(
        createShiftButton(
            "Save Shift",
            "primary-btn",
            saveShift,
            "submit"
        )
    );

    actions.appendChild(
        createShiftButton(
            "Cancel",
            "secondary-btn",
            closeShiftForm
        )
    );

    form.appendChild(actions);

    form.addEventListener("submit", event => {

        event.preventDefault();

        saveShift();

    });

    card.appendChild(form);
}

/* SAVE SHIFT */

async function saveShift() {

    const form = document.getElementById("shiftForm");

    if (!form || !form.reportValidity()) {
        return;
    }

    const payload = {

        name: document
            .getElementById("shiftName")
            .value
            .trim(),

        startTime: document
            .getElementById("shiftStartTime")
            .value,

        endTime: document
            .getElementById("shiftEndTime")
            .value,

        lateArrivalGraceMinutes: Number(
            document
                .getElementById("shiftGraceMinutes")
                .value
        )
    };

    if (
        !payload.name ||
        !payload.startTime ||
        !payload.endTime ||
        !Number.isInteger(payload.lateArrivalGraceMinutes) ||
        payload.lateArrivalGraceMinutes < 0
    ) {

        showShiftMessage(
            "Enter a valid shift name, start time, end time and non-negative grace period.",
            true
        );

        return;
    }

    try {

        if (editingShiftId === null) {

            await apiRequest(SHIFT_API, {
                method: "POST",
                body: JSON.stringify(payload)
            });

            showShiftMessage(
                "Shift created successfully."
            );

        } else {

            await apiRequest(
                `${SHIFT_API}/${editingShiftId}`,
                {
                    method: "PUT",
                    body: JSON.stringify(payload)
                }
            );

            showShiftMessage(
                "Shift updated successfully."
            );
        }

        closeShiftForm();

        await loadShifts();

    } catch (error) {

        showShiftMessage(
            "Unable to save shift: " + error.message,
            true
        );
    }
}

/* ACTIVATE / DEACTIVATE SHIFT */

async function toggleShiftStatus(shift) {

    const action = shift.active
        ? "deactivate"
        : "activate";

    const confirmed = window.confirm(
        `Are you sure you want to ${action} the shift "${shift.name}"?`
    );

    if (!confirmed) {
        return;
    }

    try {

        await apiRequest(
            `${SHIFT_API}/${shift.id}/${action}`,
            {
                method: "PATCH"
            }
        );

        showShiftMessage(
            `Shift ${action}d successfully.`
        );

        await loadShifts();

    } catch (error) {

        showShiftMessage(
            `Unable to ${action} shift: ` + error.message,
            true
        );
    }
}

/* CLOSE SHIFT FORM */

function closeShiftForm() {

    const card = document.getElementById("shiftFormCard");

    if (card) {

        card.hidden = true;
        card.replaceChildren();

    }

    editingShiftId = null;
}

/* ---------------------------------------------------------
   EMPLOYEE ROSTER MANAGEMENT (ADMIN)
   --------------------------------------------------------- */

async function loadRosterManagementData() {

    if (CURRENT_ROLE !== "ADMIN") {
        return;
    }

    try {

        const [rosters, employees] = await Promise.all([

            apiRequest(ROSTER_API),

            apiRequest(SHIFT_EMPLOYEE_API)

        ]);

        if (
            !Array.isArray(rosters) ||
            !Array.isArray(employees)
        ) {

            throw new Error(
                "The server returned invalid roster or employee data."
            );
        }

        employeeRosters = rosters;
        shiftEmployees = employees;

        renderAdminRosters();

    } catch (error) {

        showRosterMessage(
            "Unable to load roster data: " + error.message,
            true
        );
    }
}

/* DISPLAY ADMIN ROSTERS */

function renderAdminRosters() {

    const container =
        document.getElementById("rosterTableContainer");

    if (!container) {
        return;
    }

    container.replaceChildren();

    if (employeeRosters.length === 0) {

        const empty = document.createElement("p");

        empty.textContent =
            "No employee rosters have been created yet.";

        container.appendChild(empty);

        return;
    }

    const employeeById = new Map(
        shiftEmployees.map(
            employee => [
                Number(employee.id),
                employee
            ]
        )
    );

    const shiftById = new Map(
        availableShifts.map(
            shift => [
                Number(shift.id),
                shift
            ]
        )
    );

    const table = createShiftTable([
        "Roster ID",
        "Employee",
        "Roster Date",
        "Shift",
        "Day Type",
        "Actions"
    ]);

    const tbody = table.querySelector("tbody");

    [...employeeRosters]
        .sort((a, b) =>
            String(a.rosterDate).localeCompare(
                String(b.rosterDate)
            )
        )
        .forEach(roster => {

            const employee = employeeById.get(
                Number(roster.employeeId)
            );

            const shift = roster.shiftId == null
                ? null
                : shiftById.get(Number(roster.shiftId));

            const row = document.createElement("tr");

            appendShiftCell(row, roster.id);

            appendShiftCell(
                row,
                employee
                    ? `${employee.name} (${employee.employeeCode})`
                    : `Employee #${roster.employeeId}`
            );

            appendShiftCell(row, roster.rosterDate);

            appendShiftCell(
                row,
                roster.weeklyOff
                    ? "—"
                    : shift
                        ? shift.name
                        : `Shift #${roster.shiftId}`
            );

            appendShiftCell(
                row,
                roster.weeklyOff
                    ? "Weekly Off"
                    : "Working Day"
            );

            const actions = document.createElement("td");

            actions.appendChild(
                createShiftButton(
                    "Edit",
                    "secondary-btn",
                    () => openEditRosterForm(roster.id)
                )
            );

            row.appendChild(actions);

            tbody.appendChild(row);
        });

    container.appendChild(table);
}

/* CREATE ROSTER */

function openNewRosterForm() {

    editingRosterId = null;

    renderRosterForm({

        employeeId: "",

        shiftId: "",

        rosterDate: new Date()
            .toISOString()
            .slice(0, 10),

        weeklyOff: false

    });
}

/* EDIT ROSTER */

function openEditRosterForm(rosterId) {

    const roster = employeeRosters.find(
        item => Number(item.id) === Number(rosterId)
    );

    if (!roster) {

        showRosterMessage(
            "The selected roster could not be found.",
            true
        );

        return;
    }

    editingRosterId = roster.id;

    renderRosterForm(roster);
}

/* ROSTER FORM */

function renderRosterForm(roster) {

    const card = document.getElementById("rosterFormCard");

    if (!card) {
        return;
    }

    card.replaceChildren();
    card.hidden = false;

    const heading = document.createElement("h4");

    heading.textContent =
        editingRosterId
            ? "Update Employee Roster"
            : "Assign Employee Roster";

    card.appendChild(heading);

    const form = document.createElement("form");
    form.id = "rosterForm";

    /* EMPLOYEE SELECT */

    const employeeSelect = document.createElement("select");

    employeeSelect.id = "rosterEmployeeId";
    employeeSelect.required = true;

    employeeSelect.appendChild(
        new Option("Select employee", "")
    );

    shiftEmployees
        .filter(employee => employee.active !== false)
        .forEach(employee => {

            employeeSelect.appendChild(
                new Option(
                    `${employee.name} (${employee.employeeCode}) — ID ${employee.id}`,
                    String(employee.id)
                )
            );

        });

    employeeSelect.value =
        roster.employeeId == null
            ? ""
            : String(roster.employeeId);

    form.appendChild(
        createShiftField(
            "Employee",
            employeeSelect
        )
    );

    /* ROSTER DATE */

    const dateInput = document.createElement("input");

    dateInput.type = "date";
    dateInput.id = "rosterDate";
    dateInput.required = true;
    dateInput.value = roster.rosterDate || "";

    form.appendChild(
        createShiftField(
            "Roster Date",
            dateInput
        )
    );

    /* WEEKLY OFF */

    const weeklyOffLabel = document.createElement("label");

    weeklyOffLabel.className = "form-group";

    const weeklyOffCheckbox =
        document.createElement("input");

    weeklyOffCheckbox.type = "checkbox";
    weeklyOffCheckbox.id = "rosterWeeklyOff";

    weeklyOffCheckbox.checked =
        Boolean(roster.weeklyOff);

    weeklyOffLabel.append(
        weeklyOffCheckbox,
        document.createTextNode(" Weekly Off")
    );

    form.appendChild(weeklyOffLabel);

    /* SHIFT SELECT */

    const shiftSelect = document.createElement("select");

    shiftSelect.id = "rosterShiftId";

    shiftSelect.appendChild(
        new Option("Select active shift", "")
    );

    availableShifts
        .filter(shift => shift.active)
        .forEach(shift => {

            shiftSelect.appendChild(
                new Option(
                    `${shift.name} (${formatShiftTime(shift.startTime)}–${formatShiftTime(shift.endTime)})`,
                    String(shift.id)
                )
            );

        });

    shiftSelect.value =
        roster.shiftId == null
            ? ""
            : String(roster.shiftId);

    form.appendChild(
        createShiftField(
            "Shift",
            shiftSelect
        )
    );

    /* WEEKLY OFF VALIDATION */

    const updateShiftRequirement = () => {

        const isWeeklyOff =
            weeklyOffCheckbox.checked;

        shiftSelect.disabled = isWeeklyOff;

        shiftSelect.required = !isWeeklyOff;

        if (isWeeklyOff) {
            shiftSelect.value = "";
        }
    };

    weeklyOffCheckbox.addEventListener(
        "change",
        updateShiftRequirement
    );

    updateShiftRequirement();

    const note = document.createElement("p");

    note.className = "form-hint";

    note.textContent =
        "A weekly-off day must not have a shift. A working day must have an active shift.";

    form.appendChild(note);

    /* FORM ACTIONS */

    const actions = document.createElement("div");

    actions.className = "employee-toolbar";

    actions.appendChild(
        createShiftButton(
            "Save Roster",
            "primary-btn",
            saveRoster,
            "submit"
        )
    );

    actions.appendChild(
        createShiftButton(
            "Cancel",
            "secondary-btn",
            closeRosterForm
        )
    );

    form.appendChild(actions);

    form.addEventListener("submit", event => {

        event.preventDefault();

        saveRoster();

    });

    card.appendChild(form);
}

/* SAVE ROSTER */

async function saveRoster() {

    const form = document.getElementById("rosterForm");

    if (!form || !form.reportValidity()) {
        return;
    }

    const weeklyOff =
        document.getElementById("rosterWeeklyOff").checked;

    const payload = {

        employeeId: Number(
            document.getElementById("rosterEmployeeId").value
        ),

        rosterDate:
            document.getElementById("rosterDate").value,

        weeklyOff: weeklyOff,

        shiftId: weeklyOff
            ? null
            : Number(
                document.getElementById("rosterShiftId").value
            )

    };

    if (
        !payload.employeeId ||
        !payload.rosterDate ||
        (!weeklyOff && !payload.shiftId)
    ) {

        showRosterMessage(
            "Select an employee and date. Working days also require a shift.",
            true
        );

        return;
    }

    try {

        if (editingRosterId === null) {

            await apiRequest(
                ROSTER_API,
                {
                    method: "POST",
                    body: JSON.stringify(payload)
                }
            );

            showRosterMessage(
                "Employee roster created successfully."
            );

        } else {

            await apiRequest(
                `${ROSTER_API}/${editingRosterId}`,
                {
                    method: "PUT",
                    body: JSON.stringify(payload)
                }
            );

            showRosterMessage(
                "Employee roster updated successfully."
            );
        }

        closeRosterForm();

        await loadRosterManagementData();

    } catch (error) {

        showRosterMessage(
            "Unable to save roster: " + error.message,
            true
        );
    }
}

/* CLOSE ROSTER FORM */

function closeRosterForm() {

    const card = document.getElementById("rosterFormCard");

    if (card) {

        card.hidden = true;
        card.replaceChildren();

    }

    editingRosterId = null;
}

/* ---------------------------------------------------------
   EMPLOYEE ROSTER VIEW
   --------------------------------------------------------- */

async function loadCurrentEmployeeRoster() {

    const rosters = await apiRequest(
        `${ROSTER_API}/employee/${EMPLOYEE_ID}`
    );

    if (!Array.isArray(rosters)) {

        throw new Error(
            "The server returned an invalid employee roster list."
        );
    }

    employeeRosters = rosters;

    renderCurrentEmployeeRoster();
}

function renderCurrentEmployeeRoster() {

    const container =
        document.getElementById("rosterTableContainer");

    if (!container) {
        return;
    }

    container.replaceChildren();

    if (employeeRosters.length === 0) {

        const empty = document.createElement("p");

        empty.textContent =
            "No roster entries are available for your employee account.";

        container.appendChild(empty);

        return;
    }

    const shiftById = new Map(
        availableShifts.map(
            shift => [
                Number(shift.id),
                shift
            ]
        )
    );

    const table = createShiftTable([
        "Roster Date",
        "Shift",
        "Day Type"
    ]);

    const tbody = table.querySelector("tbody");

    employeeRosters.forEach(roster => {

        const shift = roster.shiftId == null
            ? null
            : shiftById.get(Number(roster.shiftId));

        const row = document.createElement("tr");

        appendShiftCell(
            row,
            roster.rosterDate
        );

        appendShiftCell(
            row,
            roster.weeklyOff
                ? "—"
                : shift
                    ? `${shift.name} (${formatShiftTime(shift.startTime)}–${formatShiftTime(shift.endTime)})`
                    : "Shift not found"
        );

        appendShiftCell(
            row,
            roster.weeklyOff
                ? "Weekly Off"
                : "Working Day"
        );

        tbody.appendChild(row);
    });

    container.appendChild(table);
}

/* ---------------------------------------------------------
   ROLE ACCESS MESSAGE
   --------------------------------------------------------- */

function renderRosterAccessMessage() {

    const container =
        document.getElementById("rosterTableContainer");

    if (!container) {
        return;
    }

    container.replaceChildren();

    const message = document.createElement("p");

    if (CURRENT_ROLE === "MANAGER") {

        message.textContent =
            "Manager roster viewing is not available through the current backend API.";

    } else {

        message.textContent =
            "Your employee profile is not linked to this login yet, so your personal roster cannot be loaded.";

    }

    container.appendChild(message);
}

/* ---------------------------------------------------------
   DOM AND DISPLAY HELPERS
   --------------------------------------------------------- */

function createShiftButton(
    label,
    className,
    onClick,
    type = "button"
) {

    const button = document.createElement("button");

    button.type = type;
    button.className = className;
    button.textContent = label;

    if (type !== "submit") {

        button.addEventListener(
            "click",
            onClick
        );

    }

    return button;
}

function createShiftInput(
    id,
    labelText,
    type,
    value,
    required,
    maxLength = null,
    min = null
) {

    const input = document.createElement("input");

    input.id = id;
    input.type = type;
    input.value = value ?? "";
    input.required = Boolean(required);

    if (maxLength !== null) {
        input.maxLength = maxLength;
    }

    if (min !== null) {
        input.min = String(min);
    }

    return createShiftField(
        labelText,
        input
    );
}

function createShiftField(labelText, control) {

    const wrapper = document.createElement("label");

    wrapper.className = "form-group";

    const label = document.createElement("span");

    label.textContent = labelText;

    wrapper.append(
        label,
        control
    );

    return wrapper;
}

function createShiftTable(headers) {

    const table = document.createElement("table");

    table.className = "table";

    const thead = document.createElement("thead");

    const headerRow = document.createElement("tr");

    headers.forEach(label => {

        const th = document.createElement("th");

        th.textContent = label;

        headerRow.appendChild(th);

    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    table.append(
        thead,
        tbody
    );

    return table;
}

function appendShiftCell(row, value) {

    const cell = document.createElement("td");

    cell.textContent =
        value === null ||
        value === undefined ||
        value === ""
            ? "—"
            : String(value);

    row.appendChild(cell);
}

function formatShiftTime(value) {

    if (!value) {
        return "—";
    }

    return String(value).slice(0, 5);
}

function showShiftMessage(
    message,
    isError = false
) {

    const element =
        document.getElementById("shiftMessage");

    if (element) {

        element.textContent = message;

        element.style.color = isError
            ? "#b91c1c"
            : "#166534";

    }
}

function showRosterMessage(
    message,
    isError = false
) {

    const element =
        document.getElementById("rosterMessage");

    if (element) {

        element.textContent = message;

        element.style.color = isError
            ? "#b91c1c"
            : "#166534";

    }
}

console.log("Shift and roster module loaded.");