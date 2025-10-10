<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CA Certificate Generation Complete - ${serverName}</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <style>
        .hero-gradient {
            <#if success>
            background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
            <#else>
            background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
            </#if>
        }
        .file-list {
            font-family: 'Courier New', monospace;
            background: #f8f9fa;
            border-radius: 6px;
            padding: 15px;
        }
        .file-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px;
            margin: 5px 0;
            background: white;
            border-radius: 4px;
            border: 1px solid #e9ecef;
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
                    <#if success>
                    <span uk-icon="icon: check; ratio: 3" class="uk-margin-small-right"></span>
                    <#else>
                    <span uk-icon="icon: close; ratio: 3" class="uk-margin-small-right"></span>
                    </#if>
                </div>
                
                <div class="uk-margin-large">
                    <#if success>
                    <h1 class="uk-heading-large uk-margin-remove">CA Certificate Generation Complete!</h1>
                    <p class="uk-text-large uk-width-2-3@m uk-margin-auto">
                        ${caName} CA certificate has been successfully generated.
                    </p>
                    <#else>
                    <h1 class="uk-heading-large uk-margin-remove">CA Certificate Generation Failed</h1>
                    <p class="uk-text-large uk-width-2-3@m uk-margin-auto">
                        An error occurred during CA certificate generation.
                    </p>
                    </#if>
                </div>
            </div>
        </div>
    </div>

    <!-- Main Content -->
    <div class="main-content uk-section uk-section-default">
        <div class="uk-container uk-container-large">
            <#if success>
            <!-- Success Content -->
            <div class="uk-grid-match uk-child-width-1-2@m" uk-grid>
                <!-- Generated Files Card -->
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body">
                        <div class="uk-text-center">
                            <span uk-icon="icon: folder; ratio: 2" class="uk-text-success uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Generated Files</h3>
                            <div class="file-list">
                                <div class="file-item">
                                    <span>${caCertFile}</span>
                                    <span class="uk-text-success">✓ Generated</span>
                                </div>
                                <div class="file-item">
                                    <span>${caKeyFile}</span>
                                    <span class="uk-text-success">✓ Generated</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Next Steps Card -->
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body">
                        <div class="uk-text-center">
                            <span uk-icon="icon: list; ratio: 2" class="uk-text-primary uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Next Steps</h3>
                            <ol class="uk-list uk-list-bullet uk-text-left">
                                <li><strong>Download CA Certificate:</strong> Click the button below to download the CA certificate.</li>
                                <li><strong>Install in Browser:</strong> Add the downloaded certificate to your browser's trusted root authorities.</li>
                                <li><strong>Restart Server:</strong> Restart the Gotpache server to apply the new SSL configuration.</li>
                                <li><strong>Test HTTPS:</strong> Access HTTPS sites to verify that the proxy is working correctly.</li>
                            </ol>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Security Warning -->
            <div class="uk-margin-large-top">
                <div class="uk-alert-warning" uk-alert>
                    <a class="uk-alert-close" uk-close></a>
                    <h3>⚠️ Security Notice</h3>
                    <p>Please store the generated private key file (${caKeyFile}) in a secure location and 
                    do not share it with others. If this file is compromised, it could pose a security risk.</p>
                </div>
            </div>

            <!-- Action Buttons -->
            <div class="uk-margin-large uk-text-center">
                <div class="uk-child-width-auto uk-grid-small uk-flex-center" uk-grid>
                    <div>
                        <a href="/ca/download" class="uk-button uk-button-primary uk-button-large">
                            <span uk-icon="icon: download"></span> Download CA Certificate
                        </a>
                    </div>
                    <div>
                        <a href="/" class="uk-button uk-button-default uk-button-large">
                            <span uk-icon="icon: home"></span> Back to Home
                        </a>
                    </div>
                </div>
            </div>

            <#else>
            <!-- Error Content -->
            <div class="uk-grid-match uk-child-width-1-2@m" uk-grid>
                <!-- Error Details Card -->
                <div>
                    <div class="uk-card uk-card-danger uk-card-hover uk-card-body">
                        <div class="uk-text-center">
                            <span uk-icon="icon: warning; ratio: 2" class="uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Error Information</h3>
                            <p><strong>Error Message:</strong> ${errorMessage}</p>
                            <p>Please check the server logs for detailed error information.</p>
                        </div>
                    </div>
                </div>

                <!-- Troubleshooting Card -->
                <div>
                    <div class="uk-card uk-card-default uk-card-hover uk-card-body">
                        <div class="uk-text-center">
                            <span uk-icon="icon: settings; ratio: 2" class="uk-text-primary uk-margin-bottom"></span>
                            <h3 class="uk-card-title">Troubleshooting</h3>
                            <ol class="uk-list uk-list-bullet uk-text-left">
                                <li><strong>Check Permissions:</strong> Verify that you have write permissions to the conf directory.</li>
                                <li><strong>Disk Space:</strong> Ensure you have sufficient disk space available.</li>
                                <li><strong>Java Version:</strong> Make sure Java 8 or higher is installed.</li>
                                <li><strong>Dependencies:</strong> Verify that the gotpache-keytool library is properly included.</li>
                            </ol>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Action Buttons -->
            <div class="uk-margin-large uk-text-center">
                <div class="uk-child-width-auto uk-grid-small uk-flex-center" uk-grid>
                    <div>
                        <a href="/ca/generate" class="uk-button uk-button-primary uk-button-large">
                            <span uk-icon="icon: refresh"></span> Try Again
                        </a>
                    </div>
                    <div>
                        <a href="/" class="uk-button uk-button-default uk-button-large">
                            <span uk-icon="icon: home"></span> Back to Home
                        </a>
                    </div>
                </div>
            </div>
            </#if>
        </div>
    </div>

    <#include "common/footer.ftl">
</body>
</html>
