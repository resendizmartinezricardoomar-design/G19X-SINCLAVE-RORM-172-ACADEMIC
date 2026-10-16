/**
 * InsightHR Unified Dashboard Engine - v1000.10.0
 * Full Audit Integration
 */

const API_BASE = "http://localhost:8000/api";
let currentUser = JSON.parse(localStorage.getItem('user')) || null;
let mainChart = null;
let compareChart = null;
let allUsers = [];

// Mapeo exacto de los 12 ejes del radar
const AUDIT_METRICS = [
    { id: "obj_cumplimiento", label: "Objetivos", desc: "Cumplimiento de metas y KPIs" },
    { id: "puntualidad_asistencia", label: "Puntualidad", desc: "Disciplina y puntualidad" },
    { id: "calidad_trabajo", label: "Calidad", desc: "Excelencia en entregables" },
    { id: "productividad", label: "Productividad", desc: "Eficiencia y rendimiento" },
    { id: "trabajo_equipo", label: "Equipo", desc: "Colaboración proactiva" },
    { id: "conocimiento_habilidad", label: "Habilidad", desc: "Competencia técnica" },
    { id: "comunicacion", label: "Comunicación", desc: "Claridad y escucha" },
    { id: "adaptabilidad", label: "Adaptabilidad", desc: "Flexibilidad al cambio" },
    { id: "iniciativa", label: "Iniciativa", desc: "Proactividad propia" },
    { id: "responsabilidad", label: "Responsabilidad", desc: "Compromiso con tareas" },
    { id: "actitud_profesional", label: "Actitud", desc: "Ética y comportamiento" },
    { id: "desarrollo_profesional", label: "Desarrollo", desc: "Crecimiento y aprendizaje" }
];

let auditScores = {};

async function apiFetch(endpoint, options = {}) {
    try {
        if (options.body && (!options.headers || !options.headers['Content-Type'])) {
            options.headers = { ...options.headers, 'Content-Type': 'application/json' };
        }
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        const data = await response.json();
        if (!response.ok) {
            alert("❌ ERROR: " + (data.detail || "Fallo en la operación"));
            return null;
        }
        return data;
    } catch (e) {
        console.error("Connection error:", e);
        return null;
    }
}

// --- NAVEGACIÓN ---
window.showSection = function(sectionId, btnElement) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.add('hidden'));
    document.getElementById(`section-${sectionId}`).classList.remove('hidden');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    if (btnElement) btnElement.classList.add('active');

    if (sectionId === 'dashboard') renderDashboard();
    if (sectionId === 'talent') renderTalent();
    if (sectionId === 'compare') renderCompare();
    if (sectionId === 'evaluations') renderAuditHistory();
};

