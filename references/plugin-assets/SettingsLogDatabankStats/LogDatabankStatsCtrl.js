angular.module('app.settings').controller('LogDatabankStatsCtrl', function ($rootScope, $scope, $timeout, $q, $element, LogDatabankStatsService, AppService, SQConstants, SQEvents, L) {
    console.log("LogDatabankStats controller initialized");

    var recentColumns = SQConstants.getColumns().recentColumns;

    var columnTypes = SQConstants.getConstants().databankColumnTypes;
    var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

    function init() {
        loaded = false;

        LogDatabankStatsService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
    }
    
    //- Add / edit --------------------------------------------------------------
    
    $scope.onAddNew = function() {
        $scope.newStats.action = 'add';
        $scope.newStats.item = null;
        $scope.newStats.type = $scope.availableTypes[0].value;
        $scope.newStats.columnObj = null;

        showPopup('#newLogDatabankStatsPopup');
    }

    LogDatabankStatsService.onEdit = function(item) {
        $scope.newStats.action = 'edit';
        $scope.newStats.item = item;
        $scope.newStats.type = item.type;
        $scope.newStats.columnObj = item.columnObj;

        var columnSettings = getColumnSettings(item.columnObj)
        $scope.columnSettingsInstance.setSettings(columnSettings);

        $scope.instanceItemChooser.selectItem(columnSettings.class);

        showPopup('#newLogDatabankStatsPopup');
    }

    $scope.onAddEditNewStatsType = function(action) {
        var data = {
            type: $scope.newStats.type,
            columnObj: getColumnObj(),
            condGridInstance: {}
        }

        if(!data.columnObj) {
            $rootScope.showError(L.tsq("Column cannot be null."));
            return;
        }

        if(action == 'edit') {
            $scope.newStats.item.columnObj = data.columnObj;
        } else {
            $scope.config.statsTypes.push(data);
        }

        $timeout(function(){ 
            $scope.onSettingsChange();
        }, 200, false);
        
        hidePopup('#newLogDatabankStatsPopup');
    }
    
    $scope.availableTypes = LogDatabankStatsService.availableTypes;

    $scope.getTypeLabel = function() {
        var type = getItem($scope.availableTypes, "value", $scope.newStats.type);
        return type ? type.name : '';
    }

    $scope.afterChooserInit = function () {
        var item = null;
        if (columns.length > 0) {
            item = "NetProfit";
        }

        $scope.instanceItemChooser.init(columns, onColumnChange, item);
    }

    LogDatabankStatsService.settingsChanged = function() {
        $scope.onSettingsChange();

        try { $scope.$digest(); } catch(er) {}
    }

    function onColumnChange() {
        var selectedItems = $scope.instanceItemChooser.getSelectedItems();
        if (selectedItems && selectedItems[0]) {
            var selectedItem = selectedItems[0];

            var column = getItem(columns, 'class', selectedItem);
            if (column) {
                $scope.newStats.stats = column.class;

                $scope.columnSettingsInstance.columnChanged(column.class);
            }
        }
    }

    function getColumnSettings(columnObj) {
        return {
            class: getAttrValue(columnObj, "class"),
            resultType: getAttrStringValue(columnObj, "resultType", SQConstants.getConstants().resultTypes.main),
            sampleType: correctAttrValue(columnObj.attributes["sampleType"]),
            direction: correctAttrValue(columnObj.attributes["direction"]),
            plType: correctAttrValue(columnObj.attributes["plType"]),
            confidenceLevel: getAttrIntValue(columnObj, "confidenceLevel", 50),
            market: getAttrIntValue(columnObj, "market", 1),
            subresult: getAttrIntValue(columnObj, "subresult", SQConstants.getConstants().wfSubresultsList[0].value)
        };
    }

    function getColumnObj() {
        var selectedItems = $scope.instanceItemChooser.getSelectedItems();
        var settings = $scope.columnSettingsInstance.getSettings();

        if (selectedItems && selectedItems[0] && settings) {
            var selectedItem = selectedItems[0];

            var column = getItem(columns, 'class', selectedItem);
            if (column) {
                var xmlDoc = document.implementation.createDocument(null, "Column");

                var columnObj = xmlDoc.createElement('Column');
                columnObj.setAttribute("class", column.class);
                columnObj.setAttribute("name", selectedItem);

                columnObj.setAttribute("sampleType", settings.sampleType);
                columnObj.setAttribute("direction", settings.direction);
                columnObj.setAttribute("plType", settings.plType);
                columnObj.setAttribute("resultType", settings.resultType);

                columnObj.setAttribute("confidenceLevel", settings.confidenceLevel);
                columnObj.setAttribute("market", settings.market);
                columnObj.setAttribute("subresult", settings.subresult);

                return columnObj;
            }
        }

        return null;
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.onSettingsChange = function() {
        if(!loaded) return;
        
        settingsChanged = true;

        LogDatabankStatsService.saveSettings();
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        
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

    $scope.newStats = {}
    $scope.instanceItemChooser = {};
    $scope.columnSettingsInstance = {};

    var loaded = false;
    
    var settingsChanged = false;
    
    $scope.config = LogDatabankStatsService.config;    

    var listenerId = "LogDatabankStatsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});