angular.module('app.datasource.darwinex').controller('darwinexImportPopupCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DarwinexService, SQConstants, SQWebSocketService, L) {
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

        if(symbols.length==0) {
            $rootScope.showError(L.tsq("No symbols selected"));
            return;
        }

        $scope.config.symbols = symbols.toString();

        $rootScope.setProgressInfo(L.tsq('Darwinex data'), L.tsq("Adding symbols..."), 0, function() {
            DarwinexService.importDataCancel();       
        }, true);

        DarwinexService.importData($scope.config, function (data) {
            hidePopup('#darwinexImportModal');
        })
    }

    $scope.onSelect = function () {
        $rootScope.showFilePicker(L.tsq('Select Darwinex data folder'), "darwinexImport", false, false, null, null, null, function (paths) {
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
        console.log("darwinexImportPopupCtrl");

        grid = grid2;

        showPopup("#darwinexImportModal");
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DarwinexService.importDataAction(symbol, action);
    }

    function initGrid() {
        symbolsGrid = new sqGrid("darwinexImportGrid");

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

        DarwinexService.loadAvailableSymbols($scope.config, function (data) {

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
        path: SQConstants.getSettings()['darwinexImport'],
        postfix: ""
    };

    $scope.selectedFor = 'NA';

    initGrid();

});