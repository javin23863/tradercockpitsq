angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
	    
	sqPluginProvider.plugin("SettingsTab", 70, {
        title: Ltsq('Ranking'),
        help: Ltsq('Define how strategy rank is computed and how many strategies to save to databank.<br/>If filtering is available you can configure filters the strategy has to pass before it is saved.'),
        helpURL: 'ranking-options',
        task: 'Build,Retest,Optimize,AutomaticRetest,NeuralNetworkTrainer',
        configElemName: 'Rankings',
        dataItem: 'settings-ranking',
        templateUrl: '../../../plugins/SettingsRankings/views/ranking.html',
        controller: 'RankingCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elFitnessCriteria = getChildElement(settingsElement, "FitnessCriteria");
            var elSettings = getChildElement(elFitnessCriteria, "Settings");
            var elRanking = getChildElement(elSettings, "Ranking");

            var method = getAttrValue(elFitnessCriteria, "method");
            var rankingType = getAttrValue(elRanking, "type");

            var settingsText = L.tsq("Fitness: %s", [rankingType]) + " (";

            switch(method){
                case "ComputeFromStrategyResult": settingsText += L.tsq("Main data backtest"); break;
                case "RetestOnAdditionalMarkets": settingsText += L.tsq("Cross check - Retest on additional markets"); break;
                case "MonteCarloRetest": settingsText += L.tsq("Cross check - Monte Carlo retest"); break;
                case "MonteCarloManipulation": settingsText += L.tsq("Cross check - Monte Carlo manipulation"); break;
                default: return "N/A";
            }

            settingsText += ")<br/>" + L.tsq("Save best %d strategies", [getNodeValue(settingsElement, "MaxStrategies")]);
			
            return settingsText;
        }
    });
    
});