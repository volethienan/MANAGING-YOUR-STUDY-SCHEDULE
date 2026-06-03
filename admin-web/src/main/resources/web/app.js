const state = {
    analytics: null,
    users: [],
    issues: [],
    templates: [],
    subjects: []
};

const loginView = document.querySelector("#loginView");
const dashboardView = document.querySelector("#dashboardView");
const loginError = document.querySelector("#loginError");
const actionStatus = document.querySelector("#actionStatus");

async function request(path, options = {}) {
    const response = await fetch(path, {
        credentials: "same-origin",
        headers: {"Content-Type": "application/json", ...(options.headers || {})},
        ...options
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(payload.error || "Không gọi được máy chủ quản trị");
    }
    return payload;
}

async function boot() {
    const session = await request("/api/auth/session");
    if (session.authenticated) {
        showDashboard(session.username);
        await refreshAll();
    } else {
        showLogin();
    }
}

function showLogin() {
    loginView.classList.remove("hidden");
    dashboardView.classList.add("hidden");
}

function showDashboard(username) {
    document.querySelector("#adminName").textContent = username;
    loginView.classList.add("hidden");
    dashboardView.classList.remove("hidden");
}

async function refreshAll() {
    await Promise.all([
        loadAnalytics(),
        loadUsers(),
        loadAnnouncements(),
        loadTemplates(),
        loadSubjects(),
        loadIssues(),
        loadAudit()
    ]);
}

async function loadAnalytics() {
    state.analytics = await request("/api/analytics");
    renderStats();
    renderOverviewCharts();
    renderReports();
}

function renderStats() {
    const stats = state.analytics.summary;
    const items = [
        ["Tài khoản", stats.users],
        ["DAU / MAU", `${stats.dau} / ${stats.mau}`],
        ["Hoàn thành task", `${stats.completionRate}%`],
        ["Phút Pomodoro", stats.focusMinutes],
        ["Premium", stats.premium],
        ["Lỗi mở", stats.openIssues]
    ];
    document.querySelector("#statGrid").innerHTML = items.map(([label, value]) => `
        <article class="stat">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(String(value))}</strong>
        </article>
    `).join("");
}

function renderOverviewCharts() {
    const stats = state.analytics.summary;
    renderBars("#growthChart", state.analytics.growth);
    renderRanks("#subjectChart", state.analytics.subjects, "Chưa có môn học nổi bật.");
    renderRanks("#featureChart", labelFeatures(state.analytics.features), "Chưa có dữ liệu sử dụng.");
    document.querySelector("#engagementList").innerHTML = `
        ${metricRow("Task hoàn thành", `${stats.completedTasks}/${stats.totalTasks}`)}
        ${metricRow("Lịch học đã tạo", stats.totalEvents)}
        ${metricRow("Phiên Pomodoro", stats.focusSessions)}
        ${metricRow("Thông báo đang bật", stats.activeAnnouncements)}
    `;
}

function renderReports() {
    const stats = state.analytics.summary;
    renderBars("#hourChart", state.analytics.studyHours);
    document.querySelector("#subscriptionReport").innerHTML = `
        ${metricRow("Người dùng Premium", stats.premium)}
        ${metricRow("Doanh thu ước tính", formatCurrency(stats.revenueEstimate))}
        ${metricRow("Tài khoản bị khóa", stats.locked)}
        ${metricRow("Tài khoản xác thực", stats.verified)}
    `;
}

async function loadUsers() {
    const payload = await request("/api/users");
    state.users = payload.users;
    renderUsers();
}

function renderUsers() {
    const search = document.querySelector("#userSearch").value.trim().toLowerCase();
    const status = document.querySelector("#userStatusFilter").value;
    const users = state.users.filter(user => {
        const matchesSearch = user.name.toLowerCase().includes(search) || user.email.toLowerCase().includes(search);
        const matchesStatus =
            status === "all" ||
            (status === "active" && !user.locked) ||
            (status === "locked" && user.locked) ||
            (status === "unverified" && !user.verified) ||
            (status === "reset" && user.passwordResetRequested);
        return matchesSearch && matchesStatus;
    });
    document.querySelector("#userRows").innerHTML = users.length ? users.map(user => `
        <tr>
            <td class="user-cell"><strong>${escapeHtml(user.name)}</strong><span>${escapeHtml(user.email)}</span><span>${escapeHtml(user.provider)} · ${escapeHtml(user.lastSeenAt)}</span></td>
            <td><span class="badge">${escapeHtml(roleLabel(user.role))}</span></td>
            <td><span class="badge ${user.plan === "Premium" ? "good" : ""}">${escapeHtml(user.plan)}</span></td>
            <td><span class="badge ${user.verified ? "good" : "warn"}">${user.verified ? "Đã xác thực" : "Chưa xác thực"}</span></td>
            <td>
                <strong>${escapeHtml(String(user.completedTasks))}/${escapeHtml(String(user.totalTasks))} task</strong>
                <span class="table-sub">${escapeHtml(String(user.focusMinutes))} phút · ${escapeHtml(user.topSubject)}</span>
            </td>
            <td>
                <span class="badge ${user.locked ? "danger" : "good"}">${user.locked ? "Đang khóa" : "Hoạt động"}</span>
                ${user.passwordResetRequested ? `<span class="badge warn">Cần reset</span>` : ""}
            </td>
            <td>
                <div class="actions">
                    <button class="secondary-button" data-user-action="${user.locked ? "unlock" : "lock"}" data-email="${escapeAttr(user.email)}">${user.locked ? "Mở khóa" : "Khóa"}</button>
                    <button class="secondary-button" data-user-action="${user.plan === "Premium" ? "makeFree" : "makePremium"}" data-email="${escapeAttr(user.email)}">${user.plan === "Premium" ? "Free" : "Premium"}</button>
                    <button class="secondary-button" data-user-action="${user.role === "Teacher" ? "makeUser" : "makeTeacher"}" data-email="${escapeAttr(user.email)}">${user.role === "Teacher" ? "User" : "Teacher"}</button>
                    <button class="secondary-button" data-user-action="${user.passwordResetRequested ? "clearReset" : "requestReset"}" data-email="${escapeAttr(user.email)}">${user.passwordResetRequested ? "Gỡ reset" : "Reset"}</button>
                    <button class="danger-button" data-user-action="delete" data-email="${escapeAttr(user.email)}">Xóa</button>
                </div>
            </td>
        </tr>
    `).join("") : `<tr><td colspan="7">Chưa có tài khoản phù hợp.</td></tr>`;
}

async function loadAnnouncements() {
    const payload = await request("/api/announcements");
    document.querySelector("#announcementList").innerHTML = payload.announcements.length
        ? payload.announcements.map(announcement => `
            <section class="list-row">
                <div class="list-head">
                    <div>
                        <h4>${escapeHtml(announcement.title)}</h4>
                        <span class="meta">${escapeHtml(announcement.createdAt)}</span>
                    </div>
                    <span class="badge ${announcement.active ? "good" : "warn"}">${announcement.active ? "Đang bật" : "Đang tắt"}</span>
                </div>
                <p>${escapeHtml(announcement.body)}</p>
                <div class="actions">
                    <button class="secondary-button" data-announcement-action="toggle" data-id="${escapeAttr(announcement.id)}">${announcement.active ? "Tắt" : "Bật"}</button>
                    <button class="danger-button" data-announcement-action="delete" data-id="${escapeAttr(announcement.id)}">Xóa</button>
                </div>
            </section>
        `).join("")
        : `<p>Chưa có thông báo.</p>`;
}

async function loadTemplates() {
    const payload = await request("/api/templates");
    state.templates = payload.templates;
    document.querySelector("#templateList").innerHTML = state.templates.length
        ? state.templates.map(template => `
            <section class="list-row">
                <div class="list-head">
                    <div>
                        <h4>${escapeHtml(template.title)}</h4>
                        <span class="meta">${escapeHtml(template.audience)} · ${escapeHtml(template.createdAt)}</span>
                    </div>
                    <span class="badge ${template.active ? "good" : "warn"}">${template.active ? "Đang bật" : "Đang tắt"}</span>
                </div>
                <p>${escapeHtml(template.description)}</p>
                <div class="actions">
                    <button class="secondary-button" data-template-action="toggle" data-id="${escapeAttr(template.id)}">${template.active ? "Tắt" : "Bật"}</button>
                    <button class="danger-button" data-template-action="delete" data-id="${escapeAttr(template.id)}">Xóa</button>
                </div>
            </section>
        `).join("")
        : `<p>Chưa có template.</p>`;
}

async function loadSubjects() {
    const payload = await request("/api/subjects");
    state.subjects = payload.subjects;
    document.querySelector("#subjectList").innerHTML = state.subjects.length
        ? state.subjects.map(subject => `
            <span class="subject-chip ${subject.active ? "" : "inactive"}">
                <strong>${escapeHtml(subject.name)}</strong>
                <small>${escapeHtml(subject.category)}</small>
                <button data-subject-action="toggle" data-id="${escapeAttr(subject.id)}">${subject.active ? "Tắt" : "Bật"}</button>
                <button data-subject-action="delete" data-id="${escapeAttr(subject.id)}">Xóa</button>
            </span>
        `).join("")
        : `<p>Chưa có danh mục.</p>`;
}

async function loadIssues() {
    const payload = await request("/api/issues");
    state.issues = payload.issues;
    renderIssues();
}

function renderIssues() {
    const filter = document.querySelector("#issueFilter").value;
    const issues = state.issues.filter(issue =>
        filter === "all" ||
        issue.status === filter ||
        issue.type.toLowerCase() === filter
    );
    document.querySelector("#issueList").innerHTML = issues.length
        ? issues.map(issue => `
            <section class="list-row">
                <div class="list-head">
                    <div>
                        <h4>${escapeHtml(issue.type.toUpperCase())} - ${escapeHtml(issue.email)}</h4>
                        <span class="meta">${escapeHtml(issue.createdAt)}</span>
                    </div>
                    <span class="badge ${issue.status === "open" ? "danger" : "good"}">${issue.status === "open" ? "Chưa xử lý" : "Đã xử lý"}</span>
                </div>
                <p>${escapeHtml(issue.message)}</p>
                <div class="actions">
                    <button class="secondary-button" data-issue-action="${issue.status === "open" ? "resolve" : "reopen"}" data-id="${escapeAttr(issue.id)}">${issue.status === "open" ? "Đóng lỗi" : "Mở lại"}</button>
                    <button class="danger-button" data-issue-action="delete" data-id="${escapeAttr(issue.id)}">Xóa</button>
                </div>
            </section>
        `).join("")
        : `<p>Chưa có lỗi phù hợp.</p>`;
}

async function loadAudit() {
    const payload = await request("/api/audit");
    document.querySelector("#auditList").innerHTML = payload.entries.length
        ? payload.entries.map(entry => `
            <div class="audit-row">
                <div><strong>${escapeHtml(entry.action)}</strong><div class="meta">${escapeHtml(entry.detail)}</div></div>
                <span>${escapeHtml(entry.createdAt)}</span>
            </div>
        `).join("")
        : `<p>Chưa có hoạt động.</p>`;
}

document.querySelector("#loginForm").addEventListener("submit", async event => {
    event.preventDefault();
    loginError.textContent = "";
    const form = new FormData(event.currentTarget);
    try {
        const payload = await request("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                username: form.get("username"),
                password: form.get("password")
            })
        });
        showDashboard(payload.username);
        await refreshAll();
    } catch (error) {
        loginError.textContent = error.message;
    }
});

