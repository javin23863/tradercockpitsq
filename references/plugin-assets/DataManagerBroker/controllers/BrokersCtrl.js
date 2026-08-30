angular.module('app.brokers').controller('BrokersController', function ($scope, $rootScope, $timeout, SQEvents, SQConstants, BrokerService, L, AppService) {
    console.log("Brokers controller initialized");

    $scope.timezones = SQConstants.getConstants().timezones;

    function initGrids() {
        $timeout(function () {
            initGrid();

            initialized = true;
            updateBrokersGridData();
        });
    }

    function initGrid() {
        var columns = [
            { title: L.tsq('Name'), type: "text", sort: 'text' },
            { title: L.tsq('Description'), type: "text", sort: 'text' },
            { title: L.tsq('Postfix'), type: "text", sort: 'text' },
            { title: L.tsq('Timezone'), type: "text", sort: 'text' },
            { title: L.tsq('Customized stocks'), type: "int", sort: 'number' },
            { title: L.tsq('Customized instruments'), type: "int", sort: 'number'},
            { title: L.tsq('Customized sessions'), type: "int", sort: 'number' }
        ];

        var widths = [250, "*", 100, 100, 130, 150, 130];

        if (isQuantDataManager()) {
            columns = [
                { title: L.tsq('Name'), type: "text", sort: 'text' },
                { title: L.tsq('Description'), type: "text", sort: 'text' },
                { title: L.tsq('Postfix'), type: "text", sort: 'text' },
                { title: L.tsq('Timezone'), type: "text", sort: 'text' },
                { title: L.tsq('Customized instruments'), type: "int", sort: 'number' },
                 { title: L.tsq('Customized sessions'), type: "int", sort: 'number' }                
            ]

            widths = [250, "*", 100, 100, 150, 130];
        }

        if (!grid) {
            grid = new sqGrid("brokersGrid");
        } else {
            grid.removeAllRows(true, false, true);
        }

        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(true, false);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No brokers are defined.'));

        grid.onCellDoubleClick = function (rowIndex) {
            $scope.onEditBroker();
        };

        grid.onSelectionChanged = function () {
            var selection = grid.getSelectedRows();
            var data = selection.selectedRowsData[0];
            if (!data) return;

            var userData = grid.rows[selection.selectedRows[0]].userData;
            $scope.selectedBroker = {
                name: userData.name,
                desc: userData.desc,
                system: userData.system,
                id: userData.id,
                postfix: userData.postfix,
                mtUse: userData.mtUse,
                stockPickerUse: userData.stockPickerUse,
                mtTimezone: userData.mtTimezone,
                customizedStocks: userData.customizedStocks,
                customizedInstruments: userData.customizedInstruments,
                customizedSessions: userData.customizedSessions
            };
            console.log($scope.selectedBroker);
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            $scope.tab.callAction(eventName, rowIndex);
        }
    }

    function updateBrokersGridData() {
        if (!initialized) return;

        grid.removeAllRows(false, false, true);
        $scope.selectedBroker = null;

        grid.loadFromUrl('brokers/list', function () {
        }, true);
    }

    $scope.onImportBrokerChange = function () {
        var broker = getItem($scope.mtBrokerProfiles, 'id', $scope.dataDetails.broker);
        $scope.dataDetails.postfix = broker.postfix;
    }

    $scope.onEditBroker = function () {
        if (!$scope.selectedBroker) {
            $rootScope.showError(L.tsq('You have to select some broker.'));
            return;
        }

        if ($scope.selectedBroker.system) {
            $rootScope.showError(L.tsq('This broker can\'t be edited.'));
            return;
        }

        $scope.canSetSpUse = $scope.selectedBroker.customizedStocks == 0;
        $scope.canSetMtUse = !isBrokerUsedInMt($scope.selectedBroker.id);
        $scope.canSetTimezone = !hasBrokerData($scope.selectedBroker.id);

        $scope.currentAction = 'Edit';
        $scope.currentBroker = $scope.selectedBroker;

        try {
            $scope.$digest();
        } catch (err) { } //needed because of doubleclick event

        showPopup('#addBrokerModal');
    }

    function hasBrokerData(brokerId) {
        var symbols = SQConstants.getConstants().data;
        for (var i = 0; i < symbols.length; i++) {
            if (symbols[i].broker == brokerId) {
                return true;
            }
        }

        return false;
    }

    $scope.onSaveBroker = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        $scope.currentBroker.stocks = null;

        BrokerService.saveBroker($scope.currentBroker);
    }

    $scope.onEditBrokerStocks = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        if ($scope.selectedBroker.system) {
            $rootScope.showError(L.tsq('This broker can\'t be edited.'));
            return;
        }

        BrokerService.editStocks($scope.currentBroker, function () {
            hidePopup('#editBrokerStocksModal');
            $rootScope.showSuccess(L.tsq('Stocks were saved.'));
        });
    }

    $scope.onExport = function () {
        hidePopup('#editBrokerStocksModal');
        console.log("Export broker")

        var data = {
            brokerId: $scope.selectedBroker.id
        };

        $rootScope.saveFile(
            L.tsq("Select file"),
            { name: "xml", description: "XML Files" },
            "SaveBrokerStocks",
            "BrokerStocks.xml",
            null,
            function (targetPath) {
                data.filePath = targetPath;
                BrokerService.export(data);
            }
        );
    }

    $scope.onImport = function () {
        hidePopup('#editBrokerStocksModal');
        if ($scope.selectedBroker.system) {
            $rootScope.showError(L.tsq('Stocks can\'t be imported to this broker.'));
            return;
        }
        console.log("Import stocks for broker")

        var data = {
            brokerId: $scope.selectedBroker.id
        };

        var fileExtension = { name: 'csv', description: 'csv files' };
        $rootScope.showFilePicker(L.tsq('Select file'), "LoadBrokers", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                data.filePath = paths[0];
                BrokerService.import(data);
            }
        });
    }

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (event == SQEvents.get('BROKERS_CHANGED')) {
            reloadBrokers();
            updateBrokersGridData();
        } else if (event == SQEvents.get('INSTRUMENTS_CHANGED') || event == SQEvents.get('SESSIONS_CHANGED')) {
            updateBrokersGridData();
        }
        else {
            return;
        }
        try {
            $scope.$digest();
        } catch (err) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.tab.callAction = function (actionName, rowIndex) {
        switch (actionName) {
            //----------------------------------------------------------------------------
            case 'add':
                $scope.currentAction = 'Add';
                $scope.canSetSpUse = true;
                $scope.canSetMtUse = true;
                $scope.canSetTimezone = true;

                $scope.currentBroker.id = null;
                $scope.currentBroker.name = '';
                $scope.currentBroker.desc = '';
                $scope.currentBroker.mtUse = true;
                $scope.currentBroker.stockPickerUse = isQuantDataManager() ? false : true;
                $scope.currentBroker.postfix = '';
                $scope.currentBroker.mtTimezone = getDefaultTimezone(),

                showPopup('#addBrokerModal');

                break;

            //----------------------------------------------------------------------------
            case 'delete':
                var selectedBroker = getSelectedBrokersIds(true);
                if (isSelectedBrokersUsed()) {
                    $rootScope.showError(L.tsq('You have to select broker which is not used by any instrument or session.'));
                    return;
                }

                if (selectedBroker == "") {
                    $rootScope.showError(L.tsq('You have to select some non default broker.'));
                    return;
                }
                var count = selectedBroker.split(",").length;

                var text = L.tsq("Are you sure you want to remove selected brokers (%d)?", [count]);
                $rootScope.showConfirm(count > 1 ? L.tsq("Remove brokers") : L.tsq("Remove broker"), text,
                    function (confirmed) {
                        if (confirmed) {
                            BrokerService.removeBrokers(selectedBroker);
                        }
                    });

                break;

            case 'update':
                console.log("Update data")

                var ids = [];
                var selection = grid.getSelectedRows();
                var selectedRows = selection.selectedRows;
                if (selectedRows.length == 0) {
                    $rootScope.showError(L.tsq('You have to select some broker.'));
                    return;
                }

                if (!$scope.selectedBroker.stockPickerUse) {
                    $rootScope.showError(L.tsq('This function is for stockpicking broker profile only.'));
                    return;
                }

                for (var iRow of selectedRows) {
                    var userData = grid.rows[iRow].userData;
                    ids.push(userData.id);
                }

                var data = {
                    brokerId: ids.toString()
                };

                BrokerService.updateData(data);

                break;

            case 'edit-stocks':
                if ($scope.selectedBroker == null) {
                    $rootScope.showError(L.tsq('You have to select some broker.'));
                    return;
                }

                if (!$scope.selectedBroker.stockPickerUse) {
                    $rootScope.showError(L.tsq('This broker profile can\'t hold any stocks.'));
                    return;
                }

                console.log("List stocks")

                var data = {
                    brokerId: $scope.selectedBroker.id
                };

                BrokerService.listStocks(data, function (response) {
                    $scope.currentAction = 'EditStocks';
                    $scope.currentBroker = $scope.selectedBroker;
                    $scope.currentBroker.stocks = response.stocks;

                    showPopup('#editBrokerStocksModal');
                });

                break;
            case 'import-sessions':
                $scope.dataDetails.postfix = '';
                $scope.dataDetails.broker = -1;
                importType = 'session';
                showPopup('#importSessionsModal');
                break;
            case 'import-instruments':
                $scope.dataDetails.postfix = '';
                $scope.dataDetails.broker = -1;
                importType = 'instrument';
                showPopup('#importInstrumentsModal');
                break;
            case "save":
                console.log("Save brokers");

                var selection = grid.getSelectedRows();
                var selectedRows = selection.selectedRows;
                if (selectedRows.length == 0) {
                    $rootScope.showError(L.tsq('You have to select some broker.'));
                    return;
                }

                var ids = [];
                for (var iRow of selectedRows) {
                    var userData = grid.rows[iRow].userData;
                    ids.push(userData.id);
                }

                var data = {
                    ids: ids,
                };

                $rootScope.saveFile(
                    L.tsq("Select file"),
                    { name: "xml", description: "XML Files" },
                    "SaveBrokers",
                    "Brokers.xml",
                    null,
                    function (targetPath) {
                        data.filePath = targetPath;
                        BrokerService.saveXml(data);
                    }
                );

                break;

            //----------------------------------------------------------------------------
            case "load":
                console.log("Load brokers");

                var data = {};

                var fileExtension = { name: "xml", description: "XML Files" };
                $rootScope.showFilePicker(
                    L.tsq("Select file"),
                    "LoadBroker",
                    true,
                    false,
                    null,
                    fileExtension,
                    null,
                    function (paths) {
                        if (paths) {
                            data.filePath = paths[0];
                            BrokerService.loadXml(data);
                        }
                    });

                break;
        }
    }

    function getDefaultTimezone() {
        if ($scope.timezones.length > 0) {
            return $scope.timezones[0].value;
        }
    }

    $scope.getTimezoneLabel = function () {
        var tz = getItem($scope.timezones, 'value', $scope.currentBroker.mtTimezone);
        return tz ? tz.name : '';
    }

    function isSelectedBrokersUsed() {
        var selectedIndexes = grid.getSelectedRows().selectedRows;

        for (var i = 0; i < selectedIndexes.length; i++) {
            var rowData = grid.rows[selectedIndexes[i]];
            if (isBrokerUsedInMt(rowData.userData.id)) {
                return true;
            }
        }

        return false;
    }

    function isBrokerUsedInMt(brokerId) {
        var instruments = SQConstants.getConstants().instruments;
        if (instruments) {
            for (var i = 0; i < instruments.length; i++) {
                if (instruments[i].broker == brokerId) {
                    return true;
                }
            }
        }

        var sessions = SQConstants.getConstants().sessions;
        if (sessions) {
            for (var i = 0; i < sessions.length; i++) {
                if (sessions[i].broker == brokerId) {
                    return true;
                }
            }
        }

        return false;
    }

    $scope.getBrokerProfileLabel = function () {
        var brokerProfile = getItem($scope.mtBrokerProfiles, 'id', $scope.dataDetails.broker);
        return brokerProfile ? brokerProfile.name : '';
    }

    function getSelectedBrokersIds(nonSystemOnly) {
        var selectedIndexes = grid.getSelectedRows().selectedRows;
        var result = "";

        for (var i = 0; i < selectedIndexes.length; i++) {
            var rowData = grid.rows[selectedIndexes[i]];
            if (!nonSystemOnly || rowData.userData.system == false) {
                result += rowData.userData.id + ",";
            }
        }

        return result.substr(0, result.length - 1);
    }

    $scope.onStartImport = function (form) {
        var selectedIndexes = importGrid.getSelectedRows().selectedRows;

        var selected = [];
        for (var i = 0; i < selectedIndexes.length; i++) {
            var rowData = importGrid.rows[selectedIndexes[i]];
            selected.push(rowData.cells[0]);
        }


        if (selected.length == 0) {
            $rootScope.showError(L.tsq("You have to select at least one record from grid below"));
            return;
        }

        var request = {
            filePath: $scope.dataDetails.importFile,
            type: importType,
            broker: $scope.dataDetails.broker,
            postfix: $scope.dataDetails.postfix,
            selected: selected
        }

        var callback = function (result) {
            if (result.success) {
                if (importType == 'session') {
                    $rootScope.showSuccess(L.tsq("XML file with sessions was imported"));
                    hidePopup('#importSessionsModal');
                } else {
                    $rootScope.showSuccess(L.tsq("XML file with instruments was imported"));
                    hidePopup('#importInstrumentsModal');
                }
            }
        }

        if (testExists(request)) {
            $rootScope.showConfirm(importType == 'session' ? L.tsq("Import sessions") : L.tsq("Import instruments"),
                L.tsq('Records with the same name already exists in StrategyQuant. Do you want to override them?'),
                function (confirmed) {
                    if (confirmed) {
                        BrokerService.importSessionInstrument(request, callback);
                    }
                });
        } else {
            BrokerService.importSessionInstrument(request, callback);
        }
    }

    function testExists(details) {
        var broker = $scope.mtBrokerProfiles.filter(b => b.id == details.broker)[0];
        if (details.type == 'session') {
            var sessions = SQConstants.getConstants().sessions;
            for (var i = 0; i < sessions.length; i++) {
                var session = sessions[i];
                for (var ii = 0; ii < details.selected.length; ii++) {
                    var name = details.selected[ii] + details.postfix;
                    if (session.name == name) {
                        return true;
                    }
                }
            }
        } else {
            var instruments = SQConstants.getConstants().instruments;
            for (var i = 0; i < instruments.length; i++) {
                var instrument = instruments[i];
                for (var ii = 0; ii < details.selected.length; ii++) {
                    var name = details.selected[ii] + details.postfix;
                    if (instrument.instrument == name) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    function showImportOverview() {
        var details = {
            filePath: $scope.dataDetails.importFile,
            type: importType
        }

        BrokerService.importGetOverview(details, function (result) {
            if (result.success) {
                refreshOverviewGrid(result.data);
            } else {
                $rootScope.showErrorModal(L.tsq("File overview error"), result.data.error, false, true);
            }
        });
    }

    function refreshOverviewGrid(data) {
        var widths;
        var columns;
        var gridId;

        if (importType == 'session') {
            gridId = "importSessionsGrid";
            columns = [{ title: L.tsq("Name"), type: "text", sort: "text" }];
            widths = ["*"];
        } else {
            gridId = "importInstrumentsGrid";
            columns = [{ title: L.tsq("Name"), type: "text", sort: "text" }, { title: L.tsq("Description"), type: "text", sort: "text" }];
            widths = [200, "*"];
        }

        importGrid = new sqGrid(gridId);
        importGrid.setColumns(columns, !!importGrid);
        importGrid.setWidths(widths, !!importGrid);
        importGrid.setEmptyGridText('No data available.');
        importGrid.disableSorting();
        importGrid.enableCheckboxes(true);

        if (arrayNotEmpty(data)) {
            for (var i = 0; i < data.length; i++) {
                var rowData = angular.copy(data[i]);
                importGrid.addRow(importType == 'session' ? [rowData.name] : [rowData.name, rowData.description], true);
            }
        }

        importGrid.headerRedraw();
        importGrid.bodyRedraw();

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.openImportFilePicker = function () {
        $rootScope.showFilePicker(L.tsq("Select file to import"), L.tsq("ImportData"), true, false, null, null, null, function (paths) {
            if (paths) {
                var filePath = paths[0];
                $scope.dataDetails.importFile = filePath;
                showImportOverview(false);
            }
        });
    }

    function reloadBrokers() {
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b => b.mtUse);
        brokerProfiles.unshift({
            id: -1,
            name: 'SQ default',
            postfix: ''
        });
        $scope.mtBrokerProfiles = brokerProfiles;
    }

    $scope.openHelp = function () {
        AppService.openHelp('broker-profiles');
    }

    //- Initialization --------------------------------------------------------------

    var grid;
    var importGrid;
    var initialized = false;
    var importType;

    initGrids();

    $scope.stockPickerVisible = !isQuantDataManager();
    $scope.isQuantDataManager = isQuantDataManager(); 

    $scope.currentBroker = {};
    $scope.selectedBroker = null;
    reloadBrokers();
    $scope.dataDetails = {
        postfix: '',
        broker: -1
    }

    var listenerId = "BrokersCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('BROKERS_CHANGED'), SQEvents.get('INSTRUMENTS_CHANGED'), SQEvents.get('SESSIONS_CHANGED')
    ], onEvent);

    $(document).on("keyup keypress keydown", "#addBrokerModal textarea, #editBrokerStocksModal textarea", function (e) {
        e.stopPropagation();
    });

});