const ADMIN_EMPLOYEE_API = "/api/v1/admin/employees";
let adminEmployees =[];
let editingEmployeeId = null;
async function loadAdminDashboard() {
    const dashboard = document.getElementById("dashboard");
    if(!dashboard) {
        console.error("Administrator dashboard container was not found.");
        return;
    }
    dashboard.replaceChildren();
    const heading = document.createElement("h2");
    heading.textContent = "Administrator Dashboard";
    const description = document.createElement("p");
    description.textContent = "Organization overview and today's attendance summary.";
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
    dashboard.append(heading, description, refreshButton, summaryGrid, attendanceHeading, attendanceGrid, anomaliesHeading, anomaliesContainer);
    const metricElements = {
    };
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
    [["Total Employees",
    "totalEmployees"],
    ["Active Employees",
    "activeEmployees"],
    ["Inactive Employees",
    "inactiveEmployees"],
    ["Teams",
    "totalTeams"],
    ["Office Locations",
    "totalOffices"],
    ["Pending Anomalies",
    "pendingAnomalies"]].forEach(([label, key]) => createMetricCard(summaryGrid, label, key));
    [["Expected Employees",
    "expectedEmployees"],
    ["Checked In",
    "checkedInEmployees"],
    ["Absent",
    "absentEmployees"],
    ["Late Arrivals",
    "lateArrivals"],
    ["On Break",
    "employeesOnBreak"],
    ["Checked Out",
    "checkedOutEmployees"]].forEach(([label, key]) => createMetricCard(attendanceGrid, label, key));
    function setMetric(key, value) {
        if(metricElements[key]) {
            metricElements[key].textContent = value === null || value === undefined ? "—" : String(value);
        }
    }
    function setUnavailable(key) {
        setMetric(key, "Unavailable");
    }
    function unwrapArray(response) {
        if(Array.isArray(response)) return response;
        if(Array.isArray(response?.content)) return response.content;
        if(Array.isArray(response?.data)) return response.data;
        if(Array.isArray(response?.items)) return response.items;
        if(Array.isArray(response?.results)) return response.results;
        return null;
    }
    function getFirstDefined(object, keys) {
        for(const key of keys) {
            if(object && object[key] !== undefined && object[key] !== null) {
                return object[key];
            }
        }
        return null;
    }
    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
    refreshButton.disabled = true;
    refreshButton.textContent = "Loading...";
    const requests = await Promise.allSettled([apiRequest(ADMIN_EMPLOYEE_API), apiRequest("/api/admin/teams"), apiRequest("/api/locations"), apiRequest(`/api/reports/daily?date=${today}&page=0&size=10`), apiRequest("/api/anomalies")]);
    if(requests[0].status === "fulfilled") {
        const employees = unwrapArray(requests[0].value);
        if(employees) {
            const active = employees.filter(employee => employee.active !== false).length;
            setMetric("totalEmployees", employees.length);
            setMetric("activeEmployees", active);
            setMetric("inactiveEmployees", employees.length - active);
        } else {
            ["totalEmployees",
            "activeEmployees",
            "inactiveEmployees"].forEach(setUnavailable);
            console.error("Unexpected employee API response:", requests[0].value);
        }
    } else {
        ["totalEmployees",
        "activeEmployees",
        "inactiveEmployees"].forEach(setUnavailable);
        console.error("Employee dashboard request failed:", requests[0].reason);
    }
    if(requests[1].status === "fulfilled") {
        const teams = unwrapArray(requests[1].value);
        if(teams) {
            setMetric("totalTeams", teams.length);
        } else {
            setUnavailable("totalTeams");
            console.error("Unexpected teams API response:", requests[1].value);
        }
    } else {
        setUnavailable("totalTeams");
        console.error("Teams dashboard request failed:", requests[1].reason);
    }
    if(requests[2].status === "fulfilled") {
        const offices = unwrapArray(requests[2].value);
        if(offices) {
            const activeOffices = offices.filter(office => office.active !== false);
            setMetric("totalOffices", activeOffices.length);
        } else {
            setUnavailable("totalOffices");
            console.error("Unexpected offices API response:", requests[2].value);
        }
    } else {
        setUnavailable("totalOffices");
        console.error("Office dashboard request failed:", requests[2].reason);
    }
    if(requests[3].status === "fulfilled") {
        const report = requests[3].value;
        const attendanceFields = {
            expectedEmployees :["expectedEmployees",
            "totalExpectedEmployees",
            "totalEmployees"],
            checkedInEmployees :["checkedInEmployees",
            "checkedIn",
            "presentEmployees"],
            absentEmployees :["absentEmployees",
            "absent"],
            lateArrivals :["lateArrivals",
            "lateEmployees",
            "lateCount"],
            employeesOnBreak :["employeesOnBreak",
            "onBreak",
            "breakCount"],
            checkedOutEmployees :["checkedOutEmployees",
            "checkedOut"]
        };
        Object.entries(attendanceFields).forEach(([metric, keys]) => {
            const value = getFirstDefined(report, keys); if(value === null) {
                setUnavailable(metric);
            } else {
                setMetric(metric, value);
            }
        });
    } else {
        ["expectedEmployees",
        "checkedInEmployees",
        "absentEmployees",
        "lateArrivals",
        "employeesOnBreak",
        "checkedOutEmployees"].forEach(setUnavailable);
        console.error("Daily attendance dashboard request failed:", requests[3].reason);
    }
    if(requests[4].status === "fulfilled") {
        const anomalies = unwrapArray(requests[4].value);
        if(anomalies) {
            const pending = anomalies.filter(anomaly => {
                const status = getFirstDefined(anomaly,["reviewStatus", "status", "resolutionStatus"]); return String(status || "").toUpperCase() === "PENDING";
            });
            setMetric("pendingAnomalies", pending.length);
            renderAdminDashboardAnomalies(anomaliesContainer, pending);
        } else {
            setUnavailable("pendingAnomalies");
            anomaliesContainer.textContent = "The anomalies API returned an unexpected response.";
            console.error("Unexpected anomalies API response:", requests[4].value);
        }
    } else {
        setUnavailable("pendingAnomalies");
        anomaliesContainer.textContent = "Unable to load attendance anomalies.";
        console.error("Anomalies dashboard request failed:", requests[4].reason);
    }
    refreshButton.disabled = false;
    refreshButton.textContent = "Refresh Dashboard";
    console.log("Administrator dashboard loaded.");
}
function renderAdminDashboardAnomalies(container, anomalies) {
    container.replaceChildren();
    if(!Array.isArray(anomalies) || anomalies.length === 0) {
        const message = document.createElement("p");
        message.textContent = "No pending attendance anomalies.";
        container.appendChild(message);
        return;
    }
    const table = document.createElement("table");
    table.className = "table";
    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");
    ["Employee",
    "Rule",
    "Severity",
    "Detected At",
    "Status"].forEach(label => {
        const th = document.createElement("th"); th.textContent = label; headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    const tbody = document.createElement("tbody");
    anomalies.slice(0, 10).forEach(anomaly => {
        const row = document.createElement("tr"); const values =[anomaly.employeeName || anomaly.employeeCode || anomaly.employeeId || "—", anomaly.ruleIdentifier || anomaly.ruleCode || anomaly.anomalyType || "—", anomaly.severity || "—", anomaly.detectedAt ? new Date(anomaly.detectedAt).toLocaleString() : "—", anomaly.reviewStatus || anomaly.status || "—"]; values.forEach(value => {
            const cell = document.createElement("td"); cell.textContent = String(value); row.appendChild(cell);
        }); tbody.appendChild(row);
    });
    table.append(thead, tbody);
    container.appendChild(table);
}
async function loadAdminEmployees() {
    const container = document.getElementById("employeeSection");
    if(!container) {
        console.error("employeeSection was not found.");
        return;
    }
    try {
        const employees = await apiRequest(ADMIN_EMPLOYEE_API);
        if(!Array.isArray(employees)) {
            throw new Error("Invalid employee data received.");
        }
        adminEmployees = employees;
        renderAdminEmployees(adminEmployees);
        updateAdminEmployeeCount(adminEmployees);
    } catch(error) {
        console.error("Unable to load employees:", error);
        container.textContent = "Unable to load employees: " + error.message;
    }
}
function renderAdminEmployees(employees) {
    const container = document.getElementById("employeeSection");
    if(!container) return;
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
    if(!employees || employees.length === 0) {
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
    ["ID",
    "Employee Code",
    "Name",
    "Email",
    "Mobile",
    "Department",
    "Username",
    "System Role",
    "Status",
    "Actions"].forEach(label => {
        const th = document.createElement("th"); th.textContent = label; headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    const tbody = document.createElement("tbody");
    employees.forEach(employee => {
        const row = document.createElement("tr"); appendAdminCell(row, employee.id); appendAdminCell(row, employee.employeeCode); appendAdminCell(row, employee.name); appendAdminCell(row, employee.email); appendAdminCell(row, employee.mobileNumber); appendAdminCell(row, employee.department); appendAdminCell(row, employee.username); appendAdminCell(row, employee.systemRole || employee.role); const statusCell = document.createElement("td"); statusCell.textContent = employee.active === false ? "Inactive" : "Active"; row.appendChild(statusCell); const actionCell = document.createElement("td"); const editButton = document.createElement("button"); editButton.type = "button"; editButton.textContent = "Edit"; editButton.addEventListener("click",() => {
            editAdminEmployee(employee.id);
        }); const statusButton = document.createElement("button"); statusButton.type = "button"; statusButton.textContent = employee.active === false ? "Activate" : "Deactivate"; statusButton.addEventListener("click",() => {
            toggleAdminEmployeeStatus(employee);
        }); actionCell.append(editButton, statusButton); row.appendChild(actionCell); tbody.appendChild(row);
    });
    table.append(thead, tbody);
    tableWrapper.appendChild(table);
    container.appendChild(tableWrapper);
}
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
    form.className = "form-grid";
    form.addEventListener("submit", saveAdminEmployee);
    const fields = [{
        id : "employeeCode",
        label : "Employee Code",
        type : "text",
        required : true
    }, {
        id : "employeeName",
        label : "Full Name",
        type : "text",
        required : true
    }, {
        id : "employeeEmail",
        label : "Email",
        type : "email",
        required : true
    }, {
        id : "employeeMobile",
        label : "Mobile Number",
        type : "tel",
        required : false
    }, {
        id : "employeeDepartment",
        label : "Department",
        type : "text",
        required : false
    }, {
        id : "employeeUsername",
        label : "Username",
        type : "text",
        required : true
    }, {
        id : "employeePassword",
        label : "Password",
        type : "password",
        required : true
    }, {
        id : "employeeShiftId",
        label : "Shift ID",
        type : "number",
        required : false
    }, {
        id : "employeeOfficeId",
        label : "Office ID",
        type : "number",
        required : false
    }, {
        id : "employeeManagerId",
        label : "Manager ID",
        type : "number",
        required : false
    }];
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
        group.append(label, input);
        form.appendChild(group);
    });
    const assignmentGroup = document.createElement("div");
    assignmentGroup.className = "form-group";
    const assignmentLabel = document.createElement("label");
    assignmentLabel.htmlFor = "employeeAssignmentType";
    assignmentLabel.textContent = "Assignment Type";
    const assignmentSelect = document.createElement("select");
    assignmentSelect.id = "employeeAssignmentType";
    assignmentSelect.name = "assignmentType";
    assignmentSelect.required = true;
    [{
        value : "OFFICE",
        label : "Office"
    }, {
        value : "FIELD",
        label : "Field"
    }].forEach(item => {
        const option = document.createElement("option");
        option.value = item.value;
        option.textContent = item.label;
        assignmentSelect.appendChild(option);
    });
    assignmentGroup.append(assignmentLabel, assignmentSelect);
    form.appendChild(assignmentGroup);
    const roleGroup = document.createElement("div");
    roleGroup.className = "form-group";
    const roleLabel = document.createElement("label");
    roleLabel.htmlFor = "employeeSystemRole";
    roleLabel.textContent = "System Role";
    const roleSelect = document.createElement("select");
    roleSelect.id = "employeeSystemRole";
    roleSelect.name = "systemRole";
    roleSelect.required = true;
    [{
        value : "EMPLOYEE",
        label : "Employee"
    }, {
        value : "MANAGER",
        label : "Manager"
    }, {
        value : "AUDITOR",
        label : "Auditor"
    }].forEach(item => {
        const option = document.createElement("option");
        option.value = item.value;
        option.textContent = item.label;
        roleSelect.appendChild(option);
    });
    roleGroup.append(roleLabel, roleSelect);
    form.appendChild(roleGroup);
    const message = document.createElement("p");
    message.id = "employeeFormMessage";
    message.setAttribute("role", "alert");
    const actions = document.createElement("div");
    actions.className = "form-actions";
    const submitButton = document.createElement("button");
    submitButton.id = "employeeSubmitButton";
    submitButton.type = "submit";
    submitButton.className = "primary-btn";
    submitButton.textContent = "Create Employee & Account";
    const cancelButton = document.createElement("button");
    cancelButton.type = "button";
    cancelButton.className = "secondary-btn";
    cancelButton.textContent = "Cancel";
    cancelButton.addEventListener("click", resetAdminEmployeeForm);
    actions.append(submitButton, cancelButton);
    form.append(message, actions);
    formCard.append(heading, form);
    container.appendChild(formCard);
}
function openAdminEmployeeForm() {
    resetAdminEmployeeForm();
    const formSection = document.getElementById("employeeFormSection");
    if(formSection) {
        formSection.hidden = false;
        formSection.scrollIntoView({
            behavior : "smooth",
            block : "start"
        });
    }
}
function appendAdminCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value === null || value === undefined ? "—" : String(value);
    row.appendChild(cell);
}
function updateAdminEmployeeCount(employees) {
    const countElement = document.getElementById("employeeCount");
    if(countElement) {
        countElement.textContent = String(employees.length);
    }
}
async function editAdminEmployee(employeeId) {
    const employee = adminEmployees.find(item => Number(item.id) === Number(employeeId));
    if(!employee) {
        alert("Employee not found.");
        return;
    }
    editingEmployeeId = employee.id;
    const formSection = document.getElementById("employeeFormSection");
    const heading = document.getElementById("employeeFormTitle");
    const submitButton = document.getElementById("employeeSubmitButton");
    if(formSection) {
        formSection.hidden = false;
        formSection.scrollIntoView({
            behavior : "smooth",
            block : "start"
        });
    }
    if(heading) {
        heading.textContent = "Edit Employee";
    }
    if(submitButton) {
        submitButton.textContent = "Update Employee";
    }
    setAdminField("employeeCode", employee.employeeCode);
    setAdminField("employeeName", employee.name);
    setAdminField("employeeEmail", employee.email);
    setAdminField("employeeMobile", employee.mobileNumber);
    setAdminField("employeeDepartment", employee.department);
    setAdminField("employeeUsername", employee.username);
    setAdminField("employeeShiftId", employee.shiftId);
    setAdminField("employeeOfficeId", employee.officeId);
    setAdminField("employeeManagerId", employee.managerId);
    setAdminField("employeeAssignmentType", employee.assignmentType);
    setAdminField("employeeSystemRole", employee.systemRole || employee.role);
    setAdminField("employeePassword", "");
    const passwordInput = document.getElementById("employeePassword");
    if(passwordInput) {
        passwordInput.required = false;
    }
    const usernameInput = document.getElementById("employeeUsername");
    if(usernameInput) {
        usernameInput.readOnly = true;
    }
    showEmployeeFormMessage("Leave password blank to keep the current password.", false);
}
function setAccountFieldsForCreate() {
    const passwordInput = document.getElementById("employeePassword");
    const usernameInput = document.getElementById("employeeUsername");
    if(passwordInput) {
        passwordInput.required = true;
    }
    if(usernameInput) {
        usernameInput.readOnly = false;
    }
    const existingInfo = document.getElementById("employeeExistingAccountInfo");
    if(existingInfo) {
        existingInfo.hidden = true;
        existingInfo.textContent = "";
    }
}
function setAdminField(id, value) {
    const element = document.getElementById(id);
    if(element) {
        element.value = value === null || value === undefined ? "" : String(value);
    }
}
async function saveAdminEmployee(event) {
    event.preventDefault();
    const form = document.getElementById("employeeForm");
    const submitButton = document.getElementById("employeeSubmitButton");
    if(!form) return;
    const employeeCode = document.getElementById("employeeCode")?.value.trim();
    const name = document.getElementById("employeeName")?.value.trim();
    const email = document.getElementById("employeeEmail")?.value.trim();
    const mobileNumber = document.getElementById("employeeMobile")?.value.trim();
    const department = document.getElementById("employeeDepartment")?.value.trim();
    const shiftIdValue = document.getElementById("employeeShiftId")?.value.trim();
    const officeIdValue = document.getElementById("employeeOfficeId")?.value.trim();
    const managerIdValue = document.getElementById("employeeManagerId")?.value.trim();
    const assignmentType = document.getElementById("employeeAssignmentType")?.value;
    const username = document.getElementById("employeeUsername")?.value.trim();
    const password = document.getElementById("employeePassword")?.value;
    const systemRole = document.getElementById("employeeSystemRole")?.value;
    if(!employeeCode || !name || !email || !assignmentType) {
        showEmployeeFormMessage("Please fill in all required employee fields.", true);
        return;
    }
    if(!username) {
        showEmployeeFormMessage("Username is required.", true);
        return;
    }
    if(!editingEmployeeId && (!password || password.length < 8)) {
        showEmployeeFormMessage("Password must contain at least 8 characters.", true);
        return;
    }
    if(password && (password.length < 8 || password.length > 72)) {
        showEmployeeFormMessage("Password must contain between 8 and 72 characters.", true);
        return;
    }
    if(!["EMPLOYEE", "MANAGER", "AUDITOR"].includes(systemRole)) {
        showEmployeeFormMessage("Please select a valid system role.", true);
        return;
    }
    const employeePayload = {
        employeeCode,
        name,
        email,
        mobileNumber : mobileNumber || null,
        department : department || null,
        shiftId : shiftIdValue ? Number(shiftIdValue) : null,
        officeId : officeIdValue ? Number(officeIdValue) : null,
        managerId : managerIdValue ? Number(managerIdValue) : null,
        assignmentType,
        username,
        systemRole,
        password : password || null
    };
    if(submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = editingEmployeeId ? "Updating..." : "Creating...";
    }
    try {
        const url = editingEmployeeId ? `${ADMIN_EMPLOYEE_API}/${editingEmployeeId}` : ADMIN_EMPLOYEE_API;
        await apiRequest(url, {
            method : editingEmployeeId ? "PUT" : "POST",
            body : JSON.stringify(employeePayload)
        });
        showEmployeeFormMessage(editingEmployeeId ? "Employee updated successfully." : "Employee and account created successfully.", false);
        editingEmployeeId = null;
        await loadAdminEmployees();
    } catch(error) {
        console.error("Unable to save employee:", error);
        showEmployeeFormMessage(error.message || "Unable to save employee.", true);
        if(submitButton) {
            submitButton.disabled = false;
        }
    }
}
function showEmployeeFormMessage(message, isError) {
    const element = document.getElementById("employeeFormMessage");
    if(!element) {
        alert(message);
        return;
    }
    element.textContent = message;
    element.className = isError ? "error-message" : "success-message";
}
function resetAdminEmployeeForm() {
    editingEmployeeId = null;
    const form = document.getElementById("employeeForm");
    if(form) {
        form.reset();
    }
    const formSection = document.getElementById("employeeFormSection");
    const heading = document.getElementById("employeeFormTitle");
    const submitButton = document.getElementById("employeeSubmitButton");
    const message = document.getElementById("employeeFormMessage");
    if(heading) {
        heading.textContent = "Add New Employee";
    }
    if(submitButton) {
        submitButton.textContent = "Create Employee & Account";
        submitButton.disabled = false;
    }
    if(message) {
        message.textContent = "";
        message.className = "";
    }
    setAccountFieldsForCreate();
    if(formSection) {
        formSection.hidden = true;
    }
}
async function toggleAdminEmployeeStatus(employee) {
    if(!employee || employee.id === undefined) {
        return;
    }
    const currentlyActive = employee.active !== false;
    const action = currentlyActive ? "deactivate" : "activate";
    const confirmed = confirm(`Are you sure you want to ${action} ${employee.name}?`);
    if(!confirmed) return;
    try {
        await apiRequest(`${ADMIN_EMPLOYEE_API}/${employee.id}/${action}`, {
            method : "PATCH"
        });
        alert(currentlyActive ? "Employee and linked account deactivated successfully." : "Employee and linked account activated successfully.");
        await loadAdminEmployees();
    } catch(error) {
        console.error("Unable to change employee status:", error);
        alert("Unable to update employee status: " + error.message);
    }
}
function ensureAdminVisitReportContainer() {
    const root = document.getElementById("visitSection");
    if(!root) return null;
    let container = document.getElementById("adminVisitReportContent");
    if(container) return container;
    root.replaceChildren();
    const card = document.createElement("section");
    card.id = "adminVisitReport";
    card.className = "card";
    const heading = document.createElement("h3");
    heading.textContent = "Field Visit Summary";
    const description = document.createElement("p");
    description.textContent = "Review field visit activity and history across the organization.";
    container = document.createElement("div");
    container.id = "adminVisitReportContent";
    card.append(heading, description, container);
    root.appendChild(card);
    return container;
}
async function loadAdminFieldVisitReport() {
    const container = ensureAdminVisitReportContainer();
    if(!container) return;
    container.textContent = "Loading field visit summary...";
    const today = new Date().toLocaleDateString("en-CA");
    try {
        const data = await apiRequest(`/api/reports/field-visits?startDate=${today}&endDate=${today}`);
        container.replaceChildren();
        const metrics =[["Total visits",
        data.totalVisits],
        ["Completed visits",
        data.completedVisits],
        ["Incomplete visits",
        data.incompleteVisits],
        ["Total duration (seconds)",
        data.totalVisitDurationSeconds]];
        const metricsRow = document.createElement("div");
        metricsRow.className = "sync-summary";
        metrics.forEach(([label, value]) => {
            const item = document.createElement("div"); item.className = "summary-item"; const number = document.createElement("h3"); number.textContent = value ?? 0; const caption = document.createElement("p"); caption.textContent = label; item.append(number, caption); metricsRow.appendChild(item);
        });
        const breakdownHeading = document.createElement("h4");
        breakdownHeading.textContent = "Visit Type Breakdown";
        breakdownHeading.style.margin = "22px 0 10px";
        const breakdown = document.createElement("div");
        breakdown.className = "table-container";
        const breakdownRows = Array.isArray(data.visitTypeBreakdown) ? data.visitTypeBreakdown :[];
        if(breakdownRows.length > 0) {
            const breakdownTable = document.createElement("table");
            const head = document.createElement("thead");
            const headRow = document.createElement("tr");
            ["Visit Type",
            "Count"].forEach(label => {
                const th = document.createElement("th"); th.textContent = label; headRow.appendChild(th);
            });
            head.appendChild(headRow);
            breakdownTable.appendChild(head);
            const body = document.createElement("tbody");
            breakdownRows.forEach(item => {
                const row = document.createElement("tr");[item.visitType, item.count].forEach(value => {
                    const cell = document.createElement("td"); cell.textContent = value ?? "—"; row.appendChild(cell);
                }); body.appendChild(row);
            });
            breakdownTable.appendChild(body);
            breakdown.appendChild(breakdownTable);
        } else {
            breakdown.textContent = "No visit type breakdown available.";
        }
        const historyHeading = document.createElement("h3");
        historyHeading.textContent = "Field Visit History";
        historyHeading.style.margin = "26px 0 12px";
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
        container.append(metricsRow, breakdownHeading, breakdown, historyHeading, filters, history);
        await loadAdminFieldVisitHistory();
    } catch(error) {
        container.textContent = "Unable to load field visit summary: " + error.message;
    }
}
async function loadAdminFieldVisitHistory() {
    const history = document.getElementById("adminVisitHistory");
    if(!history) return;
    const start = document.getElementById("adminVisitStartDate")?.value;
    const end = document.getElementById("adminVisitEndDate")?.value;
    if(!start || !end || start > end) {
        history.textContent = "Please select a valid date range.";
        return;
    }
    history.textContent = "Loading field visit history...";
    try {
        const data = await apiRequest("/api/field-visits");
        const visits = Array.isArray(data) ? data :[];
        const filteredVisits = visits.filter(visit => {
            if(!visit.startTime) return false; const visitDate = String(visit.startTime).slice(0, 10); return visitDate >= start && visitDate <= end;
        });
        history.replaceChildren();
        if(filteredVisits.length === 0) {
            history.textContent = "No field visits found for the selected date range.";
            return;
        }
        const table = document.createElement("table");
        const thead = document.createElement("thead");
        const headerRow = document.createElement("tr");
        ["Visit ID",
        "Employee",
        "Visit Type",
        "Customer Reference",
        "Start Time",
        "End Time",
        "Status",
        "Duration (seconds)"].forEach(label => {
            const th = document.createElement("th"); th.textContent = label; headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);
        const tbody = document.createElement("tbody");
        filteredVisits.forEach(visit => {
            const row = document.createElement("tr"); const values =[visit.id, visit.employeeName || visit.employeeId, visit.visitType, visit.customerReference, visit.startTime, visit.endTime, visit.status, visit.durationSeconds]; values.forEach(value => {
                const cell = document.createElement("td"); cell.textContent = value ?? "—"; row.appendChild(cell);
            }); tbody.appendChild(row);
        });
        table.appendChild(tbody);
        history.appendChild(table);
    } catch(error) {
        history.textContent = "Unable to load field visit history: " + error.message;
    }
}
console.log("Admin module loaded.");