document.querySelector("#logoutButton").addEventListener("click", async () => {
    await request("/api/auth/logout", {method: "POST", body: "{}"});
    showLogin();
});

document.querySelector("#refreshButton").addEventListener("click", async () => {
    await runWithStatus("Đang làm mới dữ liệu...", "Dữ liệu đã được làm mới.", refreshAll);
});
document.querySelector("#userSearch").addEventListener("input", renderUsers);
document.querySelector("#userStatusFilter").addEventListener("change", renderUsers);
document.querySelector("#issueFilter").addEventListener("change", renderIssues);

document.querySelector("#announcementForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runWithStatus("Đang lưu thông báo...", "Đã lưu thông báo.", async () => {
        await request("/api/announcements", {
            method: "POST",
            body: JSON.stringify({title: form.get("title"), body: form.get("body")})
        });
        event.currentTarget.reset();
        await Promise.all([loadAnnouncements(), loadAnalytics(), loadAudit()]);
    });
});

document.querySelector("#templateForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runWithStatus("Đang lưu template...", "Đã lưu template.", async () => {
        await request("/api/templates", {
            method: "POST",
            body: JSON.stringify({title: form.get("title"), audience: form.get("audience"), description: form.get("description")})
        });
        event.currentTarget.reset();
        await Promise.all([loadTemplates(), loadAudit()]);
    });
});

