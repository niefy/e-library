"use strict";

const SESSION_KEY = "elibrary.session.v1";

const state = {
    token: null,
    userId: null,
    role: null,
    expiresAt: null,
    books: {page: 0, size: 8, totalPages: 1, query: ""},
    loans: {page: 0, size: 10, totalPages: 1, status: "ACTIVE"}
};

let booksRequestSeq = 0;
let loansRequestSeq = 0;
let toastTimer = null;

const byId = (id) => document.getElementById(id);

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== undefined && text !== null) {
        node.textContent = text;
    }
    return node;
}

function toast(message, kind = "info") {
    const box = byId("toast");
    box.textContent = message;
    box.className = kind === "error" ? "toast error" : "toast";
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => box.classList.add("hidden"), 5000);
}

function formatError(error) {
    const parts = [error && error.message ? error.message : "请求失败"];
    if (error && error.code) {
        parts.push("[" + error.code + "]");
    }
    if (error && error.traceId) {
        parts.push("trace " + String(error.traceId).slice(0, 12));
    }
    return parts.join(" · ");
}

async function run(action) {
    try {
        await action();
    } catch (error) {
        toast(formatError(error), "error");
    }
}

async function api(path, options = {}) {
    const headers = {Accept: "application/json"};
    if (state.token && !options.anonymous) {
        headers.Authorization = "Bearer " + state.token;
    }
    if (options.body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    let response;
    try {
        response = await fetch(path, {
            method: options.method || "GET",
            headers,
            body: options.body === undefined ? undefined : JSON.stringify(options.body)
        });
    } catch (networkError) {
        throw new Error("无法连接服务，请确认后端已启动");
    }

    let payload = null;
    if (response.status !== 204) {
        const text = await response.text();
        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (ignored) {
                payload = null;
            }
        }
    }

    if (!response.ok) {
        if (response.status === 401 && !options.anonymous) {
            clearSession();
            showLogin();
        }
        const error = new Error(
            (payload && (payload.detail || payload.title)) || "请求失败（HTTP " + response.status + "）"
        );
        error.status = response.status;
        error.code = payload && payload.code;
        error.traceId = (payload && payload.traceId) || response.headers.get("X-Trace-Id");
        throw error;
    }
    return payload;
}

function saveSession() {
    const session = {
        token: state.token,
        userId: state.userId,
        role: state.role,
        expiresAt: state.expiresAt
    };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
    state.token = null;
    state.userId = null;
    state.role = null;
    state.expiresAt = null;
    sessionStorage.removeItem(SESSION_KEY);
}

function restoreSession() {
    try {
        const raw = sessionStorage.getItem(SESSION_KEY);
        if (!raw) {
            return false;
        }
        const session = JSON.parse(raw);
        if (!session.token || !session.expiresAt) {
            return false;
        }
        if (new Date(session.expiresAt).getTime() - Date.now() < 30_000) {
            sessionStorage.removeItem(SESSION_KEY);
            return false;
        }
        state.token = session.token;
        state.userId = session.userId;
        state.role = session.role;
        state.expiresAt = session.expiresAt;
        return true;
    } catch (ignored) {
        sessionStorage.removeItem(SESSION_KEY);
        return false;
    }
}

function showLogin() {
    byId("session").classList.add("hidden");
    byId("app-panel").classList.add("hidden");
    byId("login-panel").classList.remove("hidden");
}

function showApp() {
    byId("session-user").textContent = state.userId + " · " + state.role;
    byId("session").classList.remove("hidden");
    byId("login-panel").classList.add("hidden");
    byId("app-panel").classList.remove("hidden");
}