async function renderAuditHistory() {
    const table = document.getElementById('historyTable');
    const btnNew = document.getElementById('btn-show-audit');
    if (!table) return;

    if (currentUser && currentUser.rol === 'ADMIN') {
        if (btnNew) btnNew.classList.remove('hidden');
        const employees = await apiFetch('/empleados');
        if (employees) {
            document.getElementById('audit-user-select').innerHTML =
                employees.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }
        renderAuditQuestions();
    }

    const evals = await apiFetch('/evaluaciones');
    if (!evals || evals.length === 0) {
        table.innerHTML = '<tr><td colspan="4" class="p-10 text-center text-slate-300 font-bold uppercase text-[10px]">No hay auditorías registradas</td></tr>';
        return;
    }

    table.innerHTML = evals.map(e => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-8 py-6 text-xs text-slate-400 font-bold">${e.fecha}</td>
            <td class="px-8 py-6 font-bold text-slate-700">${e.evaluado_nombre || 'Usuario'}</td>
            <td class="px-8 py-6 text-center">
                <span class="bg-indigo-50 text-indigo-600 px-4 py-2 rounded-xl font-black">${e.puntuacion_promedio}</span>
            </td>
            <td class="px-8 py-6 text-right">
                <button onclick="downloadReport(${e.evaluado_id}, '${e.evaluado_nombre}')" class="bg-blue-50 text-blue-600 px-4 py-2 rounded-xl text-[10px] font-black uppercase hover:bg-blue-600 hover:text-white transition-all">PDF</button>
            </td>
        </tr>
    `).join('');
}

function renderAuditQuestions() {
    const container = document.getElementById('audit-questions-grid');
    if (!container) return;

    container.innerHTML = AUDIT_METRICS.map(m => `
        <div class="audit-card">
            <p class="q-label mb-1">${m.label}</p>
            <p class="text-[9px] text-slate-400 uppercase font-medium tracking-tighter">${m.desc}</p>
            <div class="rating-box" id="group-${m.id}">
                ${[1, 2, 3, 4, 5].map(v => `
                    <button type="button" onclick="setRating('${m.id}', ${v})" class="rate-btn" data-val="${v}">${v}</button>
                `).join('')}
            </div>
        </div>
    `).join('');
}

window.setRating = function(metricId, value) {
    auditScores[metricId] = value;
    const btns = document.querySelectorAll(`#group-${metricId} .rate-btn`);
    btns.forEach(b => {
        b.classList.remove('selected');
        if (parseInt(b.dataset.val) === value) b.classList.add('selected');
    });
};

window.submitPremiumAudit = async function() {
    const userId = document.getElementById('audit-user-select').value;
    const comments = document.getElementById('audit-comments').value;

    if (Object.keys(auditScores).length < 12) {
        return alert("⚠️ Por favor, califica los 12 indicadores de desempeño antes de continuar.");
    }

    const payload = {
        evaluado_id: parseInt(userId),
        evaluador_id: currentUser ? currentUser.id : null,
        comentarios_texto: comments,
        tipo_evaluacion: "Auditoría Estratégica",
        relacion_evaluador: "Administración"
    };

    AUDIT_METRICS.forEach(m => {
        payload[m.id] = auditScores[m.id] * 20;
    });

    const res = await apiFetch('/evaluaciones', { method: 'POST', body: JSON.stringify(payload) });
    if (res) {
        alert("✅ Auditoría publicada exitosamente.");
        if (typeof toggleAuditPanel === 'function') toggleAuditPanel();
        renderAuditHistory();
        auditScores = {};
    }
};

async function renderDashboard() {
    const employees = await apiFetch('/empleados');
    const table = document.getElementById('employeeTable');
    if (employees && table) {
        table.innerHTML = employees.sort((a,b) => b.score - a.score).map(u => `
            <tr class="hover:bg-slate-50 transition-colors">
                <td class="px-8 py-5 font-bold text-slate-700">${u.nombre}</td>
                <td class="px-8 py-5 text-slate-400 text-[10px] font-bold uppercase tracking-widest">${u.rol}</td>
                <td class="px-8 py-5 text-right"><span class="bg-blue-50 text-blue-600 px-4 py-2 rounded-xl font-black">${u.score}</span></td>
            </tr>
        `).join('');
        if (employees.length > 0) updateRadar('radarChartMain', employees[0].id, employees[0].nombre);
    }
}

async function renderTalent() {
    const employees = await apiFetch('/empleados');
    const grid = document.getElementById('talentGrid');
    if (employees && grid) {
        grid.innerHTML = employees.map(u => `
            <div class="bg-white p-8 rounded-[2.5rem] shadow-sm border border-slate-100 text-center relative group">
                ${currentUser && currentUser.rol === 'ADMIN' && u.email !== 'admin@insight.com' ? `
                <button onclick="window.deleteEmployee(${u.id}, '${u.nombre}')" class="absolute top-6 right-6 text-slate-200 hover:text-red-500 transition-colors"><i class="fas fa-trash-alt"></i></button>` : ''}
                <div class="w-16 h-16 bg-indigo-50 rounded-2xl mx-auto mb-4 flex items-center justify-center text-indigo-600 text-xl font-black ring-4 ring-indigo-50/50">${u.nombre[0]}</div>
                <h5 class="font-black text-slate-800 text-sm mb-1">${u.nombre}</h5>
                <p class="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-6">${u.rol}</p>
                <button onclick="downloadReport(${u.id}, '${u.nombre}')" class="w-full py-2 bg-slate-900 text-white rounded-xl text-[9px] font-black uppercase hover:bg-black transition-all">Ver Perfil</button>
            </div>
        `).join('');
    }
}

async function renderCompare() {
    const employees = await apiFetch('/empleados');
    if (employees) {
        const options = employees.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        document.getElementById('compare-user-1').innerHTML = options;
        document.getElementById('compare-user-2').innerHTML = options;
        if (employees.length > 1) document.getElementById('compare-user-2').selectedIndex = 1;
        window.updateComparison();
    }
}

window.updateComparison = async () => {
    const sel1 = document.getElementById('compare-user-1');
    const sel2 = document.getElementById('compare-user-2');
    const id1 = sel1.value;
    const id2 = sel2.value;
    const name1 = sel1.options[sel1.selectedIndex].text;
    const name2 = sel2.options[sel2.selectedIndex].text;

    const d1 = await apiFetch(`/evaluaciones/resumen/${id1}`);
    const d2 = await apiFetch(`/evaluaciones/resumen/${id2}`);
    const ctx = document.getElementById('radarChartCompare').getContext('2d');
    if (compareChart) compareChart.destroy();
    compareChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: d1.labels,
            datasets: [
                { label: name1, data: d1.scores, borderColor: '#6366F1', backgroundColor: 'rgba(99, 102, 241, 0.15)', borderWidth: 3 },
                { label: name2, data: d2.scores, borderColor: '#10B981', backgroundColor: 'rgba(16, 185, 129, 0.15)', borderWidth: 3 }
            ]
        },
        options: { scales: { r: { min: 0, max: 100 } } }
    });
};

