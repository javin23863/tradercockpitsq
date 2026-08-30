angular.module('app.data').controller('ExportToCsvCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, ExportToCsvService, $timeout, DMDataService, L) {
    var TF = SQConstants.getConstants().timeframe;

    $scope.dataDetails = {
        dateFrom: null,
        dateTo: null,
        dateType: "custom",
        session: "No Session",
        fileFormat: null,
        header: null,
        format: null,
        newFormatName: null,
        includeHeader: true,
        predefined: false,
        targetTimezone: ''
    };

    $scope.fileFormats = [];
    $scope.items = [];
    $scope.timezones = [];

    const CustomFileFormat = "Custom";

    $scope.fileExtension = '.csv';

    $scope.availableSessions = ExportToCsvService.sessions;
    $scope.minTimeframe = 'TICK';

    $scope.targetFile = {
        name: ''
    };

    $scope.dateRange = {};

    var grid;
    var symbols = [];

    $scope.action.onSelect = function (symbols2, grid2) {
        console.log("ExportToCsvCtrl");

        symbols = symbols2;
        grid = grid2;

        if (symbols.length == 0) {
            $rootScope.showError(L.tsq('You have to select a symbol.'));
            return;
        }

        symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (symbols.length == 0) {
            return;
        }

        $scope.selectedFor = symbols.length > 1 ? L.tsq('multiple') : "'" + symbols[0].symbol + "'";

        $scope.minTimeframe = getMinTimeframe() || TF.TICK;

        $scope.dataDetails.symbols = implodeObjects(symbols, "symbol");
        $scope.dataDetails.symbol = symbols[0].symbol;
        $scope.dataDetails.timeframe = $scope.minTimeframe;
        $scope.dataDetails.directory = SQConstants.getSettings()['ExportData'] || SQConstants.getSettings().projectsPath;
        $scope.dataDetails.filePrefix = getDateString(new Date());
        $scope.dataDetails.dateFrom = getMinDate();
        $scope.dataDetails.dateTo = getMaxDate();

        if(!$scope.dataDetails.dateFrom && !$scope.dataDetails.dateTo) {
            $rootScope.showError(L.tsq("There is no data to export."));
            return;
        }

        $scope.dateRange.setRange($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);  
        $scope.dateRange.setRangeLimit($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);

        for (var i = 0; i < symbols.length; i++) {
            setGridProgressBarAction(grid, symbols[i].rowIndex, progressAction);
        }

        ExportToCsvService.loadSettings(function (data) {
            loadToArray($scope.items, data.items);
            loadToArray($scope.fileFormats, data.formats);

            if ($scope.fileFormats.length > 0) {
                $scope.dataDetails.fileFormat = $scope.fileFormats[0].name

                $scope.onFileFormatChange();
            }

            if ($scope.items.length > 0) {
                $scope.headerItem = $scope.items[0].header;
                $scope.formatItem = $scope.items[0].key;
            }

            showPopup('#exportDataModal');
        })
    }

    function getMinDate() {
        var date = {str: null, timestamp: -1};

        for (var i = 0; i < symbols.length; i++) {
            if(!date.str || getDate(symbols[i].dateFrom).getTime() < date.timestamp) {
                date.str = symbols[i].dateFrom;
                date.timestamp = getDate(symbols[i].dateFrom).getTime();
            }
        }

        return date.str
    }

    function getMaxDate() {
        var date = {str: null, timestamp: -1};

        for (var i = 0; i < symbols.length; i++) {
            if(!date.str || getDate(symbols[i].dateTo).getTime() > date.timestamp) {
                date.str = symbols[i].dateTo;
                date.timestamp = getDate(symbols[i].dateTo).getTime();
            }
        }

        return date.str
    }

    function getMinTimeframe() {
        var minTimeframe = null;

        for (var i = 0; i < symbols.length; i++) {
            if (!symbols[i].timeframe) {
                continue;
            }

            if (!minTimeframe || getTFSeconds(minTimeframe) < getTFSeconds(symbols[i].timeframe)) {
                minTimeframe = symbols[i].timeframe;
            }
        }

        return minTimeframe;
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        var confirmed = false;

        if (action == 'stop') {
            $rootScope.showConfirm(L.tsq("Cancel export"), L.tsq("Do you want to keep the export file?"), function (confirmed) {
                confirmed = !confirmed;

                ExportToCsvService.action(symbol, action, confirmed);
            }, true);
        } else {
            ExportToCsvService.action(symbol, action, confirmed);
        }
    }

    $scope.onExport = function () {
        if (!valueFilled($scope.dataDetails.directory)) {
            $rootScope.showError(L.tsq("Directory cannot be empty."));
            return;
        }

        startExport();
    }

    $scope.getTimezoneLabel = function(){
        var tz = getItem($scope.timezones, 'value', $scope.dataDetails.targetTimezone);        
        return tz ? tz.name : 'Original';
    }

    function startExport() {
        ExportToCsvService.export($scope.dataDetails, function (result) {
            hidePopup('#exportDataModal');

            for (var i = 0; i < symbols.length; i++) {
                displayGridProgressBar(grid, symbols[i].rowIndex, L.tsq("Preparing export to CSV"));
            }

            grid.bodyRedraw();
        });
    }

    $scope.openExportFilePicker = function () {
        var fileExtension = { name: 'csv', description: L.tsq('Comma Separated File') };

        $rootScope.showFilePicker(L.tsq("Select export file"), L.tsq("ExportData"), false, false, $scope.dataDetails.directory, fileExtension, null, function (paths) {
            if (paths) {
                $scope.dataDetails.directory = paths[0];

                try { $scope.$digest(); } catch (er) { }
            }
        });
    }

    $scope.onFileFormatChange = function () {
        var format = getItem($scope.fileFormats, 'name', $scope.dataDetails.fileFormat);

        $scope.dataDetails.header = format.header;
        $scope.dataDetails.format = format.items;
        $scope.dataDetails.includeHeader = format.includeHeader;
        $scope.dataDetails.predefined = format.predefined;
    }

    $scope.onHeaderItemChange = function (item) {
        if (item == undefined) {
            item = getItem($scope.items, 'name', $scope.headerItem);
        }

        if ($scope.dataDetails.header) {
            $scope.dataDetails.header += "," + item.header;
        } else {
            $scope.dataDetails.header = item.header;
        }

        $scope.setCustomFormat();
    }

    $scope.onFormatItemChange = function (item) {
        if (item == undefined) {
            item = getItem($scope.items, 'name', $scope.formatItem);
        }

        if (item.key == ',' || item.key == ';') { // add item without []
            $scope.dataDetails.format += item.key;
        } else {
            if (item.format) {
                if ($scope.dataDetails.format) {
                    $scope.dataDetails.format += "," + "[" + item.key + ":" + item.format + "]";
                } else {
                    $scope.dataDetails.format = "[" + item.key + ":" + item.format + "]";
                }
            } else {
                if ($scope.dataDetails.format) {
                    $scope.dataDetails.format += "," + "[" + item.key + "]";
                } else {
                    $scope.dataDetails.format = "[" + item.key + "]";
                }
            }
        }

        $scope.setCustomFormat();
    }

    $scope.onIncludeHeaderChange = function () {
        $scope.dataDetails.includeHeader = !$scope.dataDetails.includeHeader

        $scope.setCustomFormat();
    }

    $scope.setCustomFormat = function () {
        var format = getItem($scope.fileFormats, 'name', $scope.dataDetails.fileFormat);
        if (!format.predefined) {
            return;
        }

        if ($scope.fileFormats[0].name != CustomFileFormat) {
            $scope.fileFormats.unshift({
                name: CustomFileFormat,
                header: $scope.dataDetails.header,
                items: $scope.dataDetails.format,
                includeHeader: $scope.dataDetails.includeHeader,
                predefined: false
            });
        }

        $scope.dataDetails.fileFormat = CustomFileFormat;
    }

    $scope.onSaveFormat = function () {
        if (!valueFilled($scope.dataDetails.fileFormat)) {
            $scope.onSaveFormatAs();
            return;
        }

        ExportToCsvService.saveFileFormat($scope.dataDetails, function (result) {
            loadToArray($scope.fileFormats, result.formats);
            $rootScope.showSuccess(result.success);
        });
    }

    $scope.onSaveFormatAs = function () {
        showPopup("#exportToCsvNewFormatModal");
    }

    $scope.saveNewFileFormat = function () {
        if (valueFilled($scope.dataDetails.newFormatName)) {
            ExportToCsvService.saveAsFileFormat($scope.dataDetails, function (result) {
                if (result.success) {
                    hidePopup("#exportToCsvNewFormatModal");

                    loadToArray($scope.fileFormats, result.formats);
                    $scope.dataDetails.fileFormat = $scope.dataDetails.newFormatName;
                    $scope.onFileFormatChange();

                    $rootScope.showSuccess(result.success);
                } else {
                    $rootScope.showErrorModal(L.tsq("New format error"), result.data.error, false, true);
                }
            });
        } else {
            $rootScope.showError(L.tsq("Format name cannot be empty"));
        }
    }

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.dataDetails.dateFrom = dateFrom;
        $scope.dataDetails.dateTo = dateTo;
        $scope.dataDetails.dateType = type;
    }

    $scope.onDeleteFormat = function () {
        var name = $scope.dataDetails.fileFormat;

        $rootScope.showConfirm(L.tsq("Delete file format"), "Do you want to delete file format '" + name + "'?", function (confirmed) {
            if (confirmed) {
                ExportToCsvService.deleteFileFormat(name, function (result) {
                    loadToArray($scope.fileFormats, result.formats);
                    $scope.dataDetails.fileFormat = $scope.fileFormats[0].name;
                    $scope.onFileFormatChange();

                    $rootScope.showSuccess(result.success);
                });
            }
        }, true);
    }

    function initTimezones(){
        var tzList = SQConstants.getConstants().timezones;
        var newTimezones = [];
        newTimezones.push({value:'',name:'Original'});
        for(var i=0;i<tzList.length;i++){
            newTimezones.push(tzList[i]);
        }
        $scope.timezones = newTimezones;
    }

    initTimezones();

    var destroyWatch = $scope.$watch("dataDetails", function (value) {
        $scope.targetFile.name = $scope.dataDetails.symbol + "-" + $scope.dataDetails.timeframe + "-" + $scope.dataDetails.session + ".csv";
    }, true);

    $scope.$on("$destroy", function () {
        destroyWatch();
    });

});

