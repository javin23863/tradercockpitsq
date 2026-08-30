angular.module('app.resultstabs.spoverview').controller('SPOverviewStatsCtrl', function ($scope, L) {
    console.log("SPOverviewStats controller initialized");

    $scope.stats = {
        NetProfit: 'NA',
        AvgProfitPerYear: 'NA',
        CAGR: 'NA',

        NumberOfTrades: 'NA',
        SharpeRatio: 'NA',
        ProfitFactor: 'NA',
        ReturnDDRatio: 'NA',
        AvgTrade: 'NA',

        Drawdown: 'NA',
        DrawdownPct: 'NA',
        Exposure: 'NA',
        AnnualPctReturnDDRatio: 'NA',
        AvgProfitPerMonth: 'NA'
    }

    $scope.instance.init = function() {
    }

    $scope.instance.print = function (data) {
        $scope.stats = data;

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.instance.reset = function () {
    }

	$scope.formatMoney = function(value) {
		return "$ "+value;
	}

    $scope.formatPct = function(value) {
		return value+" %";
	}
});