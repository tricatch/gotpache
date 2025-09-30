<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GotPache Console - Proxy Config</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <style>
        .header-gradient {
            background: #2c3e50;
        }
        .editor-container {
            min-height: 500px;
            padding: 20px 20px 20px 20px;
            margin: 20px 0;
        }
        .file-name {
            font-family: 'Courier New', monospace;
            font-size: 14px;
        }
        body {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .main-content {
            flex: 1;
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
                        <span uk-icon="icon: settings; ratio: 1.5" class="uk-margin-small-right"></span>
                        Proxy Config
                    </h1>
                </div>
                <div class="uk-flex uk-flex-middle uk-grid-small" uk-grid>
                    <div>
                        <span class="uk-text-white" style="font-family: 'Courier New', monospace; font-size: 16px; font-weight: bold;">${fileName}</span>
                    </div>
                    <div>
                        <button id="saveBtn" class="uk-button uk-button-primary" onclick="saveFile()">
                            <span uk-icon="icon: check"></span> Save
                        </button>
                    </div>
                    <div>
                        <a href="/" class="uk-button uk-button-default">
                            <span uk-icon="icon: home"></span> Home
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Main Content -->
    <div class="main-content" style="padding: 0; margin: 0;">
        <div class="uk-container uk-container-large" style="padding: 0 30px 0 20px; margin: 0 auto; text-align: center;">
            <div class="uk-card uk-card-body" style="border: none; box-shadow: none; background: transparent; padding: 0; margin: 0;">
                <div class="editor-container" style="height: calc(100vh - 320px);">
                    <textarea id="configEditor" class="uk-textarea" style="font-family: 'Courier New', monospace; font-size: 16px; resize: none; height: 100%; width: 100%; color: #333; font-weight: 500; overflow-x: auto; white-space: nowrap;">${fileContent}</textarea>
                </div>
                
            </div>
        </div>
    </div>

    <#include "common/footer.ftl">
    
    <script>
        let isDirty = false;
        
        // Track changes
        document.getElementById('configEditor').addEventListener('input', function() {
            isDirty = true;
            document.getElementById('saveBtn').classList.add('uk-button-danger');
            document.getElementById('saveBtn').classList.remove('uk-button-primary');
        });
        
        // Save file function
        function saveFile() {
            const content = document.getElementById('configEditor').value;
            const fileName = '${fileName}';
            
            // Show loading state
            const saveBtn = document.getElementById('saveBtn');
            const originalText = saveBtn.innerHTML;
            saveBtn.innerHTML = '<span uk-spinner="ratio: 0.8"></span> Saving...';
            saveBtn.disabled = true;
            
            // Send POST request
            fetch('/proxyconfig/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'fileName=' + encodeURIComponent(fileName) + '&content=' + encodeURIComponent(content)
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    UIkit.notification({
                        message: 'Configuration saved successfully!',
                        status: 'success',
                        pos: 'top-center',
                        timeout: 3000
                    });
                    isDirty = false;
                    saveBtn.classList.remove('uk-button-danger');
                    saveBtn.classList.add('uk-button-primary');
                } else {
                    UIkit.notification({
                        message: 'Failed to save: ' + data.message,
                        status: 'danger',
                        pos: 'top-center',
                        timeout: 5000
                    });
                }
            })
            .catch(error => {
                UIkit.notification({
                    message: 'Error saving configuration: ' + error.message,
                    status: 'danger',
                    pos: 'top-center',
                    timeout: 5000
                });
            })
            .finally(() => {
                saveBtn.innerHTML = originalText;
                saveBtn.disabled = false;
            });
        }
        
        // Warn before leaving if there are unsaved changes
        window.addEventListener('beforeunload', function(e) {
            if (isDirty) {
                e.preventDefault();
                e.returnValue = '';
            }
        });
        
        // Keyboard shortcuts
        document.addEventListener('keydown', function(e) {
            if (e.ctrlKey && e.key === 's') {
                e.preventDefault();
                saveFile();
            }
        });
    </script>
</body>
</html>
