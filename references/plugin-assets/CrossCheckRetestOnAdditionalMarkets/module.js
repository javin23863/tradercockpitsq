angular.module('app').config(function(sqPluginProvider, SQEventsProvider) {
    
    SQEventsProvider.add('AR_MAIN_SETUP_CHANGED', 'ar-main-setup-changed');

	sqPluginProvider.plugin("CrossCheck", 3, {
        name: 'RetestOnAdditionalMarkets',
        title: Ltsq('Retest on additional markets'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckRetestOnAdditionalMarkets/settings.html',
            controller: 'RetestOnAdditionalMarketsCtrl',
        },
        filteringTab: {
            templateUrl: '../../../plugins/CrossCheckRetestOnAdditionalMarkets/filtering.html',
        }
    });
});