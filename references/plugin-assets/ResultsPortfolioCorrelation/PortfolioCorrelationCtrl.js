angular.module('app.resultstabs.portfoliocorrelation').controller('PortfolioCorrelationCtrl', function ($rootScope, $scope, PortfolioCorrelationService, SQEvents, $timeout,$window, $document, SQConstants, SQWebSocketService, $q, SQResultsData, $element, L) {
    console.log("PortfolioCorrelation controller initialized");

    $scope.computing = false;

    $scope.config = {
        allowNegativeCorrelation: true,
        addEmptyPeriods: false
    };
    $scope.types = [];
    $scope.periods = []

    $scope.progress = 0;

    $scope.isPortfolio = false;    

    var lastStrategy = null;

    $scope.mainTabs = {
        tabs: [
            {
                title: L.tsq('Correlation matrix'),
                dataItem: 'correlation-matrix',
                templateUrl: '../../../plugins/ResultsPortfolioCorrelation/tabs/correlationMatrix/correlationMatrix.html',
                controller: 'CorrelationMatrixCtrl',
            },
            {
                title: L.tsq('Overlapping trades'),
                dataItem: 'overlapping-trades',
                templateUrl: '../../../plugins/ResultsPortfolioCorrelation/tabs/overlappingTrades/overlappingTrades.html',
                controller: 'OverlappingTradesCtrl',
            }
        ]
    }  

    function initCorrelationOptions() {
        loadToArray($scope.types, SQConstants.getConstants().correlationTypes);
        loadToArray($scope.periods, SQConstants.getConstants().correlationPeriods);

        if($scope.types.length>0) {
            var type = getItem($scope.types, 'value', 'ProfitLoss');
            $scope.config.type = type ? type.value : $scope.types[0].value;
        }
        
        if($scope.periods.length>0) {
            var period = getItem($scope.periods, 'value', 10);
            $scope.config.period = period ? period.value : $scope.periods[0].value;
        }
    }

    initCorrelationOptions();

    function refreshContent(strategyChanged, handlerContent) {  
        var dataInfo = SQResultsData.getDataInfo();
        $scope.isPortfolio = dataInfo.isPortfolio; 

        if(!$scope.tab.active) return;

        if(lastStrategy != dataInfo.strategyName) {
            reset();
        }

        SQResultsData.getData(handlerId, []).then(function(result) {
            //do nothing
        });
    }

    function reset() {
        SQEvents.notifyListeners(SQEvents.get('RESET_PORTFOLIO_CORRELATION_TABS'));
    }

    function fetchData(params){
        var deferred = $q.defer();
        
        $timeout(function(){ deferred.resolve({}); }, 0, false);

        return deferred.promise;
    }

    $scope.getPeriodLabel = function(){
        var period = getItem($scope.periods, 'value', $scope.config.period);
        return period ? L.tsq(period.name) : '';
    }

    $scope.getTypeLabel = function(){
        var type = getItem($scope.types, 'value', $scope.config.type);
        return type ? L.tsq(type.name) : '';
    }

    $scope.onCompute = function() {
        var dataInfo = SQResultsData.getDataInfo();

        var data = {};       
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        lastStrategy = dataInfo.strategyName;

        $scope.progress = 0;
        $scope.computing = true;

        angular.extend(data, $scope.config);

        PortfolioCorrelationService.compute(data, function(data) {
            console.log("Computing portfolio correlation.");
        });
    }

    $scope.onStop = function() {
        PortfolioCorrelationService.stop();
    }
    

  function onWebSocketData(data) {
        if(data.portfolioCorrelation) {
            if(data.portfolioCorrelation.progress && data.portfolioCorrelation.progress < 100) {
                $scope.progress = data.portfolioCorrelation.progress;
            }
            
            if(data.portfolioCorrelation.error || (data.portfolioCorrelation.progress && data.portfolioCorrelation.progress == 100)) {
                $scope.progress = 0;
                $scope.computing = false;

                if(data.portfolioCorrelation.error) {
                    $rootScope.showError(data.portfolioCorrelation.error);
                }
            }

            try { $scope.$digest(); } catch (err) {}
        }
    }

    SQWebSocketService.subscribeGeneral("PortfolioCorrelationCtrl-" + window.appConfig.appCode, onWebSocketData);

    var handlerId = $scope.tab.title; 	//handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }
});