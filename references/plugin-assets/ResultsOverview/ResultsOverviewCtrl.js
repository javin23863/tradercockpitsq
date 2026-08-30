angular.module('app.resultstabs.overview').controller('ResultsOverviewCtrl', function ($rootScope, $scope, $q, $timeout, ResultsOverviewService, SQEvents, SQConstants, SQResultsData, $element) {
    console.log("Results overview controller initialized");

    $scope.templates = [];

    $scope.config = {
        template: 'SQDefault'
    };

    $scope.onFilterChange = function (strategyName, resultkey, direction, sampleType) {
        $scope.strategyName = strategyName;
        $scope.resultkey = resultkey;
        $scope.direction = direction;
        $scope.sampleType = sampleType;

        refreshContent();
    }

    function initTemplates() {
        var templates = InitializationData().results.overview.templates;

        loadToArray($scope.templates, templates);

        $scope.direction = directions.both;
        $scope.sampleType = sampleTypes.full;

        if ($scope.templates.length > 0) {
            var item = getItem($scope.templates, 'value', 'SQDefault');
            if(item) {
                $scope.config.template = 'SQDefault';
            } else {    
                $scope.config.template = $scope.templates[0].value;
            }
        }

        $scope.$applyAsync();
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (strategyChanged) {
            updateSettings(handlerContent);
            var args = handlerContent ? {
                resultKey: $scope.resultkey,
                direction: $scope.direction,
                sampleType: $scope.sampleType
            } : null;

            $scope.toolbar.refresh(args);
            return; //data will be fetched during next function call from onFilterChange method
        }

        if (!$scope.tab.active) return;

        SQResultsData.getData(handlerId, [$scope.resultkey, $scope.direction, $scope.sampleType, $scope.config.template]).then(function (result) {
            var iframeHtml = $('#resultsOverview').contents().find('html').get(0);
            iframeHtml.innerHTML = result.data;

            applyStyles();
        });
    }

    function updateSettings(handlerContent) {
        if (!handlerContent || !handlerContent.projectName) return;

        var attributes = handlerContent.data.attributes;
        $scope.resultkey = attributes.resultKey;
        $scope.direction = attributes.direction;
        $scope.sampleType = attributes.sampleType;
        $scope.config.template = attributes.template;
    }

    function fetchData(params) {
        var deferred = $q.defer();
        
        ResultsOverviewService.getOverviewContent(params).then(function (result) {
            var htmlContent = result.overviewHtml;

            if (!result.success || !result.overviewHtml) {
                console.log("Cannot get overview content." + result.error);
                htmlContent = '<h3 style="text-align: center; font-weight: normal; font-size: 14px; font-family: Arial, Helvetica, sans-serif;">No strategy selected</h3>';
            }
            deferred.resolve(htmlContent);
        });

        return deferred.promise;
    }

    function applyStyles() {
        //remove all skin styles

        var iframeHead = $('#resultsOverview').contents().find('head').get(0);

        for(var i=iframeHead.childNodes.length-1; i >= 0; i--){
            var childElem = iframeHead.childNodes[i];
            if(childElem.nodeName.toLowerCase() != "style") continue;

            if(getAttrStringValue(childElem, "title") == "skinLink"){
                iframeHead.removeChild(childElem);
            }
        }

        //add parent skin styles

        var parentStyleSheets = document.styleSheets;
        var cssString = "";

        for (var i = 0, count = parentStyleSheets.length; i < count; i++) {
            var parentStyleSheet = parentStyleSheets[i];
            if(parentStyleSheet.title != "skinLink") continue;

            try {
                if (parentStyleSheet.cssRules) {
                    var cssRules = parentStyleSheet.cssRules;
                    
                    for (var j = 0, countJ = cssRules.length; j < countJ; j++) {
                        cssString += cssRules[j].cssText;
                    }
                }
                else {
                    cssString += parentStyleSheet.cssText;  // IE8 and earlier
                }
            }
            catch(err){
                console.error(err, parentStyleSheet);
            }
        }

        if(cssString === "") return;

        var style = document.createElement("style");
        style.type = "text/css";
        style.title = "skinLink";
        try {
            style.innerHTML = cssString;
        }
        catch (ex) {
            style.styleSheet.cssText = cssString;  // IE8 and earlier
        }

        iframeHead.appendChild(style);
    }

    $scope.getTemplateLabel = function () {
        var template = getItem($scope.templates, 'value', $scope.config.template);
        return template ? template.name : '';
    }

    $scope.onTemplateChange = function () {
        refreshContent();
    }

    function onEvent(event, data) {
        console.log("overview event: " + event);

        if (event == SQEvents.get('SKIN_CHANGED')) {
            $timeout(applyStyles, 400, 0, false);
        }
    }

    $scope.strategyName = null; //the name of the selected strategy
    $scope.toolbar = {};

    var xmlParser = new DOMParser();

    var directions = SQConstants.getConstants().directions;
    var sampleTypes = SQConstants.getConstants().sampleTypes;

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['resultKey', 'direction', 'sampleType', 'template']);

    var listenerId = "ResultsOverviewCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SKIN_CHANGED')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("ResultsOverviewCtrl");
    }, 0, false);

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
    }

    initTemplates();
});