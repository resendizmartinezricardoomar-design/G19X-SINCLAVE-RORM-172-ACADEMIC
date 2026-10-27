/**
 * InsightHR Unified Dashboard Engine - v1000.20.0
 * Seguridad Mejorada: Se eliminó el acceso rápido inseguro.
 */

const API_BASE = "http://localhost:8000/api";
let currentUser = JSON.parse(localStorage.getItem('user')) || null;
let authToken = localStorage.getItem('token') || null; // Token para peticiones protegidas
let mainChart = null;
let compareChart = null;
let auditScores = {};

const AUDIT_METRICS = [
    { id: "obj_cumplimiento", label: "Objetivos" }, { id: "puntualidad_asistencia", label: "Puntualidad" },
    { id: "calidad_trabajo", label: "Calidad" }, { id: "productividad", label: "Productividad" },
    { id: "trabajo_equipo", label: "Equipo" }, { id: "conocimiento_habilidad", label: "Habilidad" },
    { id: "comunicacion", label: "Comunicación" }, { id: "adaptabilidad", label: "Adaptabilidad" },
    { id: "iniciativa", label: "Iniciativa" }, { id: "responsabilidad", label: "Responsabilidad" },
    { id: "actitud_profesional", label: "Actitud" }, { id: "desarrollo_profesional", label: "Desarrollo" }
];

async function apiFetch(endpoint, options = {}) {
    try {
        if (!options.headers) options.headers = {};
        if (options.body) options.headers['Content-Type'] = 'application/json';

        // Incluir token si existe
        if (authToken) {
            options.headers['Authorization'] = `Bearer ${authToken}`;
        }

        const response = await fetch(`${API_BASE}${endpoint}`, options);
        if (response.status === 401) {
            handleLogout();
            return null;
        }
        if (!response.ok) return null;
        return await response.json();
    } catch (e) { return null; }
}

// --- NAVEGACIÓN ---
window.showSection = function(sectionId, btnElement) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.add('hidden'));
    const section = document.getElementById(`section-${sectionId}`);
    if (section) section.classList.remove('hidden');

    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    if (btnElement) btnElement.classList.add('active');

    const titles = { 'dashboard': 'Dashboard General', 'talent': 'Gestión de Talento', 'compare': 'Comparativa Estratégica', 'evaluations': 'Historial de Auditorías' };
    if (titles[sectionId]) document.getElementById('page-title').innerText = titles[sectionId];

    const btnNew = document.getElementById('btn-show-audit');
    if (btnNew) {
        const isAdmin = currentUser && currentUser.rol && currentUser.rol.toUpperCase() === 'ADMIN';
        btnNew.style.display = (isAdmin && sectionId === 'evaluations') ? 'block' : 'none';
    }

    if (sectionId === 'dashboard') renderDashboard();
    if (sectionId === 'talent') renderTalent();
    if (sectionId === 'compare') renderCompare();
    if (sectionId === 'evaluations') renderAuditHistory();
};

// --- MÓDULO AUDITORÍAS ---
async function renderAuditHistory() {
    const table = document.getElementById('historyTable');
    if (!table) return;

    if (currentUser && currentUser.rol.toUpperCase() === 'ADMIN') {
        const emps = await apiFetch('/empleados');
        const select = document.getElementById('audit-user-select');
        if (select && emps) {
            select.innerHTML = emps.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        }
        renderAuditQuestions();
    }

    const evals = await apiFetch('/evaluaciones');
    if (evals) {
        table.innerHTML = evals.map(e => `
            <tr class="border-b border-slate-50 text-xs hover:bg-slate-50 transition-colors">
                <td class="px-8 py-4 font-bold text-slate-400">${e.fecha}</td>
                <td class="px-8 py-4 font-black text-slate-700">${e.evaluado_nombre}</td>
                <td class="px-8 py-4 text-center"><span class="bg-indigo-50 text-indigo-600 px-3 py-1 rounded-lg font-black">${e.puntuacion_promedio}</span></td>
                <td class="px-8 py-4 text-right">
                    <button onclick="downloadReport(${e.evaluado_id}, '${e.evaluado_nombre}')" class="text-blue-600 font-black uppercase text-[10px] hover:underline">Descargar PDF</button>
                </td>
            </tr>
        `).join('');
    }
}

