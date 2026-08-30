angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
        
    SQEventsProvider.add("REFRESH_CC_TIMES", 'benchmark-finished');

	sqPluginProvider.plugin("SettingsTab", 60, {
        title: Ltsq('Cross checks (robustness)'),
        help: Ltsqt('Cross checks allow you to run a sequence of further tests (cross checks) once the strategy passes the basic filters.')+'<br/>'+Ltsqt('You can have strategy retested with higher precision or run a Monte Carlo simulation or Optimization on it.')+'<br/><br/>'+Ltsqt('If you define also filters, you can automatically dismiss strategies that will not pass the cross check filter.')+' - '+Ltsqt('this allows you to create a "strategy testing funnel" where only the strategies that pass all the tests are saved (this applies only to Builder).'),
        helpURL: 'cross-checks-automated-strategy-robustness-tests',
        task: 'Build,Retest,AutomaticRetest',
        configElemName: 'CrossChecks',
        dataItem: 'settings-crosschecks',
        templateUrl: '../../../plugins/SettingsCrossChecks/crossChecks.html',
        controller: 'CrossChecksCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var use = getAttrBooleanValue(settingsElement, "use", false);
            if(!use) return L.tsq("Cross checks disabled");

            var crossCheckElements = getNonTextChildren(settingsElement);
            var crossCheckPlugins = sqPluginProvider.getPlugins("CrossCheck");
            
            var settingsText = "";

            for(var i=0; i<crossCheckElements.length; i++){
                if(getAttrBooleanValue(crossCheckElements[i], "use", false)){
                    var plugin = getItem(crossCheckPlugins, "name", crossCheckElements[i].nodeName);
                    if(plugin){
                        settingsText += plugin.title + "<br>";
                    }
                    else console.error("Cannot find plugin for cross check '" + crossCheckElements[i].nodeName + "'");
                }
            }

            return settingsText == "" ? L.tsq("No Cross checks configured") : settingsText;
        },
        onlyInPro: true
    });
});