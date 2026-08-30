angular.module('app.datasource.files').controller('DataSourceFilesImportCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, DataSourceFilesService, DMDataService, $timeout, $compile, L) {

    var columnsGrid;
    $scope.dataDetails = {
        errorHandling: 0
    };
    $scope.timezones = [];
    $scope.fileFormats = [];
    $scope.dateFormats = [];
    $scope.separators = SQConstants.getConstants().importFileSeparators;
    $scope.timezones = SQConstants.getConstants().timezones;

    var dataSource = angular.copy(SQConstants.getConstants().dataSource);
    var TF = SQConstants.getConstants().timeframe;

    $scope.timeframes = [];

    var prevColumnTypes = [];
    var overviewData;
    var ignoreRowsChange, ignoreColsChange, ignoreSeparatorChange;

    $scope.newFormat = {
        name: ''
    };

    var columnTypeChoose = 'Choose type...';

    var columnTypes = [columnTypeChoose, 'Date', 'Time', 'Date & Time', 'Ask', 'Bid', 'Open', 'High', 'Low', 'Close', 'Volume', 'Unused'];


    var selectedSymbols = [];
    var grid;

    $scope.overviewData = [];

    initTimeframes();

    function initTimeframes() {
        $scope.timeframes.length = 0;

        $scope.timeframes.push({type: 'Recognize automatically', value: 'auto'});
        $scope.timeframes.push({type: TF.Intraday, value: TF.Intraday});
        
        var defaultTimeframes = SQConstants.getConstants().timeframes;
        for (var i = 0; i < defaultTimeframes.length; i++) {
            var tf = defaultTimeframes[i];

            $scope.timeframes.push({type: tf, value: tf});
        }

    }

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("DataSourceFilesImportCtrl2");

        grid = grid2;

        selectedSymbols = DMDataService.filterSymbolsBySourceType(symbols, dataSource.file);
        if (selectedSymbols.length==0) {
            if (symbols.length==0){
                $rootScope.showError(L.tsq('You must select at least one File record.'));
            }else{
                $rootScope.showError(L.tsq('You selected record with wrong type (Dukascopy, Darwinex etc). You must select File record.'));
            }
            return;
        }

        selectedSymbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (selectedSymbols.length==0) {
            return;
        }

        if(DMDataService.containsClonedData(symbols)) {
            $rootScope.showError(L.tsq('You cannot import to cloned data.'));
            return; 
        }

        $scope.dataDetails.connection = selectedSymbols[0].connection,
        $scope.dataDetails.symbol = selectedSymbols[0].symbol,

        $scope.dataDetails.importFile = '';
        $scope.dataDetails.timezone = getDefaultTimezone();
        for (var i = 0; i < $scope.timezones.length; i++) {
            if ($scope.timezones[i].value == $scope.dataDetails.timezone) {
                $scope.dataDetails.timezoneDisplay = $scope.timezones[i].name;
            }
        }
        $scope.dataDetails.timeframe = $scope.timeframes[0].value;
        $scope.dataDetails.fileFormat = "Custom";
        $scope.dataDetails.dateFormat = $scope.dateFormats[0].pattern;
        $scope.dataDetails.separator = $scope.separators[0].value;
        $scope.dataDetails.separatorDisplay = $scope.separators[0].type;
        $scope.dataDetails.skipRows = 0;
        $scope.dataDetails.skipCols = 0;

        initColumnsGrid();

        for (var i=0; i<selectedSymbols.length; i++) {
            setGridProgressBarAction(grid, selectedSymbols[i].rowIndex, progressAction);
        }

        showPopup('#importDataModal');
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DataSourceFilesService.importAction(symbol, action);
    }

    function getDefaultTimezone() {
        var savedTimezone = SQConstants.getSettings().timezone;
        if (savedTimezone && getItem($scope.timezones, 'value', savedTimezone)) {
            return savedTimezone;
        }

        if ($scope.timezones.length > 0) {
            return $scope.timezones[0].value;
        }
    }

    function initColumnsGrid() {
        $timeout(function () {
            refreshOverviewGrid([], ['a', 'b', 'c', 'd']);
        });
    }

    $scope.onFileFormatChange = function (newValue) {
        if (newValue == "Custom") {
            changeColumnTypes(prevColumnTypes);
            $scope.dataDetails.predefined = false;
            $scope.newFormat.name = '';
            return;
        }

        for (var i = 0; i < $scope.fileFormats.length; i++) {
            var format = $scope.fileFormats[i];
            if (format.name == newValue) {
                if ($scope.dataDetails.separator != format.separator) {
                    ignoreSeparatorChange = true;
                    $scope.dataDetails.separator = format.separator;
                }

                if ($scope.dataDetails.skipCols != format.skipColumns) {
                    ignoreColsChange = true;
                    $scope.dataDetails.skipCols = format.skipColumns;
                }

                if ($scope.dataDetails.skipRows != format.skipRows) {
                    ignoreRowsChange = true;
                    $scope.dataDetails.skipRows = format.skipRows;
                }

                $scope.dataDetails.dateFormat = format.dateFormat;
                $scope.dataDetails.predefined = format.predefined;
                $scope.newFormat.name = newValue;

                changeColumnTypes(format.columnTypes, true);
                prevColumnTypes = format.columnTypes;
                break;
            }
        }
    }

    $scope.onSaveFormat = function () {
        if (!valueFilled($scope.newFormat.name)) {
            $scope.onSaveFormatAs();
            return;
        }

        var details = getFormatDetails();

        DataSourceFilesService.importUpdateDataFormat(details, function (result) {
            removeItem($scope.fileFormats, 'name', details.name);

            details.columnTypes = details.columnTypes.split(",");
            $scope.fileFormats.push(details);

            $rootScope.showSuccess(result.success);
        });
    }

    $scope.onSaveFormatAs = function () {
        showPopup("#newFormatModal");
    }

    $scope.onDeleteFormat = function () {
        var name = $scope.newFormat.name;

        $rootScope.showConfirm(L.tsq("Delete data format"), "Do you want to delete data format '" + name + "'?", function (confirmed) {
            if (confirmed) {
                DataSourceFilesService.importDeleteDataFormat(name, function (result) {
                    removeItem($scope.fileFormats, 'name', name);

                    $scope.dataDetails.fileFormat = "Custom";
                    $scope.onFileFormatChange("Custom");

                    $rootScope.showSuccess(result.success);
                });
            }
        }, true);
    }

    $scope.saveNewDataFormat = function () {
        if (valueFilled($scope.newFormat.name)) {
            var details = getFormatDetails();

            DataSourceFilesService.importSaveNewDataFormat(details).then(function (result) {
                if (result.success) {
                    hidePopup("#newFormatModal");
                    details.columnTypes = details.columnTypes.split(",");

                    $scope.fileFormats.push(details);

                    $scope.dataDetails.fileFormat = details.name;
                    $scope.onFileFormatChange(details.name);

                    $rootScope.showSuccess(L.tsq("File format saved"));
                } else {
                    $rootScope.showErrorModal(L.tsq("New format error"), result.data.error, false, true);
                }
            });
        } else {
            $rootScope.showError(L.tsq("Name cannot be empty"));
        }
    }

    function getFormatDetails() {
        return {
            name: $scope.newFormat.name,
            separator: $scope.dataDetails.separator,
            skipColumns: $scope.dataDetails.skipCols,
            skipRows: $scope.dataDetails.skipRows,
            dateFormat: $scope.dataDetails.dateFormat,
            predefined: false,
            columnTypes: getColumnTypes()
        };
    }

    function getColumnTypes() {
        var allTypes = "";
        for (var i = 0; i < $scope.overviewData.length; i++) {
            allTypes += $scope.overviewData[i] + ",";
        }

        return allTypes.substr(0, allTypes.length - 1);
    }

    function changeColumnTypes(columnTypes, refreshOverview) {
        if (refreshOverview) {
            refreshOverviewGrid(overviewData, columnTypes);
        }

        var columnSelects = $('.importColumn');

        for (var i = 0; i < columnSelects.length; i++) {
            var columnSelect = columnSelects[i];
            if (columnTypes[i]) {
                $(columnSelect).val(columnTypes[i]);
            }
        }
    }

    $scope.onReloadData = function(fileFormat) {
        var customFormat = fileFormat=="Custom";

        showOverview(customFormat, fileFormat);
    }

    function showOverview(customFormat, fileFormat, preserveGrid) {
        if (customFormat && $scope.dataDetails.predefined) {
            $scope.dataDetails.fileFormat = "Custom";
        }

        var details = {
            filePath: $scope.dataDetails.importFile,
            customFormat: customFormat,
            skipRows: $scope.dataDetails.skipRows,
            skipColumns: $scope.dataDetails.skipCols,
            separator: $scope.dataDetails.separator,
            fileFormat: fileFormat
        }

        DataSourceFilesService.importGetOverview(details).then(function (result) {
            if (result.success) {
                if (result.data.format) {
                    var info = result.data.format;
                    overviewData = result.data.overviewData;

                    if (info.custom) {
                        ignoreSeparatorChange = true;
                        $scope.dataDetails.fileFormat = 'Custom';
                        $scope.dataDetails.separator = info.separator;
                        $scope.dataDetails.dateFormat = info.dateFormat;
                        $scope.dataDetails.skipRows = info.skipRows;
                        refreshOverviewGrid(result.data.overviewData, getCustomColumns(result.data.overviewData.length > 0 ? result.data.overviewData[0].length : 0, info.dateFormat));
                    } else {
                        $scope.dataDetails.fileFormat = info.name;
                        $scope.onFileFormatChange(info.name);

                        if(!getItem($scope.fileFormats, 'name', info.name)){
                            refreshOverviewGrid(result.data.overviewData, info.columnTypes);
                        }
                    }
                } else {
                    refreshOverviewGrid(result.data.overviewData, getPrevUsedColumns(result.data.overviewData.length > 0 ? result.data.overviewData[0].length : 0), preserveGrid);
                }
            } else {
                $rootScope.showErrorModal("File overview error", result.data.error, false, true);
            }
        });
    }

    function getPrevUsedColumns(count) {
        var columns = [];
        for (var i = 0; i < count; i++) {
            columns.push(prevColumnTypes[i] ? prevColumnTypes[i] : columnTypeChoose);
        }
        return columns;
    }

    function getCustomColumns(count, dateFormat) {
        var columns = [];
        var isDateTimeCol = dateFormat && dateFormat.indexOf(' ') > 0;

        for (var i = 0; i < count; i++) {
            if (dateFormat) {
                if (isDateTimeCol && i == 0) {
                    columns.push('Date & Time');
                    continue;
                } else if (!isDateTimeCol && i < 2) {
                    columns.push(i == 0 ? 'Date' : 'Time');
                    continue;
                }
            }
            columns.push(columnTypeChoose);
        }
        return columns;
    }

    
    function refreshOverviewGrid(data, columns, preserveGrid) {
        if(!columnsGrid || !preserveGrid) {
            var gridColumns = [{ title : "ID", type: "text", sort: "text" }];
            var widths = [1];

            var maxColumns = 0;
            if (arrayNotEmpty(data) && arrayNotEmpty(data[0])) {
                maxColumns = data[0].length;

                if (data[0].length>columns.length) {
                    for (var i = 0; i < data[0].length; i++) {
                        if(columns.length<=i) {
                            columns.push(columnTypeChoose);
                        }
                    }
                }
            }

            for (var i = 0; i < columns.length; i++) {
                if (maxColumns == i) {
                    break;
                }

                gridColumns.push({ title : columns[i], type: "text", sort: "text" });
                widths.push(100);
            }

            
            columnsGrid = new sqGrid("columnsGrid");
            columnsGrid.setFirstColumnAsId(true, true);
            columnsGrid.setColumns(gridColumns, !!columnsGrid);
            columnsGrid.setWidths(widths, !!columnsGrid);
            columnsGrid.setEmptyGridText('No data available.');
            columnsGrid.disableSorting();
            columnsGrid.enableCheckboxes(false);

            columnsGrid.onSelectionChanged = function (rows, values) {
                if(rows && rows.length){
                    updateCheckboxes(rows[0], values[0]);
                }
            };

            columnsGrid.headerRedraw();

            var headerCells = $("#columnsGrid table.sq-grid-header > tr > td");

            for (var i = 0; i < columns.length; i++) {
                if (maxColumns == i) {
                    break;
                }
                var headerCell = $(headerCells[i]);
                headerCell.html("<div id='title_" + i + "' style='width:100%; height:100%'>");
            }

            $scope.overviewData.length = 0;

            $scope.columnsValues = angular.copy(columnTypes);

            for (var i = 0; i < columns.length; i++) {
                if (maxColumns == i) {
                    break;
                }

                $scope.overviewData.push(columns[i]);

                var selectHtml = '<div class="sqn-input sqn-select importColumn"><span ng-bind="overviewData[' + i + ']"></span><select ng-model="overviewData[' + i + ']" ng-options="value as value for value in columnsValues" onFocus="sqnSelectFocus(this)" onBlur="sqnSelectBlur(this)"></select></div>';
                $('#title_' + i).append($compile(selectHtml)($scope));
            }
        }
        
        if (arrayNotEmpty(data)) {
            columnsGrid.removeAllRows(true);

            for (var r = 0; r < data.length; r++) {
                var rowId = 'r' + r;
                var rowData = angular.copy(data[r]);
                rowData.splice(0, 0, rowId);
                columnsGrid.addRow(rowData, true);
            }
        }

        columnsGrid.bodyRedraw();

        try { $scope.$digest(); } catch (err) {}
    }


    $scope.openImportFilePicker = function () {
        $rootScope.showFilePicker(L.tsq("Select file to import"), L.tsq("ImportData"), true, false, null, null, null, function (paths) {
            if (paths) {
                var filePath = paths[0];
                $scope.dataDetails.importFile = filePath;
                showOverview(false);
            }
        });
    }

    $scope.onStartImport = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        var columnTypes = "";
        var importColumns = $('.importColumn');

        for (var i = 0; i < importColumns.length; i++) {
            var importColumn = importColumns[i];
            columnTypes += $(importColumn).find(":selected").text() + ",";
        }

        if(columnTypes.includes(columnTypeChoose)) {
            $rootScope.showError(L.tsq('Select type for each column.'));

            return;
        }

        var config = {
            connectionName: $scope.dataDetails.connection,
            symbol: $scope.dataDetails.symbol,
            filePath: $scope.dataDetails.importFile,
            dateFormat: $scope.dataDetails.dateFormat,
            separator: $scope.dataDetails.separator,
            skipRows: $scope.dataDetails.skipRows,
            skipColumns: $scope.dataDetails.skipCols,
            columnTypes: columnTypes.substr(0, columnTypes.length - 1),
            timezone: $scope.dataDetails.timezone,
            timeframe: $scope.dataDetails.timeframe,
            errorHandling: $scope.dataDetails.errorHandling,
            fileFormat: $scope.dataDetails.fileFormat
        }

        DataSourceFilesService.import(config).then(function (result) {
            if (!result.success) {
                $rootScope.showErrorModal(L.tsq("Import error"), result.data.error, false, true);
                
                return;
            }

            SQConstants.getSettings().timezone = config.timezone;

            hidePopup('#importDataModal');

            for (var i=0; i<selectedSymbols.length; i++) {
                displayGridProgressBar(grid, selectedSymbols[i].rowIndex, L.tsq("Preparing File data import"));
            }

            grid.bodyRedraw();
        });
    }

    $scope.cancelImport = function () {
        $rootScope.showConfirm(L.tsq("Cancel import"), L.tsq("Do you really want to cancel the import?"), function (confirmed) {
            if (confirmed) {
                DataSourceFilesService.importCancel();
            }
        }, true);
    }

    $scope.getSeparatorLabel = function () {
        var separator = getItem($scope.separators, 'value', $scope.dataDetails.separator);
        return separator ? separator.type : '';
    }

    $scope.getTimeframeLabel = function () {
        var timeframe = getItem($scope.timeframes, 'value', $scope.dataDetails.timeframe);
        return timeframe ? timeframe.type : '';
    }
    
    $scope.$watch('dataDetails.separator', function (newValue, oldValue) {
        if (valueFilled($scope.dataDetails.importFile)) {
            if (ignoreSeparatorChange) {
                ignoreSeparatorChange = false;
            } else {
                showOverview(true);
            }
        }
    });

    $scope.$watch('dataDetails.skipRows', function (newValue, oldValue) {
        if (valueFilled($scope.dataDetails.importFile)) {
            if (ignoreRowsChange) {
                ignoreRowsChange = false;
            } else {
                showOverview(true,undefined,true);
            }
        }
    });

    $scope.$watch('dataDetails.skipCols', function (newValue, oldValue) {
        if (valueFilled($scope.dataDetails.importFile)) {
            if (ignoreColsChange) {
                ignoreColsChange = false;
            } else {
                showOverview(true);
            }
        }
    });

    $scope.$watch('dataDetails.dateFormat', function (newValue, oldValue) {
        for (var i = 0; i < $scope.dateFormats.length; i++) {
            if ($scope.dateFormats[i].pattern == newValue) {
                $scope.dateFormatExample = $scope.dateFormats[i].example;
                break;
            }
        }
    });

    function loadImportInfo() {
        $q.when(DataSourceFilesService.importGetInfo()).then(function (importInfo) {
            if (importInfo) {
                $scope.fileFormats = importInfo.fileFormats;
                $scope.dateFormats = importInfo.dateFormats;
            }
        });
    }

    loadImportInfo();
});