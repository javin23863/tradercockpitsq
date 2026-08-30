angular.module('app.data').controller('ExportToMT4Ctrl', function ($scope, $rootScope, SQConstants, ExportToMT4Service, $timeout, DMDataService, L, AppService) {
    console.log("ExportToMT4 controller initialized");    

    var TF = SQConstants.getConstants().timeframe;
    
    $scope.errors = "";

    $scope.serverNames = [];
    $scope.properties = [];

    $scope.symbols = [];

    $scope.dateRange = {};

    $scope.config = {
        dateFrom : null,
        dateTo : null,
        dateType: "custom",
        sqSymbol: null,
        mt4Symbol: null,
        encoding: '',
        specSymbol: null,
        appPath: null,
        dataPath: null,
        properties: null,
        serverName: null,
		timeframe: 'All',
        exportMode: 'All',
        targetTimezone: ''        
    };

    $scope.dataDetails = null;
    $scope.showDataFolderLink = false;
	$scope.timeframes = ['All','M1','M5','M15','M30','H1','H4','D1'];
    $scope.exportModes = ['All','hst','fxt'];

    var grid;
    var actionSymbol = null;

     function initTimezones(){
        var tzList = SQConstants.getConstants().timezones;
        var newTimezones = [];
        newTimezones.push({value:'',name:'Original'});
        for(var i=0;i<tzList.length;i++){
            newTimezones.push(tzList[i]);
        }
        $scope.timezones = newTimezones;
    }

    $scope.getTimezoneLabel = function(){
        var tz = getItem($scope.timezones, 'value', $scope.config.targetTimezone);        
        return tz ? tz.name : 'Original';
    }    

    initTimezones();

    $scope.onHelp = function() {
        AppService.openHelp('test-strategy-metatrader-4-tick-precision');
    }

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("ExportToMT4Ctrl");

        grid  = grid2;

        if (symbols.length==0) {
            $rootScope.showError(L.tsq('You have to select a symbol.'));
            return;
        }

        symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (symbols.length==0) {
            return;
        }

        if (symbols.length>1) {
            $rootScope.showError(L.tsq('You can choose only one symbol to export.'));
            return;
        }

        $scope.dataDetails = symbols[0];
        if ($scope.dataDetails.totalRecords == 0) {
            $rootScope.showError(L.tsq("Symbol '%s' doesn't contain any data.", [symbols[0].symbol]));
            return;
        }

        if ($scope.dataDetails.timeframe != TF.TICK) {
            $rootScope.showError(L.tsq("Export to MT4 works only with TICK data."));
            return;
        }

        actionSymbol = symbols[0];

        $scope.config.dateFrom = $scope.dataDetails.dateFrom;
        $scope.config.dateTo = $scope.dataDetails.dateTo;
        
        $scope.config.sqSymbol = $scope.dataDetails.symbol;

        $scope.dateRange.setRangeLimit($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);
        $scope.dateRange.setRange($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);  
                   
        loadProperties("default", function(data) {
            if(data.appPath && data.dataPath && data.serverNames) {
                $scope.config.appPath = data.appPath;
                $scope.config.dataPath = data.dataPath;
                $scope.config.serverName = data.serverName;

                loadToArray($scope.serverNames, data.serverNames);  
            }
        });
        
        setGridProgressBarAction(grid, actionSymbol.rowIndex, progressAction);
        
        showPopup('#exportToMT4Modal');
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        ExportToMT4Service.action(symbol, action);
    }

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.config.dateFrom = dateFrom;
        $scope.config.dateTo = dateTo;
        $scope.config.dateType = type;
    }

    $scope.onSymbolChange = function() {
        var properties = findPropertiesBySymbol($scope.config.specSymbol);

        $scope.properties.length = 0;

        for(var i=0; i<properties.length; i++) {
            var key = Object.keys(properties[i])[0];

            var name = key.toLowerCase();
            name = name.replace("_", " ");
            name = name.charAt(0).toUpperCase() + name.slice(1);

            $scope.properties.push({
                name: name,
                key: key,
                value: trimToDecimalPlaces(properties[i][key], 6)
            });
        }

        $scope.config.mt4Symbol = $scope.config.specSymbol;
    }

    function findPropertiesBySymbol(symbol) {
        for(var i=0; i<$scope.symbols.length; i++) {
            if($scope.symbols[i].symbol==symbol) {
                return $scope.symbols[i].properties;
            }
        }

        return [];
    }

    $scope.onSelectInstallFolder = function() {
        $rootScope.showFilePicker(L.tsq('Select MT4 installation folder'), "mt4ExportInstallFolder", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.config.appPath = paths[0];

                try { $scope.$digest(); } catch(err){}

                ExportToMT4Service.getDataFolder({appPath: $scope.config.appPath}, function(data) {
                    if(data.error) {
                        $rootScope.showError(data.error);
                        $scope.showDataFolderLink = true;
                        $scope.config.dataPath = null;
                        $scope.config.serverName = null;
                        $scope.serverNames.length = 0;

                    } else {
                        $scope.showDataFolderLink = false;
                        $scope.config.dataPath = data.dataPath;
                        $scope.config.serverName = data.serverNames.length>0 ? data.serverNames[0] : null;

                        loadToArray($scope.serverNames, data.serverNames);
                    }
                });
            }
        });
    }

    $scope.onSelectDataFolder = function() {
        $rootScope.showFilePicker(L.tsq('Select MT4 Data folder'), "mt4ExportDataFolder", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.config.dataPath = paths[0];

                try { $scope.$digest(); } catch(err){}

                ExportToMT4Service.getServerNames({dataFolderPath: paths[0]}, function(data) {
                    if(data.error) {
                        $rootScope.showError(data.error);
                        $scope.serverNames.length = 0;
                        $scope.config.serverName = null;
                    } else {
                        loadToArray($scope.serverNames, data.serverNames);
                        $scope.config.serverName = data.serverNames.length>0 ? data.serverNames[0] : null;
                    }
                });
            }
        });
    }

    $scope.onSelectConfigFile = function() {
        $rootScope.showFilePicker(L.tsq('Select MT4 data specification file'), "mt4ExportConfigFile", true, false, null, null, null, function (paths) {
            if (paths) {                
                loadProperties(paths[0]);
            }
        });
    }

    function loadProperties(filePath, callback) {
        ExportToMT4Service.loadProperties({filePath: filePath}, function(data) {
            loadToArray($scope.symbols, data.symbols);

            var symbol = data.symbols.length>0 ? data.symbols[0].symbol : null;

            //set correct symbol
            for(var i=0; i<data.symbols.length; i++) {
                if($scope.dataDetails.symbol.includes(data.symbols[i].symbol)) {
                    symbol = data.symbols[i].symbol;

                    break;
                }
            }

            $scope.config.specSymbol = symbol;
            $scope.config.mt4Symbol = symbol;

            $scope.onSymbolChange();

            if(callback) {
                callback(data)
            }

            try { $scope.$digest(); } catch (er) {}
        });   
    }

    $scope.onStartExport = function(form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        $scope.config.properties = "";
        for(var i=0; i<$scope.properties.length; i++) { 
            var property = $scope.properties[i];

            $scope.config.properties+= i>0 ? "|"+property.key+"="+property.value : property.key+"="+property.value;
        }   

        ExportToMT4Service.export($scope.config, function (result) {
            hidePopup('#exportToMT4Modal');

            displayGridProgressBar(grid, actionSymbol.rowIndex, L.tsq("Preparing export to MT4"), true);
        });
    }
});