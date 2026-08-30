angular.module('app.resultstabs.sysparampermutation').controller('SysParamPermutationCtrl', function ($rootScope, $scope, SQEvents, $timeout, $q, SQResultsData, $element, L, SQConstants, SysParamPermutationService) {
    console.log("SysParamPermutation controller initialized");
    
    $scope.instances = [];
   
    $scope.columns = [];
    $scope.columnsFrequency = [];

    $scope.instanceStatsChooser = {};

    getAvailableColumns();

    function getAvailableColumns() {        
        var columnTypes = SQConstants.getConstants().databankColumnTypes; 
        var columns = SQConstants.getColumnsByType([columnTypes.general]);

        for(var i = 0; i < columns.length; i++) {
            $scope.columnsFrequency.push({name: columns[i].name+' frequency', value: columns[i].value});
            $scope.columns.push({name: columns[i].name, value: columns[i].value});
        }
    }

    $scope.addStats = function() {
        $scope.instanceStatsChooser.clearSelection(); 
        $scope.instanceStatsChooser.show()
    }

    $scope.onAddStats = function (selectedItems) {
        addStats(selectedItems);

        printStatsTable();

        return true;
    }

    $scope.afterChooserInit = function () {
        $scope.instanceStatsChooser.init($scope.columns);
    }

    function initGrid(){
        grid = new sqGrid("sppStatsGrid");

        var columns = [
            { title: L.tsq('Stat'), type: "text", sort: 'text' },
            { title: L.tsq('Median value'), type: "number", sort: 'number' },
            { title: L.tsq('Orig. value'), type: "number", sort: 'number' },
            { title: L.tsq('% Orig. / Median'), type: "number", sort: 'number' },
            { title: L.tsq(''), type: "text", sort: 'text' }
        ];

        var widths = [ '*', 100, 100, 100, 20 ];

        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No stats available.'));
        grid.disableSorting();
        
        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "delete":
                    onDeleteStat(rowIndex);
                    break;
            }
        };

        grid.headerRedraw();

        $('#sppStatsGrid').addClass('gridbox_compressed');
    }

    function onDeleteStat(rowIndex) {
        grid.removeRowByIndex(rowIndex);
    }

    function init() {   
        var data = InitializationData().results.sysParamPermutation.lastSettings;

        for(var i = 0; i < 6; i++) {
            var instance = {
                number: i,
                onTypeSelect: print,
                selectedType: data.charts[i]
            };
            
            $scope.instances.push(instance);
        }     

        $timeout(function() {
            initGrid();
            addStats(data.stats);  
        }, 1000, false);  
    }

    function addStats(stats) {
        for(var i = 0; i <stats.length; i++) {
            var column = getItem($scope.columns, "value", stats[i]);

            grid.appendData([
                [
                    column.name,
                    0,
                    0,
                    0,
                    createActionLink('<i class="fa fa-times" aria-hidden="true"></i>', 'action-link', 'delete', i)
                ]
            ], true);

            grid.setUserData(grid.rows.length-1, 'statsKey', stats[i]);
        }  

        grid.debouncedBodyRedraw();
    }

    function reset() {
        for(var i = 0; i < $scope.instances.length; i++) {
            $scope.instances[i].reset();
        }
    }

    function print() {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;
        
        var statsKeys = [];
        for(var i = 0; i < $scope.instances.length; i++) {
            statsKeys.push($scope.instances[i].getSelectedType());
        }

        if(!statsKeys.length) {
            return;
        }

        data.statsKeys = statsKeys.toString();

        SysParamPermutationService.print(data, function (data) {
            var charts = data.charts;

            for(var i = 0; i < charts.length; i++) {
                $scope.instances[i].print(charts[i]);
            }
        });

        printStatsTable();
    }

    function printStatsTable() {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;
        
        var statsKeys = [];
        for (var i = 0; i < grid.rows.length; i++) {
            var statsKey = grid.getUserData(i, "statsKey")
            statsKeys.push(statsKey);
        }

        if(!statsKeys.length) {
            return;
        }

        data.statsKeys = statsKeys.toString();

        SysParamPermutationService.printTable(data, function (data) {
            for (var i = 0; i < data.stats.length; i++) {
                grid.modifyRowByIndex(data.stats[i].median, i, 1);
                grid.modifyRowByIndex(data.stats[i].orig, i, 2);
                grid.modifyRowByIndex(data.stats[i].origMedianPct, i, 3);
            }
        });
    }

    function refreshContent(strategyChanged, handlerContent) {
        if(!$scope.tab.active) return;

        var dataInfo = SQResultsData.getDataInfo();
        if(!dataInfo.strategyName) {
            reset();
            return;
        }
        
        SQResultsData.getData(handlerId, []).then(function(result){
            print();
        });
    }

    function fetchData(params){
        var deferred = $q.defer();
        
        $timeout(function(){ deferred.resolve({}); }, 0, false);

        return deferred.promise;
    }
    
    var grid;

    init();

    var handlerId = $scope.tab.title; 	//handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }
});