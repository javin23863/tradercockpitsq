angular.module('app.resultstabs.equitychart').controller('BenchmarkCtrl', function ($scope, L) {

    $scope.data = {
        result: 'NA',
        moneyManagement: 'NA',
        initialCapital: 'NA',
        symbol: 'NA',
        initBuyAmountTxt: 'NA',
        resultTxt: 'NA',
        normalizationTxt: '',
        normalizationInfoTxt: '',

        stats: {
            strategy: {
                NetProfit: 'NA',
                NumberOfTrades: 'NA',
                Drawdown: 'NA',
                DrawdownPct: 'NA',
                ProfitFactor: 'NA',
                SharpeRatio: 'NA',
                ReturnDDRatio: 'NA',
                AvgTrade: 'NA',
                CAGR: 'NA',
                AnnualPctReturnDDRatio: 'NA',
                Exposure: 'NA',
                Correlation: 'NA'
            },
            benchmark: {
                NetProfit: 'NA',
                NumberOfTrades: 'NA',
                Drawdown: 'NA',
                DrawdownPct: 'NA',
                ProfitFactor: 'NA',
                SharpeRatio: 'NA',
                ReturnDDRatio: 'NA',
                AvgTrade: 'NA',
                CAGR: 'NA',
                AnnualPctReturnDDRatio: 'NA',
                Exposure: 'NA'
            },
            benchmarkDD: {
                NetProfit: 'NA',
                Drawdown: 'NA',
                DrawdownPct: 'NA',
                SharpeRatio: 'NA',
                CAGR: 'NA',
                AnnualPctReturnDDRatio: 'NA',
            }
        }
    }

    $scope.instance.print = function (data) {
       console.log("Printing benchmark table...", data);

       if(!data.result) {
            return;
       }

       $scope.data = data;

       $scope.data.initBuyAmountTxt = L.tsq("Buy & hold, initial buy amount: %s", [data.initBuyAmount]);
       $scope.data.resultTxt = L.tsq("Strategy performs <b>%s</b> than benchmark if the same risk of capital (Drawdown %) is used.", [L.tsq(data.result)]);

       let normalization = data.normalization;

       switch(normalization) {
            case 'drawdown':            $scope.data.normalizationInfoTxt = L.tsq("Normalization is simulated by decreasing trading size to match maximum Drawdown $ for benchmark."); break;
            case 'drawdownPct':         $scope.data.normalizationInfoTxt = L.tsq("Normalization is simulated by decreasing trading size to match maximum Drawdown % for benchmark."); break;  
            case 'moneyManagement':     $scope.data.normalizationInfoTxt = L.tsq("Normalization is simulated by decreasing trading size to match Money Management setting of the strategy."); break;            
            case 'exposure':            $scope.data.normalizationInfoTxt = L.tsq("Normalization is simulated by decreasing exposed capital by Exposure %."); break;
            default:                    $scope.data.normalizationInfoTxt = ''; break;
       }

       $scope.data.normalizationTxt = normalizationLabel(normalization);

       try { $scope.$digest(); } catch (err) { }
    }

    function normalizationLabel(type) {
        var item = getItem($scope.types, "key", type);
        return item ? L.tsq(item.name) : type;
    }
});