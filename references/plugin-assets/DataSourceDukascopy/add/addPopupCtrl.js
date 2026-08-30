angular.module('app.datasource.dukascopy').controller('dukasAddPopupCtrl', function ($rootScope, $scope, $q, SQEvents, DukascopyService, L, SQConstants) {

    $scope.action.onSelect = function (symbols) {
        console.log("dukasAddPopupCtrl")

        if (dataGrid) dataGrid.deselectAllCheckboxes();

        shownWarning = false;
        reloadBrokers();
        try { $scope.$digest(); } catch (err) { }

        $scope.dataDetails.disclaimerConfirmed = false;

        showPopup("#dukasAddModal", function () {
            setTimeout(function () { window.dispatchEvent(new Event('resize')); }, 100);
        });
    }

    $scope.getBrokerProfileLabel = function () {
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.dataDetails.broker);
        return brokerProfile ? brokerProfile.name : '';
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

    $scope.onBrokerChanged = function () {
        if (!shownWarning && $scope.dataDetails.broker != -1) {
            $rootScope.showSuccess('You have selected a non-default broker. Data will be automatically adjusted to the broker\'s time zone during download.');
            shownWarning = true;
        }

        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.dataDetails.broker);
        var postfix = brokerProfile ? brokerProfile.postfix : '';
        $scope.dataDetails.postfix = postfix;
    }

    function initGrid() {
        dataGrid = new sqGrid("dukasDataGrid");

        var columns = [
            { title: L.tsq('Symbol'), type: "text", sort: 'text' },
            { title: L.tsq('Name'), type: "text", sort: 'text' },
            { title: L.tsq('Available M1 data range'), type: "text", sort: 'text' },
            { title: L.tsq('Available Tick data range'), type: "text", sort: 'text' },
        ];

        var widths = [150, 200, 200, 200];

        dataGrid.setFirstColumnAsId(true, false);
        dataGrid.setColumns(columns, !!dataGrid);
        dataGrid.setWidths(widths, !!dataGrid);
        dataGrid.setEmptyGridText(L.tsq('No Dukascopy symbols available.'));
        dataGrid.disableSorting();
        dataGrid.enableCheckboxes(true);

        dataGrid.onSelectionChanged = function (rows, values) {
            if (rows && rows.length) {
                updateCheckboxes(rows[0], values[0]);
            }
        };

        dataGrid.headerRedraw();

        loadData();
    }

    function loadData() {
        $q.when(DukascopyService.getData()).then(function () {
            data = DukascopyService.getData();

            $scope.filter.category = $scope.categories[0].value;

            reloadData();
        });
    }

    function reloadData(filter) {
        if (!dataGrid) {
            return;
        }

        var categories = [];
        var categoryIndexes = [];

        dataGrid.removeAllRows(true, true);

        var j = 0;
        for (var i = 0; i < data.length; i++) {
            var item = data[i];

            if (filter && !checkFilter(item)) {
                continue;
            }

            var rowIndex = j + categories.length;

            var category = item.fullCategory;

            var categoryIndex = categories.indexOf(category);
            if (categoryIndex == -1) {
                categories.push(category);
                categoryIndexes.push(i + categories.length);

                dataGrid.addRow([category, '', '', '']);
                dataGrid.setUserData(rowIndex, "category", true);
                dataGrid.setRowClass(rowIndex, "category");
            }

            dataGrid.addRow(
                [
                    item.symbol,
                    item.name,
                    "from " + timeToDateString(item.dateFromM1),
                    "from " + timeToDateString(item.dateFrom)
                ], true);

            j++;
        }

        dataGrid.bodyRedraw();
    }

    $scope.getCategoryLabel = function () {
        var category = getItem($scope.categories, "value", $scope.filter.category);
        return category ? category.name : '';
    }

    function checkFilter(item) {
        var search = $scope.filter.text.toLowerCase();

        if (item.symbol.toLowerCase().indexOf(search) < 0 && item.name.toLowerCase().indexOf(search) < 0) {
            return false;
        }

        if ($scope.filter.category && item.fullCategory.indexOf($scope.filter.category) < 0) {
            return false;
        }

        return true;
    }

    /**
     * Handles the whole categories selection
     */
    function updateCheckboxes(rowIndex, state) {
        if (dataGrid.getUserData(rowIndex, "category")) {
            dataGrid.setRowChecked(rowIndex, state, false, true);
            rowIndex++;

            var totalRows = dataGrid.getNumberOfRows();
            while (rowIndex < totalRows && !dataGrid.getUserData(rowIndex, "category")) {
                dataGrid.setRowChecked(rowIndex, state, false, true);
                rowIndex++;
            }
            dataGrid.bodyRedraw();
        }
    }

    $scope.onSave = function () {
        var symbolsArray = [];
        for (var i = 0; i < dataGrid.getNumberOfRows(); i++) {
            var isChecked = dataGrid.isRowChecked(i);
            var isCategory = dataGrid.getUserData(i, "category");

            if (isChecked && !isCategory) {
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

        if ($scope.dataDetails.broker==-1) {
            var postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';
            DukascopyService.addData(postfix, symbolsArray, $scope.dataDetails.dataType, $scope.dataDetails.broker, []).then(function (result) {
                hidePopup("#dukasAddModal");
            });
        } else {
            $scope.dataDetails.brokerName = $scope.getBrokerProfileLabel();
            selectInstruments(symbolsArray);
        }
    }

    function selectInstruments(symbols) {
        symbolGridSymbols = symbols;
        symbolGrid = new sqGrid('dukasSymbolsDataGrid');

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
        instruments = [{id:-1001, instrument:'choose instrument'}].concat(instruments);

        instruments.push({ id: -1, instrument: 'SQ default'});
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

        showPopup('#dukasSelectInstrumentsPopupModal');
    }

    function validateSelectedInstruments(){
        for(var i=0;i<symbolGridInstruments.length;i++){
            if (symbolGridInstruments[i]==-1001){
                return false;
            }
        }
        return true;
    }

    $scope.onDukascopySymbolsSave = function () {
        if (!validateSelectedInstruments()){
            $rootScope.showError('Select proper instrument or skip the symbol');
            return;
        }

        var postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';

        var symbols = [];
        var instruments = [];

        for (var i = 0; i < symbolGridInstruments.length; i++) {
            var instrument = symbolGridInstruments[i];
            if (instrument!=-1000){
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

        DukascopyService.addData(postfix, symbols, $scope.dataDetails.dataType, $scope.dataDetails.broker, instruments).then(function (result) {
            hidePopup("#dukasSelectInstrumentsPopupModal");
            hidePopup("#dukasAddModal");
        });
    }

    function getSimpleSelectCellValue(options, value) {
        var text = "{{selectWidget options='{";

        for (var i = 0; i < options.length; i++) {
            var option = options[i];
            text += (i ? "," : "") + "\"" + option.id + "\":\"" + option.instrument + "\"";
        }

        return text + "}' value='" + value + "'}}";
    }

    //- Initialization --------------------------------------------------------------

    var dataGrid;
    var data;
    var shownWarning;
    var symbolGrid;
    var symbolGridInstruments;
    var symbolGridSymbols;

    $scope.categories = DukascopyService.categories;
    $scope.dataDetails = {
        dataType: 'TICK',
        disclaimerConfirmed: false,
        broker: -1,
        brokerName: ''
    };

    $scope.filter = {
        text: '',
        onSearch: function () {
            reloadData(true);
        },
        onReset: function () {
            $scope.filter.text = '';
            reloadData();
        },
        category: null,
    };

    initGrid();
});