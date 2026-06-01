const state = {
    users: [],
    issues: []
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
    await Promise.all([loadStats(), loadUsers(), loadAnnouncements(), loadIssues(), loadAudit()]);
}

async function loadStats() {
    const stats = await request("/api/stats");
    const items = [
        ["Người dùng", stats.users],
        ["Đã xác thực", stats.verified],
        ["Đang khóa", stats.locked],
        ["Cần reset", stats.resetRequests],
        ["Lỗi mở", stats.openIssues],
        ["Thông báo bật", stats.activeAnnouncements]
    ];
    document.querySelector("#statGrid").innerHTML = items.map(([label, value]) => `
        <article class="stat">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(String(value))}</strong>
        </article>
    `).join("");
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
            <td class="user-cell"><strong>${escapeHtml(user.name)}</strong><span>${escapeHtml(user.email)}</span></td>
            <td>${escapeHtml(user.provider)}</td>
            <td><span class="badge ${user.verified ? "good" : "warn"}">${user.verified ? "Đã xác thực" : "Chưa xác thực"}</span></td>
            <td>${escapeHtml(user.lastSeenAt)}</td>
            <td>
                <span class="badge ${user.locked ? "danger" : "good"}">${user.locked ? "Đang khóa" : "Hoạt động"}</span>
                ${user.passwordResetRequested ? `<span class="badge warn">Cần reset mật khẩu</span>` : ""}
            </td>
            <td>
                <div class="actions">
                    <button class="secondary-button" data-user-action="${user.locked ? "unlock" : "lock"}" data-email="${escapeAttr(user.email)}">${user.locked ? "Mở khóa" : "Khóa"}</button>
                    <button class="secondary-button" data-user-action="${user.passwordResetRequested ? "clearReset" : "requestReset"}" data-email="${escapeAttr(user.email)}">${user.passwordResetRequested ? "Gỡ reset" : "Yêu cầu reset"}</button>
                    <button class="danger-button" data-user-action="delete" data-email="${escapeAttr(user.email)}">Xóa</button>
                </div>
            </td>
        </tr>
    `).join("") : `<tr><td colspan="6">Chưa có tài khoản phù hợp.</td></tr>`;
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
        await Promise.all([loadAnnouncements(), loadStats(), loadAudit()]);
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
            clearReset: "gỡ yêu cầu đặt lại mật khẩu"
        };
        const label = actionLabels[userAction.dataset.userAction] || "cập nhật";
        if (!confirm(`Bạn chắc chắn muốn ${label} tài khoản ${userAction.dataset.email}?`)) {
            return;
        }
        await act("/api/users/action", {email: userAction.dataset.email, action: userAction.dataset.userAction}, [loadUsers, loadStats, loadAudit]);
        return;
    }
    const announcementAction = event.target.closest("[data-announcement-action]");
    if (announcementAction) {
        if (announcementAction.dataset.announcementAction === "delete" && !confirm("Xóa thông báo này?")) {
            return;
        }
        await act("/api/announcements/action", {id: announcementAction.dataset.id, action: announcementAction.dataset.announcementAction}, [loadAnnouncements, loadStats, loadAudit]);
        return;
    }
    const issueAction = event.target.closest("[data-issue-action]");
    if (issueAction) {
        if (issueAction.dataset.issueAction === "delete" && !confirm("Xóa bản ghi lỗi này?")) {
            return;
        }
        await act("/api/issues/action", {id: issueAction.dataset.id, action: issueAction.dataset.issueAction}, [loadIssues, loadStats, loadAudit]);
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
