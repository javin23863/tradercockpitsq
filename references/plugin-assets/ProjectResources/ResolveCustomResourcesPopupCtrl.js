angular.module('app.projectresources').controller('ResolveCustomResourcesPopupCtrl', function ($rootScope, $scope, $timeout, SQEvents, SQConstants, BackendService, L) {
    console.log("ResolveCustomResourcesPopupCtrl initialized");

    $rootScope.onCheckCustomResources = function(_configXML, _actionType, _loadCallback, _filePath, _taskConfigs){
        $scope.checkingTemplate = false;

        filePath = _filePath;
        configXML = _configXML;
        actionType = _actionType;
        loadCallback = _loadCallback;
        taskConfigs = _taskConfigs;

        BackendService.sendRequest('project/checkCustomResources', { configXML: configXML, actionType: actionType, taskConfigs }, function (data) {
            if(data.resourcesXML){
                loadXMLResources(data.resourcesXML);

                if($scope.missingResources.length == 0){
                    save();
                }
                else {
                    $scope.$evalAsync();
                    showPopup("#resolveCustomResourcesPopup");
                }
            }
        }, 'POST');
    }

    $rootScope.onCheckTemplateResources = function(_configXML, resourcesXML, _filePath){
        filePath = _filePath;
        configXML = _configXML;
        actionType = SQConstants.getConstants().projectResourceActionTypes.loadTemplate;
        $scope.checkingTemplate = true;

        loadXMLResources(resourcesXML);

        if($scope.missingResources.length > 0){
            $scope.$evalAsync();
            showPopup("#resolveCustomResourcesPopup");
        }
    }

    function loadXMLResources(resourcesXML){
        elResources = xmlToObject(resourcesXML).find("Resources")[0];
        
        var elCustomSnippets = getChildElement(elResources, "CustomSnippets");
        var elCustomIndicators = getChildElement(elResources, "CustomIndicators");
        var elCustomBlocks = getChildElement(elResources, "CustomBlocks");
        var elRandomGroups = getChildElement(elResources, "RandomGroups");

        $scope.missingResources.length = 0;

        //load snippets
        var customSnippetElems = getChildElements(elCustomSnippets, "CustomSnippet");
        
        for(var i=0; i<customSnippetElems.length; i++){
            var elCustomSnippet = customSnippetElems[i];
            
            if(elCustomSnippet.childNodes && elCustomSnippet.childNodes[0]){
                addNewResource(elCustomSnippet.childNodes[0].nodeValue, 'snippet', $scope.resourceStatuses.doesntExist, elCustomSnippet);
            }
        }

        //load custom indicators
        var customIndicatorElems = getChildElements(elCustomIndicators, "CustomIndicator");
        
        for(var i=0; i<customIndicatorElems.length; i++){
            var elCustomIndicator = customIndicatorElems[i];
            
            if(elCustomIndicator.childNodes && elCustomIndicator.childNodes[0]){
                addNewResource(elCustomIndicator.childNodes[0].nodeValue, 'customIndy', $scope.resourceStatuses.doesntExist, elCustomIndicator);
            }
        }
        
        //load custom blocks
        var customBlockElems = getChildElements(elCustomBlocks, "Item");
        
        for(var i=0; i<customBlockElems.length; i++){
            var elItem = customBlockElems[i];
            addNewResource(getAttrStringValue(elItem, 'name', "Unknown"), 'customBlock', getAttrIntValue(elItem, 'status', -1), elItem);
        }

        //load random groups
        var randomGroupElems = getChildElements(elRandomGroups, "Group");
        
        for(var i=0; i<randomGroupElems.length; i++){
            var elGroup = randomGroupElems[i];
            addNewResource(getAttrStringValue(elGroup, 'name', "Unknown"), 'randomGroup', getAttrIntValue(elGroup, 'status', -1), elGroup);
        }
    }

    function addNewResource(name, type, status, xmlElement){
        $scope.missingResources.push({
            name : name,
            type : type,
            status : status,
            statusText : getStatusText(status),
            action : getDefaultAction(type, status),
            differentName : getAttrStringValue(xmlElement, 'differentName', ''),
            xmlElement : xmlElement
        });
    }

    function getDefaultAction(type, status){
        if(type == 'snippet' || type == 'customIndy') return 'ignore';

        switch(status){
            case $scope.resourceStatuses.doesntExist: return 'add';
            case $scope.resourceStatuses.differs: return 'useFromSQ';
            default: return 'Unknown';
        }
    }

    function getStatusText(status) {
        switch (status) {
            case $scope.resourceStatuses.doesntExist: return L.tsq('Not found'); 
            case $scope.resourceStatuses.differs: return L.tsq('Different'); 
            case $scope.resourceStatuses.existsWithDifferentName: return L.tsq('Exists with different name'); 
            default: return L.tsq("Unknown status '%d'", [status]);
        }
    }

    $scope.onSubmit = function () {
        var isUseFromSQ = false;

        for(var i=0; i<$scope.missingResources.length; i++){
            var missingResource = $scope.missingResources[i];

            if(missingResource.type == 'snippet' || missingResource.type == 'customIndy') continue;

            if(missingResource.action == 'useFromSQ'){
                isUseFromSQ = true;
            }

            missingResource.xmlElement.setAttribute('action', missingResource.action);
        }

        if($scope.checkingTemplate && isUseFromSQ){
            $rootScope.showConfirm(L.tsq("Template overwrite confirmation"), L.tsq("By selecting 'Use from SQ' you will overwrite settings in the template config file. Do you want to continue?"), function(confirmed){
                if(confirmed){
                    save();
                }
            }); 
        }
        else {
            save();
        }
    }

    function save(){
        var args = {
            configXML: configXML, 
            resourcesXML: xmlToString(elResources), 
            actionType: actionType 
        }

        if(filePath) {
            args.filePath = filePath;
        }

        if(taskConfigs){
            args.taskConfigs = taskConfigs;
        }

        BackendService.sendRequest('project/resolveCustomResources', args, function (data) {
            if(loadCallback) {
                loadCallback(data);
            }

            $rootScope.showSuccess(data.success);

        }, 'POST');
    }

    $scope.onCancelImport = function () {
        hidePopup("#resolveCustomResourcesPopup");
    }

    $scope.resourceStatuses = SQConstants.getConstants().projectResourceStatuses;
    $scope.checkingTemplate = false;

    $scope.missingResources = [];

    var configXML;
    var elResources;
    var loadCallback;
    var actionType;
    var filePath;
    var taskConfigs;

});