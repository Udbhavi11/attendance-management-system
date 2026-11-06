
/* =========================================================
   WORKFORCE ATTENDANCE MANAGEMENT SYSTEM
   NAVIGATION MODULE
   File: navigation.js
   ========================================================= */

let portalSectionHistory = [];
let isRestoringSection = false;

document.addEventListener("DOMContentLoaded", function () {

    // Role selection
    document.querySelectorAll("[data-select-role]")
        .forEach(button => {
            button.addEventListener("click", function () {
                selectRole(this.dataset.selectRole);
            });
        });

    // Back to role selection from login
    document.getElementById("backToRolesBtn")
        ?.addEventListener("click", showRoleSelection);

    // Logout
    document.getElementById("logoutBtn")
        ?.addEventListener("click", logoutApplication);

    // Sidebar navigation
    document.querySelectorAll(".nav-btn")
        .forEach(button => {
            button.addEventListener("click", function () {
                showSection(this.dataset.section);
            });
        });

    // Header Back button
    document.getElementById("backBtn")
        ?.addEventListener("click", handlePortalBack);

    // Header Refresh button
    document.getElementById("refreshBtn")
        ?.addEventListener("click", refreshCurrentSection);

    // Browser Back / Forward
    window.addEventListener("popstate", handleBrowserNavigation);

    console.log("Navigation module initialized.");
});


/* =========================================================
   ROLE SELECTION
   ========================================================= */

function selectRole(role) {

    const allowedRoles = [
        "EMPLOYEE",
        "MANAGER",
        "ADMIN",
        "AUDITOR"
    ];

    if (!allowedRoles.includes(role)) {
        alert("Invalid portal selection.");
        return;
    }

    resetApplicationState();

    CURRENT_ROLE = role;

    portalSectionHistory = [];

    history.pushState(
        { screen: "login" },
        "",
        "#login"
    );

    document.getElementById("roleSelection").hidden = true;
    document.getElementById("appLayout").hidden = true;
    document.getElementById("roleAccess").hidden = false;

    document.getElementById("accessTitle").textContent =
        role === "EMPLOYEE" ? "Employee Login" :
        role === "MANAGER" ? "Manager Login" :
        role === "AUDITOR" ? "Auditor Login" :
        "Administrator Login";

    document.getElementById("accessDescription").textContent =
        "Enter your username and password to continue.";

    document.getElementById("loginUsername").value = "";
    document.getElementById("loginPassword").value = "";
    document.getElementById("loginMessage").textContent = "";

    document.getElementById("loginUsername").focus();
}


/* =========================================================
   OPEN ROLE PORTAL
   ========================================================= */

async function openPortal() {

    if (!CURRENT_ROLE) {
        throw new Error("No authenticated role is available.");
    }

    /*
     * Clear employee-specific profile information when
     * opening any portal other than the Employee Portal.
     *
     * This prevents stale employee profile details from
     * appearing when switching between user roles.
     */
    if (CURRENT_ROLE !== "EMPLOYEE") {

        const employeeProfileSection =
            document.getElementById("employeeProfileSection");

        if (employeeProfileSection) {
            employeeProfileSection.replaceChildren();
        }
    }

    portalSectionHistory = [];

    history.pushState(
        { screen: "portal" },
        "",
        "#portal"
    );

    document.getElementById("roleSelection").hidden = true;
    document.getElementById("roleAccess").hidden = true;
    document.getElementById("appLayout").hidden = false;

    const portalNames = {
        EMPLOYEE: "Employee Portal",
        MANAGER: "Manager Portal",
        ADMIN: "Administrator Portal",
        AUDITOR: "Auditor Portal"
    };

    const portalName = portalNames[CURRENT_ROLE];

    document.getElementById("portalHeading").textContent =
        portalName;

    document.getElementById("dashboardTitle").textContent =
        portalName;

    const welcomeMessage =
        document.getElementById("welcomeMessage");

    if (welcomeMessage) {
        welcomeMessage.textContent =
            "Welcome, " + CURRENT_USERNAME + ".";
    }

    ensureRoleSpecificNavigation();
    applyRoleNavigation();

    await showSection("dashboard", false);

    try {

        if (CURRENT_ROLE === "EMPLOYEE") {
            await loadEmployeeDashboard();

        } else if (CURRENT_ROLE === "MANAGER") {
            await loadManagerDashboard();

        } else if (CURRENT_ROLE === "ADMIN") {
            await loadAdminDashboard();

        } else if (CURRENT_ROLE === "AUDITOR") {
            await loadAuditorDashboard();
        }

    } catch (error) {

        console.error("Dashboard loading failed:", error);

        alert(
            "Unable to load dashboard: " + error.message
        );
    }
}


