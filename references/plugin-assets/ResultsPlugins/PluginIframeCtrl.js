angular.module('app.resultstabs.resultsplugins').controller('PluginIframeCtrl', function($scope, $element, $rootScope, SQResultsData, $timeout, $http, ResultsPluginsService, SkinService, SQEvents, SQConstants) {
    console.log("PluginIframeCtrl initialized for plugin:", $scope.tab);
    
    $scope.iframeUrl = '';
    
    // list of symbol info objects
    var symbols = [];

    function refreshContent(strategyChanged, handlerContent) {
        // Only send data if tab is active
        if (!$scope.tab || !$scope.tab.active) {
            return;
        }
        
        // Send data to iframe when strategy changed or tab refreshed
        sendDataToIframe();
    }
    
    function sendDataToIframe() {
        if (!$scope.tab) {
            return;
        }
        
        var dataInfo = SQResultsData.getDataInfo();
        dataInfo = JSON.parse(JSON.stringify(dataInfo)); //AW postMessage object clone issue fix

        var dataItem = $scope.tab.dataItem;
        
        // Get iframe and send data
        var iframe = document.getElementById('plugin-iframe-' + dataItem);
        if (iframe && iframe.contentWindow) {
            iframe.contentWindow.postMessage({
                type: 'STRATEGY_DATA',
                data: dataInfo
            }, '*');
        }
    }
    
    function updateIframeUrl(reload) {
        if (!$scope.tab) {
            return;
        }
        
        var pluginName = $scope.tab.pluginName;
        var url = $scope.tab.url;
        
        if (!url) {
            return;
        }
        
        // Always add cache parameter with timestamp
        var separator = url.indexOf('?') === -1 ? '?' : '&';
        url = url + separator + 'cache=' + Date.now();
        
        console.log("Updating iframe url for plugin:", pluginName, "with url:", url);
        $scope.iframeUrl = url;
        
        $timeout(function() {
            $scope.$apply();
            if (reload) {
                $timeout(function() {
                    sendDataToIframe();
                }, 100);
            }
        }, 0);
    }
    
    // Expose reloadIframe to scope
    $scope.reloadIframe = function() {
        updateIframeUrl(true);
    };
    

    async function onEvent(event, data) {
        if(event == SQEvents.get('SKIN_CHANGED')) {
            updateSkin();

        } else if (event == SQEvents.get('LANGUAGE_CHANGED')) {
            updateLanguage(data);

        } else if (event == SQEvents.get('DATA_CHANGED')) {
            symbols = data;
        }
    }

    function updateSkin() {
        if (!$scope.tab) {
            return;
        }
        
        var dataItem = $scope.tab.dataItem;
        var iframe = document.getElementById('plugin-iframe-' + dataItem);
        
        if (!iframe || !iframe.contentWindow) {
            return;
        }
        
        var theme = SkinService.selectedSkin.name == "Dark skin" ? 'dark' : 'light';
        console.log("Updating plugin iframe theme to:", theme);
        
        iframe.contentWindow.postMessage({ 
            type: 'SET_THEME', 
            theme: theme 
        }, '*');
    }

    function updateLanguage(language) {
        if (!$scope.tab) {
            return;
        }

        var dataItem = $scope.tab.dataItem;
        var iframe = document.getElementById('plugin-iframe-' + dataItem);

        if (!iframe || !iframe.contentWindow) {
            return;
        }

        // Host sends the selected language object (title+flag).
        var langObj = language || $rootScope.selectedLanguage || $rootScope.language || null;

        iframe.contentWindow.postMessage({
            type: 'SET_LANGUAGE',
            language: langObj
        }, '*');
    }
    
    function mergeParamsWithDataInfo(userParams) {
        var dataInfo = SQResultsData.getDataInfo();
        
        var params = Object.assign({}, userParams || {}, {
            projectName: dataInfo.projectName,
            databankName: dataInfo.databankName,
            strategyName: dataInfo.strategyName
        });
        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) {
            params.backtestID = dataInfo.backtestID;
        }
        if (dataInfo.isAlgoWizardCloud) {
            params.nodeID = dataInfo.nodeID;
            params.isAlgoWizardCloud = dataInfo.isAlgoWizardCloud;
        }
        return params;
    }
    
    // PostMessage communication with iframe (if needed)
    window.addEventListener('message', function(event) {
        if (!$scope.tab) {
            return;
        }
        
        var dataItem = $scope.tab.dataItem;
        var iframe = document.getElementById('plugin-iframe-' + dataItem);
        
        if (!iframe || !iframe.contentWindow) {
            return;
        }
        
        // Check if message came from this plugin's iframe by comparing event.source
        if (event.source !== iframe.contentWindow) {
            return; // Message is not from this plugin's iframe, ignore it
        }
        
        if (event.data && event.data.type === 'GET_STATS') {
            var params = mergeParamsWithDataInfo(event.data.params);
            
            ResultsPluginsService.getStats(params, function(data) {
                iframe.contentWindow.postMessage({
                    type: 'STATS_RESPONSE',
                    data: data
                }, '*');
            });
        }

        if (event.data && event.data.type === 'GET_ORDERS') {
            var params = mergeParamsWithDataInfo(event.data.params);
            
            ResultsPluginsService.getOrders(params, function(data) {
                iframe.contentWindow.postMessage({
                    type: 'ORDERS_RESPONSE',
                    data: data
                }, '*');
            });
        }

        if (event.data && event.data.type === 'GET_LAST_SETTINGS_XML') {
            var params = mergeParamsWithDataInfo(event.data.params);
            
            ResultsPluginsService.getLastSettingsXml(params, function(data) {
                iframe.contentWindow.postMessage({
                    type: 'LAST_SETTINGS_XML_RESPONSE',
                    data: data
                }, '*');
            });
        }

        if (event.data && event.data.type === 'GET_SOURCE_CODE') {
            var params = mergeParamsWithDataInfo(event.data.params || {});
            
            ResultsPluginsService.getSourceCode(params, function(data) {
                iframe.contentWindow.postMessage({
                    type: 'SOURCE_CODE_RESPONSE',
                    data: data
                }, '*');
            });
        }

        if (event.data && event.data.type === 'GET_SYMBOL_INFO') {
            var symbol = event.data.symbol;

            var found = symbols.find(s => s.symbol === symbol);
            iframe.contentWindow.postMessage({
                type: 'SYMBOL_INFO_RESPONSE',
                data: found || null
            }, '*');
        }
    });
    
    // Register handler for strategy changes
    var handlerId = 'PluginIframeCtrl-' + $scope.tab.dataItem;
    SQResultsData.addHandler(handlerId, null, refreshContent, []);
    
    // Refresh content when tab becomes active
    $scope.tab.tabChanged = function () {
        if ($scope.tab.active) {
            refreshContent(false, null);
        }
    }
    
    // Initialization
    function init() {
        symbols = SQConstants.getConstants().data;

        updateIframeUrl();
        
        // Send theme immediately and also after iframe loads
        $timeout(function() {
            var dataItem = $scope.tab.dataItem;
            var iframe = document.getElementById('plugin-iframe-' + dataItem);
            
            if (iframe) {
                // Try to send theme immediately
                updateSkin();
                updateLanguage($rootScope.selectedLanguage || $rootScope.language);
                
                // Also send theme when iframe loads (in case it wasn't ready)
                iframe.onload = function() {
                    updateSkin();
                    updateLanguage($rootScope.selectedLanguage || $rootScope.language);
                };
            }
        }, 0);
    }
    
    // Register listener for skin changes
    if (window.parent && window.parent.addListener) {
        var listenerId = 'PluginIframeCtrl-' + $scope.tab.dataItem;
        window.parent.addListener(listenerId, [SQEvents.get('SKIN_CHANGED'), SQEvents.get('LANGUAGE_CHANGED'), SQEvents.get('DATA_CHANGED')], onEvent);
    }
    
    init();
});