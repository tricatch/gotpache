<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>502 Bad Gateway</title>
    <style>
        body { font-family: system-ui, sans-serif; margin: 2rem; line-height: 1.5; }
        .brand { font-weight: 600; font-size: 1.1rem; color: #2c3e50; margin-bottom: 0.25rem; }
        h1 { color: #c0392b; margin-top: 0.5rem; }
        table { border-collapse: collapse; margin-top: 1rem; }
        th, td { border: 1px solid #ccc; padding: 0.5rem 0.75rem; text-align: left; }
        th { background: #f5f5f5; }
        .muted { color: #666; font-size: 0.9rem; }
    </style>
</head>
<body>
<p class="brand">Gotpache</p>
<h1>502 Bad Gateway</h1>
<p>The proxy could not connect to the upstream server.</p>
<table>
    <tr><th>Request ID</th><td>${rid?html}</td></tr>
    <tr><th>Request Host</th><td>${requestHost?html}</td></tr>
    <tr><th>Route path</th><td>${routePath?html}</td></tr>
    <tr><th>Target URL</th><td>${targetUrl?html}</td></tr>
</table>
<#if errorMessage??>
<p class="muted">${errorMessage?html}</p>
</#if>
</body>
</html>
