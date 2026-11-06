
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   EMPLOYEE MODULE
   File: employee.js
   ========================================================= */


/* =========================================================
   1. LOAD EMPLOYEE DASHBOARD
   ========================================================= */
   async function loadEmployeeDashboard() {

       if (!EMPLOYEE_ID) {
           throw new Error(
               "Employee information is unavailable."
           );
       }

       // Fetch the complete profile of the authenticated employee.
       const employee = await apiRequest("/api/auth/me");

       if (!employee || Number(employee.id) !== Number(EMPLOYEE_ID)) {
           throw new Error(
               "Unable to retrieve the authenticated employee profile."
           );
       }

       if (employee.active === false) {
           throw new Error("Your employee account is inactive.");
       }

       CURRENT_EMPLOYEE = employee;

       // Display employee ID.
       const employeeIdElement =
           document.getElementById("employeeId");

       if (employeeIdElement) {
           employeeIdElement.textContent = employee.id;
       }

       // Display welcome message.
       const welcomeElement =
           document.getElementById("welcomeMessage");

       if (welcomeElement) {
           welcomeElement.textContent =
               "Welcome, " + employee.name + "!";
       }

       // Display complete employee profile.
       renderEmployeeProfile(employee);

       // Existing dashboard functionality — preserved.
       renderEmployeeDashboardSummary();

       await refreshAttendanceDashboard();

       updateAttendanceButtons();

       console.log("Employee dashboard loaded.");
   }


/* =========================================================
   2. RENDER EMPLOYEE PROFILE
   ========================================================= */

function renderEmployeeProfile(employee) {

    const profileSection =
        document.getElementById("employeeProfileSection");

    if (!profileSection) {
        return;
    }

    profileSection.replaceChildren();

    const profileCard = document.createElement("div");

    profileCard.className = "card";

    const heading = document.createElement("h3");

    heading.textContent = "My Profile";

    profileCard.appendChild(heading);

    addProfileDetail(
        profileCard,
        "Employee ID",
        employee.id
    );

    addProfileDetail(
        profileCard,
        "Employee Code",
        employee.employeeCode
    );

    addProfileDetail(
        profileCard,
        "Name",
        employee.name
    );

    addProfileDetail(
        profileCard,
        "Email",
        employee.email
    );

    addProfileDetail(
        profileCard,
        "Mobile",
        employee.mobileNumber
    );

    addProfileDetail(
        profileCard,
        "Department",
        employee.department
    );

    addProfileDetail(
        profileCard,
        "Shift ID",
        employee.shiftId
    );

    addProfileDetail(
        profileCard,
        "Office ID",
        employee.officeId
    );

    addProfileDetail(
        profileCard,
        "Manager ID",
        employee.managerId
    );

    profileSection.appendChild(profileCard);
}


/* =========================================================
   3. DISPLAY EMPLOYEE PROFILE DETAIL
   ========================================================= */

function addProfileDetail(container, label, value) {

    const paragraph = document.createElement("p");

    paragraph.textContent =
        label + ": " +
        (value !== null && value !== undefined && value !== ""
            ? value
            : "Not assigned");

    container.appendChild(paragraph);
}


/* =========================================================
   4. RENDER EMPLOYEE DASHBOARD SUMMARY
   ========================================================= */

function renderEmployeeDashboardSummary() {

    const dashboardSection =
        document.getElementById("employeeDashboardSection");

    if (!dashboardSection) {
        return;
    }

    dashboardSection.replaceChildren();

    const attendanceCard = document.createElement("div");

    attendanceCard.className = "card";

    const attendanceHeading = document.createElement("h3");

    attendanceHeading.textContent = "My Attendance";

    const attendanceDescription = document.createElement("p");

    attendanceDescription.textContent =
        "Use the Attendance section to check in, check out and manage breaks.";

    attendanceCard.appendChild(attendanceHeading);

    attendanceCard.appendChild(attendanceDescription);

    const shiftCard = document.createElement("div");

    shiftCard.className = "card";

    const shiftHeading = document.createElement("h3");

    shiftHeading.textContent = "My Shift & Roster";

    const shiftDescription = document.createElement("p");

    shiftDescription.textContent =
        "Your assigned shift and roster information can be viewed in the Shifts & Rosters section.";

    shiftCard.appendChild(shiftHeading);

    shiftCard.appendChild(shiftDescription);

    dashboardSection.appendChild(attendanceCard);

    dashboardSection.appendChild(shiftCard);
}


/* =========================================================
   5. REFRESH EMPLOYEE PROFILE
   ========================================================= */

async function refreshEmployeeProfile() {

       if (!EMPLOYEE_ID) {
           throw new Error(
               "No employee is currently logged in."
           );
       }

       const employee = await apiRequest("/api/auth/me");

       if (!employee ||
           Number(employee.id) !== Number(EMPLOYEE_ID)) {

           throw new Error(
               "Employee details could not be retrieved."
           );
       }

       if (employee.active === false) {

           alert("Your employee account is inactive.");

           showRoleSelection();

           return;
       }

       CURRENT_EMPLOYEE = employee;

       renderEmployeeProfile(employee);

       console.log("Employee profile refreshed.");
   }


/* =========================================================
   6. EMPLOYEE MODULE LOADED
   ========================================================= */

console.log("Employee module loaded.");
