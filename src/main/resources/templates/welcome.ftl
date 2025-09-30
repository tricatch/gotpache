<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GotPache Console - Welcome</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <style>
        .hero-gradient {
            background: #2c3e50;
        }
        .feature-card {
            transition: transform 0.3s ease;
        }
        .feature-card:hover {
            transform: translateY(-5px);
        }
        .menu-button {
            width: 200px;
            min-width: 200px;
            padding-left: 20px;
            padding-right: 20px;
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
    <!-- Hero Section -->
    <div class="hero-gradient uk-section uk-section-large uk-light">
        <div class="uk-container uk-container-large">
            <div class="uk-text-center">
                <div class="uk-margin-large-bottom">
                    <h1 class="uk-heading-large uk-margin-remove">
                        <span uk-icon="icon: rocket; ratio: 2" class="uk-margin-small-right"></span>
                        gotpache
                    </h1>
                </div>
                
                <div class="uk-margin-large">
                    <p class="uk-text-large uk-width-2-3@m uk-margin-auto">
                    Development utility that works as an Apache ProxyPass or Nginx reverse proxy with built-in HTTPS/SSL, automatically generating self-signed certificates for development sites.
                    </p>
                </div>

                <!-- Action Buttons -->
                <div class="uk-margin-large">
                    <div class="uk-child-width-auto uk-grid-small uk-flex-center" uk-grid>
                        <div>
                            <a href="#" onclick="showComingSoonAlert(); return false;" class="uk-button uk-button-primary uk-button-large menu-button">
                                <span uk-icon="icon: world"></span> Monitor
                            </a>
                        </div>
                        <div>
                            <a href="#" onclick="openSettings(); return false;" class="uk-button uk-button-default uk-button-large menu-button">
                                <span uk-icon="icon: settings"></span> Proxy Config
                            </a>
                        </div>
                        <div>
                            <a href="/ca/download" class="uk-button uk-button-default uk-button-large menu-button">
                                <span uk-icon="icon: download"></span> CA Download
                            </a>
                        </div>
                        <div>
                            <a href="#" onclick="confirmCaGenerate(); return false;" class="uk-button uk-button-default uk-button-large menu-button">
                                <span uk-icon="icon: plus"></span> CA Generate
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Features Section -->
    <div class="main-content uk-section uk-section-default">
        <div class="uk-container uk-container-large">
            <div class="uk-text-center uk-margin-large-bottom">
                <h2 class="uk-heading-medium">Key Features</h2>
                <p class="uk-text-muted">Discover the core features of gotpache</p>
            </div>
            
            <div class="uk-child-width-1-3@m uk-grid-match" uk-grid>
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body feature-card">
                        <div class="uk-text-center">
                            <span uk-icon="icon: lock; ratio: 2" class="uk-text-primary uk-margin-bottom"></span>
                            <h3 class="uk-card-title">SSL Automation</h3>
                            <p>Enhance security with automatic SSL certificate issuance and renewal using our built-in certificate module.</p>
                        </div>
                    </div>
                </div>
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body feature-card">
                        <div class="uk-text-center">
                            <span uk-icon="icon: world; ratio: 2" class="uk-text-primary uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Virtual Hosts</h3>
                            <p>Efficient server management with multi-domain support and individual configuration settings.</p>
                        </div>
                    </div>
                </div>
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body feature-card">
                        <div class="uk-text-center">
                            <span uk-icon="icon: desktop; ratio: 2" class="uk-text-primary uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Real-time Monitoring</h3>
                            <p>Monitor server status in real-time through traffic analysis and performance metrics.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>


    <#include "common/footer.ftl">
    
    <script>
        function showComingSoonAlert() {
            UIkit.notification({
                message: 'Monitor feature is coming soon!',
                status: 'primary',
                pos: 'top-center',
                timeout: 3000
            });
        }
        
        function confirmCaGenerate() {
            UIkit.modal.confirm('Are you sure you want to generate a new CA certificate?<br><br><strong>Warning:</strong> This will overwrite any existing CA certificate and may require server restart.').then(function() {
                window.location.href = '/ca/generate';
            }, function() {
                // User cancelled
            });
        }
        
        function openSettings() {
            window.location.href = '/proxyconfig';
        }
    </script>
</body>
</html>
