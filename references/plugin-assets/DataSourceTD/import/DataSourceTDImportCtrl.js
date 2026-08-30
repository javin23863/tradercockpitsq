angular.module('app.datasource.td').controller('DataSourceTDImportCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DataSourceTDService, SQConstants, SQWebSocketService, DMDataService, L) {
    var grid;
    var symbolsGrid;

    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.errors = "";

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
            if (symbolsGrid.isRowChecked(i)) {
                symbols.push(symbolsGrid.getCellValue(i, 0));
            }
        }

        $scope.config.symbols = symbols.toString();

        DataSourceTDService.importData($scope.config, function (data) {
            hidePopup('#importTDModal');
        })
    }

    $scope.onSelect = function () {
        $rootScope.showFilePicker(L.tsq('Select TickDownloader data folder'), "dukasImportTD", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.config.path = paths[0];
                loadAvailableSymbols();

                try {
                    $scope.$digest();
                } catch (er) {}
            }
        });
    }

    var dukasSymbols = [];

    $scope.action.onSelect = function (symbols, grid2) {
        console.log("DataSourceTDImportCtrl")

        grid = grid2;

        showPopup("#importTDModal");
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DataSourceTDService.importDataAction(symbol, action);
    }

    function initGrid() {
        symbolsGrid = new sqGrid("tdimportGrid");

        var columns = [{
            title: L.tsq('Symbol'),
            type: "text",
            sort: 'text'
        }, ];

        var widths = ["*"];

        symbolsGrid.setFirstColumnAsId(true, false);
        symbolsGrid.setColumns(columns, !!symbolsGrid);
        symbolsGrid.setWidths(widths, !!symbolsGrid);
        symbolsGrid.setEmptyGridText(L.tsq('No symbols available.'));
        symbolsGrid.disableSorting();
        symbolsGrid.enableCheckboxes(true);
        symbolsGrid.headerRedraw();

        loadAvailableSymbols();
    }

    function loadAvailableSymbols() {
        if (!$scope.config.path) {
            return;
        }

        symbolsGrid.removeAllRows();

        DataSourceTDService.loadAvailableSymbols($scope.config, function (data) {

            for (var i = 0; i < data.symbols.length; i++) {
                var symbol = data.symbols[i];

                symbolsGrid.addRow([symbol], true);
            }

            symbolsGrid.bodyRedraw();
        });
    }

    //- Initialization --------------------------------------------------------------

    $scope.config = {
        symbols: null,
        path: SQConstants.getSettings()['dukasImportTD'],
        postfix: ""
    };

    $scope.selectedFor = 'NA';

    initGrid();

});