
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   TEAM MANAGEMENT MODULE

   File: team.js

   ADMIN APIs:
   GET    /api/admin/teams
   GET    /api/admin/teams/{id}
   POST   /api/admin/teams
   PUT    /api/admin/teams/{id}
   DELETE /api/admin/teams/{id}

   MANAGER APIs:
   GET    /api/manager/my-team/members
   POST   /api/manager/my-team/members/{employeeId}
   ========================================================= */

const ADMIN_TEAM_API = "/api/admin/teams";

const MANAGER_TEAM_API = "/api/manager/my-team/members";

let adminTeams = [];
let editingTeamId = null;
let managerTeamMembers = [];


/* =========================================================
   1. ADMIN - LOAD ALL TEAMS
   ========================================================= */

async function loadAdminTeams() {

    const container = document.getElementById(
        "teamManagementSection"
    );

    if (!container) {
        console.error("Team management section not found.");
        return;
    }

    container.textContent = "Loading teams...";

    try {

        const teams = await apiRequest(ADMIN_TEAM_API);

        if (!Array.isArray(teams)) {
            throw new Error("Invalid team data received.");
        }

        adminTeams = teams;

        renderAdminTeams();

    } catch (error) {

        console.error("Unable to load teams:", error);

        container.replaceChildren();

        const message = document.createElement("p");
        message.className = "error-message";
        message.textContent =
            "Unable to load teams: " + error.message;

        const retryButton = document.createElement("button");
        retryButton.type = "button";
        retryButton.className = "secondary-btn";
        retryButton.textContent = "Retry";

        retryButton.addEventListener("click", loadAdminTeams);

        container.append(message, retryButton);
    }
}


/* =========================================================
   2. ADMIN - RENDER TEAMS
   ========================================================= */

