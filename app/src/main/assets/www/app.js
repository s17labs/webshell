/**
 * WebShell — starter app.js
 *
 * Replace this with your own application logic.
 * bridge.js is already loaded and NativeBridge.* is available.
 */

document.addEventListener('DOMContentLoaded', () => {

    // ── Bridge status ─────────────────────────────────
    const statusEl = document.getElementById('bridge-status');
    if (NativeBridge.isNative()) {
        statusEl.textContent = '✅ Running in Android WebView — bridge active';
        statusEl.style.color = 'var(--color-success)';
    } else {
        statusEl.textContent = '🌐 Running in browser — bridge in fallback mode';
        statusEl.style.color = 'var(--color-warning)';
    }

    // ── Device info ───────────────────────────────────
    const infoEl = document.getElementById('device-info');
    const info = NativeBridge.getDeviceInfo();
    infoEl.textContent = JSON.stringify(info, null, 2);

    // ── Buttons ───────────────────────────────────────
    document.getElementById('btn-toast').addEventListener('click', () => {
        NativeBridge.toast('Hello from WebShell! 👋');
    });

    document.getElementById('btn-vibrate').addEventListener('click', () => {
        NativeBridge.vibrate(200);
    });

    document.getElementById('btn-share').addEventListener('click', () => {
        NativeBridge.share('Check out WebShell — build Android apps with HTML/CSS/JS!');
    });

});
