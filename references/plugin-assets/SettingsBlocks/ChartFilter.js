angular.module('app').filter('chart', function(){
    return function(complexity){
        return complexity.chartName + " (" + complexity.symbol + " / " + complexity.timeframe + ")";
    }
});
