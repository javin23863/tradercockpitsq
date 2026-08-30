angular.module('app.settings').controller('AutomaticPortfolioBuilderCtrl', function ($rootScope, $scope, sqPlugin, $timeout, $q, $element, AppService, SQEvents, L, SQWebSocketService, SQConstants, AutomaticPortfolioBuilderService, MoneyManagementService) {
    console.log("AutomaticPortfolioBuilder controller initialized");

    $scope.config = AutomaticPortfolioBuilderService.config;

    $scope.sampleTypes = [
        { value: SQConstants.getConstants().sampleTypes.in, name: L.tsq('In Sample') },
        { value: SQConstants.getConstants().sampleTypes.full, name: L.tsq('Full Sample') }
    ]

    function init() {
        loaded = false;

        var types = AutomaticPortfolioBuilderService.fitnessTypes;
        loadToArray($scope.fitnessTypes, types);

        cgInitDeferred.promise.then(function() {    
            $timeout(function () {
                AutomaticPortfolioBuilderService.loadSettings();

                loaded = true;
            }, 100);
        });

        //money management
        console.log("AutomaticPortfolioBuilderCtrl init, config: ", $scope.config.mmType, $scope.config.mmSettings);
        $scope.config.mmPropGridInstance.setSelectedType($scope.config.mmType);
        if($scope.config.mmSettings) $scope.config.mmPropGridInstance.setSettings($scope.config.mmSettings);

        try { $scope.$digest(); } catch(er) {}
    }

    $scope.showCorrelationPopup = function () {
        showPopup("#pmCorrelationSettingsPopup");
    }

    $scope.showGeneticSettingsPopup = function () {
        showPopup("#pmGeneticSettingsPopup");
    }    

    $scope.printCorrelation = function () {
        var type = getItem($scope.correlationTypes, 'value', $scope.config.correlation.type);
        var typeText = type ? type.name : 'NA';

        var period = getItem($scope.correlationPeriods, 'value', $scope.config.correlation.period);
        var periodText = period ? period.name : 'NA';

        var sample = getItem($scope.sampleTypes, 'value', $scope.config.correlation.sampleType);
        var sampleText = sample ? sample.name : 'NA';

        return L.tsq("max. %f, %s by %s on %s", [$scope.config.correlation.max, typeText, periodText, sampleText]);
    }

    $scope.getCorrelationPeriodLabel = function () {
        var period = getItem($scope.correlationPeriods, 'value', $scope.config.correlation.period);
        return period ? period.name : '';
    }

    $scope.getSampleTypeLabel = function () {
        var item = getItem($scope.sampleTypes, 'value', $scope.config.correlation.sampleType);
        return item ? item.name : '';
    }

    $scope.getFitnessLabel = function () {
        var type = getItem($scope.fitnessTypes, 'type', $scope.config.fitnessType);
        return type ? type.name : '';
    }

    $scope.getCorrelationTypeLabel = function () {
        var type = getItem($scope.correlationTypes, 'value', $scope.config.correlation.type);
        return type ? type.name : '';
    }

    //-- Conditions -------------------------------------------------------------

    $scope.afterCGInit = function (data) {
        loadedCG = true;

        var columnTypes = SQConstants.getConstants().databankColumnTypes;
        var columns = SQConstants.getColumnsByType([columnTypes.general], null, true);

        $timeout(function () {
            $scope.config.condGridInstance.setDefaultSampleType(SQConstants.getConstants().sampleTypes.full);
            $scope.config.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);

            cgInitDeferred.resolve();
        });
    }

    $scope.onCGRefresh = function (grid) {
        if (!loadedCG) {
            return;
        }

        $scope.settingsChanged();
    }

    //-- Money Management -------------------------------------------------------
    $scope.onMMPropGridChange = function(){
        $scope.settingsChanged();
    }

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function() {
        if(!loaded) return;

        settingsChanged = true;

        AutomaticPortfolioBuilderService.saveSettings();
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        
            init();

        } else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    $scope.$on("$destroy", function(){
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    var settingsChanged = false;
    
    var loadedCG = false;
    var cgInitDeferred = $q.defer();

    $scope.correlationTypes = [];
    $scope.correlationPeriods = [];

    $scope.fitnessTypes = [];

    loadToArray($scope.correlationTypes, SQConstants.getConstants().correlationTypes);
    loadToArray($scope.correlationPeriods, SQConstants.getConstants().correlationPeriods);

    var listenerId = "AutomaticPortfolioBuilderCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.mmMethods = angular.copy(MoneyManagementService.mmMethods);
    console.log("mmMethods", $scope.mmMethods)
});