document.querySelector("#subjectForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runWithStatus("Đang lưu danh mục...", "Đã lưu danh mục.", async () => {
        await request("/api/subjects", {
            method: "POST",
            body: JSON.stringify({name: form.get("name"), category: form.get("category")})
        });
        event.currentTarget.reset();
        await Promise.all([loadSubjects(), loadAudit()]);
    });
});

document.addEventListener("click", async event => {
    const nav = event.target.closest("[data-target]");
    if (nav) {
        document.querySelectorAll(".nav-item").forEach(button => button.classList.toggle("active", button === nav));
        document.querySelectorAll(".panel").forEach(panel => panel.classList.toggle("active-panel", panel.id === nav.dataset.target));
        return;
    }
    const userAction = event.target.closest("[data-user-action]");
    if (userAction) {
        const actionLabels = {
            delete: "xóa khỏi registry",
            lock: "khóa",
            unlock: "mở khóa",
            requestReset: "yêu cầu đặt lại mật khẩu",
            clearReset: "gỡ yêu cầu đặt lại mật khẩu",
            makePremium: "nâng lên Premium",
            makeFree: "chuyển về Free",
            makeTeacher: "đổi vai trò thành Teacher",
            makeUser: "đổi vai trò thành User"
        };
        const label = actionLabels[userAction.dataset.userAction] || "cập nhật";
        if (!confirm(`Bạn chắc chắn muốn ${label} tài khoản ${userAction.dataset.email}?`)) {
            return;
        }
        await act("/api/users/action", {email: userAction.dataset.email, action: userAction.dataset.userAction}, [loadUsers, loadAnalytics, loadAudit]);
        return;
    }
    const announcementAction = event.target.closest("[data-announcement-action]");
    if (announcementAction) {
        if (announcementAction.dataset.announcementAction === "delete" && !confirm("Xóa thông báo này?")) {
            return;
        }
        await act("/api/announcements/action", {id: announcementAction.dataset.id, action: announcementAction.dataset.announcementAction}, [loadAnnouncements, loadAnalytics, loadAudit]);
        return;
    }
    const templateAction = event.target.closest("[data-template-action]");
    if (templateAction) {
        if (templateAction.dataset.templateAction === "delete" && !confirm("Xóa template này?")) {
            return;
        }
        await act("/api/templates/action", {id: templateAction.dataset.id, action: templateAction.dataset.templateAction}, [loadTemplates, loadAudit]);
        return;
    }
    const subjectAction = event.target.closest("[data-subject-action]");
    if (subjectAction) {
        if (subjectAction.dataset.subjectAction === "delete" && !confirm("Xóa danh mục này?")) {
            return;
        }
        await act("/api/subjects/action", {id: subjectAction.dataset.id, action: subjectAction.dataset.subjectAction}, [loadSubjects, loadAudit]);
        return;
    }
    const issueAction = event.target.closest("[data-issue-action]");
    if (issueAction) {
        if (issueAction.dataset.issueAction === "delete" && !confirm("Xóa bản ghi lỗi này?")) {
            return;
        }
        await act("/api/issues/action", {id: issueAction.dataset.id, action: issueAction.dataset.issueAction}, [loadIssues, loadAnalytics, loadAudit]);
    }
});

