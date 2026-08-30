angular.module('app.portfoliocomposer').controller('PortfolioComposerCtrl', function ($scope, $rootScope, PortfolioComposerService, AppService, SQEvents, BackendService, L, SQWebSocketService, SQConstants) {
    console.log("PortfolioComposer controller initialized");

    var appConfig = AppService.getAppConfig();
    $scope.appPlugins = SQConstants.getConstants().appPlugins;
    
    $scope.disabledForSpecialTrial = false;

    $scope.lastEvent = "";
    var lastDisplayedResults = null;

    function init() {
    }

    function disabledForSpecialTrial(license) {
        var plugin = getItem($scope.appPlugins, 'appCode', appConfig.product);
        if (plugin) {
            $scope.disabledForSpecialTrial = (plugin.disabledForSpecialTrial && license?.isSpecialTrial);
            $scope.disabledForSpecialTrialTitle = L.tsq("%s is available only in full version, not in this special trial.", [appConfig.productName]);
        }
    }

    init();

    $scope.start = function(autocomputation) {
        console.log("Recompute portfolio");

        let data = angular.copy(PortfolioComposerService.config);
        if (data.results.length < 2) {
            $rootScope.showError(L.tsq("You need to select at least two strategies to create a portfolio."));
            return;
        } 

        if(!$rootScope.license.isUltimateVersion && data.results.length > 4) {
            let error = L.tsq("Portfolio Composer in Pro version is limited to maximum 4 source strategies.") + "\n" + L.tsq("Upgrade your license to Ultimate version to build portfolio from more strategies."); 

            $rootScope.showErrorModal('Error', error, true, true);
            $rootScope.showError();
            return;
        }
        
        data.results = data.results.join(",");
        data.weights = data.weights.join(",");
        data.autocomputation = autocomputation;

        PortfolioComposerService.recompute(data, function(result) {     
            $scope.startBtnEnabled = false;
            $scope.stopBtnEnabled = true;
        });
    }

    $scope.stop = function() {
        console.log("Stop portfolio computation");
        PortfolioComposerService.stop();
    }

    function displayResults(strategyName) {
        console.log("Display results");

        let db = 'Portfolios';

        loadDataItems(strategyName, db, function (data) {
            if (data.strDoesntExist) {
                return;
            }

            PortfolioComposerService.config.portfolioName = strategyName;

            var params = {
                projectName: AppService.getProject(),
                databankName: db,
                strategyName: strategyName,
                resultInfo: 'PortfolioComposer',
                product: window.appConfig.product,
                dataItems: data.dataItems,
                resultTypes: data.resultTypes,
                noResultItems: data.noResultItems
            };
            
            var args = {
                productCode: "RESULTSPC",
                action: {
                    name: 'showResults',
                    params: params || {}
                }
            }
    
            showResults(args);
        });
    }

    function showResults(args) {
        lastDisplayedResults = args;

        console.log("PortfolioComposer broadcast event: " + SQEvents.get("ACTION_REQUEST"), args);
        window.parent.broadcastEvent(SQEvents.get("ACTION_REQUEST"), args);
    }

    function loadDataItems(strategyName, databankName, callback) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            reportName: strategyName
        }
        var url = 'project/getDataItems';

        BackendService.getPromise(url, data).then(function (result) {
            if (result.data.success && result.data.dataItems) {
                dataItems = result.data.dataItems;
            } else if (result.data.error) {
                $rootScope.showError(result.data.error);
            }

            if (callback) {
                callback(result.data);
            }
        });
    }

    $scope.settings = {};
    $scope.results = {};

    $scope.startBtnEnabled = true;
    $scope.stopBtnEnabled = false;
    $scope.progressPct = 0;

    function onWebSocketData(data) {
        if(data.portfolioComposer) {
            if(data.portfolioComposer.progress && data.portfolioComposer.progress < 100) {
                $scope.progressPct = data.portfolioComposer.progress;
            }
            
            if(data.portfolioComposer.error || (data.portfolioComposer.progress && data.portfolioComposer.progress == 100)) {
                $scope.progressPct = 0;
                $scope.startBtnEnabled = true;
                $scope.stopBtnEnabled = false;

                $scope.lastEvent = "";

                if(data.portfolioComposer.error) {
                    $rootScope.showError(data.portfolioComposer.error);

                } else if(data.portfolioComposer.strategyName) {
                    const strategyName = data.portfolioComposer.strategyName;
                    const weights = data.portfolioComposer.weights;

                    console.log("Portfolio computed: " + strategyName + " with weights: " + weights);
                    window.parent.broadcastEvent(SQEvents.get('PORTFOLIO_COMPOSER_WEIGHTS'), weights);

                    displayResults(strategyName);
                }
            }

            if(data.portfolioComposer.message) {
                $scope.lastEvent = data.portfolioComposer.message;
            }

            try { $scope.$digest(); } catch (err) {}
        }
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('APP_SWITCHED') && data == window.appConfig.product) {
            if(!lastDisplayedResults) {
                return;
            }

            showResults(lastDisplayedResults);
        } else if(event == SQEvents.get('LICENSE_CHANGED')) {
            disabledForSpecialTrial(data);
        } 
    }

    window.top.addListener("PortfolioComposer-plugin", [ SQEvents.get('APP_SWITCHED'), SQEvents.get('LICENSE_CHANGED')], onEvent);

    SQWebSocketService.subscribeGeneral("PortfolioComposerCtrl", onWebSocketData);
});