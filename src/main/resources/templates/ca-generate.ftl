<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CA Certificate Generation - ${serverName}</title>
    <!-- UIkit CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/css/uikit.min.css" />
    <!-- UIkit JS -->
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/uikit@3.23.12/dist/js/uikit-icons.min.js"></script>
    <#include "common/styles.ftl">
</head>
<body>
    <!-- Hero Section -->
    <div class="hero-gradient uk-section uk-section-large uk-light">
        <div class="uk-container uk-container-large">
            <div class="uk-text-center">
                <div class="uk-margin-large-bottom">
                    <h1 class="uk-heading-large uk-margin-remove">
                        <span uk-icon="icon: lock; ratio: 2" class="uk-margin-small-right"></span>
                        CA Certificate Generation
                    </h1>
                </div>
                
                <div class="uk-margin-large">
                    <p class="uk-text-large uk-width-2-3@m uk-margin-auto">
                        Generate root CA certificate for gotpache SSL/TLS proxy.
                    </p>
                    <div class="uk-margin-top">
                        <ul class="uk-list uk-text-center uk-width-1-2@m uk-margin-auto">
                            <li>Self-signed root CA</li>
                            <li>10-year validity period</li>
                            <li>RSA 2048-bit key generation</li>
                            <li>X.509 v3 certificate format</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Main Content -->
    <div class="main-content uk-section uk-section-default">
        <div class="uk-container uk-container-large">
            <!-- Main Description -->
            <div class="uk-text-center uk-margin-large-bottom">
            </div>

            <!-- Warning Alert -->
            <div class="uk-margin-large-top">
                <div class="uk-alert-warning" uk-alert>
                    <h3>⚠️ Important Notice</h3>
                    <p>Generated CA certificates should only be used in development/test environments. 
                    For production environments, please use certificates issued by trusted CAs.</p>
                </div>
            </div>

            <!-- CA Name Input Form -->
            <div class="uk-margin-large uk-text-center">
                <div class="uk-card uk-card-default uk-card-body uk-width-1-2@m uk-margin-auto">
                    <h3 class="uk-card-title">CA Certificate Settings</h3>
                    <form action="/ca/create" method="post" class="uk-form-stacked">
                        <div class="uk-margin">
                            <label class="uk-form-label" for="caName">CA Name</label>
                            <div class="uk-form-controls">
                                <input class="uk-input" type="text" id="caName" name="caName" 
                                       placeholder="Enter CA name (e.g., MyCompanyCA)" 
                                       value="Gotpache Self CA - ${.now?string('yyMMdd-HHmm')}" required>
                            </div>
                        </div>
                        <div class="uk-margin">
                            <button type="submit" class="uk-button uk-button-primary uk-button-large uk-width-1-1">
                                <span uk-icon="icon: plus"></span> Generate CA Certificate
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Action Buttons -->
            <div class="uk-margin uk-text-center">
                <div class="uk-child-width-auto uk-grid-small uk-flex-center" uk-grid>
                    <div>
                        <a href="/" class="uk-button uk-button-default uk-button-large">
                            <span uk-icon="icon: home"></span> Back to Home
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <#include "common/footer.ftl">
</body>
</html>
