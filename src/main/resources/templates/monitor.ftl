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
            font-size: 12px;
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
        .log-table .col-method { font-family: monospace; font-size: 11px; color: #5f6368; white-space: nowrap; }
        .log-table .col-host { font-size: 11px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; color: #5f6368; }
        .log-table .col-path { font-family: monospace; font-size: 11px; overflow: hidden; text-overflow: ellipsis; }
        .log-table .col-start { font-size: 11px; color: #5f6368; white-space: nowrap; }
        .log-table .col-duration { font-size: 11px; color: #5f6368; white-space: nowrap; }
        .log-table .col-code { color: #1e8e3e; font-weight: 500; white-space: nowrap; }
        .log-table .col-size { font-size: 11px; color: #5f6368; white-space: nowrap; }
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
            overflow-y: auto;
            overflow-x: hidden;
            padding: 8px 12px;
            font-size: 12px;
            font-family: monospace;
            background: #fff;
            white-space: pre-wrap;
            word-break: break-all;
            scrollbar-gutter: stable;
        }
        .detail-panel { display: none; }
        .detail-panel.active { display: block; }
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
        </div>

        <div class="detail-content">
            <div id="detail-header" class="detail-panel active">(Select a request)</div>
            <div id="detail-cookie" class="detail-panel">(Select a request)</div>
            <div id="detail-request" class="detail-panel">(Select a request)</div>
            <div id="detail-response" class="detail-panel">(Select a request)</div>
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
                    if (panel) panel.classList.add('active');
                });
            });

            // Log list click
            logList.addEventListener('click', function(e) {
                var row = e.target.closest('tr[data-rid]');
                if (row) {
                    logTbody.querySelectorAll('tr[data-rid]').forEach(function(r) { r.classList.remove('active'); });
                    row.classList.add('active');
                    populateDetail(row.getAttribute('data-rid'));
                }
            });

            clearBtn.addEventListener('click', function() {
                if (logTbody) logTbody.innerHTML = '';
                ridStartMap = {};
                ridRowData = {};
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
                    size: (rowData.size != null) ? rowData.size + ' B' : '-'
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
                logTbody.insertBefore(tr, logTbody.firstChild);
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
            function upsertRow(data) {
                if (!logTbody || !isRecording) return;
                var rid = data.rid || '';
                if (!rid) return;
                var type = data.type || '';
                var timestamp = data.timestamp || 0;
                var headers = data.headers;
                var body = data.body;

                if (type === 'REQ_HEADER') {
                    ridStartMap[rid] = timestamp;
                    if (!ridRowData[rid]) ridRowData[rid] = {};
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
                } else if (type === 'RES_HEADER') {
                    if (!ridRowData[rid]) ridRowData[rid] = { method: '-', host: '-', path: '-', startTs: timestamp };
                    ridRowData[rid].resHeaders = headers;
                    ridRowData[rid].code = '-';
                    var line = parseFirstHeaderLine(headers);
                    if (line) ridRowData[rid].code = parseResponseLine(line) || '-';
                    var startTs = ridStartMap[rid];
                    ridRowData[rid].startTs = ridRowData[rid].startTs || startTs;
                    ridRowData[rid].timeMs = (startTs != null && timestamp > 0) ? (timestamp - startTs) : null;
                } else if (type === 'REQ_BODY' && body != null) {
                    if (!ridRowData[rid]) ridRowData[rid] = {};
                    ridRowData[rid].reqBody = body;
                } else if (type === 'RES_BODY' && body != null) {
                    if (!ridRowData[rid]) ridRowData[rid] = {};
                    ridRowData[rid].resBody = body;
                }
                if (body != null) {
                    if (!ridRowData[rid]) ridRowData[rid] = {};
                    if (typeof body === 'string') {
                        try { ridRowData[rid].size = atob(body).length; } catch (x) { ridRowData[rid].size = body.length; }
                    } else if (Array.isArray(body)) ridRowData[rid].size = body.length;
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

            function populateDetail(rid) {
                var empty = '(Select a request)';
                var panels = {
                    header: document.getElementById('detail-header'),
                    cookie: document.getElementById('detail-cookie'),
                    request: document.getElementById('detail-request'),
                    response: document.getElementById('detail-response')
                };

                if (!rid || !ridRowData[rid]) {
                    panels.header.textContent = empty;
                    panels.cookie.textContent = empty;
                    panels.request.textContent = empty;
                    panels.response.textContent = empty;
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
                panels.request.textContent = reqBody ? tryPrettyJson(reqBody) : '(No payload for ' + method + ' request)';

                var resBody = decodeBody(d.resBody);
                panels.response.textContent = resBody ? tryPrettyJson(resBody) : '(No response body)';
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
