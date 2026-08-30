//product:BUILDER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 5, {
		title: Ltsq('What to build'),
		help: Ltsq('Choose what kind of strategy to build, its style, build mode, number of conditions in a strategy and Stop Loss + Profit Target ranges.'),
        helpURL: 'what-to-build',
        task: 'Build',
        configElemName: 'WhatToBuild',
        dataItem: 'settings-what-to-build',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/whatToBuild.html',
        controller: 'WhatToBuildCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elStrategyType = getChildElement(settingsElement, "StrategyType");
            var elMarketSides = getChildElement(settingsElement, "MarketSides");
            var elSLPTOptions = getChildElement(settingsElement, "SLPTOptions");
            var settingsText = "";

            switch(getAttrValue(elStrategyType, "type")) {
				case "simple": settingsText += L.tsq("Simple strategies"); break;
				case "multi-tf": settingsText += L.tsq("Multi-symbol/Multi-TF strategies"); break;
				case "template": settingsText += L.tsq("Strategies using template"); break;
				case "improve": settingsText += L.tsq("Improve existing strategy"); break;
				default: settingsText += L.tsq("Unknown settings"); break;
            }
            
            var msType = getAttrValue(elMarketSides, "type");
            settingsText += msType == "both" ? "" : ", " + msType + " only";

            var SLRequired = getNodeBooleanValue(elSLPTOptions, "SLRequired", false);
			var SLFixedPips = getNodeBooleanValue(elSLPTOptions, "SLFixedPips", false);
			var SLATR = getNodeBooleanValue(elSLPTOptions, "SLATR", false);
			var PTRequired = getNodeBooleanValue(elSLPTOptions, "PTRequired", false);
			var PTFixedPips = getNodeBooleanValue(elSLPTOptions, "PTFixedPips", false);
			var PTATR = getNodeBooleanValue(elSLPTOptions, "PTATR", false);
			var SeparatedSettings = getNodeBooleanValue(elSLPTOptions, "SeparatedSettings", false);

			settingsText += ", ";
			
			if(!SLRequired && !PTRequired){
				settingsText += L.tsq("SL/PT not required");
			}
			else {
				settingsText += SLRequired ? L.tsq("SL") : "";
				settingsText += (SLRequired && PTRequired ? (SeparatedSettings ? "&" : "/") : "");
				settingsText += (PTRequired ? L.tsq("PT required") : "");
				/*
				var SLText = "";
				var PTText = "";

				if(SLRequired){
					if(SLATR){
						SLText += ", ATR";
					}
					if(SLFixedPips){
						SLText += SLATR ? " or " : ", ";
						SLText += "fixed pips";
					}
				}

				settingsText += SLText;
				
				if(PTRequired && SeparatedSettings){
					if(PTATR){
						PTText += ", ATR";
					}
					if(PTFixedPips){
						PTText += PTATR ? " or " : ", ";
						PTText += "fixed pips";
					}
				}

				settingsText += SLText != PTText ? PTText : "";
				*/

				var ATRUsed = (SLRequired && SLATR) || (PTRequired && PTATR);
				var fixedPipsUsed = (SLRequired && SLFixedPips) || (PTRequired && PTFixedPips);

				var typeText = ATRUsed ? L.tsq("ATR") : "";
				typeText += fixedPipsUsed ? (ATRUsed ? " " + L.tsq("or fixed pips") : L.tsq("fixed pips")) : "";

				settingsText += typeText.length ? (", " + typeText) : "";
			}
			
			var BuildModeService = injector.get("BuildModeService");
			if(BuildModeService){
				if(!BuildModeService.loaded) BuildModeService.loadSettings();

				settingsText += "<br>" + BuildModeService.getDescription();
			}

            return settingsText;
        }
    });
});