function renderAuditQuestions() {
    const grid = document.getElementById('audit-questions-grid');
    if (!grid) return;
    grid.innerHTML = AUDIT_METRICS.map(m => `
        <div class="audit-card">
            <p class="text-[10px] font-black text-slate-700 uppercase mb-1">${m.label}</p>
            <div class="rating-box" id="group-${m.id}">
                ${[1, 2, 3, 4, 5].map(v => `<button type="button" onclick="setRating('${m.id}', ${v})" class="rate-btn" data-val="${v}">${v}</button>`).join('')}
            </div>
        </div>
    `).join('');
}

window.setRating = function(metricId, value) {
    auditScores[metricId] = value;
    document.querySelectorAll(`#group-${metricId} .rate-btn`).forEach(b => {
        b.classList.remove('selected');
        if (parseInt(b.dataset.val) === value) b.classList.add('selected');
    });
};

window.toggleAuditPanel = function() {
    const panel = document.getElementById('audit-form-panel');
    if (panel) panel.classList.toggle('hidden');
};

window.submitPremiumAudit = async function() {
    const userId = document.getElementById('audit-user-select').value;
    if (Object.keys(auditScores).length < 12) return alert("⚠️ Califica los 12 puntos estratégicos.");

    const payload = {
        evaluado_id: parseInt(userId),
        evaluador_id: currentUser.id,
        comentarios_texto: document.getElementById('audit-comments').value,
        tipo_evaluacion: "Auditoría Web Premium",
        relacion_evaluador: "Administración"
    };

    AUDIT_METRICS.forEach(m => { payload[m.id] = auditScores[m.id] * 20; });

    const res = await apiFetch('/evaluaciones', { method: 'POST', body: JSON.stringify(payload) });
    if (res) {
        alert("✅ Auditoría Publicada Exitosamente");
        window.toggleAuditPanel();
        renderAuditHistory();
        auditScores = {};
        document.getElementById('audit-comments').value = "";
    }
};

// --- DASHBOARD E IA ---
async function renderDashboard() {
    const emps = await apiFetch('/empleados');
    const table = document.getElementById('employeeTable');
    if (emps && table) {
        table.innerHTML = emps.sort((a,b) => b.score - a.score).map(u => `
            <tr class="hover:bg-slate-50 transition-colors border-b border-slate-50 cursor-pointer" onclick="updateRadar('radarChartMain', ${u.id}, '${u.nombre}')">
                <td class="px-8 py-5 font-bold text-slate-700">${u.nombre}</td>
                <td class="px-8 py-5 text-slate-400 text-[10px] font-bold uppercase">${u.rol}</td>
                <td class="px-8 py-5 text-right"><span class="bg-blue-50 text-blue-600 px-4 py-2 rounded-xl font-black">${u.score}</span></td>
            </tr>
        `).join('');
        if (emps.length > 0) updateRadar('radarChartMain', emps[0].id, emps[0].nombre);
    }
}

async function updateRadar(canvasId, userId, label) {
    const data = await apiFetch(`/evaluaciones/resumen/${userId}`);
    if (!data) return;

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

    if (data.analisis_ia) renderAIInsights(data.analisis_ia);
}

