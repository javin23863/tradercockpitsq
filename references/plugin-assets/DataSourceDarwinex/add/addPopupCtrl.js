angular.module('app.datasource.darwinex').controller('darwinexAddPopupCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, DarwinexService, L) {

    $scope.action.onSelect = function (symbols) {
        console.log("darwinexAddPopupCtrl")

        if (dataGrid) dataGrid.deselectAllCheckboxes();

        shownWarning = false;
        reloadBrokers();
        $scope.dataDetails.disclaimerConfirmed = false;

        try { $scope.$digest(); } catch (err) { }

        //if ($scope.fullLicense){
        showPopup("#darwinexAddModal", function () {
            setTimeout(function () { window.dispatchEvent(new Event('resize')); }, 100);
        });
        /*}else{
            $rootScope.showError(L.tsq('Darwinex CDN download is available only for users of full license.'));
        }*/
    }

    $scope.getBrokerProfileLabel = function () {
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.dataDetails.broker);
        return brokerProfile ? brokerProfile.name : '';
    }

    $scope.onBrokerChanged = function () {
        if (!shownWarning && $scope.dataDetails.broker != -1) {
            $rootScope.showSuccess('You have selected a non-default broker. Data will be automatically adjusted to the broker\'s time zone during download.');
            shownWarning = true;
        }

        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.dataDetails.broker);
        var postfix = brokerProfile ? brokerProfile.postfix : '';
        $scope.dataDetails.postfix = postfix;
    }

    $scope.onAllDefaultInstrument = function () {
        for (var i = 0; i < symbolGrid.rows.length; i++) {
            if (symbolGridInstruments[i] ==-1001) {
                sqGridSelectValueSet(symbolGrid, i, 1, -1, true);
                symbolGridInstruments[i] = -1;
            }
        }

        symbolGrid.bodyRedraw();
        try { $scope.$digest(); } catch (err) { }
    }

    $scope.onAllSkipSymbols = function () {
        for (var i = 0; i < symbolGrid.rows.length; i++) {
            if (symbolGridInstruments[i] ==-1001) {
                sqGridSelectValueSet(symbolGrid, i, 1, -1000, true);
                symbolGridInstruments[i] = -1000;
            }
        }

        symbolGrid.bodyRedraw();
        try { $scope.$digest(); } catch (err) { }
    }

    function reloadBrokers() {
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b => b.mtUse);
        brokerProfiles.unshift({
            id: -1,
            name: 'SQ default',
            postfix: ''
        });
        $scope.brokerProfiles = brokerProfiles;
    }


    function initGrid() {
        dataGrid = new sqGrid("darwinexDataGrid");

        var columns = [
            { title: L.tsq('Symbol'), type: "text", sort: 'text' },
            { title: L.tsq('Available data range'), type: "text", sort: 'text' },
        ];

        var widths = [150, 200];

        dataGrid.setFirstColumnAsId(true, false);
        dataGrid.setColumns(columns, !!dataGrid);
        dataGrid.setWidths(widths, !!dataGrid);
        dataGrid.setEmptyGridText(L.tsq('No Darwinex symbols available.'));
        dataGrid.enableCheckboxes(true);

        dataGrid.headerRedraw();

        loadData();
    }

    function loadData() {
        $q.when(DarwinexService.getData()).then(function () {
            data = DarwinexService.getData();

            reloadData();
        });
    }

    function reloadData(filter) {
        if (!dataGrid) {
            return;
        }

        dataGrid.removeAllRows(true, true);

        var j = 0;
        for (var i = 0; i < data.length; i++) {
            var item = data[i];

            if (filter && !checkFilter(item)) {
                continue;
            }

            dataGrid.addRow(
                [
                    item.symbol,
                    "from " + timeToDateString(item.dateFrom)
                ], true);
        }

        dataGrid.bodyRedraw();
    }

    function checkFilter(item) {
        var search = $scope.filter.text.toLowerCase();

        if (item.symbol.toLowerCase().indexOf(search) < 0) {
            return false;
        }

        return true;
    }

    $scope.onSave = function () {
        var symbolsArray = [];
        var postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';

        for (var i = 0; i < dataGrid.getNumberOfRows(); i++) {
            if (dataGrid.isRowChecked(i)) {
                var symbol = dataGrid.getCellValue(i, 0);
                symbolsArray.push(symbol);
            }
        }

        if (symbolsArray.length == 0) {
            $rootScope.showError(L.tsq("No symbols selected"));
            return;
        }

        if (!$scope.dataDetails.disclaimerConfirmed) {
            $rootScope.showError(FreeDataDisclaimer);
            return;
        }
        if ($scope.dataDetails.broker == -1) {
            DarwinexService.addData(postfix, symbolsArray, $scope.dataDetails.broker, []).then(function (result) {
                if (result.success) {
                    $rootScope.setProgressInfo(L.tsq('Darwinex data'), L.tsq("Adding symbols..."), 0, function () {
                        DarwinexService.dataCancel();
                    }, true);

                   // $rootScope.showSuccess(result.success);
                    hidePopup("#darwinexAddModal");
                }
                else {
                    $rootScope.showError(result.data.error);
                }
            });
        } else {
            $scope.dataDetails.brokerName = $scope.getBrokerProfileLabel();
            selectInstruments(symbolsArray);
        }
    }

    function getSimpleSelectCellValue(options, value) {
        var text = "{{selectWidget options='{";

        for (var i = 0; i < options.length; i++) {
            var option = options[i];
            text += (i ? "," : "") + "\"" + option.id + "\":\"" + option.instrument + "\"";
        }

        return text + "}' value='" + value + "'}}";
    }

    function selectInstruments(symbols) {
        symbolGridSymbols = symbols;
        symbolGrid = new sqGrid('darwinexSymbolsDataGrid');

        var columns = [
            // { title: L.tsq('Use'), type: "text", sort: 'text' },
            {
                title: L.tsq('Symbol'),
                type: "text",
                sort: 'text'
            },
            {
                title: 'Instrument',
                type: "text",
                sort: 'text'
            }
        ];

        var widths = ['*', 200];

        symbolGrid.setFirstColumnAsId(false);
        symbolGrid.setColumns(columns, !!symbolGrid);
        symbolGrid.setWidths(widths, !!symbolGrid);
        symbolGrid.disableSorting();
        symbolGrid.enableCheckboxes(false);

        // grid.defineWidget('checkboxWidget', sqGridCheckbox);
        symbolGrid.defineWidget('selectWidget', sqGridSelect);

        symbolGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "selectUpdated":
                    symbolGridInstruments[rowIndex] = sqGridSelectValueGet(symbolGrid, rowIndex, 1);
                    break;
            }
        };

        symbolGrid.headerRedraw();
        symbolGrid.bodyRedraw();

        var instruments = angular.copy(SQConstants.getConstants().instruments)
            .filter(i => i.broker == $scope.dataDetails.broker && !i.instrument.startsWith('['));
        instruments.forEach(i => { i.id = i.instrument });
        instruments = [{ id: -1001, instrument: 'choose instrument' }].concat(instruments);

        instruments.push({ id: -1, instrument: 'SQ default' });
        instruments.push({ id: -1000, instrument: 'Skip adding this symbol' });

        symbolGridInstruments = [];
        for (var i = 0; i < symbols.length; i++) {
            var symbol = symbols[i];
            var suitableInstruments = instruments.filter(i => i.instrument.startsWith(symbol));
            var instrument = suitableInstruments.length == 0 ? -1001 : suitableInstruments[0].id;
            symbolGridInstruments.push(instrument);
            symbolGrid.addRow([symbol, getSimpleSelectCellValue(instruments, instrument)]);
        }

        try { $scope.$digest(); } catch (err) { }
        showPopup('#darwinexSelectInstrumentsPopupModal');
    }

    $scope.onDarwinexSymbolsSave = function () {
        if (!validateSelectedInstruments()) {
            $rootScope.showError('Select proper instrument or skip the symbol');
            return;
        }

        var postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';

        var symbols = [];
        var instruments = [];

        for (var i = 0; i < symbolGridInstruments.length; i++) {
            var instrument = symbolGridInstruments[i];
            if (instrument != -1000) {
                symbols.push(symbolGridSymbols[i]);
                instruments.push(instrument);
            }
        }

        if (symbols.length == 0) {
            $rootScope.showError(L.tsq("No symbols selected"));
            return;
        }

        $rootScope.setProgressInfo(L.tsq('Dukascopy data'), L.tsq("Adding symbols..."), 0, function () {
            DukascopyService.addCancel();
        }, true);

        DarwinexService.addData(postfix, symbols, $scope.dataDetails.broker, instruments).then(function (result) {
            if (result.success) {
                $rootScope.setProgressInfo(L.tsq('Darwinex data'), L.tsq("Adding symbols..."), 0, function () {
                    DarwinexService.dataCancel();
                }, true);

                $rootScope.showSuccess(result.success);
                hidePopup("#darwinexSelectInstrumentsPopupModal");
                hidePopup("#darwinexAddModal");
            }
            else {
                $rootScope.showError(result.data.error);
            }
        });
    }

    function validateSelectedInstruments() {
        for (var i = 0; i < symbolGridInstruments.length; i++) {
            if (symbolGridInstruments[i] == -1001) {
                return false;
            }
        }
        return true;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('LICENSE_CHANGED')) {
            $scope.fullLicense = !$rootScope.license.isTrial;
            try { $scope.$digest(); } catch (err) { }
        }
    }

    //- Initialization --------------------------------------------------------------

    var dataGrid;
    var data;
    var shownWarning;

    $scope.dataDetails = {
        disclaimerConfirmed: false,
        broker: -1,
        brokerName: ''
    }

    $scope.filter = {
        text: '',
        onSearch: function () {
            reloadData(true);
        },
        onReset: function () {
            $scope.filter.text = '';
            reloadData();
        }
    };

    $scope.fullLicense = true;
    initGrid();

    SQEvents.addListener("DarwinexAddPopupCtrl", [SQEvents.get('LICENSE_CHANGED')], onEvent);
});