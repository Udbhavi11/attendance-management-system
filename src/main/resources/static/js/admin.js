
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   ADMIN MODULE
   File: admin.js
   Employee CRUD + Login Account Management
   ========================================================= */

const ADMIN_EMPLOYEE_API = "/api/v1/admin/employees";

let adminEmployees = [];
let editingEmployeeId = null;


/* =========================================================
   1. ADMIN DASHBOARD
   ========================================================= */

async function loadAdminDashboard() {
    const dashboard = document.getElementById("dashboard");

    if (!dashboard) {
        console.error("Administrator dashboard container was not found.");
        return;
    }

    dashboard.replaceChildren();

    const heading = document.createElement("h2");
    heading.textContent = "Administrator Dashboard";

    const description = document.createElement("p");
    description.textContent =
        "Organization overview and today's attendance summary.";

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.className = "secondary-btn";
    refreshButton.textContent = "Refresh Dashboard";
    refreshButton.addEventListener("click", loadAdminDashboard);

    const summaryGrid = document.createElement("div");
    summaryGrid.className = "admin-dashboard-grid";

    const attendanceHeading = document.createElement("h3");
    attendanceHeading.textContent = "Today's Attendance";

    const attendanceGrid = document.createElement("div");
    attendanceGrid.className = "admin-dashboard-grid";

    const anomaliesHeading = document.createElement("h3");
    anomaliesHeading.textContent = "Pending Attendance Anomalies";

    const anomaliesContainer = document.createElement("div");
    anomaliesContainer.className = "table-container";

    dashboard.append(
        heading,
        description,
        refreshButton,
        summaryGrid,
        attendanceHeading,
        attendanceGrid,
        anomaliesHeading,
        anomaliesContainer
    );

    const metricElements = {};

    function createMetricCard(container, label, key) {
        const card = document.createElement("div");
        card.className = "card";

        const title = document.createElement("h3");
        title.textContent = label;

        const value = document.createElement("p");
        value.textContent = "Loading...";
        value.dataset.metric = key;
        value.style.fontSize = "26px";
        value.style.fontWeight = "bold";

        card.append(title, value);
        container.appendChild(card);

        metricElements[key] = value;
    }

    [
        ["Total Employees", "totalEmployees"],
        ["Active Employees", "activeEmployees"],
        ["Inactive Employees", "inactiveEmployees"],
        ["Teams", "totalTeams"],
        ["Office Locations", "totalOffices"],
        ["Pending Anomalies", "pendingAnomalies"]
    ].forEach(([label, key]) =>
        createMetricCard(summaryGrid, label, key)
    );

    [
        ["Expected Employees", "expectedEmployees"],
        ["Checked In", "checkedInEmployees"],
        ["Absent", "absentEmployees"],
        ["Late Arrivals", "lateArrivals"],
        ["On Break", "employeesOnBreak"],
        ["Checked Out", "checkedOutEmployees"]
    ].forEach(([label, key]) =>
        createMetricCard(attendanceGrid, label, key)
    );

    function setMetric(key, value) {
        if (metricElements[key]) {
            metricElements[key].textContent =
                value === null || value === undefined
                    ? "—"
                    : String(value);
        }
    }

    function setUnavailable(key) {
        setMetric(key, "Unavailable");
    }

    function unwrapArray(response) {
        if (Array.isArray(response)) return response;
        if (Array.isArray(response?.content)) return response.content;
        if (Array.isArray(response?.data)) return response.data;
        if (Array.isArray(response?.items)) return response.items;
        if (Array.isArray(response?.results)) return response.results;

        return null;
    }

    function getFirstDefined(object, keys) {
        for (const key of keys) {
            if (
                object &&
                object[key] !== undefined &&
                object[key] !== null
            ) {
                return object[key];
            }
        }

        return null;
    }

    const now = new Date();

    const today =
        `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

    refreshButton.disabled = true;
    refreshButton.textContent = "Loading...";

    const requests = await Promise.allSettled([
        apiRequest(ADMIN_EMPLOYEE_API),
        apiRequest("/api/admin/teams"),
        apiRequest("/api/locations"),
        apiRequest(
            `/api/reports/daily?date=${today}&page=0&size=10`
        ),
        apiRequest("/api/anomalies")
    ]);

    // EMPLOYEE STATISTICS

    if (requests[0].status === "fulfilled") {
        const employees = unwrapArray(requests[0].value);

        if (employees) {
            const active = employees.filter(
                employee => employee.active !== false
            ).length;

            setMetric("totalEmployees", employees.length);
            setMetric("activeEmployees", active);
            setMetric("inactiveEmployees", employees.length - active);

        } else {
            [
                "totalEmployees",
                "activeEmployees",
                "inactiveEmployees"
            ].forEach(setUnavailable);

            console.error(
                "Unexpected employee API response:",
                requests[0].value
            );
        }

    } else {
        [
            "totalEmployees",
            "activeEmployees",
            "inactiveEmployees"
        ].forEach(setUnavailable);

        console.error(
            "Employee dashboard request failed:",
            requests[0].reason
        );
    }

    // TEAM STATISTICS

    if (requests[1].status === "fulfilled") {
        const teams = unwrapArray(requests[1].value);

        if (teams) {
            setMetric("totalTeams", teams.length);
        } else {
            setUnavailable("totalTeams");

            console.error(
                "Unexpected teams API response:",
                requests[1].value
            );
        }

    } else {
        setUnavailable("totalTeams");

        console.error(
            "Teams dashboard request failed:",
            requests[1].reason
        );
    }

    // OFFICE STATISTICS

    if (requests[2].status === "fulfilled") {
        const offices = unwrapArray(requests[2].value);

        if (offices) {
            const activeOffices = offices.filter(
                office => office.active !== false
            );

            setMetric("totalOffices", activeOffices.length);

        } else {
            setUnavailable("totalOffices");

            console.error(
                "Unexpected offices API response:",
                requests[2].value
            );
        }

    } else {
        setUnavailable("totalOffices");

        console.error(
            "Office dashboard request failed:",
            requests[2].reason
        );
    }

    // TODAY'S ATTENDANCE REPORT

    if (requests[3].status === "fulfilled") {
        const report = requests[3].value;

        const attendanceFields = {
            expectedEmployees: [
                "expectedEmployees",
                "totalExpectedEmployees",
                "totalEmployees"
            ],
            checkedInEmployees: [
                "checkedInEmployees",
                "checkedIn",
                "presentEmployees"
            ],
            absentEmployees: [
                "absentEmployees",
                "absent"
            ],
            lateArrivals: [
                "lateArrivals",
                "lateEmployees",
                "lateCount"
            ],
            employeesOnBreak: [
                "employeesOnBreak",
                "onBreak",
                "breakCount"
            ],
            checkedOutEmployees: [
                "checkedOutEmployees",
                "checkedOut"
            ]
        };

        Object.entries(attendanceFields).forEach(([metric, keys]) => {
            const value = getFirstDefined(report, keys);

            if (value === null) {
                setUnavailable(metric);
            } else {
                setMetric(metric, value);
            }
        });

    } else {
        [
            "expectedEmployees",
            "checkedInEmployees",
            "absentEmployees",
            "lateArrivals",
            "employeesOnBreak",
            "checkedOutEmployees"
        ].forEach(setUnavailable);

        console.error(
            "Daily attendance dashboard request failed:",
            requests[3].reason
        );
    }

    // PENDING ANOMALIES

    if (requests[4].status === "fulfilled") {
        const anomalies = unwrapArray(requests[4].value);

        if (anomalies) {
            const pending = anomalies.filter(anomaly => {
                const status = getFirstDefined(
                    anomaly,
                    [
                        "reviewStatus",
                        "status",
                        "resolutionStatus"
                    ]
                );

                return String(status || "").toUpperCase() === "PENDING";
            });

            setMetric("pendingAnomalies", pending.length);

            renderAdminDashboardAnomalies(
                anomaliesContainer,
                pending
            );

        } else {
            setUnavailable("pendingAnomalies");

            anomaliesContainer.textContent =
                "The anomalies API returned an unexpected response.";

            console.error(
                "Unexpected anomalies API response:",
                requests[4].value
            );
        }

    } else {
        setUnavailable("pendingAnomalies");

        anomaliesContainer.textContent =
            "Unable to load attendance anomalies.";

        console.error(
            "Anomalies dashboard request failed:",
            requests[4].reason
        );
    }

    refreshButton.disabled = false;
    refreshButton.textContent = "Refresh Dashboard";

    console.log("Administrator dashboard loaded.");
}


/* =========================================================
   ADMIN DASHBOARD - PENDING ANOMALIES TABLE
   ========================================================= */

function renderAdminDashboardAnomalies(container, anomalies) {
    container.replaceChildren();

    if (!Array.isArray(anomalies) || anomalies.length === 0) {
        const message = document.createElement("p");
        message.textContent = "No pending attendance anomalies.";

        container.appendChild(message);
        return;
    }

    const table = document.createElement("table");
    table.className = "table";

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    [
        "Employee",
        "Rule",
        "Severity",
        "Detected At",
        "Status"
    ].forEach(label => {
        const th = document.createElement("th");
        th.textContent = label;

        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    anomalies.slice(0, 10).forEach(anomaly => {
        const row = document.createElement("tr");

        const values = [
            anomaly.employeeName ||
                anomaly.employeeCode ||
                anomaly.employeeId ||
                "—",

            anomaly.ruleIdentifier ||
                anomaly.ruleCode ||
                anomaly.anomalyType ||
                "—",

            anomaly.severity || "—",

            anomaly.detectedAt
                ? new Date(anomaly.detectedAt).toLocaleString()
                : "—",

            anomaly.reviewStatus ||
                anomaly.status ||
                "—"
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = String(value);

            row.appendChild(cell);
        });

        tbody.appendChild(row);
    });

    table.append(thead, tbody);
    container.appendChild(table);
}


/* =========================================================
   2. LOAD EMPLOYEES
   ========================================================= */

async function loadAdminEmployees() {
    const container = document.getElementById("employeeSection");

    if (!container) {
        console.error("employeeSection was not found.");
        return;
    }

    try {
        const employees = await apiRequest(ADMIN_EMPLOYEE_API);

        if (!Array.isArray(employees)) {
            throw new Error("Invalid employee data received.");
        }

        adminEmployees = employees;

        renderAdminEmployees(adminEmployees);
        updateAdminEmployeeCount(adminEmployees);

    } catch (error) {
        console.error("Unable to load employees:", error);

        container.textContent =
            "Unable to load employees: " + error.message;
    }
}


/* =========================================================
   3. RENDER EMPLOYEE MANAGEMENT
   ========================================================= */

function renderAdminEmployees(employees) {
    const container = document.getElementById("employeeSection");

    if (!container) return;

    container.replaceChildren();

    const heading = document.createElement("h2");
    heading.textContent = "Employee Management";
    container.appendChild(heading);

    const toolbar = document.createElement("div");
    toolbar.className = "employee-toolbar";

    const addButton = document.createElement("button");
    addButton.type = "button";
    addButton.textContent = "Add Employee";
    addButton.className = "primary-btn";
    addButton.addEventListener("click", openAdminEmployeeForm);

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.textContent = "Refresh";
    refreshButton.className = "secondary-btn";
    refreshButton.addEventListener("click", loadAdminEmployees);

    toolbar.append(addButton, refreshButton);
    container.appendChild(toolbar);

    createAdminEmployeeForm(container);

    const tableHeading = document.createElement("h3");
    tableHeading.textContent = "Employee List";
    container.appendChild(tableHeading);

    if (!employees || employees.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.textContent = "No employees found.";
        container.appendChild(emptyMessage);
        return;
    }

    const tableWrapper = document.createElement("div");
    tableWrapper.className = "table-container";

    const table = document.createElement("table");
    table.className = "table";

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    [
        "ID",
        "Employee Code",
        "Name",
        "Email",
        "Mobile",
        "Department",
        "Username",
        "System Role",
        "Status",
        "Actions"
    ].forEach(label => {
        const th = document.createElement("th");
        th.textContent = label;
        headerRow.appendChild(th);
    });

    thead.appendChild(headerRow);

    const tbody = document.createElement("tbody");

    employees.forEach(employee => {
        const row = document.createElement("tr");

        appendAdminCell(row, employee.id);
        appendAdminCell(row, employee.employeeCode);
        appendAdminCell(row, employee.name);
        appendAdminCell(row, employee.email);
        appendAdminCell(row, employee.mobileNumber);
        appendAdminCell(row, employee.department);
        appendAdminCell(row, employee.username);

        appendAdminCell(
            row,
            employee.systemRole || employee.role
        );

        const statusCell = document.createElement("td");

        statusCell.textContent =
            employee.active === false ? "Inactive" : "Active";

        row.appendChild(statusCell);

        const actionCell = document.createElement("td");

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.textContent = "Edit";

        editButton.addEventListener("click", () => {
            editAdminEmployee(employee.id);
        });

        const statusButton = document.createElement("button");
        statusButton.type = "button";

        statusButton.textContent =
            employee.active === false ? "Activate" : "Deactivate";

        statusButton.addEventListener("click", () => {
            toggleAdminEmployeeStatus(employee);
        });

        actionCell.append(editButton, statusButton);
        row.appendChild(actionCell);

        tbody.appendChild(row);
    });

    table.append(thead, tbody);
    tableWrapper.appendChild(table);
    container.appendChild(tableWrapper);
}


/* =========================================================
   4. CREATE EMPLOYEE FORM
   ========================================================= */

function createAdminEmployeeForm(container) {
    const formCard = document.createElement("div");

    formCard.id = "employeeFormSection";
    formCard.className = "card";
    formCard.hidden = true;

    const heading = document.createElement("h3");
    heading.id = "employeeFormTitle";
    heading.textContent = "Add New Employee";

    const form = document.createElement("form");
    form.id = "employeeForm";

    const fields = [
        {
            id: "employeeCode",
            label: "Employee Code",
            type: "text",
            required: true,
            maxLength: 30
        },
        {
            id: "employeeName",
            label: "Employee Name",
            type: "text",
            required: true,
            maxLength: 100
        },
        {
            id: "employeeEmail",
            label: "Email",
            type: "email",
            required: true,
            maxLength: 150
        },
        {
            id: "employeeMobile",
            label: "Mobile Number",
            type: "tel",
            required: false,
            maxLength: 15
        },
        {
            id: "employeeDepartment",
            label: "Department",
            type: "text",
            required: false,
            maxLength: 100
        },
        {
            id: "employeeShiftId",
            label: "Shift ID",
            type: "number",
            required: false
        },
        {
            id: "employeeOfficeId",
            label: "Office ID",
            type: "number",
            required: false
        },
        {
            id: "employeeManagerId",
            label: "Reporting Manager Employee ID",
            type: "number",
            required: false
        }
    ];

    fields.forEach(field => {
        const group = document.createElement("div");
        group.className = "form-group";

        const label = document.createElement("label");
        label.htmlFor = field.id;
        label.textContent = field.label;

        const input = document.createElement("input");
        input.id = field.id;
        input.name = field.id;
        input.type = field.type;
        input.required = field.required;

        if (field.maxLength) {
            input.maxLength = field.maxLength;
        }

        if (field.type === "number") {
            input.min = "1";
            input.step = "1";
        }

        group.append(label, input);
        form.appendChild(group);
    });

    /* Assignment type */

    const assignmentGroup = document.createElement("div");
    assignmentGroup.className = "form-group";

    const assignmentLabel = document.createElement("label");
    assignmentLabel.htmlFor = "employeeAssignmentType";
    assignmentLabel.textContent = "Assignment Type";

    const assignmentSelect = document.createElement("select");
    assignmentSelect.id = "employeeAssignmentType";
    assignmentSelect.name = "assignmentType";
    assignmentSelect.required = true;

    [
        { value: "OFFICE", label: "Office" },
        { value: "FIELD_WORK", label: "Field Work" }
    ].forEach(optionData => {
        const option = document.createElement("option");
        option.value = optionData.value;
        option.textContent = optionData.label;
        assignmentSelect.appendChild(option);
    });

    assignmentGroup.append(assignmentLabel, assignmentSelect);
    form.appendChild(assignmentGroup);

    /* Login account section */

    const accountHeading = document.createElement("h3");
    accountHeading.id = "accountFormHeading";
    accountHeading.textContent = "Login Account Details";
    form.appendChild(accountHeading);

    const accountInfo = document.createElement("p");
    accountInfo.id = "accountFormInfo";
    accountInfo.textContent =
        "Create login credentials for this employee.";
    form.appendChild(accountInfo);

    /* Username */

    const usernameGroup = document.createElement("div");
    usernameGroup.className = "form-group";
    usernameGroup.id = "employeeUsernameGroup";

    const usernameLabel = document.createElement("label");
    usernameLabel.htmlFor = "employeeUsername";
    usernameLabel.textContent = "Username";

    const usernameInput = document.createElement("input");
    usernameInput.id = "employeeUsername";
    usernameInput.name = "username";
    usernameInput.type = "text";
    usernameInput.maxLength = 100;
    usernameInput.required = true;
    usernameInput.autocomplete = "off";

    usernameGroup.append(usernameLabel, usernameInput);
    form.appendChild(usernameGroup);

    /* Initial / New password */

    const passwordGroup = document.createElement("div");
    passwordGroup.className = "form-group";
    passwordGroup.id = "employeePasswordGroup";

    const passwordLabel = document.createElement("label");
    passwordLabel.htmlFor = "employeePassword";
    passwordLabel.textContent = "Initial / New Password";

    const passwordInput = document.createElement("input");
    passwordInput.id = "employeePassword";
    passwordInput.name = "password";
    passwordInput.type = "password";
    passwordInput.minLength = 8;
    passwordInput.maxLength = 72;
    passwordInput.required = true;
    passwordInput.autocomplete = "new-password";

    passwordGroup.append(passwordLabel, passwordInput);
    form.appendChild(passwordGroup);

    /* System role */

    const roleGroup = document.createElement("div");
    roleGroup.className = "form-group";
    roleGroup.id = "employeeSystemRoleGroup";

    const roleLabel = document.createElement("label");
    roleLabel.htmlFor = "employeeSystemRole";
    roleLabel.textContent = "System Role";

    const roleSelect = document.createElement("select");
    roleSelect.id = "employeeSystemRole";
    roleSelect.name = "systemRole";
    roleSelect.required = true;

    [
        { value: "EMPLOYEE", label: "Employee" },
        { value: "MANAGER", label: "Manager" }
    ].forEach(roleData => {
        const option = document.createElement("option");
        option.value = roleData.value;
        option.textContent = roleData.label;
        roleSelect.appendChild(option);
    });

    roleGroup.append(roleLabel, roleSelect);
    form.appendChild(roleGroup);

    /* Existing account information during edit */

    const existingAccountInfo = document.createElement("p");
    existingAccountInfo.id = "existingAccountInfo";
    existingAccountInfo.hidden = true;
    form.appendChild(existingAccountInfo);

    /* Form messages */

    const message = document.createElement("p");
    message.id = "employeeFormMessage";
    message.setAttribute("aria-live", "polite");
    form.appendChild(message);

    /* Buttons */

    const buttonGroup = document.createElement("div");
    buttonGroup.className = "form-actions";

    const submitButton = document.createElement("button");
    submitButton.id = "employeeSubmitButton";
    submitButton.type = "submit";
    submitButton.textContent = "Create Employee & Account";
    submitButton.className = "primary-btn";

    const cancelButton = document.createElement("button");
    cancelButton.id = "cancelEmployeeButton";
    cancelButton.type = "button";
    cancelButton.textContent = "Cancel";
    cancelButton.className = "secondary-btn";

    buttonGroup.append(submitButton, cancelButton);
    form.appendChild(buttonGroup);

    form.addEventListener("submit", saveAdminEmployee);

    cancelButton.addEventListener("click", resetAdminEmployeeForm);

    formCard.append(heading, form);
    container.appendChild(formCard);
}


/* =========================================================
   5. OPEN EMPLOYEE FORM
   ========================================================= */

function openAdminEmployeeForm() {
    editingEmployeeId = null;

    resetAdminEmployeeForm();

    const formSection =
        document.getElementById("employeeFormSection");

    const heading =
        document.getElementById("employeeFormTitle");

    const submitButton =
        document.getElementById("employeeSubmitButton");

    if (heading) {
        heading.textContent = "Add New Employee";
    }

    if (submitButton) {
        submitButton.textContent = "Create Employee & Account";
    }

    setAccountFieldsForCreate();

    if (formSection) {
        formSection.hidden = false;
        formSection.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }
}


/* =========================================================
   6. TABLE CELL HELPER
   ========================================================= */

function appendAdminCell(row, value) {
    const cell = document.createElement("td");

    cell.textContent =
        value !== null && value !== undefined && value !== ""
            ? String(value)
            : "-";

    row.appendChild(cell);
}


/* =========================================================
   7. EMPLOYEE COUNTS
   ========================================================= */

function updateAdminEmployeeCount(employees) {
    const totalElement = document.getElementById("totalEmployees");
    const activeElement = document.getElementById("activeEmployees");
    const inactiveElement = document.getElementById("inactiveEmployees");

    const total = employees.length;

    const active = employees.filter(
        employee => employee.active !== false
    ).length;

    const inactive = total - active;

    if (totalElement) totalElement.textContent = total;
    if (activeElement) activeElement.textContent = active;
    if (inactiveElement) inactiveElement.textContent = inactive;
}


/* =========================================================
   8. EDIT EMPLOYEE
   ========================================================= */

function editAdminEmployee(employeeId) {
    const employee = adminEmployees.find(
        item => Number(item.id) === Number(employeeId)
    );

    if (!employee) {
        alert("Employee not found.");
        return;
    }

    editingEmployeeId = employee.id;

    setAdminField("employeeCode", employee.employeeCode);
    setAdminField("employeeName", employee.name);
    setAdminField("employeeEmail", employee.email);
    setAdminField("employeeMobile", employee.mobileNumber);
    setAdminField("employeeDepartment", employee.department);
    setAdminField("employeeShiftId", employee.shiftId);
    setAdminField("employeeOfficeId", employee.officeId);
    setAdminField("employeeManagerId", employee.managerId);

    setAdminField(
        "employeeAssignmentType",
        employee.assignmentType || "OFFICE"
    );

    setAdminField(
        "employeeUsername",
        employee.username || ""
    );

    setAdminField(
        "employeeSystemRole",
        employee.systemRole || "EMPLOYEE"
    );

    const heading =
        document.getElementById("employeeFormTitle");

    const submitButton =
        document.getElementById("employeeSubmitButton");

    const formSection =
        document.getElementById("employeeFormSection");

    if (heading) {
        heading.textContent = "Update Employee";
    }

    if (submitButton) {
        submitButton.textContent = "Update Employee & Account";
    }

    setAccountFieldsForEdit(employee);

    if (formSection) {
        formSection.hidden = false;
        formSection.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }
}


/* =========================================================
   9. ACCOUNT FIELD VISIBILITY
   ========================================================= */

function setAccountFieldsForCreate() {
    const usernameGroup =
        document.getElementById("employeeUsernameGroup");

    const passwordGroup =
        document.getElementById("employeePasswordGroup");

    const roleGroup =
        document.getElementById("employeeSystemRoleGroup");

    const accountInfo =
        document.getElementById("accountFormInfo");

    const existingInfo =
        document.getElementById("existingAccountInfo");

    const usernameInput =
        document.getElementById("employeeUsername");

    const passwordInput =
        document.getElementById("employeePassword");

    const roleSelect =
        document.getElementById("employeeSystemRole");

    if (usernameGroup) usernameGroup.hidden = false;
    if (passwordGroup) passwordGroup.hidden = false;
    if (roleGroup) roleGroup.hidden = false;

    if (usernameInput) {
        usernameInput.required = true;
        usernameInput.disabled = false;
        usernameInput.value = "";
    }

    if (passwordInput) {
        passwordInput.required = true;
        passwordInput.disabled = false;
        passwordInput.value = "";
        passwordInput.placeholder = "Enter initial password";
    }

    if (roleSelect) {
        roleSelect.disabled = false;
        roleSelect.required = true;
        roleSelect.value = "EMPLOYEE";
    }

    if (accountInfo) {
        accountInfo.hidden = false;
        accountInfo.textContent =
            "Create login credentials for this employee.";
    }

    if (existingInfo) {
        existingInfo.hidden = true;
        existingInfo.textContent = "";
    }
}


function setAccountFieldsForEdit(employee) {
    const usernameGroup =
        document.getElementById("employeeUsernameGroup");

    const passwordGroup =
        document.getElementById("employeePasswordGroup");

    const roleGroup =
        document.getElementById("employeeSystemRoleGroup");

    const accountInfo =
        document.getElementById("accountFormInfo");

    const existingInfo =
        document.getElementById("existingAccountInfo");

    const usernameInput =
        document.getElementById("employeeUsername");

    const passwordInput =
        document.getElementById("employeePassword");

    const roleSelect =
        document.getElementById("employeeSystemRole");

    const hasLinkedAccount = Boolean(
        employee.username && employee.username.trim()
    );

    // Show credential fields while editing an employee.

    if (usernameGroup) usernameGroup.hidden = false;
    if (passwordGroup) passwordGroup.hidden = false;
    if (roleGroup) roleGroup.hidden = false;

    if (usernameInput) {
        usernameInput.disabled = false;
        usernameInput.required = true;
        usernameInput.value = employee.username || "";
        usernameInput.autocomplete = "off";
    }

    if (passwordInput) {
        passwordInput.disabled = false;
        passwordInput.required = !hasLinkedAccount;
        passwordInput.value = "";

        passwordInput.placeholder = hasLinkedAccount
            ? "Leave blank to keep current password"
            : "Enter initial password";

        passwordInput.autocomplete = "new-password";
    }

    if (roleSelect) {
        roleSelect.disabled = false;
        roleSelect.required = true;
        roleSelect.value = employee.systemRole || "EMPLOYEE";
    }

    if (accountInfo) {
        accountInfo.hidden = false;

        accountInfo.textContent = hasLinkedAccount
            ? "Update the username or role. Enter a new password only if you want to reset it."
            : "This employee has no login account. Enter a username and initial password to create one.";
    }

    if (existingInfo) {
        existingInfo.hidden = false;

        existingInfo.textContent = hasLinkedAccount
            ? `Current login username: ${employee.username}. The password is never displayed.`
            : "Login account: Not linked. Saving this form will create and link an account.";
    }
}


/* =========================================================
   10. FIELD HELPERS
   ========================================================= */

function setAdminField(id, value) {
    const element = document.getElementById(id);

    if (element) {
        element.value =
            value !== null && value !== undefined
                ? value
                : "";
    }
}


function getAdminField(id) {
    const element = document.getElementById(id);

    return element
        ? element.value.trim()
        : "";
}


function getOptionalNumericField(id) {
    const value = getAdminField(id);

    if (!value) return null;

    const number = Number(value);

    return Number.isInteger(number) && number > 0
        ? number
        : null;
}


/* =========================================================
   11. BUILD EMPLOYEE PAYLOAD
   ========================================================= */

function buildAdminEmployeePayload(includeCredentials) {
    const payload = {
        employeeCode: getAdminField("employeeCode"),
        name: getAdminField("employeeName"),
        email: getAdminField("employeeEmail"),
        mobileNumber: getAdminField("employeeMobile"),
        department: getAdminField("employeeDepartment"),

        shiftId: getOptionalNumericField("employeeShiftId"),
        officeId: getOptionalNumericField("employeeOfficeId"),
        managerId: getOptionalNumericField("employeeManagerId"),

        assignmentType:
            getAdminField("employeeAssignmentType") || "OFFICE"
    };

    if (includeCredentials) {
        payload.username = getAdminField("employeeUsername");
        payload.password = getAdminField("employeePassword") || null;
        payload.systemRole =
            getAdminField("employeeSystemRole") || "EMPLOYEE";
    }

    return payload;
}


/* =========================================================
   12. CREATE / UPDATE EMPLOYEE
   ========================================================= */

async function saveAdminEmployee(event) {
    if (event) {
        event.preventDefault();
    }

    const isEditing = editingEmployeeId !== null;

    // Credentials are sent for both create and update operations.
    const payload = buildAdminEmployeePayload(true);

    if (!payload.employeeCode || !payload.name || !payload.email) {
        showEmployeeFormMessage(
            "Employee code, name and email are required.",
            true
        );
        return;
    }

    if (!payload.assignmentType) {
        showEmployeeFormMessage(
            "Select an assignment type.",
            true
        );
        return;
    }

    if (!payload.username) {
        showEmployeeFormMessage(
            "Username is required.",
            true
        );
        return;
    }

    const employeeBeingEdited = isEditing
        ? adminEmployees.find(
            employee => Number(employee.id) === Number(editingEmployeeId)
        )
        : null;

    const hasLinkedAccount = Boolean(
        employeeBeingEdited && employeeBeingEdited.username
    );

    // Password is required for new accounts and for existing employees
    // who do not yet have a linked login account.

    if (!isEditing || !hasLinkedAccount) {
        if (!payload.password) {
            showEmployeeFormMessage(
                "Initial password is required to create the login account.",
                true
            );
            return;
        }
    }

    // If a password is supplied, validate its length.

    if (
        payload.password &&
        (payload.password.length < 8 || payload.password.length > 72)
    ) {
        showEmployeeFormMessage(
            "Password must contain between 8 and 72 characters.",
            true
        );
        return;
    }

    if (!["EMPLOYEE", "MANAGER"].includes(payload.systemRole)) {
        showEmployeeFormMessage(
            "Select a valid system role.",
            true
        );
        return;
    }

    const submitButton =
        document.getElementById("employeeSubmitButton");

    try {
        if (submitButton) {
            submitButton.disabled = true;
        }

        if (isEditing) {
            await apiRequest(
                `${ADMIN_EMPLOYEE_API}/${editingEmployeeId}`,
                {
                    method: "PUT",
                    body: JSON.stringify(payload)
                }
            );

        } else {
            await apiRequest(
                ADMIN_EMPLOYEE_API,
                {
                    method: "POST",
                    body: JSON.stringify(payload)
                }
            );
        }

        alert(
            isEditing
                ? "Employee and login account details updated successfully."
                : "Employee and login account created successfully."
        );

        editingEmployeeId = null;

        await loadAdminEmployees();

    } catch (error) {
        console.error("Unable to save employee:", error);

        showEmployeeFormMessage(
            error.message || "Unable to save employee.",
            true
        );

        if (submitButton) {
            submitButton.disabled = false;
        }
    }
}


/* =========================================================
   13. FORM MESSAGE
   ========================================================= */

function showEmployeeFormMessage(message, isError) {
    const element =
        document.getElementById("employeeFormMessage");

    if (!element) {
        alert(message);
        return;
    }

    element.textContent = message;

    element.className = isError
        ? "error-message"
        : "success-message";
}


/* =========================================================
   14. RESET FORM
   ========================================================= */

function resetAdminEmployeeForm() {
    editingEmployeeId = null;

    const form = document.getElementById("employeeForm");

    if (form) {
        form.reset();
    }

    const formSection =
        document.getElementById("employeeFormSection");

    const heading =
        document.getElementById("employeeFormTitle");

    const submitButton =
        document.getElementById("employeeSubmitButton");

    const message =
        document.getElementById("employeeFormMessage");

    if (heading) {
        heading.textContent = "Add New Employee";
    }

    if (submitButton) {
        submitButton.textContent = "Create Employee & Account";
        submitButton.disabled = false;
    }

    if (message) {
        message.textContent = "";
        message.className = "";
    }

    setAccountFieldsForCreate();

    if (formSection) {
        formSection.hidden = true;
    }
}


/* =========================================================
   15. ACTIVATE / DEACTIVATE EMPLOYEE
   ========================================================= */

async function toggleAdminEmployeeStatus(employee) {
    if (!employee || employee.id === undefined) {
        return;
    }

    const currentlyActive = employee.active !== false;

    const action = currentlyActive
        ? "deactivate"
        : "activate";

    const confirmed = confirm(
        `Are you sure you want to ${action} ${employee.name}?`
    );

    if (!confirmed) return;

    try {
        await apiRequest(
            `${ADMIN_EMPLOYEE_API}/${employee.id}/${action}`,
            {
                method: "PATCH"
            }
        );

        alert(
            currentlyActive
                ? "Employee and linked account deactivated successfully."
                : "Employee and linked account activated successfully."
        );

        await loadAdminEmployees();

    } catch (error) {
        console.error("Unable to change employee status:", error);

        alert(
            "Unable to update employee status: " + error.message
        );
    }
}


/* =========================================================
   16. ADMIN FIELD VISIT REPORT CONTAINER
   ========================================================= */

function ensureAdminVisitReportContainer() {
    const root = document.getElementById("visitSection");

    if (!root) return null;

    let container = document.getElementById("adminVisitReportContent");

    if (container) return container;

    // Admin only reviews field visits.
    // No start/end visit controls are rendered here.

    root.replaceChildren();

    const card = document.createElement("section");
    card.id = "adminVisitReport";
    card.className = "card";

    const heading = document.createElement("h3");
    heading.textContent = "Field Visit Summary";

    const description = document.createElement("p");
    description.textContent =
        "Review field visit activity and history across the organization.";

    container = document.createElement("div");
    container.id = "adminVisitReportContent";

    card.append(heading, description, container);
    root.appendChild(card);

    return container;
}


/* =========================================================
   17. LOAD ADMIN FIELD VISIT SUMMARY
   ========================================================= */

async function loadAdminFieldVisitReport() {
    const container = ensureAdminVisitReportContainer();

    if (!container) return;

    container.textContent = "Loading field visit summary...";

    const today = new Date().toLocaleDateString("en-CA");

    try {
        const data = await apiRequest(
            `/api/reports/field-visits?startDate=${today}&endDate=${today}`
        );

        container.replaceChildren();

        /* Summary metrics */

        const metrics = [
            ["Total visits", data.totalVisits],
            ["Completed visits", data.completedVisits],
            ["Incomplete visits", data.incompleteVisits],
            ["Total duration (seconds)", data.totalVisitDurationSeconds]
        ];

        const metricsRow = document.createElement("div");
        metricsRow.className = "sync-summary";

        metrics.forEach(([label, value]) => {
            const item = document.createElement("div");
            item.className = "summary-item";

            const number = document.createElement("h3");
            number.textContent = value ?? 0;

            const caption = document.createElement("p");
            caption.textContent = label;

            item.append(number, caption);
            metricsRow.appendChild(item);
        });

        /* Visit type breakdown */

        const breakdownHeading = document.createElement("h4");
        breakdownHeading.textContent = "Visit Type Breakdown";
        breakdownHeading.style.margin = "22px 0 10px";

        const breakdown = document.createElement("div");
        breakdown.className = "table-container";

        const breakdownRows = Array.isArray(data.visitTypeBreakdown)
            ? data.visitTypeBreakdown
            : [];

        if (breakdownRows.length > 0) {
            const breakdownTable = document.createElement("table");
            const head = document.createElement("thead");
            const headRow = document.createElement("tr");

            ["Visit Type", "Count"].forEach(label => {
                const th = document.createElement("th");
                th.textContent = label;
                headRow.appendChild(th);
            });

            head.appendChild(headRow);
            breakdownTable.appendChild(head);

            const body = document.createElement("tbody");

            breakdownRows.forEach(item => {
                const row = document.createElement("tr");

                [item.visitType, item.count].forEach(value => {
                    const cell = document.createElement("td");
                    cell.textContent = value ?? "—";
                    row.appendChild(cell);
                });

                body.appendChild(row);
            });

            breakdownTable.appendChild(body);
            breakdown.appendChild(breakdownTable);

        } else {
            breakdown.textContent = "No visit type breakdown available.";
        }

        /* Visit history heading */

        const historyHeading = document.createElement("h3");
        historyHeading.textContent = "Field Visit History";
        historyHeading.style.margin = "26px 0 12px";

        /* Date filters */

        const filters = document.createElement("div");
        filters.className = "form-grid";

        const fromGroup = document.createElement("div");
        fromGroup.className = "form-group";

        const fromLabel = document.createElement("label");
        fromLabel.htmlFor = "adminVisitStartDate";
        fromLabel.textContent = "From date";

        const from = document.createElement("input");
        from.type = "date";
        from.id = "adminVisitStartDate";
        from.value = today;

        fromGroup.append(fromLabel, from);

        const toGroup = document.createElement("div");
        toGroup.className = "form-group";

        const toLabel = document.createElement("label");
        toLabel.htmlFor = "adminVisitEndDate";
        toLabel.textContent = "To date";

        const to = document.createElement("input");
        to.type = "date";
        to.id = "adminVisitEndDate";
        to.value = today;

        toGroup.append(toLabel, to);

        const actionGroup = document.createElement("div");
        actionGroup.className = "form-group";

        const button = document.createElement("button");
        button.type = "button";
        button.className = "primary-btn";
        button.textContent = "View Visit History";
        button.addEventListener("click", loadAdminFieldVisitHistory);

        actionGroup.appendChild(button);

        filters.append(fromGroup, toGroup, actionGroup);

        const history = document.createElement("div");
        history.id = "adminVisitHistory";
        history.className = "table-container";

        container.append(
            metricsRow,
            breakdownHeading,
            breakdown,
            historyHeading,
            filters,
            history
        );

        await loadAdminFieldVisitHistory();

    } catch (error) {
        container.textContent =
            "Unable to load field visit summary: " + error.message;
    }
}


/* =========================================================
   18. LOAD ADMIN FIELD VISIT HISTORY
   ========================================================= */

async function loadAdminFieldVisitHistory() {
    const history = document.getElementById("adminVisitHistory");

    if (!history) return;

    const start =
        document.getElementById("adminVisitStartDate")?.value;

    const end =
        document.getElementById("adminVisitEndDate")?.value;

    if (!start || !end || start > end) {
        history.textContent = "Please select a valid date range.";
        return;
    }

    history.textContent = "Loading field visit history...";

    try {
        const data = await apiRequest("/api/field-visits");

        const visits = Array.isArray(data) ? data : [];

        const filteredVisits = visits.filter(visit => {
            if (!visit.startTime) return false;

            const visitDate = String(visit.startTime).slice(0, 10);

            return visitDate >= start && visitDate <= end;
        });

        history.replaceChildren();

        if (filteredVisits.length === 0) {
            history.textContent =
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
            "Duration (seconds)"
        ].forEach(label => {
            const th = document.createElement("th");
            th.textContent = label;
            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement("tbody");

        filteredVisits.forEach(visit => {
            const row = document.createElement("tr");

            const values = [
                visit.id,
                visit.employeeName || visit.employeeId,
                visit.visitType,
                visit.customerReference,
                visit.startTime,
                visit.endTime,
                visit.status,
                visit.durationSeconds
            ];

            values.forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value ?? "—";
                row.appendChild(cell);
            });

            tbody.appendChild(row);
        });

        table.appendChild(tbody);
        history.appendChild(table);

    } catch (error) {
        history.textContent =
            "Unable to load field visit history: " + error.message;
    }
}


/* =========================================================
   19. INITIALIZATION
   ========================================================= */

console.log("Admin module loaded.");