/* =========================================================
   ROLE-BASED NAVIGATION
   ========================================================= */

function applyRoleNavigation() {

    document.querySelectorAll(".nav-btn")
        .forEach(button => {

            const roles = (button.dataset.roles || "")
                .split(",")
                .map(role => role.trim());

            button.hidden = !roles.includes(CURRENT_ROLE);
        });
}


/* =========================================================
   SHOW SECTION
   ========================================================= */

async function showSection(sectionId, rememberHistory = true) {

    const selectedSection =
        document.getElementById(sectionId);

    if (!selectedSection || selectedSection.hidden) {
        return;
    }

    const currentSection =
        document.querySelector(".page-section.active");

    if (
        rememberHistory &&
        currentSection &&
        currentSection.id !== sectionId &&
        !isRestoringSection
    ) {
        portalSectionHistory.push(currentSection.id);
    }

    document.querySelectorAll(".page-section")
        .forEach(section => {
            section.classList.remove("active");
        });

    document.querySelectorAll(".nav-btn")
        .forEach(button => {
            button.classList.remove("active");
        });

    selectedSection.classList.add("active");

    const selectedButton = document.querySelector(
        `.nav-btn[data-section="${sectionId}"]`
    );

    selectedButton?.classList.add("active");

    await loadSectionContent(sectionId);
}


/* =========================================================
   LOAD SECTION CONTENT
   Preserve existing module loading
   ========================================================= */

async function loadSectionContent(sectionId) {

    try {

        // ADMIN
        if (CURRENT_ROLE === "ADMIN") {

            if (sectionId === "employees") {
                await loadAdminEmployees();
            }

            if (sectionId === "teams") {
                await loadAdminTeams();
            }

            if (
                sectionId === "offices" &&
                typeof loadAdminOffices === "function"
            ) {
                await loadAdminOffices();
            }

            if (
                sectionId === "visits" &&
                typeof loadAdminFieldVisitReport === "function"
            ) {
                await loadAdminFieldVisitReport();
            }
        }

        // MANAGER
        if (CURRENT_ROLE === "MANAGER") {

            if (
                sectionId === "team" &&
                typeof loadManagerTeam === "function"
            ) {
                await loadManagerTeam();
            }

            if (
                sectionId === "visits" &&
                typeof loadManagerFieldVisits === "function"
            ) {
                await loadManagerFieldVisits();
            }
        }

        // AUDITOR
        if (
            CURRENT_ROLE === "AUDITOR" &&
            sectionId === "reports" &&
            typeof loadAuditorReports === "function"
        ) {
            await loadAuditorReports();
        }

        // EMPLOYEE
        if (CURRENT_ROLE === "EMPLOYEE") {

            if (
                sectionId === "shifts" &&
                typeof loadShiftSection === "function"
            ) {
                await loadShiftSection();
            }

            if (
                sectionId === "attendance" &&
                typeof loadAttendance === "function"
            ) {
                await loadAttendance();
            }
        }

        // Corrections
        if (
            sectionId === "corrections" &&
            typeof loadCorrectionSection === "function"
        ) {
            await loadCorrectionSection();
        }

        // Field visits for employee
        if (
            CURRENT_ROLE === "EMPLOYEE" &&
            sectionId === "visits" &&
            typeof loadFieldVisitSection === "function"
        ) {
            await loadFieldVisitSection();
        }

        // Anomalies
        if (
            sectionId === "anomalies" &&
            typeof loadAnomalies === "function"
        ) {
            await loadAnomalies();
        }

    } catch (error) {

        console.error(
            "Unable to load section:",
            sectionId,
            error
        );
    }
}