function renderAdminTeams() {

    const container = document.getElementById(
        "teamManagementSection"
    );

    if (!container) {
        return;
    }

    container.replaceChildren();

    const card = document.createElement("div");
    card.className = "card";

    const header = document.createElement("div");
    header.className = "section-header";

    const heading = document.createElement("div");

    const title = document.createElement("h3");
    title.textContent = "Registered Teams";

    const count = document.createElement("p");
    count.textContent = "Total teams: " + adminTeams.length;

    heading.append(title, count);

    const actions = document.createElement("div");
    actions.className = "form-actions";

    const addButton = document.createElement("button");
    addButton.type = "button";
    addButton.className = "primary-btn";
    addButton.textContent = "+ Add Team";

    addButton.addEventListener("click", () => {
        showTeamForm();
    });

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.className = "secondary-btn";
    refreshButton.textContent = "Refresh";

    refreshButton.addEventListener("click", loadAdminTeams);

    actions.append(addButton, refreshButton);
    header.append(heading, actions);

    card.appendChild(header);

    const formContainer = document.createElement("div");
    formContainer.id = "teamFormContainer";
    formContainer.hidden = true;

    card.appendChild(formContainer);

    const tableContainer = document.createElement("div");
    tableContainer.className = "table-container";

    if (adminTeams.length === 0) {

        const emptyMessage = document.createElement("p");
        emptyMessage.textContent =
            "No teams found. Add your first team.";

        tableContainer.appendChild(emptyMessage);

    } else {

        const table = document.createElement("table");
        table.className = "table";

        const thead = document.createElement("thead");
        const headerRow = document.createElement("tr");

        [
            "ID",
            "Team Name",
            "Description",
            "Manager",
            "Actions"
        ].forEach(label => {

            const th = document.createElement("th");
            th.textContent = label;

            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);

        const tbody = document.createElement("tbody");

        adminTeams.forEach(team => {

            const row = document.createElement("tr");

            appendTeamCell(row, team.id);
            appendTeamCell(row, team.name);
            appendTeamCell(row, team.description);
            appendTeamCell(
                row,
                team.managerName ||
                team.managerId ||
                "Not assigned"
            );

            const actionCell = document.createElement("td");

            const editButton = document.createElement("button");
            editButton.type = "button";
            editButton.className = "secondary-btn";
            editButton.textContent = "Edit";

            editButton.addEventListener("click", () => {
                editTeam(team.id);
            });

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "danger-btn";
            deleteButton.textContent = "Delete";

            deleteButton.addEventListener("click", () => {
                deleteTeam(team.id);
            });

            actionCell.append(editButton, deleteButton);
            row.appendChild(actionCell);

            tbody.appendChild(row);
        });

        table.append(thead, tbody);
        tableContainer.appendChild(table);
    }

    card.appendChild(tableContainer);
    container.appendChild(card);
}


/* =========================================================
   3. CREATE TABLE CELL
   ========================================================= */

function appendTeamCell(row, value) {

    const cell = document.createElement("td");

    cell.textContent =
        value !== null && value !== undefined && value !== ""
            ? String(value)
            : "-";

    row.appendChild(cell);
}


/* =========================================================
   4. ADMIN - SHOW TEAM FORM
   ========================================================= */

function showTeamForm(team = null) {

    const formContainer = document.getElementById(
        "teamFormContainer"
    );

    if (!formContainer) {
        return;
    }

    editingTeamId = team ? Number(team.id) : null;

    formContainer.hidden = false;
    formContainer.replaceChildren();

    const formCard = document.createElement("div");
    formCard.className = "card";

    const heading = document.createElement("h3");

    heading.textContent = team
        ? "Edit Team"
        : "Add New Team";

    const form = document.createElement("form");
    form.id = "teamForm";

    const nameGroup = document.createElement("div");
    nameGroup.className = "form-group";

    const nameLabel = document.createElement("label");
    nameLabel.htmlFor = "teamName";
    nameLabel.textContent = "Team Name";

    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.id = "teamName";
    nameInput.name = "name";
    nameInput.maxLength = 100;
    nameInput.required = true;
    nameInput.placeholder = "Enter team name";
    nameInput.value = team ? team.name || "" : "";

    nameGroup.append(nameLabel, nameInput);

    const descriptionGroup = document.createElement("div");
    descriptionGroup.className = "form-group";

    const descriptionLabel = document.createElement("label");
    descriptionLabel.htmlFor = "teamDescription";
    descriptionLabel.textContent = "Description";

    const descriptionInput = document.createElement("textarea");
    descriptionInput.id = "teamDescription";
    descriptionInput.name = "description";
    descriptionInput.maxLength = 255;
    descriptionInput.rows = 3;
    descriptionInput.placeholder = "Enter team description";
    descriptionInput.value =
        team ? team.description || "" : "";

    descriptionGroup.append(
        descriptionLabel,
        descriptionInput
    );

    const managerGroup = document.createElement("div");
    managerGroup.className = "form-group";

    const managerLabel = document.createElement("label");
    managerLabel.htmlFor = "teamManagerId";
    managerLabel.textContent = "Manager Employee ID";

    const managerInput = document.createElement("input");
    managerInput.type = "number";
    managerInput.id = "teamManagerId";
    managerInput.name = "managerId";
    managerInput.min = "1";
    managerInput.step = "1";
    managerInput.required = true;
    managerInput.placeholder = "Enter manager employee ID";
    managerInput.value =
        team && team.managerId ? team.managerId : "";

    managerGroup.append(managerLabel, managerInput);

    const message = document.createElement("p");
    message.id = "teamFormMessage";
    message.setAttribute("role", "status");

    const buttons = document.createElement("div");
    buttons.className = "form-actions";

    const saveButton = document.createElement("button");
    saveButton.type = "submit";
    saveButton.className = "primary-btn";
    saveButton.textContent =
        team ? "Update Team" : "Create Team";

    const cancelButton = document.createElement("button");
    cancelButton.type = "button";
    cancelButton.className = "secondary-btn";
    cancelButton.textContent = "Cancel";

    cancelButton.addEventListener("click", closeTeamForm);

    buttons.append(saveButton, cancelButton);

    form.append(
        nameGroup,
        descriptionGroup,
        managerGroup,
        message,
        buttons
    );

    form.addEventListener("submit", saveTeam);

    formCard.append(heading, form);
    formContainer.appendChild(formCard);

    nameInput.focus();
}


/* =========================================================
   5. ADMIN - CLOSE TEAM FORM
   ========================================================= */

function closeTeamForm() {

    const formContainer = document.getElementById(
        "teamFormContainer"
    );

    if (formContainer) {
        formContainer.hidden = true;
        formContainer.replaceChildren();
    }

    editingTeamId = null;
}


/* =========================================================
   6. ADMIN - SAVE TEAM
   ========================================================= */

async function saveTeam(event) {

    event.preventDefault();

    const nameInput = document.getElementById("teamName");

    const descriptionInput =
        document.getElementById("teamDescription");

    const managerInput =
        document.getElementById("teamManagerId");

    const message =
        document.getElementById("teamFormMessage");

    const saveButton = event.submitter;

    const name = nameInput.value.trim();
    const description = descriptionInput.value.trim();
    const managerId = managerInput.value.trim();

    if (!name) {
        message.textContent = "Team name is required.";
        message.className = "error-message";
        nameInput.focus();
        return;
    }

    if (name.length > 100 || description.length > 255) {
        message.textContent =
            "Team name cannot exceed 100 characters and description cannot exceed 255 characters.";

        message.className = "error-message";
        return;
    }

    if (!managerId || !Number.isSafeInteger(Number(managerId)) ||
        Number(managerId) <= 0) {

        message.textContent =
            "A valid manager employee ID is required.";

        message.className = "error-message";
        managerInput.focus();
        return;
    }

    const teamData = {
        name,
        description: description || null,
        managerId: Number(managerId)
    };

    const isEditing = editingTeamId !== null;

    const url = isEditing
        ? `${ADMIN_TEAM_API}/${editingTeamId}`
        : ADMIN_TEAM_API;

    try {

        saveButton.disabled = true;

        saveButton.textContent =
            isEditing ? "Updating..." : "Creating...";

        await apiRequest(url, {
            method: isEditing ? "PUT" : "POST",
            body: JSON.stringify(teamData)
        });

        alert(
            isEditing
                ? "Team updated successfully."
                : "Team created successfully."
        );

        editingTeamId = null;

        await loadAdminTeams();

    } catch (error) {

        console.error("Team save failed:", error);

        message.textContent =
            error.message || "Unable to save team.";

        message.className = "error-message";

        saveButton.disabled = false;

        saveButton.textContent =
            isEditing ? "Update Team" : "Create Team";
    }
}


/* =========================================================
   7. ADMIN - EDIT TEAM
   ========================================================= */

function editTeam(teamId) {

    const team = adminTeams.find(
        item => Number(item.id) === Number(teamId)
    );

    if (!team) {
        alert("Team not found.");
        return;
    }

    showTeamForm(team);
}


/* =========================================================
   8. ADMIN - DELETE TEAM
   ========================================================= */

async function deleteTeam(teamId) {

    const team = adminTeams.find(
        item => Number(item.id) === Number(teamId)
    );

    if (!team) {
        alert("Team not found.");
        return;
    }

    const confirmed = confirm(
        `Are you sure you want to delete team "${team.name}"?`
    );

    if (!confirmed) {
        return;
    }

    try {

        await apiRequest(
            `${ADMIN_TEAM_API}/${teamId}`,
            {
                method: "DELETE"
            }
        );

        alert("Team deleted successfully.");

        await loadAdminTeams();

    } catch (error) {

        console.error("Team deletion failed:", error);

        alert(
            "Unable to delete team: " + error.message
        );
    }
}


/* =========================================================
   9. MANAGER - LOAD OWN TEAM MEMBERS
   ========================================================= */

async function loadManagerTeam() {

    const container = document.getElementById("teamSection");

    if (!container) {
        console.error("Manager team section not found.");
        return;
    }

    container.textContent = "Loading your team...";

    try {

        // The backend derives manager identity from the
        // authenticated account. No manager ID is supplied.

        const employees = await apiRequest(MANAGER_TEAM_API);

        if (!Array.isArray(employees)) {
            throw new Error("Invalid team member data received.");
        }

        managerTeamMembers = employees;

        renderManagerTeam(managerTeamMembers);

    } catch (error) {

        console.error("Unable to load manager team:", error);

        container.replaceChildren();

        const message = document.createElement("p");
        message.className = "error-message";
        message.textContent =
            "Unable to load your team: " + error.message;

        const retryButton = document.createElement("button");
        retryButton.type = "button";
        retryButton.className = "secondary-btn";
        retryButton.textContent = "Retry";

        retryButton.addEventListener("click", loadManagerTeam);

        container.append(message, retryButton);
    }
}


/* =========================================================
   10. MANAGER - RENDER OWN TEAM
   ========================================================= */

function renderManagerTeam(employees) {

    const container = document.getElementById("teamSection");

    if (!container) {
        return;
    }

    container.replaceChildren();

    const card = document.createElement("div");
    card.className = "card";

    const header = document.createElement("div");
    header.className = "section-header";

    const heading = document.createElement("div");

    const title = document.createElement("h3");
    title.textContent = "My Team";

    const count = document.createElement("p");
    count.textContent =
        "Total assigned employees: " + employees.length;

    heading.append(title, count);

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.className = "secondary-btn";
    refreshButton.textContent = "Refresh";

    refreshButton.addEventListener("click", loadManagerTeam);

    header.append(heading, refreshButton);

    card.appendChild(header);

    // Manager can add an employee to their own team.
    // Backend validates that the employee is eligible
    // and does not already belong to another team.

    const addCard = document.createElement("div");
    addCard.className = "card";

    const addHeading = document.createElement("h4");
    addHeading.textContent = "Add Team Member";

    const addForm = document.createElement("form");
    addForm.id = "managerAddTeamMemberForm";

    const group = document.createElement("div");
    group.className = "form-group";

    const label = document.createElement("label");
    label.htmlFor = "teamMemberEmployeeId";
    label.textContent = "Employee ID";

    const input = document.createElement("input");
    input.id = "teamMemberEmployeeId";
    input.name = "employeeId";
    input.type = "number";
    input.min = "1";
    input.step = "1";
    input.required = true;
    input.placeholder = "Enter employee ID";

    group.append(label, input);

    const message = document.createElement("p");
    message.id = "managerTeamMessage";
    message.setAttribute("role", "status");

    const submitButton = document.createElement("button");
    submitButton.type = "submit";
    submitButton.className = "primary-btn";
    submitButton.textContent = "Add Employee";

    addForm.append(group, message, submitButton);

    addForm.addEventListener(
        "submit",
        addManagerTeamMember
    );

    addCard.append(addHeading, addForm);

    card.appendChild(addCard);

    const tableContainer = document.createElement("div");
    tableContainer.className = "table-container";

    if (employees.length === 0) {

        const emptyMessage = document.createElement("p");
        emptyMessage.textContent =
            "No employees are currently assigned to your team.";

        tableContainer.appendChild(emptyMessage);

    } else {

        const table = document.createElement("table");
        table.className = "table";

        const thead = document.createElement("thead");
        const headerRow = document.createElement("tr");

        [
            "Employee ID",
            "Employee Code",
            "Name",
            "Email",
            "Mobile",
            "Department",
            "Assignment",
            "Status"
        ].forEach(labelText => {

            const th = document.createElement("th");
            th.textContent = labelText;

            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);

        const tbody = document.createElement("tbody");

        employees.forEach(employee => {

            const row = document.createElement("tr");

            appendTeamCell(row, employee.id);
            appendTeamCell(row, employee.employeeCode);
            appendTeamCell(row, employee.name);
            appendTeamCell(row, employee.email);
            appendTeamCell(row, employee.mobileNumber);
            appendTeamCell(row, employee.department);

            appendTeamCell(
                row,
                employee.assignmentType === "FIELD_WORK"
                    ? "Field Work"
                    : "Office"
            );

            appendTeamCell(
                row,
                employee.active === false
                    ? "Inactive"
                    : "Active"
            );

            tbody.appendChild(row);
        });

        table.append(thead, tbody);
        tableContainer.appendChild(table);
    }

    card.appendChild(tableContainer);
    container.appendChild(card);
}


/* =========================================================
   11. MANAGER - ADD TEAM MEMBER
   ========================================================= */

async function addManagerTeamMember(event) {

    event.preventDefault();

    const input = document.getElementById(
        "teamMemberEmployeeId"
    );

    const message = document.getElementById(
        "managerTeamMessage"
    );

    const button = event.submitter;

    const employeeId = Number(input.value);

    if (
        !Number.isSafeInteger(employeeId) ||
        employeeId <= 0
    ) {

        message.textContent =
            "Enter a valid employee ID.";

        message.className = "error-message";

        input.focus();

        return;
    }

    try {

        button.disabled = true;
        button.textContent = "Adding...";

        await apiRequest(
            `${MANAGER_TEAM_API}/${employeeId}`,
            {
                method: "POST"
            }
        );

        alert("Employee added to your team successfully.");

        await loadManagerTeam();

    } catch (error) {

        console.error("Unable to add team member:", error);

        message.textContent =
            error.message || "Unable to add employee.";

        message.className = "error-message";

        button.disabled = false;
        button.textContent = "Add Employee";
    }
}


/* =========================================================
   12. TEAM MODULE INITIALIZATION
   ========================================================= */

console.log("Team management module loaded.");
