angular
    .module("app.data")
    .controller(
        "DMDataCtrl",
        function(
            $scope,
            $rootScope,
            $injector,
            $timeout,
            SQEvents,
            DMDataService,
            SQConstants,
            SQWebSocketService,
            L,
            AppService
        ) {
            $scope.totalRecords = 0;

            $scope.isQuantDataManager = isQuantDataManager();

            var SQFuturesDataService = null;
            try {SQFuturesDataService = $injector.get("SQFuturesDataService"); } catch(err) {}

            function initGrid() {
                $timeout(
                    function() {
                        grid = new sqGrid("dataGrid");

                        var columns = [{
                                title: L.tsq("Symbol Name"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Instrument"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Broker profile"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Underlying Symbol"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Timeframe"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Timezone"),
                                type: "text",
                                sort: "text",
                            },                          
                            {
                                title: L.tsq("Date from"),
                                type: "text",
                                sort: "dateDotted",
                            },
                            {
                                title: L.tsq("Date to"),
                                type: "text",
                                sort: "dateDotted",
                            },
                            {
                                title: L.tsq("Total Days"),
                                type: "float2",
                                sort: "number",
                            },
                            {
                                title: L.tsq("Total Records"),
                                type: "float2",
                                sort: "number",
                            },
                            {
                                title: L.tsq("Source"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Bar type"),
                                type: "text",
                                sort: "text",
                            },                              
                            {
                                title: L.tsq("Data type"),
                                type: "text",
                                sort: "text",
                            },
                            {
                                title: L.tsq("Hide"),
                                type: "text",
                                sort: "text",
                                tooltip: L.tsq(
                                    "Hide this data in Symbol choice in Builder/Retester/Optimizer"
                                ),
                            },
                            {
                                title: "",
                                type: "text",
                            },
                            {
                                title: "",
                                type: "text",
                                sort: "text",
                            },
                        ];
                        var widths = [
                            140,
                            "9%",
                            "9%",                            
                            "9%",
                            "5%",
                            "5%",                         
                            "6%",
                            "6%",
                            "6%",
                            "6%",
                            "6%",                            
                            "6%",
                            "6%",                               
                            "4%",
                            "*",
                            20,
                        ];

                        grid.defineWidget("progress-bar", sqGridProgressBar);
                        grid.defineWidget("tooltipWidget", sqGridTooltip);
                        grid.defineWidget("checkboxWidget", sqGridCheckbox);

                        grid.setFirstColumnAsId(true, false);
                        grid.enableCheckboxes(true, false);
                        grid.setColumns(columns, !!grid);
                        grid.setWidths(widths, !!grid);
                        grid.setEmptyGridText(L.tsq("No data defined."));

                        grid.onCellClick = function(row, col) {
                            $scope.selectedRow = grid.getRowId(row);

                            rowsSelected();
                        };

                        grid.onCellDoubleClick = function(row) {
                            $scope.selectedRow = grid.getRowId(row);

                            callAction("edit", row);
                        };

                        grid.cellEventHandler = function(
                            rowIndex,
                            cellIndex,
                            eventName,
                            args
                        ) {
                            var actionLink = grid.getUserData(rowIndex, "actionLink");
                            if (eventName == "actionStop") {
                                if (confirm(L.tsq("Do you really want to stop?"))) {
                                    DMDataService.callAction(
                                        actionLink,
                                        grid.getRowId(rowIndex),
                                        "stop"
                                    );
                                }
                            } else if (eventName == "actionPause") {
                                DMDataService.callAction(
                                    actionLink,
                                    grid.getRowId(rowIndex),
                                    "pause"
                                );
                            } else if (eventName == "actionContinue") {
                                DMDataService.callAction(
                                    actionLink,
                                    grid.getRowId(rowIndex),
                                    "continue"
                                );
                            } else {
                                callAction(eventName, args[0], args);
                            }
                        };

                        grid.setFirstColumnAsId(true, false);

                        reloadDataGrid();

                        rowsSelected();

                        $scope.filter.onSearch();

                        grid.setSort(0, false);
                    },
                    0,
                    false
                );
            }

            function rowsSelected() {
                SQEvents.notifyListeners(SQEvents.get("DATA_ITEMS_SELECTED"), {
                    rowIds: getSelectedIDs(),
                    grid: grid,
                });
            }

            function getSelectedIDs() {
                if (!grid) return "";

                var data = grid.getSelectedRows().selectedRowsData;
                var ids = "";
                for (var i = 0; i < data.length; i++) {
                    ids += (i == 0 ? "" : ",") + data[i][0];
                }

                return ids;
            }

            $scope.tab.getSelectedRows = function() {
                return {
                    rows: getSelectedSymbols(),
                    grid: grid,
                };
            };

            $scope.tab.callAction = function(actionName) {
                callAction(actionName);
            };

            $scope.callAction = function(actionName) {
                callAction(actionName);
            };

            $scope.onAddBr = function (form) {
                console.log(form);

                if (form) {
                    $scope.errors = validate(form);
                    if ($scope.errors) {
                        console.error($scope.errors);
                        return;
                    }
                }
        
                if ($scope.brConfig.brAgreed==false) {
                    $rootScope.showError(L.tsq("Please read and agree to StrategyQuant Data Usage Conditions"));
                    return;
                }
        
                $scope.brConfig.symbols = getSelectedBmfTickers();
                if (!$scope.brConfig.symbols) {
                    $rootScope.showError(L.tsq("No tickers selected"));
                    return;
                }
        
                $rootScope.setProgressInfo(L.tsq('SQ BMF Data'), L.tsq("Adding symbols..."), 0, function() {
                    SQFuturesDataService.addCancel();       
                }, true);
        
                SQFuturesDataService.add($scope.brConfig);
            
                hidePopup('#brFuturesDataModal');
            }

            function getSelectedBmfTickers() {
                var tickers = "";
        
                for (var i = 0; i < brGrid.getNumberOfRows(); i++) {
                    if (brGrid.isRowChecked(i)) {
                        tickers += brGrid.getCellValue(i, 0) + ",";
                    }
                }
        
                if (tickers) tickers = tickers.substr(0, tickers.length - 1);
        
                return tickers;
            }
        
            function printBarType(barType) {
                var text = "N/A";

                switch (barType) {
                    case $scope.barDataTypes.startOfBar:
                        text = L.tsq(
                            "Timestamp represents start of bar time (MetaTrader, Dukascopy, forex data)"
                        );
                        break;

                    case $scope.barDataTypes.endOfBar:
                        text = L.tsq(
                            "Timestamp represents end of bar time (NinjaTrader, Tradestation, futures data)"
                        );
                        break;
                }

                return text;
            }

            function performRemove(symbols) {
                var data = DMDataService.getSymbolsWithConnections(symbols);
                DMDataService.removeData(data);

                if (symbols.length >= 5) {
                    //removing 5 and more symbols is performed async
                    batchOperation = "removed";
                    $rootScope.setProgressInfo(
                        L.tsq("SQ Data"),
                        L.tsq("Removing data..."),
                        0,
                        function() {
                            DMDataService.cancelDataOperation();
                        },
                        true
                    );
                }
            }
            
            function updateBrData(){
                SQFuturesDataService.updateBr();
            }

            function addBrData(){
                SQFuturesDataService.lookup(
                    {
                        exchange: 100000,
                        postfix: "",
                        symbols: null,
                        searchInName: true,
                        searchInTicker: true,
                        onlyContFutures: true
                    }
                    ,function(data){
                        showPopup("#brFuturesDataModal");
                        brGrid = new sqGrid("brDataGrid");

                        var columns = [
                            { title: L.tsq('Ticker'), type: "text", sort: 'text' },
                            { title: L.tsq('Name'), type: "text", sort: 'text' },
                            { title: L.tsq('Exchange'), type: "text", sort: 'text' },
                            { title: L.tsq('Data from'), type: "text", sort: 'text' },
                        ];

                        var widths = [100, "*", 80, 100];

                        brGrid.setFirstColumnAsId(true, false);
                        brGrid.setColumns(columns, !!brGrid);
                        brGrid.setWidths(widths, !!brGrid);
                        brGrid.setEmptyGridText(L.tsq('No tickers found.'));
                        brGrid.enableCheckboxes(true);
                        brGrid.headerRedraw();

                        brGrid.onSelectionChanged = function (selected) {
                            $timeout(function () {
                                $scope.tickersSelected = brGrid.checkedRows.length;
                            }, 100);
                        };

                        brGrid.bodyRedraw();
                        loadToArray($scope.brTickers, data.tickers);
                        $scope.lookedup = true;
            
                        for (var i = 0; i < $scope.brTickers.length; i++) {
                            var allowed = $scope.brTickers[i][4];
                            var rowData = $scope.brTickers[i];
                            rowData.pop();
                            brGrid.addRow(rowData, true);
            
                            if (!allowed) {
                                brGrid.setRowDisabled(i, true);
                            }
                        }
            
                        $timeout(function () {
                            brGrid.setNewDimensions();
                            brGrid.headerRedraw();
                            brGrid.bodyRedraw();
                        }, 0);
                });
            }

            function callAction(actionName, rowIndex, args) {
                rowIndex = parseInt(rowIndex);

                var symbols = getSelectedSymbols(rowIndex);
                if (actionName != "updateall" && actionName != "load" && actionName != 'brData' && actionName!='brDataUpdate') {
                    if (symbols.length == 0) {
                        $rootScope.showError(L.tsq("You have to select a symbol."));
                        return;
                    }

                    symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
                    if (symbols.length == 0) {
                        return;
                    }
                }

                switch (actionName) {
                    case "brData":
                        addBrData();
                        break;
                    case "brDataUpdate":
                        updateBrData();
                        break;                        
                    //----------------------------------------------------------------------------
                    case "edit":
                        $scope.dataDetails = symbols[0];
                        $scope.dataDetails.barTypeText = printBarType(
                            $scope.dataDetails.barType
                        );

                        $scope.instrumentEditable = true;
                        $scope.instrumentChooser.setSymbol($scope.dataDetails.symbol);
                        $scope.instrumentChooser.selectInstrument(
                            $scope.dataDetails.instrument
                        );
                        $scope.instrumentChooser.reset();

                        $scope.dataDetails.symbolNew = $scope.dataDetails.symbol;

                        showPopup("#editSymbolModal");
                        break;

                        //----------------------------------------------------------------------------
                    case "delete":
                        var count = symbols.length;

                        if (count > 1) {
                            $scope.dataDeleteConfirmText = L.tsq(
                                "Do you want to remove selected symbols (%d) or only clear their data ?", [count]
                            );
                        } else {
                            $scope.dataDeleteConfirmText = L.tsq(
                                "Do you want to remove symbol '%s' or only clear its data ?", [symbols[0].symbol]
                            );
                        }

                        showPopup("#dataDeleteConfirmPopup");
                        break;

                        //----------------------------------------------------------------------------
                    case "deleteconfirm":
                        hidePopup("#dataDeleteConfirmPopup");

                        var clonedData = clonedDataExists(symbols);
                        if (clonedData) {
                            var sourceSymbols = listSourceSymbols(symbols);

                            $rootScope.showConfirm(
                                L.tsq("Delete data"),
                                L.tsq(
                                    "Data '%s' are used as source for some cloned data.<br>If you'll delete them you will be not able to update the cloned data.<br>Do you really want to delete them ?", [sourceSymbols]
                                ),
                                function(confirmed) {
                                    if (confirmed) {
                                        performRemove(symbols);
                                    }
                                },
                                true, null, null, true
                            );
                        } else {
                            performRemove(symbols);
                        }

                        break;

                        //----------------------------------------------------------------------------
                    case "clear":
                        hidePopup("#dataDeleteConfirmPopup");

                        if (symbols.length == 0) {
                            $rootScope.showError(L.tsq("You have to select some symbol."));
                            return;
                        }

                        var clonedData = clonedDataExists(symbols);
                        if (clonedData) {
                            var sourceSymbols = listSourceSymbols(symbols);

                            $rootScope.showConfirm(
                                L.tsq("Clear data"),
                                L.tsq(
                                    "Data '%s' are used as source for some cloned data.<br>If you'll delete them you will be not able to update the cloned data.<br>Do you really want to delete them ?", [sourceSymbols]
                                ),
                                function(confirmed) {
                                    if (confirmed) {
                                        var data = DMDataService.getSymbolsWithConnections(symbols);
                                        DMDataService.clearData(data);
                                    }
                                },
                                true, null, null, true
                            );
                        } else {
                            var data = DMDataService.getSymbolsWithConnections(symbols);
                            DMDataService.clearData(data);
                        }

                        if (symbols.length >= 5) {
                            //clearing 5 and more symbols is performed async
                            batchOperation = "cleared";
                            $rootScope.setProgressInfo(
                                L.tsq("SQ Data"),
                                L.tsq("Clearing data..."),
                                0,
                                function() {
                                    DMDataService.cancelDataOperation();
                                },
                                true
                            );
                        }

                        break;

                    case "updateall":
                        DMDataService.updateAll();
                        break;

                    case "updateSelected":
                        var data = DMDataService.getSymbolsWithConnections(symbols);
                        DMDataService.updateSelected(data);
                        break;

                    case "checkboxChecked":
                        var data = symbols[0];
                        data.show = !args[2];

                        for(var i =0;i<$scope.currentData.length;i++){
                            if ($scope.currentData[i].symbol==data.symbol){
                                $scope.currentData[i].show = data.show;
                                break;
                            }
                        }

                        DMDataService.showData(data);
                        break;

                        //----------------------------------------------------------------------------
                    case "save":
                        console.log("Save data");

                        if (symbols.length == 0) {
                            $rootScope.showError(
                                L.tsq("You have to select at least one symbol.")
                            );
                            return;
                        }

                        var data = DMDataService.getSymbolsWithConnections(symbols);

                        $rootScope.saveFile(
                            L.tsq("Select file"), 
                            { name: "xml", description: "XML Files" }, 
                            "SaveData", 
                            "Data.xml", 
                            null, 
                            function(targetPath) {
                                data.filePath = targetPath;
                                DMDataService.save(data);
                            }
                        );

                        break;

                        //----------------------------------------------------------------------------
                    case "load":
                        console.log("Load data");

                        var data = {};

                        var fileExtension = { name: "xml", description: "XML Files" };
                        $rootScope.showFilePicker(
                            L.tsq("Select file"),
                            "LoadData",
                            true,
                            false,
                            null,
                            fileExtension,
                            null, 
                            function(paths) {
                            if (paths) {
                                data.filePath = paths[0];
                                DMDataService.load(data);
                            }
                        });

                        break;
                }

                try {
                    $scope.$digest();
                } catch (err) {}
            }

            function clonedDataExists(symbols) {
                for (var i_symbol = 0; i_symbol < symbols.length; i_symbol++) {
                    for (var i_row = 0; i_row < grid.getNumberOfRows(); i_row++) {
                        var sourceDataId = grid.getUserData(i_row, "sourceDataId");

                        if (sourceDataId == symbols[i_symbol].id) {
                            return symbols[i_symbol].symbol;
                        }
                    }
                }
            }

            function listSourceSymbols(symbols) {
                var sourceSymbols = [];

                for (var i_symbol = 0; i_symbol < symbols.length; i_symbol++) {
                    for (var i_row = 0; i_row < grid.getNumberOfRows(); i_row++) {
                        var sourceDataId = grid.getUserData(i_row, "sourceDataId");

                        if (
                            sourceDataId == symbols[i_symbol].id &&
                            !sourceSymbols.includes(symbols[i_symbol].symbol)
                        ) {
                            sourceSymbols.push(symbols[i_symbol].symbol);
                        }
                    }
                }

                return sourceSymbols.toString();
            }

            $scope.onEditSave = function(form) {
                if (form) {
                    $scope.errors = validate(form);
                    if ($scope.errors) {
                        console.error($scope.errors);
                        return;
                    }
                }

                if (!isSymbolNameValid($scope.dataDetails.symbolNew)) {
                    $rootScope.showError(
                        L.tsq("Symbol name cannot contain any special characters!")
                    );

                    return;
                }

                try {
                    $scope.dataDetails.instrument =
                        $scope.instrumentChooser.getInstrument().instrument;
                } catch (err) {
                    console.error("Cannot set symbol instrument. " + err);
                }

                try {
                    $scope.dataDetails.swap = $scope.instrumentChooser.getInstrument().swap
                } catch (err) {
                    console.error("Cannot set symbol swap. " + err);
                }

                DMDataService.editData($scope.dataDetails);

                hidePopup("#editSymbolModal");
            };

            function getSelectedSymbols(rowIndex) {
                var symbols = [];
                var rowData = [];

                if (rowIndex >= 0) {
                    rowData.push(grid.getRowData(rowIndex).cells);
                } else {
                    rowData = grid.getSelectedRows().selectedRowsData;
                }

                for (var i = 0; i < rowData.length; i++) {
                    var data = rowData[i];

                    var rowIndex = grid.getRowIndex(data[0]);
                    var connection = grid.getUserData(rowIndex, "connection");
                    var timezone = grid.getUserData(rowIndex, "timezone");
                    var barType = grid.getUserData(rowIndex, "barType");
                    var uSymbol = grid.getUserData(rowIndex, "uSymbol");
                    var sourceDataId = grid.getUserData(rowIndex, "sourceDataId");
                    var source = grid.getUserData(rowIndex, "source");
                    var id = grid.getUserData(rowIndex, "id");

                    symbols.push({
                        rowIndex: rowIndex,
                        symbol: data[0],
                        instrument: data[1],
                        timeframe: data[4],
                        timezone: timezone,
                        dateFrom: data[6],
                        dateTo: data[7],
                        totalRecords: data[9],
                        sourceType: getSourceType(data[10]),
                        dataType: getDataType(data[11]),
                        connection: connection,
                        barType: barType,
                        uSymbol: uSymbol,
                        sourceDataId: sourceDataId,
                        id: id,
                    });
                }

                return symbols;
            }

            function isBmfSymbol(uSymbol){
                return uSymbol && 
                    (uSymbol.includes('$D') || uSymbol.includes('$N') || uSymbol.includes('@D')|| uSymbol.includes('@N') && 
                    !uSymbol.startsWith('$') && !uSymbol.startsWith('@'));
            }

            function updateGridData(data) {
                var actions = loadGridProgressBarActions(grid);
                grid.removeAllRows(true, true, true);

                if (data) {
                    for (var i = 0; i < data.length; i++) {
                        var uSymbol = "";
                        if (data[i].sourceDataId) {
                            //clone
                            var item = getItem(data, "id", data[i].sourceDataId);
                            if (item) {
                                uSymbol = L.tsq("Clone of %s", [item.symbol]);
                            }
                        } else {
                            //other symbol dukascopy,barchart)
                            uSymbol = data[i].uSymbol;

                            var bmfSymbol = isBmfSymbol(uSymbol);
                            if (bmfSymbol){
                                uSymbol = data[i].uSymbolName;
                            }else if (uSymbol && uSymbol != data[i].uSymbolName) {
                                uSymbol = uSymbol + " (" + data[i].uSymbolName + ")";
                            }
                        }

                        grid.appendData(
                            [
                                [
                                    data[i].symbol,
                                    data[i].instrument,
                                    data[i].brokerName,
                                    uSymbol,
                                    data[i].timeframe ? data[i].timeframe : "",
                                    data[i].timezoneShort ? data[i].timezoneShort : "",                                 
                                    data[i].dateFrom == 0 ?
                                    "" :
                                    timeToDateString(data[i].dateFrom),
                                    data[i].dateTo == 0 ? "" : timeToDateString(data[i].dateTo),
                                    data[i].totalDays,
                                    data[i].rows,
                                    sourceTypeToString(data[i].source),
                                    data[i].barType ? (data[i].barType==1?"Start of bar":"End of bar") : "",                                       
                                    dataTypeToString(data[i].dataType),
                                    "{{checkboxWidget}}",
                                    "{{progress-bar}}",
                                    createActionLink(
                                        '<i class="fa fa-times" aria-hidden="true"></i>',
                                        "action-link",
                                        "delete",
                                        i
                                    ),
                                ],
                            ],
                            true
                        );

                        var rowIndex = grid.getRowIndex(data[i].symbol);
                        grid.setUserData(rowIndex, "connection", data[i].connection);
                        grid.setUserData(rowIndex, "barType", data[i].barType);
                        grid.setUserData(rowIndex, "timezone", data[i].timezone);
                        grid.setUserData(rowIndex, "uSymbol", data[i].uSymbol);
                        grid.setUserData(rowIndex, "sourceDataId", data[i].sourceDataId);
                        grid.setUserData(rowIndex, "id", data[i].id);

                        sqGridCheckboxSetValue(grid, rowIndex, 11, !data[i].show, true);
                    }
                }

                if (!grid.getSelectedRows().selectedRowsData.length) {
                    $scope.selectedRow = null;
                }

                applyGridProgressBarActions(grid, actions);

                grid.sortAll();

                $scope.totalRecords = grid.getNumberOfRows();

                try {
                    $scope.$digest();
                } catch (err) {}
            }

            $scope.dataTypes = angular.copy(SQConstants.getConstants().dataTypes);
            $scope.dataSources = angular.copy(SQConstants.getConstants().dataSources);

            function dataTypeToString(type) {
                var dataType = getItem($scope.dataTypes, "value", type);
                return dataType ? dataType.name : "";
            }

            function sourceTypeToString(type) {
                var dataSource = getItem($scope.dataSources, "value", type);
                return dataSource ? dataSource.name : "";
            }

            function getDataType(name) {
                var dataType = getItem($scope.dataTypes, "name", name);
                return dataType ? dataType.value : "";
            }

            function getSourceType(name) {
                var dataSource = getItem($scope.dataSources, "name", name);
                return dataSource ? dataSource.value : "";
            }

            $scope.getBrokerProfileLabel = function(){
                if (!$scope.filter.brokerId) return L.tsq('All broker profiles');
        
                var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.filter.brokerId);
                return brokerProfile ? brokerProfile.name : '';
              }      

            $scope.getDataSourceLabel = function() {
                if (!$scope.filter.dataSource) return L.tsq("All data sources");

                var dataSource = getItem(
                    $scope.dataSources,
                    "value",
                    $scope.filter.dataSource
                );
                return dataSource ? L.tsq(dataSource.name) : "";
            };

            $scope.getDataTypeLabel = function() {
                if (!$scope.filter.dataType) return L.tsq("All data types");

                var dataType = getItem(
                    $scope.dataTypes,
                    "value",
                    $scope.filter.dataType
                );
                return dataType ? L.tsq(dataType.name) : "";
            };

            $scope.getGroupLabel = function() {
                if (!$scope.filter.group) return L.tsq("By stock group - none");

                var dataGroup = getItem(
                    $scope.groups,
                    "name",
                    $scope.filter.group
                );
                return dataGroup ? L.tsq(dataGroup.name) : "";
            };
            
            function modifyDataGrid(data){
                for(var i =0;i<data.length;i++){
                    var uSymbol = "";
                    if (data[i].sourceDataId) {
                        //clone
                        var item = getItem(data, "id", data[i].sourceDataId);
                        if (item) {
                            uSymbol = L.tsq("Clone of %s", [item.symbol]);
                        }
                    } else {
                        //dukascopy symbol
                        uSymbol = data[i].uSymbol;
                        if (uSymbol && uSymbol != data[i].uSymbolName) {
                            uSymbol = uSymbol + " (" + data[i].uSymbolName + ")";
                        }
                    }

                    var record = [
                        data[i].symbol,
                        data[i].instrument,
                        data[i].brokerName,
                        uSymbol,
                        data[i].timeframe ? data[i].timeframe : "",
                        data[i].timezoneShort ? data[i].timezoneShort : "",
                        data[i].dateFrom == 0 ?
                        "" :
                        timeToDateString(data[i].dateFrom),
                        data[i].dateTo == 0 ? "" : timeToDateString(data[i].dateTo),
                        data[i].totalDays,
                        data[i].rows,
                        sourceTypeToString(data[i].source),
                        data[i].barType ? (data[i].barType==1?"Start of bar":"End of bar") : "",                                       
                        dataTypeToString(data[i].dataType),
                        "{{checkboxWidget}}",
                        "{{progress-bar}}",
                        createActionLink(
                            '<i class="fa fa-times" aria-hidden="true"></i>',
                            "action-link",
                            "delete",
                            i
                        )];
                    grid.modifyRowById(
                        record,data[i].symbol);

                    var rowIndex = grid.getRowIndex(data[i].symbol);
                    grid.setUserData(rowIndex, "connection", data[i].connection);
                    grid.setUserData(rowIndex, "barType", data[i].barType);
                    grid.setUserData(rowIndex, "timezone", data[i].timezone);
                    grid.setUserData(rowIndex, "uSymbol", data[i].uSymbol);
                    grid.setUserData(rowIndex, "sourceDataId", data[i].sourceDataId);
                    grid.setUserData(rowIndex, "id", data[i].id);

                    sqGridCheckboxSetValue(grid, rowIndex, 11, !data[i].show, true);
                }
            }

            function reloadDataGrid() {
                $scope.currentData = [];

                if ($scope.data) {
                    for (var i = 0; i < $scope.data.length; i++) {
                        var data = $scope.data[i];
                        $scope.currentData.push(data);
                    }

                    $scope.filter.onSearch();
                }

                try {
                    $scope.$digest();
                } catch (err) {}
            }

            function passesFilter(item) {
                if (!window.parent.pickerDebug && item.symbol!=null && isBasketAlias(item.symbol)) {
                    return false;
                }

                var search = $scope.filter.text.toLowerCase();

                if (
                    item.symbol.toLowerCase().indexOf(search) < 0 &&
                    item.instrument.toLowerCase().indexOf(search) < 0
                ) {
                    return false;
                }

                if ($scope.filter.brokerId  && item.broker!=$scope.filter.brokerId){
                    return false;
                }

                if (
                    $scope.filter.dataSource &&
                    item.source != $scope.filter.dataSource
                ) {
                    return false;
                }

                if ($scope.filter.dataType && item.dataType != $scope.filter.dataType) {
                    return false;
                }

                if ($scope.filter.group){
                    var dataGroup = getItem(
                        $scope.groups,
                        "name",
                        $scope.filter.group
                    );

                    var ticker = item.symbol;
                    if (dataGroup.stocks.indexOf(","+ticker+",")==-1) {
                        return false;
                    }
                }

                return true;
            }

            //- Event Handlers --------------------------------------------------------------

            function onEvent(event, data, args) {
                if (!grid) {
                    return;
                }

                if (event == SQEvents.get("BROKERS_CHANGED")){
                    reloadBrokers();
                }else if (event == SQEvents.get("DATA_CHANGED")) {
                    $scope.data = angular.copy(SQConstants.getConstants().data);

                    if (data.action == "show") {
                        grid.bodyRedraw();
                    } else if (data.action == "download_single"){
                        modifyDataGrid(data.data);
                    }else {
                        reloadDataGrid();
                    }
                }

                try {
                    $scope.$digest();
                } catch (err) {}
            }

            function reloadBrokers(){
                var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.mtUse);
                brokerProfiles.unshift({
                    id:-1,
                    name:'SQ default'
                });
                $scope.brokerProfiles = brokerProfiles;
            }

            var proccessingWebSocket = false;
            var websocketDataBuffer = [];
            var batchOperation = "added";
            var gridRedrawTimeout = null;

            function onWebSocketData(newData) {
                if (!grid) {
                    return;
                }
                websocketDataBuffer.push(angular.copy(newData));

                if (!proccessingWebSocket) {
                    processWSUpdates();
                }
            }

            function processWSUpdates() {
                proccessingWebSocket = true;

                try {
                    while (websocketDataBuffer.length > 0) {
                        var args = websocketDataBuffer[0];

                        if (args.DMDataProgresses) {
                            var progresses = args.DMDataProgresses;
                            var appInProgress = false;

                            for (var i = 0; i < progresses.length; i++) {
                                var progress = progresses[i];

                                if (progress.key == "TotalProgressSymbol") {
                                    if (!appInProgress && progress.pct >= 0 && progress.pct < 100) {
                                        appInProgress = true;
                                    }
                                    
                                    continue;
                                }

                                var rowIndex = grid.getRowIndex(progress.key);
                                if (rowIndex < 0) {
                                    continue;
                                }

                                var tooltipText = "";
                                if (progress.downloadType)
                                    tooltipText += progress.downloadType + "<br>";
                                if (progress.dataType)
                                    tooltipText += progress.dataType + "<br>";
                                if (progress.speed)
                                    tooltipText +=
                                    L.tsq("Download speed") + ": " + progress.speed + "<br>";
                                if (progress.estimation)
                                    tooltipText +=
                                    L.tsq("Estimated time left") +
                                    ": " +
                                    progress.estimation +
                                    "<br>";

                                if (progress.pct == -1) {
                                    tooltipText = progress.message;
                                }

                                if (progress.pct == 100) {
                                    tooltipText = "";
                                }

                                grid.setUserData(rowIndex, "progress", progress.pct);
                                grid.setUserData(rowIndex, "progressText", progress.message);
                                grid.setUserData(rowIndex, "tooltipText", tooltipText);
                                grid.setUserData(rowIndex, "paused", progress.paused);
                                grid.setUserData(rowIndex, "actionLink", progress.actionLink);

                                if (!appInProgress && progress.pct >= 0 && progress.pct < 100) {
                                    appInProgress = true;
                                }
                            }

                            if (gridRedrawTimeout == null) {
                                gridRedrawTimeout = setTimeout(function() {
                                    gridRedrawTimeout = null;
                                    grid.bodyRedraw();
                                }, 1000);
                            }

                            displayAppMenuProgress(appInProgress);
                        }

                        if (args.DMDataAdd) {
                            var data = args.DMDataAdd;

                            if (data.percent == 100) {
                                $rootScope.closeProgressModal();

                                if (data.error) {
                                    $rootScope.showError(data.error);
                                } else {
                                    if(data.message){
                                        $rootScope.showSuccess(
                                            L.tsq(data.message)
                                        );
                                    }else{
                                        $rootScope.showSuccess(
                                            L.tsq("Symbols " + batchOperation + ".")
                                        );
                                    }
                                    //switch oepration to default
                                    batchOperation = "added";
                                }
                            } else {
                                $rootScope.updateProgress(data.percent, data.info, null);
                            }
                        }

                        websocketDataBuffer.splice(0, 1);
                    }
                } catch (err) {
                    console.error(
                        "Error while processing data manager websocket updates - " + err
                    );
                }

                proccessingWebSocket = false;
            }

            function displayAppMenuProgress(inProgress) {
                if (isQuantDataManager()) {
                    return;
                }

                if (inProgress) {
                    window.parent.document.getElementById(
                        window.appConfig.product + "-menuprogress"
                    ).style.display = "block";
                } else {
                    window.parent.document.getElementById(
                        window.appConfig.product + "-menuprogress"
                    ).style.display = "none";
                }
            }

            $scope.$on("$destroy", function() {
                SQEvents.removeListener(listenerId);
            });

            
            function getDefaultTimezone() {
                if($scope.timezones.length>0) {
                    return $scope.timezones[0].value;
                }
            }

            $scope.getTimezoneLabel = function(){
                var tz = getItem($scope.timezones, 'value', $scope.brConfig.timezone);        
                return tz ? tz.name : '';
            }

            //- Initialization --------------------------------------------------------------

            var grid;
            var brGrid;

            reloadBrokers();

            $scope.barDataTypes = angular.copy(SQConstants.getConstants().barDataTypes);
            $scope.timezones = SQConstants.getConstants().timezones;

            $scope.brConfig = {
                postfix: "",
                barType: $scope.barDataTypes.endOfBar,
                timezoneType: 2,
                timezoneShift: 5,
                timezone: getDefaultTimezone(),
                brAgreed:false
            };

            $scope.brTickers= [];
            $scope.brTickersSelected= [];

            $scope.dataDetails = {
                dataType: 1,
            };

            $scope.filter = {
                text: "",
                onSearch: function() {
                    if (grid) {
                        var data = [];

                        for (var i = 0; i < $scope.currentData.length; i++) {
                            var item = $scope.currentData[i];
                            if (passesFilter(item)) {
                                data.push(item);
                            }
                        }

                        updateGridData(data);
                    }
                },
                onReset: function() {
                    $scope.filter.text = "";
                    reloadDataGrid();
                },
                dataType: null,
                dataSource: null,
                group: null,
                brokerId: ''
            };

            $scope.instrumentChooser = {};
            $scope.instrumentEditable = true;

            $scope.dataDeleteConfirmText = "N/A";

            $scope.dataTypes = angular.copy(SQConstants.getConstants().dataTypes);
            $scope.dataSources = angular.copy(SQConstants.getConstants().dataSources);
            $scope.dataSource = angular.copy(SQConstants.getConstants().dataSource) ;
            $scope.groups = angular.copy(SQConstants.getConstants().baskets) ;
            $scope.data = angular.copy(SQConstants.getConstants().data);
            $scope.barDataTypes = angular.copy(
                SQConstants.getConstants().barDataTypes
            );

            initGrid();

            var listenerId = "DMDataCtrl-" + window.appConfig.appCode;
            SQEvents.addListener(listenerId, [SQEvents.get("DATA_CHANGED"),SQEvents.get("BROKERS_CHANGED")], onEvent);
            
            SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);
        }
    );