async function act(path, body, loaders) {
    await runWithStatus("Đang cập nhật...", "Đã cập nhật thành công.", async () => {
        await request(path, {method: "POST", body: JSON.stringify(body)});
        await Promise.all(loaders.map(loader => loader()));
    });
}

async function runWithStatus(loadingMessage, successMessage, work) {
    setActionStatus(loadingMessage);
    try {
        await work();
        setActionStatus(successMessage, "success");
    } catch (error) {
        setActionStatus(error.message || "Thao tác thất bại", "error");
    }
}

function renderBars(selector, items) {
    const max = Math.max(1, ...items.map(item => item.value));
    document.querySelector(selector).innerHTML = items.length
        ? items.map(item => `
            <div class="bar-row">
                <span>${escapeHtml(item.label)}</span>
                <div><i style="width:${Math.max(6, Math.round(item.value / max * 100))}%"></i></div>
                <strong>${escapeHtml(String(item.value))}</strong>
            </div>
        `).join("")
        : `<p>Chưa có dữ liệu.</p>`;
}

function renderRanks(selector, items, emptyText) {
    const max = Math.max(1, ...items.map(item => item.value));
    document.querySelector(selector).innerHTML = items.length
        ? items.map(item => `
            <div class="rank-row">
                <span>${escapeHtml(item.label)}</span>
                <div><i style="width:${Math.max(8, Math.round(item.value / max * 100))}%"></i></div>
                <strong>${escapeHtml(String(item.value))}</strong>
            </div>
        `).join("")
        : `<p>${escapeHtml(emptyText)}</p>`;
}

function labelFeatures(items) {
    const labels = {tasks: "Task", schedule: "Lịch học", pomodoro: "Pomodoro", ai: "AI đọc lịch"};
    return items.map(item => ({label: labels[item.label] || item.label, value: item.value}));
}

function metricRow(label, value) {
    return `<div class="metric-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(String(value))}</strong></div>`;
}

function roleLabel(role) {
    const labels = {Admin: "Quản trị viên", Teacher: "Giáo viên", User: "Người dùng"};
    return labels[role] || role || "Người dùng";
}

function formatCurrency(value) {
    return `${Number(value || 0).toLocaleString("vi-VN")}đ`;
}

function setActionStatus(message, type = "") {
    if (!actionStatus) {
        return;
    }
    actionStatus.textContent = message;
    actionStatus.className = "action-status" + (type ? ` ${type}` : "");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}

function escapeAttr(value) {
    return escapeHtml(value).replaceAll("'", "&#39;");
}

boot().catch(error => {
    loginError.textContent = error.message;
    showLogin();
});