function formatInstant(value) {
    if (!value) {
        return "—";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "—";
    }
    return new Intl.DateTimeFormat("zh-CN", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function updatePager(prefix, pageState) {
    const total = Math.max(1, pageState.totalPages || 1);
    byId(prefix + "-page").textContent = "第 " + Math.min(pageState.page + 1, total) + " / " + total + " 页";
    byId(prefix + "-prev").disabled = pageState.page <= 0;
    byId(prefix + "-next").disabled = pageState.page + 1 >= total;
}

function renderEmpty(container, message) {
    container.replaceChildren(el("div", "empty-state", message));
}

function bookCard(book) {
    const available = Number(book.availableCopies || 0);
    const total = Number(book.totalCopies || 0);

    const card = el("article", "book-card");
    card.append(el(
        "span",
        available > 0 ? "badge available" : "badge unavailable",
        available > 0 ? "可借 " + available + " / 共 " + total : "暂无可借"
    ));
    card.append(el("h2", null, book.title));
    card.append(el("p", "book-author", book.author));
    card.append(el("p", "book-isbn", "ISBN " + book.isbn));
    card.append(el("div", "spacer"));

    const button = el("button", "button primary", available > 0 ? "借阅" : "暂无库存");
    button.type = "button";
    button.disabled = available <= 0;
    button.addEventListener("click", () => run(async () => {
        button.disabled = true;
        button.textContent = "借阅中…";
        try {
            const loan = await api("/api/v1/loans", {
                method: "POST",
                body: {bookId: book.id}
            });
            toast("借阅成功，请在 " + formatInstant(loan.dueAt) + " 前归还");
            await Promise.all([loadBooks(), loadLoans()]);
        } finally {
            button.disabled = available <= 0;
            button.textContent = available > 0 ? "借阅" : "暂无库存";
        }
    }));
    card.append(button);
    return card;
}

function loanCard(loan) {
    const active = loan.status === "ACTIVE";
    const overdue = active && new Date(loan.dueAt).getTime() < Date.now();

    const card = el("div", "loan-card");
    const info = el("div");
    const titleRow = el("div", "loan-title");
    titleRow.append(el("h2", null, loan.bookTitle || "未知书目"));
    titleRow.append(el(
        "span",
        "badge " + (overdue ? "overdue" : active ? "active" : "returned"),
        overdue ? "已逾期" : active ? "借阅中" : "已归还"
    ));
    info.append(titleRow);

    const meta = el("div", "loan-meta");
    meta.append(el("span", null, "借出：" + formatInstant(loan.borrowedAt)));
    meta.append(el("span", null, "应还：" + formatInstant(loan.dueAt)));
    if (loan.returnedAt) {
        meta.append(el("span", null, "归还：" + formatInstant(loan.returnedAt)));
    }
    info.append(meta);
    card.append(info);

    if (active) {
        const button = el("button", "button primary", "归还");
        button.type = "button";
        button.addEventListener("click", () => run(async () => {
            button.disabled = true;
            button.textContent = "归还中…";
            try {
                const updated = await api(
                    "/api/v1/loans/" + encodeURIComponent(loan.id) + "/return",
                    {method: "POST"}
                );
                toast("已归还《" + (updated.bookTitle || "该书") + "》");
                await Promise.all([loadBooks(), loadLoans()]);
            } finally {
                button.disabled = false;
                button.textContent = "归还";
            }
        }));
        card.append(button);
    }
    return card;
}

async function loadBooks() {
    const requestSeq = ++booksRequestSeq;
    const params = new URLSearchParams({
        page: String(state.books.page),
        size: String(state.books.size)
    });
    if (state.books.query) {
        params.set("q", state.books.query);
    }

    const data = await api("/api/v1/books?" + params.toString());
    if (requestSeq !== booksRequestSeq) {
        return;
    }

    state.books.page = data.page;
    state.books.totalPages = data.totalPages;
    const container = byId("book-list");
    if (!data.items || data.items.length === 0) {
        renderEmpty(container, "没有找到匹配的书目");
    } else {
        container.replaceChildren(...data.items.map(bookCard));
    }
    updatePager("books", state.books);
}

async function loadLoans() {
    const requestSeq = ++loansRequestSeq;
    const params = new URLSearchParams({
        page: String(state.loans.page),
        size: String(state.loans.size)
    });
    if (state.loans.status) {
        params.set("status", state.loans.status);
    }

    const data = await api("/api/v1/me/loans?" + params.toString());
    if (requestSeq !== loansRequestSeq) {
        return;
    }

    state.loans.page = data.page;
    state.loans.totalPages = data.totalPages;
    const container = byId("loan-list");
    if (!data.items || data.items.length === 0) {
        renderEmpty(container, state.loans.status === "ACTIVE" ? "当前没有借阅中的书目" : "没有符合条件的借阅记录");
    } else {
        container.replaceChildren(...data.items.map(loanCard));
    }
    updatePager("loans", state.loans);
}

async function login(userId, role) {
    const data = await api("/api/v1/dev/tokens", {
        method: "POST",
        anonymous: true,
        body: {userId, role}
    });

    state.token = data.token;
    state.userId = data.userId;
    state.role = (data.roles && data.roles[0]) || role;
    state.expiresAt = data.expiresAt;
    saveSession();

    state.books.page = 0;
    state.books.query = "";
    state.loans.page = 0;
    state.loans.status = "ACTIVE";
    byId("search-input").value = "";
    byId("loan-status").value = "ACTIVE";

    showApp();
    await Promise.all([loadBooks(), loadLoans()]);
}

function switchTab(name) {
    document.querySelectorAll(".tab").forEach((tab) => {
        tab.classList.toggle("active", tab.dataset.tab === name);
    });
    byId("catalog-tab").classList.toggle("hidden", name !== "catalog");
    byId("loans-tab").classList.toggle("hidden", name !== "loans");
    if (name === "loans") {
        run(loadLoans);
    }
}

function bindEvents() {
    byId("login-form").addEventListener("submit", (event) => {
        event.preventDefault();
        const userId = byId("login-user").value.trim();
        const role = byId("login-role").value;
        if (!userId) {
            toast("请输入用户 ID", "error");
            return;
        }
        run(() => login(userId, role));
    });

    document.querySelectorAll("[data-demo-user]").forEach((button) => {
        button.addEventListener("click", () => {
            const userId = button.dataset.demoUser;
            const role = button.dataset.demoRole;
            byId("login-user").value = userId;
            byId("login-role").value = role;
            run(() => login(userId, role));
        });
    });

    byId("logout").addEventListener("click", () => {
        clearSession();
        showLogin();
        toast("已退出登录");
    });

    document.querySelectorAll(".tab").forEach((tab) => {
        tab.addEventListener("click", () => switchTab(tab.dataset.tab));
    });

    byId("search-form").addEventListener("submit", (event) => {
        event.preventDefault();
        state.books.query = byId("search-input").value.trim();
        state.books.page = 0;
        run(loadBooks);
    });

    byId("books-prev").addEventListener("click", () => {
        if (state.books.page > 0) {
            state.books.page -= 1;
            run(loadBooks);
        }
    });
    byId("books-next").addEventListener("click", () => {
        if (state.books.page + 1 < state.books.totalPages) {
            state.books.page += 1;
            run(loadBooks);
        }
    });

    byId("loan-status").addEventListener("change", (event) => {
        state.loans.status = event.target.value;
        state.loans.page = 0;
        run(loadLoans);
    });
    byId("refresh-loans").addEventListener("click", () => run(loadLoans));

    byId("loans-prev").addEventListener("click", () => {
        if (state.loans.page > 0) {
            state.loans.page -= 1;
            run(loadLoans);
        }
    });
    byId("loans-next").addEventListener("click", () => {
        if (state.loans.page + 1 < state.loans.totalPages) {
            state.loans.page += 1;
            run(loadLoans);
        }
    });
}

async function init() {
    bindEvents();
    if (!restoreSession()) {
        showLogin();
        return;
    }
    showApp();
    await run(async () => {
        await Promise.all([loadBooks(), loadLoans()]);
    });
}

init();
