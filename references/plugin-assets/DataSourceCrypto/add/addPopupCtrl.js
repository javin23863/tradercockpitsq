angular.module('app.datasource.crypto').controller('cryptoAddPopupCtrl', function ($rootScope, $scope, $q, SQEvents, DataSourceCryptoService, L, SQConstants) {

    function initGrid() {
        dataGrid = new sqGrid("cryptoDataGrid");

        var columns = [
            { title: L.tsq('Symbol'), type: "text", sort: 'text' },
        ];

        var widths = [200];

        dataGrid.setFirstColumnAsId(true, false);
        dataGrid.setColumns(columns, !!dataGrid);
        dataGrid.setWidths(widths, !!dataGrid);
        dataGrid.setEmptyGridText(L.tsq('No symbols available.'));
        dataGrid.enableCheckboxes(true);


        dataGrid.headerRedraw();
    }

    function reloadData(filter) {
        if (!dataGrid) {
            return;
        }

        dataGrid.removeAllRows(true, true);

        for (var i = 0; i < symbols.length; i++) {
            var symbol = symbols[i];

            if (filter && !checkFilter(symbol)) {
                continue;
            }

            dataGrid.addRow([symbol], true);
        }

        dataGrid.bodyRedraw();
    }

    function checkFilter(symbol) {
        var search = $scope.filter.text.toLowerCase();

        if (symbol.toLowerCase().indexOf(search) < 0) {
            return false;
        }

        return true;
    }

    var TF = SQConstants.getConstants().timeframe;
    $scope.timeframes = []

    $scope.action.onSelect = function (symbols, grid2, item) {
        console.log("cryptoAddPopupCtrl", item)

        $scope.title = item.title;
        $scope.dataDetails.exchange = item.id;
        $scope.filter.text = '';
        
        init()
    }

    function init() {
        DataSourceCryptoService.getSymbols($scope.dataDetails.exchange, function(data) {
            loadToArray(symbols, data.symbols);
            loadToArray($scope.timeframes, data.timeframes);

            if($scope.timeframes.length > 0) {
                $scope.dataDetails.timeframe = $scope.timeframes[0];
            }

            reloadData();
            
            $scope.dataDetails.disclaimerConfirmed = false;

            showPopup("#cryptoAddModal", function () {
                setTimeout(function () { window.dispatchEvent(new Event('resize')); }, 100);
            });
        });
    }

    $scope.onSave = function () {
        $scope.dataDetails.symbols = "";
        $scope.dataDetails.postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';

        for (var i = 0; i < dataGrid.getNumberOfRows(); i++) {
            if (dataGrid.isRowChecked(i)) {
                $scope.dataDetails.symbols += dataGrid.getCellValue(i, 0) + ",";
            }
        }

        if (!$scope.dataDetails.symbols) {
            $rootScope.showError(L.tsq("No symbols selected"));
            return;
        }

        if(!$scope.dataDetails.disclaimerConfirmed) {
            $rootScope.showError(FreeDataDisclaimer);
            return;
        }

        $scope.dataDetails.symbols = $scope.dataDetails.symbols.substr(0, $scope.dataDetails.symbols.length - 1);

        $rootScope.setProgressInfo(L.tsq('Crypto data'), L.tsq("Adding symbols..."), 0, function() {
            DataSourceCryptoService.addCancel();       
        }, true);

        DataSourceCryptoService.add($scope.dataDetails, function(result) {
            //commneted because backend returns another message, when the symbol was really added into databank
            //so this cause to have 2 different message: Add symbol, Symbol added
           // $rootScope.showSuccess(result.success);
            hidePopup("#cryptoAddModal");
        });
    }

    //- Initialization --------------------------------------------------------------

    var symbols = [];
    
    $scope.dataDetails = {
        exchange: null,
        symbol: null,
        timeframe: null,
        postfix: null,
        disclaimerConfirmed: false
    };

    var dataGrid;
    var data;

    $scope.filter = {
        text: '',
        onSearch: function () {
            reloadData(true);
        },
        onReset: function () {
            $scope.filter.text = '';
            reloadData();
        },
    };

    initGrid();
});