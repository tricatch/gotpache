<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gotpache Console - Network Monitor</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- Font Awesome (MIT) -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" crossorigin="anonymous" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <style>
        .header-gradient {
            background: #2c3e50;
        }
        body {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .main-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            overflow: hidden;
        }
        .monitor-split {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            overflow: hidden;
        }
        .monitor-url-list {
            flex: 0 0 45%;
            min-height: 120px;
            display: flex;
            flex-direction: column;
            background: #fff;
        }
        .monitor-toolbar {
            flex: 0 0 auto;
            padding: 4px 8px;
            border-bottom: 1px solid #e5e5e5;
            background: #fafafa;
            display: flex;
            gap: 6px;
            align-items: center;
            justify-content: flex-start;
        }
        .monitor-toolbar .uk-button {
            padding: 4px 8px;
            font-size: 11px;
        }
        .monitor-record-btn {
            --record-icon-size: 11px;
            width: 28px;
            height: 28px;
            padding: 0;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 2px solid #757575;
        }
        .monitor-record-btn.recording {
            border-color: #f0506e;
        }
        .monitor-record-btn .monitor-toolbar-icon,
        .monitor-clear-btn .monitor-toolbar-icon,
        .monitor-toolbar-icon-btn .monitor-toolbar-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: var(--record-icon-size);
            line-height: 0;
            width: 1em;
            height: 1em;
        }
        .monitor-record-btn .monitor-toolbar-icon {
            color: #f0506e;
        }
        .monitor-clear-btn .monitor-toolbar-icon,
        .monitor-toolbar-icon-btn .monitor-toolbar-icon {
            color: inherit;
        }
        .monitor-clear-btn {
            width: 28px;
            height: 28px;
            padding: 0;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 2px solid #757575;
        }
        .monitor-toolbar-icon-btn {
            width: 28px;
            height: 28px;
            padding: 0;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 2px solid #757575;
        }
        .monitor-url-list-body {
            flex: 1;
            min-height: 0;
            overflow-y: scroll;
            scrollbar-gutter: stable;
        }
        .monitor-resizer {
            flex: 0 0 6px;
            background: #e5e5e5;
            cursor: ns-resize;
            user-select: none;
            position: relative;
            border-top: 1px solid #dadce0;
            border-bottom: 1px solid #dadce0;
        }
        .monitor-resizer:hover,
        .monitor-resizer.dragging {
            background: #2196f3;
        }
        .monitor-resizer::after {
            content: '';
            position: absolute;
            left: 50%;
            top: 50%;
            transform: translate(-50%, -50%);
            width: 24px;
            height: 4px;
            border-top: 2px solid #9e9e9e;
            border-bottom: 2px solid #9e9e9e;
        }
        .monitor-resizer:hover::after,
        .monitor-resizer.dragging::after {
            border-color: #fff;
        }
        .monitor-request-table {
            width: 100%;
            font-size: 12px;
            border-collapse: collapse;
            font-family: system-ui, -apple-system, sans-serif;
        }
        .monitor-request-table th {
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
        .monitor-request-table td {
            padding: 6px 10px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            transition: background 0.15s;
        }
        .monitor-request-table tbody tr:hover {
            background: #f8f9fa;
        }
        .monitor-request-table tbody tr.uk-active {
            background: #e8f0fe;
        }
        .monitor-request-table .col-name {
            font-family: monospace;
            font-size: 11px;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .monitor-request-table .col-status { color: #1e8e3e; }
        .monitor-request-table .col-type { color: #5f6368; }
        .monitor-request-table .col-initiator {
            font-size: 11px;
            max-width: 140px;
            overflow: hidden;
            text-overflow: ellipsis;
            color: #5f6368;
        }
        .monitor-request-table .col-size, .monitor-request-table .col-time {
            color: #5f6368;
            white-space: nowrap;
        }
        .monitor-detail {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
            margin-top: 2px;
        }
        /* DevTools-style tab bar (X button excluded) */
        .monitor-detail .uk-tab {
            padding: 0 0 0 0px;
            margin: 0;
            border-bottom: 1px solid #90b4e8;
            background: #fff;
            gap: 0;
        }
        .monitor-detail .uk-tab::before {
            display: none;
        }
        .monitor-detail .uk-tab li {
            margin: 0 0 0 0px;
        }
        .monitor-detail .uk-tab li a {
            padding: 4px 4px;
            font-size: 11px;
            color: #5f6368;
            text-transform: capitalize;
            border: none;
            border-bottom: 3px solid transparent;
            border-radius: 4px 4px 0 0;
            margin-bottom: -1px;
        }
        .monitor-detail .uk-tab li a:hover {
            color: #202124;
        }
        .monitor-detail .uk-tab li.uk-active a {
            color: #1967d2;
            background: #e8f0fe;
            border-bottom: 3px solid #1967d2;
        }
        .monitor-detail .uk-switcher {
            margin-top: 0;
        }
        .monitor-detail-tab-content {
            flex: 1;
            overflow-y: scroll;
            scrollbar-gutter: stable;
            padding: 2px 6px 6px 6px;
            font-size: 12px;
            font-family: monospace;
            background: #fff;
        }
        .monitor-detail-tab-content pre {
            margin: 0;
            white-space: pre-wrap;
            word-break: break-all;
        }
    </style>
</head>
<body>
    <!-- Header Section -->
    <div class="header-gradient uk-section uk-section-small uk-light">
        <div class="uk-container uk-container-large">
            <div class="uk-flex uk-flex-between uk-flex-middle">
                <div>
                    <h1 class="uk-heading-small uk-margin-remove">
                        <span uk-icon="icon: world; ratio: 1.5" class="uk-margin-small-right"></span>
                        Network Monitor
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

    <!-- Main: Top/Bottom split layout -->
    <div class="main-content">
        <div class="monitor-split">
            <!-- Top: Request list (Chrome Network tab style) -->
            <div class="monitor-url-list">
                <div class="monitor-toolbar">
                    <button type="button" class="uk-button uk-button-default uk-button-small monitor-record-btn recording" id="monitor-start-stop-btn" title="Start/Stop">
                        <i class="fa-solid fa-circle monitor-toolbar-icon uk-hidden" id="monitor-start-icon"></i>
                        <i class="fa-solid fa-square monitor-toolbar-icon" id="monitor-stop-icon"></i>
                    </button>
                    <button type="button" class="uk-button uk-button-default uk-button-small monitor-clear-btn" id="monitor-clear-btn" title="Clear">
                        <i class="fa-solid fa-broom monitor-toolbar-icon"></i>
                    </button>
                </div>
                <div class="monitor-url-list-body">
                <table class="monitor-request-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Status</th>
                            <th>Type</th>
                            <th>Initiator</th>
                            <th>Size</th>
                            <th>Time</th>
                        </tr>
                    </thead>
                    <tbody id="monitor-request-tbody">
                    </tbody>
                </table>
                </div>
            </div>
            <div class="monitor-resizer" id="monitor-resizer" title="Drag to resize"></div>
            <!-- Bottom: Detail tabs -->
            <div class="monitor-detail">
                <ul class="uk-tab" uk-tab="connect: #monitor-detail-tabs">
                    <li><a href="#">Header</a></li>
                    <li><a href="#">Cookie</a></li>
                    <li><a href="#">Payload</a></li>
                    <li><a href="#">Response</a></li>
                </ul>
                <ul id="monitor-detail-tabs" class="uk-switcher uk-margin-small">
                    <li class="monitor-detail-tab-content">
                        <pre id="monitor-headers-content">General
  Request URL: https://localhost/api/users
  Request Method: GET
  Status Code: 200 OK

Response Headers
  content-type: application/json
  cache-control: no-cache</pre>
                    </li>
                    <li class="monitor-detail-tab-content">
                        <pre id="monitor-cookies-content">Cookie name    Value
session_id      abc123...
user_pref       theme=dark</pre>
                    </li>
                    <li class="monitor-detail-tab-content">
                        <pre id="monitor-payload-content">(No payload for GET request)</pre>
                    </li>
                    <li class="monitor-detail-tab-content">
                        <pre id="monitor-response-content">[
  { "id": 1, "name": "User One" },
  { "id": 2, "name": "User Two" }
]</pre>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <#include "common/footer.ftl">

    <script>
        (function() {
            var tbody = document.getElementById('monitor-request-tbody');
            var clearBtn = document.getElementById('monitor-clear-btn');
            var startStopBtn = document.getElementById('monitor-start-stop-btn');
            var startIcon = document.getElementById('monitor-start-icon');
            var stopIcon = document.getElementById('monitor-stop-icon');
            var isRecording = true;  // Start recording on page load to show events

            // Event delegation for row click
            if (tbody) {
                tbody.addEventListener('click', function(e) {
                    var row = e.target.closest('.url-item');
                    if (row) {
                        document.querySelectorAll('.monitor-url-list .url-item').forEach(function(i) { i.classList.remove('uk-active'); });
                        row.classList.add('uk-active');
                    }
                });
            }

            clearBtn.addEventListener('click', function() {
                if (tbody) tbody.innerHTML = '';
            });

            startStopBtn.addEventListener('click', function() {
                isRecording = !isRecording;
                if (isRecording) {
                    startIcon.classList.add('uk-hidden');
                    stopIcon.classList.remove('uk-hidden');
                    startStopBtn.classList.add('recording');
                } else {
                    startIcon.classList.remove('uk-hidden');
                    stopIcon.classList.add('uk-hidden');
                    startStopBtn.classList.remove('recording');
                }
            });

            // SSE: connect to /monitor/event and add rows
            var eventSource = new EventSource('/monitor/event');
            eventSource.onmessage = function(e) {
                try {
                    var data = JSON.parse(e.data);
                    if (!tbody || !isRecording) return;
                    var tr = document.createElement('tr');
                    tr.className = 'url-item';
                    tr.setAttribute('data-url', data.name || '/');
                    tr.setAttribute('data-method', data.method || 'GET');
                    tr.innerHTML = '<td class="col-name">' + escapeHtml(data.name || '/') + '</td>' +
                        '<td class="col-status">' + escapeHtml(data.status || '-') + '</td>' +
                        '<td class="col-type">' + escapeHtml(data.type || 'fetch') + '</td>' +
                        '<td class="col-initiator">' + escapeHtml(data.initiator || '-') + '</td>' +
                        '<td class="col-size">' + escapeHtml(data.size || '-') + '</td>' +
                        '<td class="col-time">' + escapeHtml(data.time || '-') + '</td>';
                    tbody.insertBefore(tr, tbody.firstChild);
                } catch (err) {
                    console.warn('Monitor SSE parse error:', err);
                }
            };
            eventSource.onerror = function() {
                console.warn('Monitor SSE connection error, reconnecting...');
            };

            function escapeHtml(s) {
                var div = document.createElement('div');
                div.textContent = s;
                return div.innerHTML;
            }
        })();

        (function() {
            var resizer = document.getElementById('monitor-resizer');
            var topPanel = document.querySelector('.monitor-url-list');
            var split = document.querySelector('.monitor-split');
            var minTop = 120;
            var minBottom = 120;

            resizer.addEventListener('mousedown', function(e) {
                e.preventDefault();
                resizer.classList.add('dragging');
                document.body.style.cursor = 'ns-resize';
                document.body.style.userSelect = 'none';

                function onMove(e) {
                    var splitRect = split.getBoundingClientRect();
                    var newHeight = e.clientY - splitRect.top;
                    if (newHeight < minTop) newHeight = minTop;
                    if (newHeight > splitRect.height - minBottom) newHeight = splitRect.height - minBottom;
                    topPanel.style.flex = '0 0 ' + newHeight + 'px';
                    topPanel.style.minHeight = newHeight + 'px';
                }
                function onUp() {
                    resizer.classList.remove('dragging');
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
