angular.module('app.resultstabs.portfoliocomposer').controller('PortfolioComposerLogCtrl', function ($scope, $timeout, $q, PortfolioComposerLogService, SQResultsData, $element, SkinService, SQEvents, L) {
    console.log("PortfolioComposerLog controller initialized");

    function init() {
        initEditor();
    }

    function initEditor() {
        require.config({
            paths: {
                'vs': 'libs/vs'
            }
        });

        require(['vs/editor/editor.main'], function () {
            $timeout(function () {
                var config = {
                    value: L.tsq('No strategy selected.'),
                    readOnly: true,
                    language: 'html',
                    fontSize: 12,
					minimap: {
						enabled: false
					}
                };
                var targetElement = document.getElementById('pc-editor');

                editor = monaco.editor.create(targetElement, config);

                var skin = SkinService.selectedSkin.name;
                if (skin == 'Dark skin') {
                    monaco.editor.setTheme('vs-dark')
                } else {
                    monaco.editor.setTheme('vs')
                }

                $timeout(function () {
                    editor.layout();
                    loaded = true;
                    editorDeferred.resolve();
                }, 100);
            }, 0, false);
        });
    }

    $scope.onRefresh = function () {
        refreshContent(true);
    }

    //- Event Handlers --------------------------------------------------------------

    function refreshContent(force) {
        if (!$scope.tab.active) return;

        if (!loaded && !initCalled) {
            initCalled = true;
            init();
        }

        var dataInfo = SQResultsData.getDataInfo();
        if(!dataInfo.strategyName) {
            return;
        }
        
        SQResultsData.getData(
            handlerId, 
            [], 
            force
        ).then(function (result) {
            latestResult = result;
            
            if(loaded){
                displayCode();
            }
            else {
                if(!initCallbackDefined){
                    editorDeferred.promise.then(displayCode);
                    initCallbackDefined = true;
                }
            }
        });
    }

    function displayCode(){
        if (!editor) {
            return;
        }

        monaco.editor.setModelLanguage(editor.getModel(), "txt");
        editor.layout();
        editor.setValue(latestResult.data.text || L.tsq('Log not available.'));
        try { $scope.$digest(); } catch (err) { }
    }

    function fetchData(params) {
        var deferred = $q.defer();

        PortfolioComposerLogService.print(params, function (result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    $scope.$on('$destroy', function () {
        destroyConfigWatch();

        if (window.parent && window.parent.removeListener) {
            window.parent.removeListener("PortfolioComposerLogCtrlSkin");
        }

        SQResultsData.removeHandler(handlerId);
    });


    //- Initialization --------------------------------------------------------------

    var initCalled = false;
    var loaded = false;
    var latestResult = null;
    var initCallbackDefined = false;

    var editor = null;

    var editorDeferred = $q.defer();

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    var handler = SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }

    function onSkinChanged() {
        var skin = SkinService.selectedSkin.name;
        if (typeof monaco !== 'undefined') {
            if (skin == 'Dark skin') { //dark
                monaco.editor.setTheme('vs-dark')
            } else { //light
                monaco.editor.setTheme('vs')
            }
        }
    }

    $(window).on("resize", function () {
        if (editor) {
            editor.layout();
        }
    });

    if (window.parent && window.parent.addListener) {
        window.parent.addListener("PortfolioComposerLogCtrlSkin", [SQEvents.get("SKIN_CHANGED")], onSkinChanged);
    }

    if (isAlgoWizard()) {
        SQEvents.addListener("PortfolioComposerLogCtrlSkin", [
            SQEvents.get("SKIN_CHANGED")
        ], onSkinChanged);
    }

});