/* =========================================================
   HEADER BACK BUTTON
   ========================================================= */

async function handlePortalBack() {

    if (!CURRENT_ROLE) {

        if (!document.getElementById("roleAccess").hidden) {
            showRoleSelection();
        }

        return;
    }

    const previousSection = portalSectionHistory.pop();

    if (previousSection) {

        isRestoringSection = true;

        try {
            await showSection(previousSection, false);
        } finally {
            isRestoringSection = false;
        }

    } else {

        // No previous section: return to dashboard.
        await showSection("dashboard", false);
    }
}


/* =========================================================
   REFRESH CURRENT SECTION
   ========================================================= */

async function refreshCurrentSection() {

    const refreshButton =
        document.getElementById("refreshBtn");

    if (refreshButton) {
        refreshButton.disabled = true;
        refreshButton.textContent = "Refreshing...";
    }

    try {

        if (!CURRENT_ROLE) {
            window.location.reload();
            return;
        }

        const activeSection =
            document.querySelector(".page-section.active");

        const sectionId =
            activeSection ? activeSection.id : "dashboard";

        // Refresh dashboard using the existing role loaders.
        if (sectionId === "dashboard") {

            if (CURRENT_ROLE === "EMPLOYEE") {
                await loadEmployeeDashboard();

            } else if (CURRENT_ROLE === "MANAGER") {
                await loadManagerDashboard();

            } else if (CURRENT_ROLE === "ADMIN") {
                await loadAdminDashboard();

            } else if (CURRENT_ROLE === "AUDITOR") {
                await loadAuditorDashboard();
            }

        } else {

            // Refresh only the currently open section.
            await loadSectionContent(sectionId);
        }

    } catch (error) {

        console.error("Refresh failed:", error);

        alert(
            "Unable to refresh this section. " +
            error.message
        );

    } finally {

        if (refreshButton) {
            refreshButton.disabled = false;
            refreshButton.textContent = "↻ Refresh";
        }
    }
}


/* =========================================================
   LOGOUT
   ========================================================= */

async function logoutApplication() {

    try {

        await apiRequest("/api/auth/logout", {
            method: "POST"
        });

    } catch (error) {

        console.error("Logout request failed:", error);

        alert(
            "Unable to complete logout. Please try again."
        );

        return;
    }

    portalSectionHistory = [];

    resetApplicationState();

    showHomeScreen();
}


/* =========================================================
   RETURN TO ROLE SELECTION
   ========================================================= */

function showRoleSelection() {

    resetApplicationState();

    portalSectionHistory = [];

    showHomeScreen();
}


/* =========================================================
   HOME SCREEN
   ========================================================= */

function showHomeScreen() {

    history.pushState(
        { screen: "home" },
        "",
        window.location.pathname
    );

    document.getElementById("appLayout").hidden = true;
    document.getElementById("roleAccess").hidden = true;
    document.getElementById("roleSelection").hidden = false;

    document.getElementById("loginMessage").textContent = "";
}


/* =========================================================
   BROWSER BACK / FORWARD NAVIGATION
   ========================================================= */

