angular.module('app.settings').controller('DataCtrl', function ($rootScope, $scope, $element, $timeout, $q, DataService, ComplexitiesService, AppService, SQEvents, SQConstants, L, sqPlugin) {
    console.log("Settings->Data controller initialized");

    $scope.types = [{
        value: 'isv',
        name: L.tsq('In sample - Validation'),
        label: L.tsq('ISV')
    }, {
        value: 'oos',
        name: L.tsq('Out of sample - Test'),
        label: L.tsq('OOS')
    }, {
        value: 'notrade',
        name: L.tsq('No trade'),
        label: L.tsq('NoTrade')
    }
    ];

    function init(callback, forceReload) {
        $q.all([
            ComplexitiesService.complexitiesPromise,
            DataService.loadCommissionMethods()
        ]).then(function () {
            DataService.loadDataConfig(forceReload);
            $scope.enableOOSSettings = DataService.EnableOOSSettings;
            
            //console.error("DataCtrl init", $scope.config.setups[0].commissions);
            $scope.config.complexities = ComplexitiesService.settings.complexities;

            if (arrayNotEmpty($scope.config.setups)) {
                if (AppService.getTask().type == 'Optimize' || AppService.getTask().type == 'Retest') {
                    $scope.subcharts.count = $scope.config.setups[0].subcharts ? $scope.config.setups[0].subcharts.length : 0;
                    $scope.config.complexities.length = $scope.subcharts.count + 1;
                    ComplexitiesService.correctComplexities();
                }
                initOOSComponent($scope.config.setups[0].dateFrom, $scope.config.setups[0].dateTo, $scope.config.setups[0].symbolReplaced);
            } else {
                if (AppService.getTask().type == 'Optimize' || AppService.getTask().type == 'Retest') {
                    $scope.subcharts.count = 0;
                    $scope.config.complexities.length = 1;
                    ComplexitiesService.correctComplexities();
                }
                initOOSComponent('2000.1.1', '2000.31.12', true);
            }

            toggleSettingsTabs();

            if (callback) callback();
        });
    }

    function initOOSComponent(dateFrom, dateTo, resetRanges, shouldRecalculateOOSRanges = true) {
        if (!tabActive) {         //delay OOS init, it will be processed when this tab becomes active
            lastOOSSettings.callInit = true;
            lastOOSSettings.dateFrom = dateFrom;
            lastOOSSettings.dateTo = dateTo;
            lastOOSSettings.resetRanges = resetRanges;
            return;
        }

        lastOOSSettings.callInit = false;

        if (!$scope.enableOOSSettings) {
            $timeout(function () {
                initialized = true;
                $scope.reloading = false;
            }, 0, false);
            return;
        }

        var addDefaultRange = resetRanges && ($scope.config.oosRanges.length > 0 || AppService.getTask().type != "Retest");
        var saveChanges = false;

        sortRangesByDate($scope.config.oosRanges);

        $timeout(function () {
            var oldRanges = resetRanges ? [] : DataService.getOOSRanges($scope.config.oosRanges);
            var newRanges = oldRanges;

            if (!resetRanges && shouldRecalculateOOSRanges && $scope.oosBounds.dateFrom && $scope.oosBounds.dateTo && !$scope.reloading) {
                newRanges = DataService.recalculateOOSRanges(oldRanges, $scope.oosBounds.dateFrom, $scope.oosBounds.dateTo, dateFrom, dateTo);
                saveChanges = true;
            }

            oosComponent = $('#dataOOSComponent').elessarComponent({
                min: dateFrom,
                max: dateTo,
                dateFormat: 'YYYY.MM.DD',
                values: [],
                maxRanges: DataService.MaxRanges,
                label: function () {
                    let index = getRangeIndex(this);
                    var newLabel = getPartLabel(index, this.$data["class"]);
                    return newLabel;
                },
            });

            if (newRanges) {
                var dateRangeFrom = getDate(dateFrom, true);
                var dateRangeTo = getDate(dateTo, true);

                $scope.config.oosRanges.length = 0;

                for (var i = 0; i < newRanges.length; i++) {
                    var newRange = newRanges[i];

                    if(getDate(newRange[0], true) < dateRangeFrom){
                        newRange[0] = dateFrom;
                    }

                    if(getDate(newRange[1], true) > dateRangeTo){
                        newRange[1] = dateTo;
                    }

                    if($scope.oosBounds.dateFrom && newRange[0] == $scope.oosBounds.dateFrom){
                        newRange[0] = dateFrom;
                    }

                    if($scope.oosBounds.dateTo && newRange[1] == $scope.oosBounds.dateTo){
                        newRange[1] = dateTo;
                    }

                    $scope.config.oosRanges.push({
                        dateFrom: newRange[0],
                        dateTo: newRange[1],
                        type: newRange[2]
                    });
                }

                sortRangesByDate($scope.config.oosRanges);
            }

            redrawOosComponent();

            oosComponent.on('changing', onOOSChange);
            oosComponent.on('change', onOOSChange);

            $scope.oosBounds.dateFrom = dateFrom;
            $scope.oosBounds.dateTo = dateTo;

            // updateOOSModel(DataService.convertRanges(newRanges));

            if (resetRanges && addDefaultRange) {
                $scope.config.oosRanges.length = 0;

                $scope.addOOSRange(true);
                saveChanges = true;
            }

            updateOOSPercents();
            refreshGraph();

            $timeout(function () {
                updateOOSRanges();
                
                $scope.reloading = false;
                if (saveChanges) {
                    onSettingsChange();
                }
            }, 0, false);
        }, 0, false);
    }

    function onOOSChange(ev, ranges, changed) {
        if (initialized && !$scope.reloading) {
            if (oosDebounceTimeoutId == null) {
                oosDebounceTimeoutId = setTimeout(function () {
                    updateOOSRanges();
                    onSettingsChange();
                    oosDebounceTimeoutId = null;
                }, 50);
            }
        }
    }

    function refreshGraph() {
        if (arrayNotEmpty($scope.config.setups)) {
            var mainSetup = $scope.config.setups[0];
            var disableWarning = !initialized;

            mainSetup.session = !valueFilled(mainSetup.session) || mainSetup.session == 'null' ? L.tsq('No Session') : mainSetup.session;

            if (valueFilled(mainSetup.symbol) && !isBasketAlias(mainSetup.symbol)) {
                DataService.getOOSGraphData(mainSetup.dateFrom, mainSetup.dateTo, mainSetup.symbol, mainSetup.session).then(function (result) {
                    if (!result.success || !result.data) {
                        if (!disableWarning) {
                            $rootScope.showError(L.tsq("Cannot load OOS graph - %s", [result.error]));
                        }
                        $('.oosComponent')[0].drawGraph([]);
                    } else {
                        $('.oosComponent')[0].drawGraph(result.data);
                    }
                });
            } else {
                $('.oosComponent')[0].drawGraph([]);
            }
        } 

        $timeout(function () {
            initialized = true;
        }, 0, false);

        if ($scope.config.oosGraphShown) {
            oosComponent[0].showGraph();
        } else {
            oosComponent[0].hideGraph();
        }
    }

    //called when modified ranges through the OOS component
    function updateOOSRanges() {
        var ranges = oosComponent[0].getRanges();
        var newRanges = DataService.convertRanges(ranges);

        updateOOSModel(newRanges);
        updateOOSPercents();
        try {
            $scope.$digest();
        } catch (err) { }
    }

    function updateOOSModel(updatedRanges) {
        if (!updatedRanges || !updatedRanges.length) {
            $scope.config.oosRanges.length = 0;
            return;
        }

        sortRangesByDate(updatedRanges);

        $scope.config.oosRanges.length = 0;
        for (let i = 0; i < updatedRanges.length; i++) {
            $scope.config.oosRanges.push(updatedRanges[i]);
        }
    }

    function sortRangesByDate(ranges) {
        ranges.sort(function (a, b) {
            return getDate(a.dateFrom) - getDate(b.dateFrom);
        });
    }

    function updateOOSPercents() {
        var mainSetup = $scope.config.setups[0];

        for (var i = 0; i < $scope.config.oosRanges.length; i++) {
            var oosRange = $scope.config.oosRanges[i];
            var oosDays = getDaysBetweenDates(getDate(oosRange.dateFrom), getDate(oosRange.dateTo));
            var totalDays = getDaysBetweenDates(getDate(mainSetup.dateFrom), getDate(mainSetup.dateTo));
            $scope.oosPercents[i] = parseInt(100.0 * oosDays / totalDays);
        }
    }

    function redrawOosComponent() {
        oosComponent[0].removeAllRanges();

        for (var i = 0; i < $scope.config.oosRanges.length; i++) {
            oosComponent[0].addRange([$scope.config.oosRanges[i].dateFrom, $scope.config.oosRanges[i].dateTo], {
                class: $scope.config.oosRanges[i].type,
            });
        }
    }

    $scope.modifyOOSRange = function (index, type, dateFrom, dateTo) {
        if (initialized) {
            try {
                redrawOosComponent();
                $scope.$digest();
            } catch (err) { }
        }
    }

    function getPartLabel(index, type) {
        var item = getItem($scope.types, "value", type);
        if (item) {
            return item.label + getPartNumber(index, type);
        }
    }

    function getRangeIndex(range) {
        for (let i = 0; i < range.perant.ranges.length; i++) {
            if (range == range.perant.ranges[i]) {
                return i;
            }
        }
        return 0;
    }

    function getPartNumber(index, type) {
        var number = 0;

        for (var i = 0; i < $scope.config.oosRanges.length; i++) {
            if (i == index) break;

            if ($scope.config.oosRanges[i].type == type) number++;
        }

        return number + 1;
    }

    $scope.addOOSRange = function (reset) {
        var newRange = DataService.createNewOOSRange($scope.oosBounds, $scope.config.oosRanges, reset);
        if (arrayEmpty(newRange)) {
            $rootScope.showError(L.tsq("Not enough space for new part"));
        } else {
            oosComponent[0].addRange([newRange[0], newRange[1]], {
                class: "oos",
            });
            /* redrawOosComponent(); */
        }
    }

    $scope.removeOOSRange = function (index) {
        $scope.config.oosRanges.splice(index, 1);
        redrawOosComponent();
    }

    $scope.hideOOSGraph = function () {
        oosComponent[0].hideGraph();
        $scope.config.oosGraphShown = false;
    }

    $scope.showOOSGraph = function () {
        oosComponent[0].showGraph();
        $scope.config.oosGraphShown = true;
    }

    $scope.onMainSymbolChange = function (symbol) {
        var mainSetup = $scope.config.setups[0];

        if(!$scope.reloading) {
            SQEvents.notifyListeners(SQEvents.get('MAIN_SYMBOL_CHANGED'), mainSetup.symbol);
        }

        initOOSComponent(mainSetup.dateFrom, mainSetup.dateTo);
        updateSubcharts();
    }

    $scope.onMainDatesChange = function (dateFrom, dateTo, reset) {
        $scope.reloading = false;
        initOOSComponent(dateFrom, dateTo, reset);
        onSettingsChange();
    }

    $scope.onSubchartsChange = function (subchartCount) {
        //console.error("Subchart count changed to : " + subchartCount);
        //if (AppService.getTask().type == "Build") return;

        $scope.subcharts.count = parseInt(subchartCount);
        $scope.config.complexities.length = $scope.subcharts.count + 1;

        ComplexitiesService.correctComplexities();
        updateSubcharts();
    }

    function updateSubcharts() {
        for (var i = 0; i < $scope.config.setups.length; i++) {
            var setup = $scope.config.setups[i];
            DataService.correctSubcharts(setup);
        }
    }

    function toggleSettingsTabs(){
        // The default function, will shows ATMs and CrossChecks in all tasks in Custom Project. I have changed to this.
        let show = $scope.config.setups[0].engine !== 'Stockpicker' && $scope.config.setups[0].engine !== 'Single-asset cloud strategy';
        let settingPlugins = sqPlugin.getPlugins('SettingsTab');
        let settingPlugin = getItem(settingPlugins, 'configElemName', "ATMs")
        if(settingPlugin){
            SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_SHOW'), { tab : 'settings-advancedtm', show: show && (settingPlugin.task.split(',').indexOf(AppService.getTask().type) >= 0 || (settingPlugin.relatedTask && settingPlugin.relatedTask.split(',').indexOf(AppService.getTask().type)) >= 0)});
        }
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        settingsChanged = true;
        DataService.saveConfig();
    }

    var destroyConfigWatch = $scope.$watch("config", function (newValue, oldValue) {
        //console.error("DataCtrl config watch 1", initialized, disableWatch, $scope.reloading)
        if (initialized && !disableWatch && !$scope.reloading) {
            //console.error("DataCtrl config watch 2")
            if ((arrayNotEmpty(newValue.complexities) && (!oldValue.complexities || newValue.complexities.length != oldValue.complexities.length)) ||
                !angular.equals(newValue.setups, oldValue.setups)
            ) {
                updateSubcharts();
            }

            if (!angular.equals(newValue.setups[0], oldValue.setups[0])) {
                SQEvents.notifyListeners(SQEvents.get('MAIN_SETUP_CHANGED'), newValue.setups[0]);
                
                if(newValue.setups[0].engine !== oldValue.setups[0].engine){
                    toggleSettingsTabs();
                }
            }

            onSettingsChange();
        }

        if (disableWatch) {
            disableWatch = false;
        }
    }, true);

    function handleSessionChange(newSession) {
        disableWatch = true;

        for (var i = 0; i < $scope.config.setups.length; i++) {
            $scope.config.setups[i].session = newSession;
        }

        onSettingsChange();
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if (data == $scope.tab.title || data == 'all') {
                $scope.reloading = true;

                init(function () { try { $scope.$digest(); } catch (err) { } }, true);
            }
            return;
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            tabActive = data.title == $scope.tab.title;
            //watchersHandler.onActivated(tabActive);

            if (tabActive && lastOOSSettings.callInit) {     //init OOS component when Data tab becomes active and when there was an attempt to init it earlier
                initOOSComponent(lastOOSSettings.dateFrom, lastOOSSettings.dateTo, lastOOSSettings.ranges);
            }
        }
        else if (event == SQEvents.get('SETTINGS_SESSION_CHANGED')) {
            if (!initialized) {
                init(function () {
                    handleSessionChange(data);
                });
            } else {
                handleSessionChange(data);
            }
        }
        else if (event == SQEvents.get('OPTIMIZATION_FILE_CHANGED')) {
            if (!data.lastSettingsXml) {
                return;
            }

            var settingsObj = data.lastSettingsXml;
            var dataObj = getChildElement(settingsObj, "Data");

            var count = DataService.correctSubchartsOptim(dataObj);
            if (DataService.subchartsCountChanged) {
                DataService.subchartsCountChanged(count);

                init(function () { try { $scope.$digest(); } catch (err) { } }, true);
            }
        }
        else return;

        try {
            $scope.$digest();
        } catch (er) { }
    }

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });


    $scope.setSampleConfig = function (type) {
        try {
            var dateFrom = getDate($scope.oosBounds.dateFrom);
            var dateTo = getDate($scope.oosBounds.dateTo);
            var totalDays = getDaysBetweenDates(dateFrom, dateTo);

            var ranges = [];

            var part = totalDays / 100;

            if (type == 1) { //IS: 50, ISV: 20, OOS: 30                
                var partDateFrom = addDays(dateFrom, totalDays / 2);
                var partDateTo = addDays(partDateFrom, part * 20);

                ranges.push({
                    type: 'isv',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )
                ranges.push({
                    type: 'oos',
                    dateFrom: getDateString(partDateTo),
                    dateTo: getDateString(dateTo)
                }
                )

            } else if (type == 2) { //OOS: 30, ISV: 20, IS: 50
                var partDateTo = addDays(dateFrom, part * 30);

                ranges.push({
                    type: 'oos',
                    dateFrom: getDateString(dateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )

                var partDateFrom = partDateTo;
                partDateTo = addDays(partDateFrom, part * 20);
                ranges.push({
                    type: 'isv',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )

            } else if (type == 3) { //IS: 20, ISV: 20, OOS:10, IS: 20, ISV: 20, OOS:10
                var partDateFrom = addDays(dateFrom, part * 20);
                partDateTo = addDays(partDateFrom, part * 20);
                ranges.push({
                    type: 'isv',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )


                var partDateFrom = partDateTo;
                partDateTo = addDays(partDateFrom, part * 10);
                ranges.push({
                    type: 'oos',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )

                var partDateFrom = addDays(partDateTo, part * 20);
                partDateTo = addDays(partDateFrom, part * 20);
                ranges.push({
                    type: 'isv',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(partDateTo)
                }
                )

                var partDateFrom = partDateTo;
                ranges.push({
                    type: 'oos',
                    dateFrom: getDateString(partDateFrom),
                    dateTo: getDateString(dateTo)
                }
                )
            } else if (type == 4) { //TVTVTVTVTVTVTVTVTVTV,OOS:30
                var lastPartDateTo = addPart(ranges, part, 3.5, 'ist', dateFrom);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 3.5, 'isv', lastPartDateTo);
                addPart(ranges, part, 30, 'oos', lastPartDateTo, dateTo);

            } else if (type == 5) { //TVTVTVTVTVTVTVTVTVTV
                var lastPartDateTo = addPart(ranges, part, 5, 'ist', dateFrom);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'oos', lastPartDateTo);
                lastPartDateTo = addPart(ranges, part, 5, 'ist', lastPartDateTo);
                addPart(ranges, part, 5, 'oos', lastPartDateTo, dateTo);
            }

            loadToArray($scope.config.oosRanges, ranges);

            try {
                redrawOosComponent();
                $scope.$digest();
            } catch (err) { }

        } catch (err) {
            console.log("Cannot apply config.", err)
        }
    }

    function addPart(ranges, part, pct, type, dateFrom, dateTo) {
        var partDateTo = dateTo ? dateTo : addDays(dateFrom, part * pct);

        if (type == 'isv' || type == 'oos') {
            ranges.push({
                type: type,
                dateFrom: getDateString(dateFrom),
                dateTo: getDateString(partDateTo)
            }
            )
        }

        return partDateTo;
    }

    function checkFreeSpace(freeSpace, totalDays) {
        if (freeSpace / totalDays > 0.1) {
            throw new Error('Free space should be greater than 10% of the whole range');
        }
    }

    //- Initialization --------------------------------------------------------------

    var initialized = false;
    var disableWatch = false; //to disable saving when additional backtests change
    var oosComponent;
    var tabActive = false;
    var oosDebounceTimeoutId = null;

    var settingsChanged = false;

    var lastOOSSettings = {};

    $scope.reloading = true;

    $scope.subcharts = {
        count: 0
    };
    $scope.oosBounds = {}; //start/end dateTo
    $scope.oosPercents = [0, 0, 0];

    $scope.generationTypes = DataService.generationTypes;
    $scope.config = DataService.config;
    $scope.enableOOSSettings = DataService.EnableOOSSettings;

    init(null, true);

    var listenerId = "DataCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'),
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('SETTINGS_SESSION_CHANGED'),
        SQEvents.get('OPTIMIZATION_FILE_CHANGED'),
    ], onEvent);

    //Causes problems with commissions settings
    /*
        var watchersHandler = new WatchersHandler($($element));
        $timeout(function () {
            watchersHandler.onActivated(false);
        }, 0, false);
    */
});