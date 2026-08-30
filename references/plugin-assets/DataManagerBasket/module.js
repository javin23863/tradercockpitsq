angular.module('app.baskets', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {
	sqPluginProvider.plugin("MainTab", 51, {
        title: Ltsq('Stock groups'),
        help: Ltsq('Stock groups are - as name suggests - groups of stocks.')+" "+Ltsq('It can be indexes like S&P500, which consists of 500 stocks, or you can define your own custom group of symbols.')+
        "<br/>"+Ltsq('They support also complete history of index composition changes, which prevents survivorship bias. Default unmodifiable stock groups are enclosed in double braces [[ ]].')+
        "<br/>"+Ltsq('Stock groups must be fullly downloaded and updated before they will be available to use.'),
        dataItem: 'baskets',
        templateUrl: '../../../plugins/DataManagerBasket/views/baskets.html',
        controller: 'BasketsController',
        menuActive: 'baskets'
    });
});