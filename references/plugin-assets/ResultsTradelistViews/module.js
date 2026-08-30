//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.tradelist.views', []).config(function(sqPluginProvider, SQEventsProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsTradelistViews/tradelistViews.html", 'TradelistViewsCtrl', 'RESULTS');
    
    SQEventsProvider.add("TRADELIST_VIEW_CHANGED", "tradelist-view-changed");       //called after active tradelist view changed
    SQEventsProvider.add("TRADELIST_RELOAD_VIEWS", "tradelist-views-reload");       //called to reload the list of views
    SQEventsProvider.add("TRADELIST_VIEWS_LOADED", "tradelist-views-loaded");       //called after the list of views has been fetched from backend
    
});