angular.module('app.datasource.sqFuturesData').controller('SQFuturesDataAddCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, SQFuturesDataService, L, $timeout) {

    $scope.barDataTypes = angular.copy(SQConstants.getConstants().barDataTypes);
    $scope.timezones = SQConstants.getConstants().timezones;

    $scope.config = {
        exchange: 0,
        postfix: "",
        symbols: null,
        searchInName: true,
        searchInTicker: true,
        exact: false,
        onlyContFutures: true,
        barType: $scope.barDataTypes.endOfBar,
        timezoneType: 2,
        timezoneShift: 5,
        timezone: getDefaultTimezone(),
    };


    $scope.exchanges = SQFuturesDataService.exchanges;
    $scope.tickers = [];
    $scope.tickersSelected = 0;
    $scope.agreedConditions = false;
    
    $scope.timeFrame = 0;

    $scope.subscription = SQFuturesDataService.subscription;

    var grid;
    var tickerColIndex;
    var exchangeColIndex;

    $scope.lookedup = false;

    function getDefaultTimezone() {
        if($scope.timezones.length>0) {
            return $scope.timezones[0].value;
        }
    }

    $scope.getTimezoneLabel = function(){
        var tz = getItem($scope.timezones, 'value', $scope.config.timezone);        
        return tz ? tz.name : '';
    }

    $scope.searchAgain = function () {
        $scope.tickers.length = 0;
        $scope.tickersSelected = 0;
        $scope.lookedup = false;
    }

    $scope.getExchangeLabel = function () {
        var exchange = getItem($scope.exchanges, 'id', $scope.config.exchange);
        return exchange ? exchange.name : '';
    }

    $scope.action.onSelect = function () {
        console.log("SQFuturesDataAddCtrl")

        SQFuturesDataService.getExchanges();
        
        $scope.tickers.length = 0;
        $scope.tickersSelected = 0;
        $scope.config.symbols = null;
        $scope.lookedup = false;

        SQFuturesDataService.verifySubscription(function () {
            showPopup('#sqFuturesDataModal');
        });
    }

    $scope.onLookup = function (form) {
        grid.removeAllRows();

        SQFuturesDataService.lookup($scope.config, function (data) {
            loadToArray($scope.tickers, data.tickers);
            $scope.lookedup = true;

            for (var i = 0; i < $scope.tickers.length; i++) {
				var allowed = $scope.tickers[i][4];
				var rowData = $scope.tickers[i];
				rowData.pop();
                grid.addRow(rowData, true);

                if (!allowed) {
                    grid.setRowDisabled(i, true);
                }
            }

            $timeout(function () {
                grid.setNewDimensions();
                grid.headerRedraw();
                grid.bodyRedraw();
            }, 0);

        });
    }

    $scope.onAdd = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        if (!$scope.agreedConditions) {
            $rootScope.showError(L.tsq("Please read and agree to StrategyQuant Data Usage Conditions"));
            return;
        }

        $scope.config.symbols = getSelectedTickers();
        if (!$scope.config.symbols) {
            $rootScope.showError(L.tsq("No tickers selected"));
            return;
        }

        $rootScope.setProgressInfo(L.tsq('SQ Futures Data'), L.tsq("Adding symbols..."), 0, function() {
            SQFuturesDataService.addCancel();       
        }, true);

        SQFuturesDataService.add($scope.config);
    
        hidePopup('#sqFuturesDataModal');
    }

    function getSelectedTickers() {
        var tickers = "";

        for (var i = 0; i < grid.getNumberOfRows(); i++) {
            if (grid.isRowChecked(i)) {
                tickers += grid.getCellValue(i, 0) + ",";
            }
        }

        if (tickers) tickers = tickers.substr(0, tickers.length - 1);

        return tickers;
    }

    function initGrid() {
        grid = new sqGrid("sqFuturesDataGrid");

        //!!!!!!!!!!! if orders of column in grid will be changed - must be updated these two variables holding indexes of ticker and exchange
        tickerColIndex=0;
        exchangeColIndex = 2;

        var columns = [
            { title: L.tsq('Ticker'), type: "text", sort: 'text' },
            { title: L.tsq('Name'), type: "text", sort: 'text' },
            { title: L.tsq('Exchange'), type: "text", sort: 'text' },
            { title: L.tsq('Data from'), type: "text", sort: 'text' },
        ];



        var widths = [100, "*", 80, 100];

        grid.setFirstColumnAsId(true, false);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No tickers found.'));
        // grid.disableSorting();
        grid.enableCheckboxes(true);
        grid.headerRedraw();

        grid.onSelectionChanged = function (selected) {
            $timeout(function () {
                $scope.tickersSelected = grid.checkedRows.length;
            }, 100);
        };
    }

    function init() {
        initGrid();
    }

    init();

     SQEvents.addListener("SQFuturesDataAddCtrl", [SQEvents.get('LICENSE_CHANGED')], onEvent);
    
    function onEvent(event, data) {
        if (event == SQEvents.get('LICENSE_CHANGED')) {
            SQFuturesDataService.subscription.verified = false;
            SQFuturesDataService.verifySubscription(function () {
                 $scope.subscription = SQFuturesDataService.subscription;
            });
        } 
    }
});