function handleBrowserNavigation(event) {

    const screen = event.state?.screen || "home";

    if (screen === "home") {

        if (CURRENT_ROLE) {

            document.getElementById("appLayout").hidden = false;
            document.getElementById("roleAccess").hidden = true;
            document.getElementById("roleSelection").hidden = true;

        } else {

            document.getElementById("appLayout").hidden = true;
            document.getElementById("roleAccess").hidden = true;
            document.getElementById("roleSelection").hidden = false;
        }

        return;
    }

    if (screen === "login" && !CURRENT_ROLE) {

        document.getElementById("appLayout").hidden = true;
        document.getElementById("roleSelection").hidden = true;
        document.getElementById("roleAccess").hidden = false;

        return;
    }

    if (screen === "portal" && CURRENT_ROLE) {

        document.getElementById("roleSelection").hidden = true;
        document.getElementById("roleAccess").hidden = true;
        document.getElementById("appLayout").hidden = false;

        applyRoleNavigation();

        const activeSection =
            document.querySelector(".page-section.active");

        if (!activeSection || activeSection.hidden) {
            showSection("dashboard", false);
        }
    }
}


/* =========================================================
   ROLE-SPECIFIC NAVIGATION ITEMS
   ========================================================= */

function ensureRoleSpecificNavigation() {

    const nav = document.querySelector(".sidebar-nav");
    const main = document.querySelector(".main-content");

    if (!nav || !main) return;

    const addNav = (id, label, roles, symbol) => {

        if (nav.querySelector(`[data-section="${id}"]`)) {
            return;
        }

        const button = document.createElement("button");

        button.type = "button";
        button.className = "nav-btn";
        button.dataset.section = id;
        button.dataset.roles = roles;

        const icon = document.createElement("span");
        icon.textContent = symbol;

        button.append(icon, document.createTextNode(" " + label));

        button.addEventListener("click", () => {
            showSection(id);
        });

        nav.appendChild(button);
    };

    const addPage = (id, title, description) => {

        if (document.getElementById(id)) return;

        const section = document.createElement("section");

        section.id = id;
        section.className = "page-section";

        const heading = document.createElement("h2");
        heading.textContent = title;

        const paragraph = document.createElement("p");
        paragraph.className = "section-description";
        paragraph.textContent = description;

        const content = document.createElement("div");
        content.id = id + "Content";

        section.append(heading, paragraph, content);

        main.appendChild(section);
    };

    // Auditor reports
    addNav(
        "reports",
        "Reports & Audit",
        "AUDITOR",
        "▤"
    );

    addPage(
        "reports",
        "Reports & Audit History",
        "Read-only reports and audit information."
    );

    // Admin and manager field visits are history/report views.
    const visitsButton =
        nav.querySelector('[data-section="visits"]');

    if (visitsButton) {
        visitsButton.dataset.roles =
            "ADMIN,MANAGER,EMPLOYEE";
    }

    // Offline sync is employee-only.
    const syncButton =
        nav.querySelector('[data-section="sync"]');

    if (syncButton) {
        syncButton.dataset.roles = "EMPLOYEE";
    }

    // Corrections
    const correctionsButton =
        nav.querySelector('[data-section="corrections"]');

    if (correctionsButton) {
        correctionsButton.dataset.roles =
            "EMPLOYEE,MANAGER";
    }

    // Attendance
    const attendanceButton =
        nav.querySelector('[data-section="attendance"]');

    if (attendanceButton) {
        attendanceButton.dataset.roles = "EMPLOYEE";
    }

    // Anomalies
    const anomalyButton =
        nav.querySelector('[data-section="anomalies"]');

    if (anomalyButton) {
        anomalyButton.dataset.roles =
            "ADMIN,MANAGER";
    }

    applyRoleNavigation();
}


/* =========================================================
   BACKWARD-COMPATIBLE REFRESH FUNCTION
   ========================================================= */

function refreshCurrentPortalSection() {
    return refreshCurrentSection();
}


/* =========================================================
   MODULE INITIALIZATION
   ========================================================= */

console.log("Navigation module loaded.");