function renderAIInsights(aiData) {
    let container = document.getElementById('ai-insights-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'ai-insights-container';
        document.getElementById('section-dashboard').appendChild(container);
    }
    container.innerHTML = `
        <div class="mt-8 p-6 bg-indigo-50 rounded-3xl border border-indigo-100 shadow-sm col-span-full">
            <h6 class="text-indigo-900 font-black text-xs uppercase mb-3 flex items-center"><i class="fas fa-robot mr-2"></i> Análisis Insight AI</h6>
            <p class="text-slate-600 text-xs mb-3">Sentimiento: <b>${aiData.sentimiento}</b></p>
            <div class="flex flex-wrap gap-2">
                ${aiData.recomendaciones.map(r => `<span class="bg-white text-indigo-700 text-[9px] font-bold px-3 py-2 rounded-xl border border-indigo-100">${r}</span>`).join('')}
            </div>
        </div>
    `;
}

async function renderTalent() {
    const emps = await apiFetch('/empleados');
    if (emps) {
        document.getElementById('talentGrid').innerHTML = emps.map(u => `
            <div class="bg-white p-8 rounded-[2.5rem] shadow-sm border border-slate-100 text-center">
                <div class="w-16 h-16 bg-indigo-50 rounded-2xl mx-auto mb-4 flex items-center justify-center text-indigo-600 text-xl font-black">${u.nombre ? u.nombre[0] : '?'}</div>
                <h5 class="font-black text-slate-800 text-sm mb-1">${u.nombre || 'Sin nombre'}</h5>
                <button onclick="downloadReport(${u.id}, '${u.nombre}')" class="w-full py-2 bg-slate-900 text-white rounded-xl text-[9px] font-black uppercase mt-4">Reporte PDF</button>
            </div>
        `).join('');
    }
}

async function renderCompare() {
    const emps = await apiFetch('/empleados');
    const s1 = document.getElementById('compare-user-1');
    const s2 = document.getElementById('compare-user-2');
    if (emps && s1 && s2) {
        const html = emps.map(u => `<option value="${u.id}">${u.nombre}</option>`).join('');
        s1.innerHTML = html; s2.innerHTML = html;
        if(emps.length > 1) s2.selectedIndex = 1;
        updateComparison();
    }
}

window.updateComparison = async function() {
    const id1 = document.getElementById('compare-user-1').value;
    const id2 = document.getElementById('compare-user-2').value;
    const d1 = await apiFetch(`/evaluaciones/resumen/${id1}`);
    const d2 = await apiFetch(`/evaluaciones/resumen/${id2}`);
    if (!d1 || !d2) return;
    const ctx = document.getElementById('radarChartCompare').getContext('2d');
    if (compareChart) compareChart.destroy();
    compareChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: d1.labels,
            datasets: [{ label: 'Colaborador A', data: d1.scores, borderColor: '#6366F1', borderWidth: 2 }, { label: 'Colaborador B', data: d2.scores, borderColor: '#10B981', borderWidth: 2 }]
        }
    });
}

window.downloadReport = async (id, name) => {
    try {
        const response = await fetch(`${API_BASE}/reporte/${id}`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Reporte_InsightHR_${name.replace(/\s+/g, '_')}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
    } catch (e) { alert("Error al generar PDF"); }
};

window.handleLogin = async function() {
    const email = document.getElementById('login-email').value.trim();
    const pass = document.getElementById('login-pass').value.trim();
    const res = await apiFetch('/login', { method: 'POST', body: JSON.stringify({email, password: pass}) });
    if (res && res.user && res.access_token) {
        localStorage.setItem('user', JSON.stringify(res.user));
        localStorage.setItem('token', res.access_token);
        location.reload();
    } else { alert("Credenciales incorrectas o cuenta no activa"); }
};

window.handleLogout = function() {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    location.reload();
};

document.addEventListener('DOMContentLoaded', () => {
    if (currentUser && authToken) {
        document.getElementById('login-screen').style.display = 'none';
        const app = document.getElementById('app-content');
        app.classList.remove('hidden'); app.style.display = 'flex';
        document.body.style.overflow = 'auto';
        document.getElementById('user-role-badge').innerText = currentUser.rol;
        window.showSection('dashboard');
    }
});
