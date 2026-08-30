angular.module('app.customdata', ['sqplugin']).config(function (sqPluginProvider, SQEventsProvider) {

    SQEventsProvider.add("CUSTOM_DATA_EDIT", 'custom-data-edit');
    SQEventsProvider.add("CUSTOM_DATA_VIEW", 'custom-data-view');

    sqPluginProvider.plugin("MainTab", 50, {
        title: Ltsq('External indicators'),
        help: Ltsq('External indicators are indicators / signals / anything computed outside of StrategyQuant.')+'<br>'+Ltsq('This way you can add new data that will be used when building your strategies - it doesn\'t need to be only indicators -  fundamental data or signals are also possible.'),
        dataItem: 'custom-da-ta',
        templateUrl: '../../../plugins/DataManagerCustomData/customData.html',
        controller: 'DMCustomDataCtrl',
        menuActive: 'custom-data'
    });
});