async function updateRadar(canvasId, userId, label) {
    const data = await apiFetch(`/evaluaciones/resumen/${userId}`);
    const ctx = document.getElementById(canvasId).getContext('2d');
    if (mainChart) mainChart.destroy();
    mainChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: data.labels,
            datasets: [{ label: label, data: data.scores, borderColor: '#6366F1', backgroundColor: 'rgba(99, 102, 241, 0.15)', borderWidth: 3 }]
        },
        options: { scales: { r: { min: 0, max: 100 } } }
    });
}

// --- ACCIONES ---
window.downloadReport = async (userId, userName) => {
    try {
        const response = await fetch(`${API_BASE}/reporte/${userId}`);
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Reporte_${userName.replace(/\s+/g, '_')}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
    } catch (e) { alert("Error al generar PDF"); }
};

window.deleteEmployee = async (id, name) => {
    if (confirm(`¿Estás seguro de eliminar a ${name}?`)) {
        await apiFetch(`/usuarios/${id}`, { method: 'DELETE' });
        renderTalent();
    }
};

window.handleLogin = async function() {
    const email = document.getElementById('login-email').value;
    const pass = document.getElementById('login-pass').value;
    const res = await apiFetch('/login', { method: 'POST', body: JSON.stringify({email, password: pass}) });
    if (res && res.user) {
        localStorage.setItem('user', JSON.stringify(res.user));
        location.reload();
    }
};

window.publicRegister = async function() {
    const nombre = document.getElementById('reg-pub-nombre').value;
    const email = document.getElementById('reg-pub-email').value;
    const password = document.getElementById('reg-pub-pass').value;
    if (!nombre || !email || !password) return alert("Completa los campos.");
    const res = await apiFetch('/usuarios', { method: 'POST', body: JSON.stringify({ nombre, email, password }) });
    if (res) alert("✅ Registro iniciado. Revisa tu correo o terminal.");
};

window.verifyWebAccount = async function() {
    const emailLogin = document.getElementById('login-email').value;
    const emailReg = document.getElementById('reg-pub-email').value;
    const email = (emailReg || emailLogin || "").trim();
    const codigo = document.getElementById('verify-code').value.trim();

    if (!email) return alert("⚠️ Escribe tu correo en el campo de arriba.");
    if (!codigo) return alert("⚠️ Escribe el código de verificación.");

    const res = await apiFetch('/verificar', { method: 'POST', body: JSON.stringify({ email, codigo }) });
    if (res) {
        alert("✅ Cuenta activada. Procede a ingresar.");
        if (typeof switchTab === 'function') switchTab('login');
        document.getElementById('login-email').value = email;
    }
};

window.quickLogin = function() {
    const select = document.getElementById('quick-login-select');
    const user = allUsers.find(u => u.id == select.value);
    if (user) { localStorage.setItem('user', JSON.stringify(user)); location.reload(); }
};

document.addEventListener('DOMContentLoaded', async () => {
    if (currentUser) {
        document.getElementById('login-screen').classList.add('hidden');
        document.getElementById('app-content').classList.remove('hidden');
        document.getElementById('user-role-badge').innerText = currentUser.rol;
        renderDashboard();
    } else {
        const rawUsers = await apiFetch('/empleados');
        if (rawUsers) {
            allUsers = rawUsers;
            document.getElementById('quick-login-select').innerHTML = allUsers.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }
    }
});
