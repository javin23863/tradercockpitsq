angular.module('app.datasource.mt5api').controller('DataSourceMt5ApiImportCtrl', function ($rootScope, $scope, DataSourceMt5ApiService, SQConstants, SQWebSocketService, AppService, DMDataService, L) {
    var grid;
    var symbolsGrid;

    $scope.errors = 
    $scope.onStartImport = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        var symbols = [];

        for (var i = 0; i < symbolsGrid.getNumberOfRows(); i++) {
            var isChecked = symbolsGrid.isRowChecked(i);
            var isCategory = symbolsGrid.getUserData(i, "category");

            if (isChecked && !isCategory) {
                symbols.push(symbolsGrid.getCellValue(i, 0));
            }
        }

        $scope.config.symbols = symbols.toString();
        $rootScope.setProgressInfo("Importing MT5 symbols", "");
        DataSourceMt5ApiService.importData($scope.config, function (data) {
            hidePopup('#importMT5ApiModal');
        })
    }

    $scope.onSelect = function () {
        if ($scope.config.source!=='portable'){
            return;
        }

        $rootScope.showFilePicker(L.tsq('Select MT5 installation folder'), "mt5InstallFolder", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.config.path = paths[0];

                try {
                    $scope.$digest();
                } catch (er) {}
            }
        });
    }

    $scope.action.onSelect = function (symbols, grid2) {
        console.log("DataSourceMt5ApiImportCtrl")

        grid = grid2;

        showPopup("#importMT5ApiModal");

        $scope.dateRange.setRange($scope.config.dateFrom, $scope.config.dateTo, $scope.config.dateFrom, true);  

    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DataSourceMt5ApiService.importDataAction(symbol, action);
    }

    function initGrid() {
        symbolsGrid = new sqGrid("mt5ApiImportGrid");

        var columns = [
            { title: L.tsq('Symbol'), type: "text", sort: 'text' },
            { title: L.tsq('Name'), type: "text", sort: 'text' }
        ];

        var widths = [150, "*"];

        symbolsGrid.onSelectionChanged = function (rows, values) {
            if (rows && rows.length) {
                updateCheckboxes(rows[0], values[0]);
            }
        };

        symbolsGrid.setFirstColumnAsId(true, false);
        symbolsGrid.setColumns(columns, !!symbolsGrid);
        symbolsGrid.setWidths(widths, !!symbolsGrid);
        symbolsGrid.setEmptyGridText(L.tsq('No symbols available.'));
        symbolsGrid.disableSorting();
        symbolsGrid.enableCheckboxes(true);
        symbolsGrid.headerRedraw();
    }

    $scope.getCategoryLabel = function () {
        var category = getItem($scope.categories, "value", $scope.filter.category);
        return category ? category.name : '';
    }


      $scope.getBrokerProfileLabel = function () {
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.config.broker);
        return brokerProfile ? brokerProfile.name : '';
    }

    function reloadBrokers() {
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b => b.mtUse);
        brokerProfiles.unshift({
            id: -1,
            name: 'SQ default',
            postfix: '',
        });
        $scope.brokerProfiles = brokerProfiles;
    }

    function checkFilter(item) {
        var search = $scope.filter.text.toLowerCase();

        if (item.name.toLowerCase().indexOf(search) < 0 && item.description.toLowerCase().indexOf(search) < 0) {
            return false;
        }

        if ($scope.filter.category && item.path.indexOf($scope.filter.category) < 0) {
            return false;
        }

        return true;
    }    

 function updateCheckboxes(rowIndex, state) {
        if (symbolsGrid.getUserData(rowIndex, "category")) {
            symbolsGrid.setRowChecked(rowIndex, state, false, true);
            rowIndex++;

            var totalRows = symbolsGrid.getNumberOfRows();
            while (rowIndex < totalRows && !symbolsGrid.getUserData(rowIndex, "category")) {
                symbolsGrid.setRowChecked(rowIndex, state, false, true);
                rowIndex++;
            }
            symbolsGrid.bodyRedraw();
        }
    }    

    $scope.loadAvailableSymbols = function(filter){
        symbolsGrid.setEmptyGridText(L.tsq('Loading symbols...'));
        symbolsGrid.removeAllRows();

        $scope.loading = true;
        DataSourceMt5ApiService.loadAvailableSymbols($scope.config, function (data) {
            $scope.loading = false;
            if (!data.success){
                symbolsGrid.setEmptyGridText(L.tsq('Error while fetching symbol. Check the log please.'));
                symbolsGrid.removeAllRows();
                return;
            }

            symbolsGrid.removeAllRows();

            $scope.categories = [];
            var usedCategory = []
            for (var i = 0; i < data.symbols.length; i++) {
                var item = data.symbols[i];
                var category = item.path;
                if (usedCategory.indexOf(category) == -1) {
                    $scope.categories.push({value:category, name:category});
                    usedCategory.push(category);
                }
            }

            var categories = [];
            var categoryIndexes = [];
            var j = 0;

            for (var i = 0; i < data.symbols.length; i++) {
                var item = data.symbols[i];

                if (filter && !checkFilter(item)) {
                    continue;
                }

                var rowIndex = j + categories.length;
                var category = item.path;

                var categoryIndex = categories.indexOf(category);
                if (categoryIndex == -1) {
                    categories.push(category);
                    categoryIndexes.push(i + categories.length);

                    symbolsGrid.addRow([category, '']);
                    symbolsGrid.setUserData(rowIndex, "category", true);
                    symbolsGrid.setRowClass(rowIndex, "category");
                }

                symbolsGrid.addRow(
                    [
                        item.name,
                        item.description
                    ], true);

                j++;
            }

            symbolsGrid.bodyRedraw();
        });
    }

    $scope.onBrokerChanged = function () {
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.config.broker);
        var postfix = brokerProfile ? brokerProfile.postfix : '';
        $scope.config.postfix = postfix;
    }    

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.config.dateFrom = dateFrom;
        $scope.config.dateTo = dateTo;
        $scope.config.dateType = type;
    }

    function getDateRange() {
        var today = new Date();
        var oneYearAgo = new Date(today);
        oneYearAgo.setFullYear(today.getFullYear() - 1);

        var todaysTime = getDateString(today);
        var dateFrom = getDateString(oneYearAgo);

        return [dateFrom, todaysTime];
    }

    $scope.openLink = function(){
        AppService.openBrowser('https://strategyquant.com/doc/quantdatamanager/metatrader5-data-import');
    }

    //- Initialization --------------------------------------------------------------

    var dateRange = getDateRange();

    $scope.dateRange = {};
    $scope.config = {
        symbols: null,
        path: SQConstants.getSettings()['mt5InstallPath'],
        source:"portable",
        timeframe:"M1",
        postfix: "",
        broker: -1,
        brokerName: '',
        dateFrom: dateRange[0],
        dateTo: dateRange[1],
        dateType: 'custom'
    };

    $scope.categories = [];
    $scope.filter = {
        text: '',
        onSearch: function () {
            $scope.loadAvailableSymbols(true);
        },
        onReset: function () {
            $scope.filter.text = '';
            $scope.loadAvailableSymbols();
        },
        category: null,
    };    

    $scope.selectedFor = 'NA';
    $scope.loading=false;

    reloadBrokers();
    initGrid();

});