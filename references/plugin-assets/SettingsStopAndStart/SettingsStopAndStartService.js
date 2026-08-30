angular.module('app.settings').service('SettingsStopAndStartService', function ($q, $timeout, AppService, L, ProjectConditionsService, TMProjectsService) {

    this.loadSettings = function () {
        instance.gui.loading = true;

        loadProjects();

        var stopAndStartObj = AppService.getCurrentTaskTabSettings("StopAndStart");

        instance.config.conditions = ProjectConditionsService.loadConditions(stopAndStartObj);

        var elStartProject = getChildElement(stopAndStartObj, 'StartProject');
        if(elStartProject){
            instance.config.startAnother = getAttrBooleanValue(elStartProject, "use", instance.config.startAnother);
            instance.config.skipToFirstProject = getAttrBooleanValue(elStartProject, "skipToFirstProject", instance.config.startAnother);
            instance.config.projectName = elStartProject.childNodes[0] ? elStartProject.childNodes[0].nodeValue : '';
        }
        
        if(!instance.config.projectName && instance.customProjects.length > 0) {
            instance.config.projectName = instance.customProjects[0].value;
            if(elStartProject) setNodeValue(elStartProject, instance.config.projectName, AppService.xmlDoc);
        }

        $timeout(function(){ instance.gui.loading = false; }, 0, false);
    }           

    function loadProjects() {
        var allProjects = angular.copy(TMProjectsService.projects);
        var thisProjectName = AppService.getProject();

        instance.customProjects.length = 0;

        for(var i=0; i<allProjects.length; i++){
            var projectName = allProjects[i].projectName;
            if(projectName != thisProjectName){
                instance.customProjects.push({value: projectName, name: projectName});
            }
        }

        instance.customProjects.push({value: 'next-project', name: L.tsq('<< next >>')});
    }

    this.saveSettings = function () {
        var xmlDoc = AppService.xmlDoc;
        var stopAndStartObj = AppService.getCurrentTaskTabSettings("StopAndStart");
        if(!stopAndStartObj) return;

        removeAllChildren(stopAndStartObj);

        ProjectConditionsService.saveConditions(stopAndStartObj, instance.config.conditions, xmlDoc);

        var elStartProject = createChild(stopAndStartObj, 'StartProject', xmlDoc);
        elStartProject.setAttribute("use", instance.config.startAnother);
        elStartProject.setAttribute("skipToFirstProject", instance.config.skipToFirstProject);
        setNodeValue(elStartProject, instance.config.projectName, xmlDoc);
    }

    var instance = this;

    this.config = {
        conditions: [],
        startAnother: false,
        projectName: null,
        skipToFirstProject: false
    }

    this.customProjects = [];

    this.gui = {
        loading : false
    }

});