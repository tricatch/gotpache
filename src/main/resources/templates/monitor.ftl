<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gotpache Console - Log Monitor</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" crossorigin="anonymous" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            font-family: system-ui, -apple-system, sans-serif;
        }
        .header-gradient { background: #2c3e50; }
        .main-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            overflow: hidden;
        }
        .app {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            overflow: hidden;
        }
        .toolbar {
            flex: 0 0 auto;
            padding: 8px 12px;
            background: #fafafa;
            border-bottom: 1px solid #e5e5e5;
            display: flex;
            gap: 8px;
            align-items: center;
        }
        .toolbar-btn {
            width: 28px;
            height: 28px;
            padding: 0;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 2px solid #757575;
            cursor: pointer;
        }
        .toolbar-btn:hover { background: #f5f5f5; }
        .toolbar-btn.recording { border-color: #f0506e; }
        .toolbar-btn .toolbar-icon {
            font-size: 11px;
            color: inherit;
        }
        .toolbar-btn.recording .toolbar-icon { color: #f0506e; }
        .toolbar-btn .toolbar-icon.hidden { display: none !important; }
        .log-list {
            flex: 1 1 40%;
            min-height: 100px;
            overflow-y: auto;
            font-size: 13px;
            background: #fff;
            border-bottom: 1px solid #e0e0e0;
        }
        .log-table {
            width: 100%;
            border-collapse: collapse;
            font-family: system-ui, -apple-system, sans-serif;
        }
        .log-table th {
            text-align: left;
            padding: 6px 10px;
            background: #f1f3f4;
            border-bottom: 1px solid #dadce0;
            font-weight: 600;
            color: #5f6368;
            white-space: nowrap;
            position: sticky;
            top: 0;
            z-index: 1;
        }
        .log-table td {
            padding: 6px 10px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            transition: background 0.15s;
        }
        .log-table tbody tr:hover { background: #f8f9fa; }
        .log-table tbody tr.active { background: #e8f0fe; }
        .log-table .col-method { font-family: monospace; font-size: 12px; color: #5f6368; white-space: nowrap; }
        .log-table .col-host { font-size: 12px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; color: #5f6368; }
        .log-table .col-path { font-family: monospace; font-size: 12px; overflow: hidden; text-overflow: ellipsis; }
        .log-table .col-start { font-size: 12px; color: #5f6368; white-space: nowrap; }
        .log-table .col-duration { font-size: 12px; color: #5f6368; white-space: nowrap; }
        .log-table .col-code { color: #1e8e3e; font-weight: 500; white-space: nowrap; }
        .log-table .col-size { font-size: 12px; color: #5f6368; white-space: nowrap; }
        .splitter {
            flex: 0 0 6px;
            background: #e0e0e0;
            cursor: ns-resize;
            user-select: none;
        }
        .splitter:hover, .splitter.dragging { background: #2196f3; }
        .detail-tabs {
            flex: 0 0 auto;
            display: flex;
            gap: 0;
            padding: 0 8px;
            background: #fff;
            border-bottom: 1px solid #90b4e8;
        }
        .detail-tabs button {
            padding: 8px 12px;
            font-size: 12px;
            cursor: pointer;
            border: none;
            border-bottom: 3px solid transparent;
            background: transparent;
            color: #5f6368;
            margin-bottom: -1px;
        }
        .detail-tabs button:hover { color: #202124; }
        .detail-tabs button.active {
            color: #1967d2;
            background: #e8f0fe;
            border-bottom-color: #1967d2;
        }
        .detail-content {
            flex: 1 1 0;
            min-height: 0;
            position: relative;
            overflow: hidden;
        }
        .detail-panel {
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            overflow-y: scroll;
            overflow-x: hidden;
            padding: 8px 12px;
            font-size: 13px;
            font-family: monospace;
            background: #fff;
            white-space: pre-wrap;
            word-break: break-all;
            visibility: hidden;
            pointer-events: none;
        }
        .detail-panel.active {
            visibility: visible;
            pointer-events: auto;
        }
        .ws-frame {
            margin-bottom: 12px;
            padding: 8px 10px;
            background: #f8f9fa;
            border-left: 4px solid #2196f3;
            border-radius: 0 4px 4px 0;
        }
        .ws-frame .ws-frame-meta {
            font-size: 11px;
            color: #5f6368;
            display: block;
            margin-bottom: 4px;
        }
        .ws-frame .ws-frame-payload {
            margin: 0;
            font-size: 12px;
            white-space: pre-wrap;
            word-break: break-all;
        }
    </style>
</head>
<body>
    <!-- Header Section (external - previous style) -->
    <div class="header-gradient uk-section uk-section-small uk-light">
        <div class="uk-container uk-container-large">
            <div class="uk-flex uk-flex-between uk-flex-middle">
                <div>
                    <h1 class="uk-heading-small uk-margin-remove">
                        <span uk-icon="icon: world; ratio: 1.5" class="uk-margin-small-right"></span>
                        Log Monitor
                    </h1>
                    <p class="uk-margin-remove-top uk-text-meta">Select a request to view details</p>
                </div>
                <div>
                    <a href="/" class="uk-button uk-button-default">
                        <span uk-icon="icon: home"></span> Home
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- Main content (internal app) -->
    <div class="main-content">
        <div class="app">
            <div class="toolbar">
                <button type="button" class="toolbar-btn recording" id="monitor-start-stop-btn" title="Start/Stop">
                    <i class="fa-solid fa-circle toolbar-icon hidden" id="monitor-start-icon"></i>
                    <i class="fa-solid fa-square toolbar-icon" id="monitor-stop-icon"></i>
                </button>
                <button type="button" class="toolbar-btn" id="monitor-clear-btn" title="Clear">
                    <i class="fa-solid fa-broom toolbar-icon"></i>
                </button>
            </div>

        <div class="log-list" id="monitor-log-list" style="flex: 0 0 300px; min-height: 300px;">
            <table class="log-table">
                <thead>
                    <tr>
                        <th>Method</th>
                        <th>Host</th>
                        <th>Path</th>
                        <th>Start</th>
                        <th>Duration</th>
                        <th>Code</th>
                        <th>Size</th>
                    </tr>
                </thead>
                <tbody id="monitor-log-tbody"></tbody>
            </table>
        </div>

        <div class="splitter" id="monitor-splitter"></div>

        <div class="detail-tabs">
            <button type="button" data-tab="header" class="active">Header</button>
            <button type="button" data-tab="cookie">Cookie</button>
            <button type="button" data-tab="request">Request</button>
            <button type="button" data-tab="response">Response</button>
            <button type="button" data-tab="websocket">WebSocket</button>
        </div>

        <div class="detail-content">
            <div id="detail-header" class="detail-panel active">(Select a request)</div>
            <div id="detail-cookie" class="detail-panel">(Select a request)</div>
            <div id="detail-request" class="detail-panel">(Select a request)</div>
            <div id="detail-response" class="detail-panel">(Select a request)</div>
            <div id="detail-websocket" class="detail-panel">(Select a request)</div>
        </div>
        </div>
    </div>

    <#include "common/footer.ftl">

    <script>
        (function() {
            var logList = document.getElementById('monitor-log-list');
            var logTbody = document.getElementById('monitor-log-tbody');
            var clearBtn = document.getElementById('monitor-clear-btn');
            var startStopBtn = document.getElementById('monitor-start-stop-btn');
            var startIcon = document.getElementById('monitor-start-icon');
            var stopIcon = document.getElementById('monitor-stop-icon');
            var isRecording = true;

            // Tab switching
            document.querySelectorAll('.detail-tabs button').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var tab = this.getAttribute('data-tab');
                    document.querySelectorAll('.detail-tabs button').forEach(function(b) { b.classList.remove('active'); });
                    document.querySelectorAll('.detail-content .detail-panel').forEach(function(p) { p.classList.remove('active'); });
                    this.classList.add('active');
                    var panel = document.getElementById('detail-' + tab);
                    if (panel) {
                        panel.classList.add('active');
                        panel.scrollTop = 0;
                    }
                });
            });

            // Log list click
            logList.addEventListener('click', function(e) {
                var row = e.target.closest('tr[data-rid]');
                if (row) {
                    logTbody.querySelectorAll('tr[data-rid]').forEach(function(r) { r.classList.remove('active'); });
                    row.classList.add('active');
                    selectedRid = row.getAttribute('data-rid');
                    populateDetail(selectedRid);
                }
            });

            clearBtn.addEventListener('click', function() {
                if (logTbody) logTbody.innerHTML = '';
                ridStartMap = {};
                ridRowData = {};
                selectedRid = null;
                populateDetail(null);
            });

            startStopBtn.addEventListener('click', function() {
                isRecording = !isRecording;
                if (isRecording) {
                    startIcon.classList.add('hidden');
                    stopIcon.classList.remove('hidden');
                    startStopBtn.classList.add('recording');
                } else {
                    startIcon.classList.remove('hidden');
                    stopIcon.classList.add('hidden');
                    startStopBtn.classList.remove('recording');
                }
            });

            var ridStartMap = {};
            var ridRowData = {};
            var selectedRid = null;

            function parseFirstHeaderLine(headers) {
                if (!headers || !Array.isArray(headers) || headers.length === 0) return null;
                var first = headers[0];
                return (typeof first === 'string') ? first.trim() : null;
            }
            function getHeaderValue(headers, name) {
                if (!headers || !Array.isArray(headers)) return null;
                var lower = name.toLowerCase();
                for (var i = 1; i < headers.length; i++) {
                    var line = headers[i];
                    if (typeof line !== 'string') continue;
                    line = line.trim();
                    if (line.toLowerCase().indexOf(lower + ':') === 0) {
                        var idx = line.indexOf(':');
                        return idx >= 0 ? line.substring(idx + 1).trim() : null;
                    }
                }
                return null;
            }
            function getAllHeaderValues(headers, name) {
                if (!headers || !Array.isArray(headers)) return [];
                var lower = name.toLowerCase();
                var result = [];
                for (var i = 1; i < headers.length; i++) {
                    var line = headers[i];
                    if (typeof line !== 'string') continue;
                    line = line.trim();
                    if (line.toLowerCase().indexOf(lower + ':') === 0) {
                        var idx = line.indexOf(':');
                        if (idx >= 0) result.push(line.substring(idx + 1).trim());
                    }
                }
                return result;
            }
            function parseRequestLine(line) {
                if (!line) return { method: '-', path: '-' };
                var parts = line.split(/\s+/);
                if (parts.length >= 2) return { method: parts[0], path: parts[1] };
                return { method: '-', path: line || '-' };
            }
            function parseResponseLine(line) {
                if (!line) return null;
                var parts = line.split(/\s+/);
                if (parts.length >= 2) return parts[1];
                return null;
            }
            function formatTime(ms) {
                if (ms == null || ms < 0) return '-';
                if (ms < 1000) return ms + ' ms';
                return (ms / 1000).toFixed(2) + ' s';
            }
            function formatStart(ts) {
                if (ts == null || ts <= 0) return '-';
                var d = new Date(ts);
                return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2) + ':' + ('0' + d.getSeconds()).slice(-2) + '.' + ('00' + d.getMilliseconds()).slice(-3);
            }
            function formatSize(bytes) {
                if (bytes == null || bytes < 0) return '-';
                if (bytes < 1024) return bytes + ' B';
                if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1).replace(/\.0$/, '') + ' KB';
                return (bytes / (1024 * 1024)).toFixed(1).replace(/\.0$/, '') + ' MB';
            }
            function escapeHtml(s) {
                var div = document.createElement('div');
                div.textContent = String(s);
                return div.innerHTML;
            }
            function findRowByRid(rid) {
                return logTbody ? logTbody.querySelector('tr[data-rid="' + rid + '"]') : null;
            }
            function buildDisplay(rowData) {
                return {
                    method: rowData.method || '-',
                    host: rowData.host || '-',
                    path: rowData.path || '/',
                    start: formatStart(rowData.startTs),
                    duration: formatTime(rowData.timeMs),
                    code: rowData.code || '-',
                    size: formatSize(rowData.size)
                };
            }
            function addRow(rid, display) {
                var tr = document.createElement('tr');
                tr.setAttribute('data-rid', rid);
                tr.innerHTML = '<td class="col-method">' + escapeHtml(display.method) + '</td>' +
                    '<td class="col-host">' + escapeHtml(display.host) + '</td>' +
                    '<td class="col-path">' + escapeHtml(display.path) + '</td>' +
                    '<td class="col-start">' + escapeHtml(display.start) + '</td>' +
                    '<td class="col-duration">' + escapeHtml(display.duration) + '</td>' +
                    '<td class="col-code">' + escapeHtml(display.code) + '</td>' +
                    '<td class="col-size">' + escapeHtml(display.size) + '</td>';
                var newStartTs = (ridRowData[rid] && ridRowData[rid].startTs != null) ? ridRowData[rid].startTs : 0;
                var rows = logTbody.querySelectorAll('tr[data-rid]');
                var insertBefore = null;
                for (var i = 0; i < rows.length; i++) {
                    var existingRid = rows[i].getAttribute('data-rid');
                    var existingStartTs = (ridRowData[existingRid] && ridRowData[existingRid].startTs != null) ? ridRowData[existingRid].startTs : 0;
                    if (existingStartTs > newStartTs) {
                        insertBefore = rows[i];
                        break;
                    }
                }
                logTbody.insertBefore(tr, insertBefore);
            }
            function updateRow(row, display) {
                if (!row) return;
                row.querySelector('.col-method').textContent = display.method;
                row.querySelector('.col-host').textContent = display.host;
                row.querySelector('.col-path').textContent = display.path;
                row.querySelector('.col-start').textContent = display.start;
                row.querySelector('.col-duration').textContent = display.duration;
                row.querySelector('.col-code').textContent = display.code;
                row.querySelector('.col-size').textContent = display.size;
            }
            // 4가지 타입(REQ_HEADER/REQ_BODY/RES_HEADER/RES_BODY)은 모두 동일한 rid에 종속
            function upsertRow(data) {
                if (!logTbody || !isRecording) return;
                var rid = data.rid || '';
                if (!rid) return;
                var type = data.type || '';
                var timestamp = data.timestamp || 0;
                var headers = data.headers;
                var body = data.body;

                if (type === 'REQ_BODY' || type === 'RES_HEADER' || type === 'RES_BODY' || type === 'WS_FRAME') {
                    if (!findRowByRid(rid)) return;
                }

                if (!ridRowData[rid]) ridRowData[rid] = { method: '-', host: '-', path: '/', startTs: timestamp };

                if (type === 'WS_FRAME') {
                    if (!ridRowData[rid].wsFrames) ridRowData[rid].wsFrames = [];
                    ridRowData[rid].wsFrames.push({
                        opcode: data.opcode != null ? data.opcode : 0,
                        direction: data.wsDirection || '-',
                        body: body,
                        timestamp: timestamp
                    });
                    if (rid === selectedRid) updateWebSocketPanel(rid);
                    return;
                }

                if (type === 'REQ_HEADER') {
                    ridStartMap[rid] = timestamp;
                    ridRowData[rid].reqHeaders = headers;
                    ridRowData[rid].method = '-';
                    ridRowData[rid].host = '-';
                    ridRowData[rid].path = '/';
                    ridRowData[rid].startTs = timestamp;
                    var line = parseFirstHeaderLine(headers);
                    if (line) {
                        var req = parseRequestLine(line);
                        ridRowData[rid].method = req.method;
                        ridRowData[rid].path = req.path;
                    }
                    var host = getHeaderValue(headers, 'Host');
                    if (host) ridRowData[rid].host = host;
                    var reqCl = getHeaderValue(headers, 'Content-Length');
                    if (reqCl) { var n = parseInt(reqCl, 10); if (!isNaN(n)) ridRowData[rid].size = n; }
                } else if (type === 'REQ_BODY' && body != null) {
                    ridRowData[rid].reqBody = body;
                } else if (type === 'RES_HEADER') {
                    ridRowData[rid].resHeaders = headers || [];
                    ridRowData[rid].code = '-';
                    var line = parseFirstHeaderLine(headers);
                    if (line) ridRowData[rid].code = parseResponseLine(line) || '-';
                    var startTs = ridStartMap[rid];
                    if (startTs != null) ridRowData[rid].startTs = ridRowData[rid].startTs || startTs;
                    ridRowData[rid].timeMs = (startTs != null && startTs > 0 && timestamp > 0) ? (timestamp - startTs) : null;
                    var resCl = getHeaderValue(headers, 'Content-Length');
                    if (resCl) { var n = parseInt(resCl, 10); if (!isNaN(n)) ridRowData[rid].size = n; }
                } else if (type === 'RES_BODY' && body != null) {
                    ridRowData[rid].resBody = body;
                    if (ridRowData[rid].size == null) {
                        if (typeof body === 'string') {
                            try { ridRowData[rid].size = atob(body).length; } catch (x) { ridRowData[rid].size = body.length; }
                        } else if (Array.isArray(body)) ridRowData[rid].size = body.length;
                    }
                }

                var rowData = ridRowData[rid] || {};
                var display = buildDisplay(rowData);
                var row = findRowByRid(rid);
                if (row) {
                    updateRow(row, display);
                } else {
                    addRow(rid, display);
                }
            }

            function decodeBody(body) {
                if (body == null) return null;
                if (typeof body === 'string') {
                    try {
                        var binary = atob(body);
                        var bytes = new Uint8Array(binary.length);
                        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
                        return new TextDecoder().decode(bytes);
                    } catch (e) { return body; }
                }
                if (Array.isArray(body)) {
                    var bytes = new Uint8Array(body);
                    return new TextDecoder().decode(bytes);
                }
                return String(body);
            }
            function bodyToBase64(body) {
                if (body == null) return null;
                if (typeof body === 'string') return body;
                if (Array.isArray(body)) {
                    try {
                        var bytes = new Uint8Array(body);
                        var binary = '';
                        for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
                        return btoa(binary);
                    } catch (e) { return null; }
                }
                return null;
            }
            var IMAGE_EXT_MIME = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', gif: 'image/gif', webp: 'image/webp', bmp: 'image/bmp', svg: 'image/svg+xml', ico: 'image/x-icon' };
            function bodyToBytes(body) {
                if (body == null) return null;
                if (typeof body === 'string') {
                    try {
                        var binary = atob(body);
                        var arr = new Uint8Array(binary.length);
                        for (var i = 0; i < binary.length; i++) arr[i] = binary.charCodeAt(i);
                        return arr;
                    } catch (e) { return null; }
                }
                if (Array.isArray(body)) return new Uint8Array(body);
                return null;
            }
            function detectImageFromMagicBytes(bytes) {
                if (!bytes || bytes.length < 4) return null;
                if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47) return 'image/png';
                if (bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) return 'image/jpeg';
                if (bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x38) return 'image/gif';
                if (bytes[0] === 0x42 && bytes[1] === 0x4D) return 'image/bmp';
                if (bytes.length >= 12 && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46 &&
                    bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50) return 'image/webp';
                return null;
            }
            function isImageResponse(headers, body) {
                if (headers && Array.isArray(headers)) {
                    var ct = getHeaderValue(headers, 'Content-Type');
                    if (ct && ct.toLowerCase().indexOf('image/') === 0) return true;
                    var cd = getHeaderValue(headers, 'Content-Disposition');
                    if (cd) {
                        var m = cd.match(/filename\s*=\s*["']?([^"'\s]+)["']?/i);
                        if (m) {
                            var ext = (m[1].split('.').pop() || '').toLowerCase();
                            if (IMAGE_EXT_MIME[ext]) return true;
                        }
                    }
                }
                if (body) {
                    var bytes = bodyToBytes(body);
                    return bytes && !!detectImageFromMagicBytes(bytes);
                }
                return false;
            }
            function getImageMimeType(headers, body) {
                if (headers && Array.isArray(headers)) {
                    var ct = getHeaderValue(headers, 'Content-Type');
                    if (ct && ct.toLowerCase().indexOf('image/') === 0) {
                        var m = ct.split(';')[0].trim();
                        if (m) return m;
                    }
                    var cd = getHeaderValue(headers, 'Content-Disposition');
                    if (cd) {
                        var m = cd.match(/filename\s*=\s*["']?([^"'\s]+)["']?/i);
                        if (m) {
                            var ext = (m[1].split('.').pop() || '').toLowerCase();
                            return IMAGE_EXT_MIME[ext] || 'image/jpeg';
                        }
                    }
                }
                if (body) {
                    var bytes = bodyToBytes(body);
                    var mime = detectImageFromMagicBytes(bytes);
                    if (mime) return mime;
                }
                return 'image/jpeg';
            }
            function parseCookies(cookieHeader) {
                if (!cookieHeader) return [];
                var pairs = cookieHeader.split(';');
                var result = [];
                for (var i = 0; i < pairs.length; i++) {
                    var idx = pairs[i].indexOf('=');
                    var name = idx >= 0 ? pairs[i].substring(0, idx).trim() : pairs[i].trim();
                    var value = idx >= 0 ? pairs[i].substring(idx + 1).trim() : '';
                    if (name) result.push({ name: name, value: value });
                }
                return result;
            }
            function tryPrettyJson(str) {
                if (!str || typeof str !== 'string') return str;
                try {
                    var obj = JSON.parse(str);
                    return JSON.stringify(obj, null, 2);
                } catch (e) { return str; }
            }
            function parseUrlEncoded(str) {
                if (!str || typeof str !== 'string') return null;
                str = str.trim();
                if (!str) return null;
                var result = [];
                var pairs = str.split('&');
                for (var i = 0; i < pairs.length; i++) {
                    var eq = pairs[i].indexOf('=');
                    var name = eq >= 0 ? pairs[i].substring(0, eq) : pairs[i];
                    var value = eq >= 0 ? pairs[i].substring(eq + 1) : '';
                    try {
                        name = decodeURIComponent(name.replace(/\+/g, ' '));
                    } catch (e) { name = name.replace(/\+/g, ' '); }
                    try {
                        value = decodeURIComponent(value.replace(/\+/g, ' '));
                    } catch (e) { value = value.replace(/\+/g, ' '); }
                    result.push({ name: name, value: value });
                }
                return result.length ? result : null;
            }
            function parseQueryString(path) {
                if (!path || typeof path !== 'string') return null;
                var idx = path.indexOf('?');
                if (idx < 0) return null;
                return parseUrlEncoded(path.substring(idx + 1));
            }
            function formatQueryStringDisplay(path) {
                var params = parseQueryString(path);
                if (!params || params.length === 0) return null;
                var lines = ['Query String'];
                for (var i = 0; i < params.length; i++) {
                    lines.push('  ' + params[i].name + ': ' + params[i].value);
                }
                return lines.join('\n');
            }
            function isFormUrlEncoded(headers) {
                if (!headers || !Array.isArray(headers)) return false;
                var ct = getHeaderValue(headers, 'Content-Type');
                return ct && ct.toLowerCase().indexOf('application/x-www-form-urlencoded') >= 0;
            }
            function looksLikeFormUrlEncoded(str) {
                return str && typeof str === 'string' && str.indexOf('&') >= 0 && str.indexOf('=') >= 0;
            }
            function formatFormDisplay(bodyStr) {
                if (!bodyStr || typeof bodyStr !== 'string') return null;
                var params = parseUrlEncoded(bodyStr);
                if (!params || params.length === 0) return null;
                var lines = ['Form Data'];
                for (var i = 0; i < params.length; i++) {
                    lines.push('  ' + params[i].name + ': ' + params[i].value);
                }
                return lines.join('\n');
            }

            var OPCODE_NAMES = { 0: 'continuation', 1: 'text', 2: 'binary', 8: 'close', 9: 'ping', 10: 'pong' };
            function formatWsFramePayload(body) {
                if (!body) return '';
                var str = decodeBody(body);
                if (str && /^[\x20-\x7E\r\n\t]*$/.test(str)) return str;
                var bytes = bodyToBytes(body);
                if (!bytes || bytes.length === 0) return '(empty)';
                var hex = '';
                for (var i = 0; i < Math.min(bytes.length, 64); i++) hex += ('0' + bytes[i].toString(16)).slice(-2) + ' ';
                return bytes.length > 64 ? hex + '...' : hex.trim();
            }
            function renderWebSocketFrames(wsFrames) {
                if (!wsFrames || wsFrames.length === 0) return '(No WebSocket frames)';
                var html = [];
                for (var i = 0; i < wsFrames.length; i++) {
                    var f = wsFrames[i];
                    var dir = f.direction === 'REQ' ? 'Client →' : '← Server';
                    var op = OPCODE_NAMES[f.opcode] || ('0x' + f.opcode);
                    var time = formatStart(f.timestamp);
                    var payload = formatWsFramePayload(f.body);
                    html.push('<div class="ws-frame"><span class="ws-frame-meta">[' + time + '] ' + dir + ' ' + op + '</span><pre class="ws-frame-payload">' + escapeHtml(payload) + '</pre></div>');
                }
                return html.join('');
            }
            function updateWebSocketPanel(rid) {
                var panel = document.getElementById('detail-websocket');
                if (!panel) return;
                if (!rid || !ridRowData[rid]) {
                    panel.innerHTML = '(Select a request)';
                    return;
                }
                var wsFrames = ridRowData[rid].wsFrames || [];
                panel.innerHTML = renderWebSocketFrames(wsFrames);
                panel.scrollTop = panel.scrollHeight;
            }

            function populateDetail(rid) {
                selectedRid = rid;
                var empty = '(Select a request)';
                var panels = {
                    header: document.getElementById('detail-header'),
                    cookie: document.getElementById('detail-cookie'),
                    request: document.getElementById('detail-request'),
                    response: document.getElementById('detail-response'),
                    websocket: document.getElementById('detail-websocket')
                };

                if (!rid || !ridRowData[rid]) {
                    panels.header.textContent = empty;
                    panels.cookie.textContent = empty;
                    panels.request.textContent = empty;
                    panels.response.textContent = empty;
                    if (panels.websocket) panels.websocket.innerHTML = empty;
                    return;
                }

                var d = ridRowData[rid];
                var reqHeaders = d.reqHeaders || [];
                var resHeaders = d.resHeaders || [];
                var host = d.host || 'localhost';
                var method = d.method || 'GET';
                var path = d.path || '/';
                var code = d.code || '-';

                var headerLines = [];
                headerLines.push('General');
                headerLines.push('  Request URL: ' + (path.indexOf('://') >= 0 ? path : 'https://' + host + path));
                headerLines.push('  Request Method: ' + method);
                headerLines.push('  Status Code: ' + code);
                headerLines.push('');
                headerLines.push('Request Headers');
                for (var i = 0; i < reqHeaders.length; i++) {
                    if (typeof reqHeaders[i] === 'string') headerLines.push('  ' + reqHeaders[i]);
                }
                headerLines.push('');
                headerLines.push('Response Headers');
                for (var j = 0; j < resHeaders.length; j++) {
                    if (typeof resHeaders[j] === 'string') headerLines.push('  ' + resHeaders[j]);
                }
                panels.header.textContent = headerLines.join('\n');

                var cookieLines = [];
                var reqCookie = getHeaderValue(reqHeaders, 'Cookie');
                if (reqCookie) {
                    cookieLines.push('Request Cookies');
                    var cookies = parseCookies(reqCookie);
                    for (var k = 0; k < cookies.length; k++) {
                        cookieLines.push('  ' + cookies[k].name + ': ' + cookies[k].value);
                    }
                }
                var setCookies = getAllHeaderValues(resHeaders, 'Set-Cookie');
                if (setCookies && setCookies.length > 0) {
                    if (cookieLines.length) cookieLines.push('');
                    cookieLines.push('Response Set-Cookie');
                    for (var s = 0; s < setCookies.length; s++) {
                        cookieLines.push('  ' + setCookies[s]);
                    }
                }
                panels.cookie.textContent = cookieLines.length ? cookieLines.join('\n') : '(No cookies)';

                var reqBody = decodeBody(d.reqBody);
                var requestText = null;
                if (reqBody) {
                    if (looksLikeFormUrlEncoded(reqBody)) {
                        requestText = formatFormDisplay(reqBody) || reqBody;
                    } else if (isFormUrlEncoded(reqHeaders)) {
                        requestText = formatFormDisplay(reqBody) || reqBody;
                    } else {
                        requestText = tryPrettyJson(reqBody);
                    }
                }
                if (!requestText && method.toUpperCase() === 'GET') {
                    requestText = formatQueryStringDisplay(path) || '(No payload for GET request)';
                } else if (!requestText) {
                    requestText = '(No payload for ' + method + ' request)';
                }
                panels.request.textContent = requestText;

                if (isImageResponse(resHeaders, d.resBody) && d.resBody) {
                    var base64 = bodyToBase64(d.resBody);
                    if (base64) {
                        var mime = getImageMimeType(resHeaders, d.resBody);
                        panels.response.innerHTML = '<img src="data:' + mime + ';base64,' + base64 + '" alt="Response image" style="max-width:100%; height:auto;">';
                    } else {
                        panels.response.textContent = '(Failed to decode image)';
                    }
                } else {
                    var resBody = decodeBody(d.resBody);
                    panels.response.textContent = resBody ? tryPrettyJson(resBody) : '(No response body)';
                }

                if (panels.websocket) {
                    panels.websocket.innerHTML = renderWebSocketFrames(d.wsFrames || []);
                }
            }

            var eventSource = new EventSource('/monitor/event');
            eventSource.onmessage = function(e) {
                try {
                    var data = JSON.parse(e.data);
                    if (data.rid) upsertRow(data);
                } catch (err) { console.warn('Monitor SSE parse error:', err); }
            };
            eventSource.onerror = function() { console.warn('Monitor SSE connection error'); };
        })();

        (function() {
            var splitter = document.getElementById('monitor-splitter');
            var logList = document.querySelector('.log-list');
            var toolbar = document.querySelector('.toolbar');
            var pageHeader = document.querySelector('.header-gradient');
            var minTop = 80;
            var minBottom = 100;

            splitter.addEventListener('mousedown', function(e) {
                e.preventDefault();
                splitter.classList.add('dragging');
                document.body.style.cursor = 'ns-resize';
                document.body.style.userSelect = 'none';

                function onMove(e) {
                    var topOffset = (pageHeader ? pageHeader.offsetHeight : 0) + (toolbar ? toolbar.offsetHeight : 0);
                    var newHeight = e.clientY - topOffset;
                    if (newHeight < minTop) newHeight = minTop;
                    var maxH = window.innerHeight - topOffset - minBottom - 60;
                    if (newHeight > maxH) newHeight = maxH;
                    logList.style.flex = '0 0 ' + newHeight + 'px';
                    logList.style.minHeight = newHeight + 'px';
                }
                function onUp() {
                    splitter.classList.remove('dragging');
                    document.body.style.cursor = '';
                    document.body.style.userSelect = '';
                    document.removeEventListener('mousemove', onMove);
                    document.removeEventListener('mouseup', onUp);
                }
                document.addEventListener('mousemove', onMove);
                document.addEventListener('mouseup', onUp);
            });
        })();
    </script>
</body>
</html>
