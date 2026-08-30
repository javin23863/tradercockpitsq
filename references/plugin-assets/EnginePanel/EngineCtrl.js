angular
  .module("app.engine")
  .controller(
    "EngineCtrl",
    function (
      $rootScope,
      $scope,
      $timeout,
      sqPlugin,
      SQEvents,
      SQConstants,
      EngineService,
      AppService,
      ProjectControlPanelService,
      SQWebSocketService,
      L,
      SettingsService
    ) {
      console.log("Engine controller initialized");

    $scope.appPlugins = SQConstants.getConstants().appPlugins;
    $scope.disabledForSpecialTrial = false;

      $scope.clearLog = function () {
        if ($scope.projectName == "TaskManager") {
          return;
        }

        EngineService.clearLog($scope.projectName, function (data) {
          clearLogUI();
        });
      };

      function clearLogUI() {
        /* $('.log-scroll-content').html(""); */
        $scope.totalLog = "";
      }

      $scope.cleanupMemory = function () {
        EngineService.cleanupMemory(function (data) {
          $rootScope.showSuccess(data.success);
        });
      };

      $scope.onDeleteLogOnStart = function () {
        EngineService.saveSettings({
          product: AppService.getProduct(),
          deleteLogOnStart: $scope.deleteLogOnStart,
        });
      };

      $scope.showHowToAddConfig = function () {
        showPopup("#projectConfigHelpPopup");
      };

      $scope.showDismissalStats = function () {
        refreshDismissalStats();

        $scope.dismissalStats.open = true;
        showPopup("#dismissalStatsModal");
      };

      $scope.applyMassConfig = function () {
        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        $scope.applyMassConfigDialog.show();
      };

      $scope.showAcceptedStats = function () {
        refreshAcceptedStats();

        $scope.acceptedStats.open = true;
        showPopup("#acceptedStatsModal");
      };

      $scope.showDurationStats = function () {
        refreshDurationStats();

        $scope.durationStats.open = true;
        showPopup("#durationStatsModal");
      };

      $scope.showTextLog = function () {
        showPopup("#textLogModal");

        $scope.refreshTextLog();
      };

      $scope.showVisualLog = function () {
        showPopup("#visualLogModal");

        $scope.refreshVisualLog();
      };

      $scope.refreshVisualLog = function () {
        $scope.visualLog.tasks.length = 0;

        EngineService.loadVisualLog(
          $scope.projectName,
          $scope.visualLog.type,
          function (data) {
            loadToArray($scope.visualLog.tasks, data.tasks);

            let maxBarCount = {}
            for (var i = 0; i < $scope.visualLog.tasks.length; i++) {
              let stats = $scope.visualLog.tasks[i].stats
              for(let stat of stats){
                let count = maxBarCount[stat.order];
                count = count ? count : 0
                if(stat.count > count){
                  count = stat.count;
                }
                maxBarCount[stat.order] = count;
              }
            }

            for (var i = 0; i < $scope.visualLog.tasks.length; i++) {
              let stats = $scope.visualLog.tasks[i].stats
              let mappedStats = {}
              for(let stat of stats){
                mappedStats[stat.action] = stat;
              }

              stats = stats.map((stat) => {
                if(stat.order == 1){
                  stat.pct = Math.round(stat.count / maxBarCount[stat.order] * 100 * 100) / 100
                } else {
                  stat.relPct = Math.round(stat.count / maxBarCount[stat.order] * 100 * 100) / 100
                  if(stat.percentageCompareAction){
                    stat.pct = Math.round(stat.count / mappedStats[stat.percentageCompareAction].count * 100 * 100) / 100
                  }
                }
                return stat;
              })
              stats = stats.sort((a, b) => {
                return a.order - b.order
              });
              $scope.visualLog.tasks[i].stats = stats
            }

            tryDigest();
          }
        );
      };

      $scope.formattedTime = function(milliseconds){
        let SECONDS = 1000;
        let MINUTES = SECONDS * 60;
        let HOURS = MINUTES * 60;
        let DAYS = HOURS * 24;
        
        let days = milliseconds > DAYS ? parseInt(milliseconds / DAYS) : 0;
        milliseconds -= (days * DAYS);
        let hours = milliseconds > HOURS ? parseInt(milliseconds / HOURS) : 0;
        milliseconds -= (hours * HOURS);
        let minutes = milliseconds > MINUTES ? parseInt(milliseconds / MINUTES) : 0;
        milliseconds -= (minutes * MINUTES);
        let seconds = (milliseconds/1000).toFixed(3);
        
        let string = "";
        
        if(days > 0){
          string += (string ? " " : "") + days + " Day" + (days > 1 ? "s" : "");
        }
        if(hours > 0){
          string += (string ? " " : "") + hours + " Hour" + (hours > 1 ? "s" : "")
        }
        if(minutes > 0){
          string += (string ? " " : "") + minutes + " Minute" + (minutes > 1 ? "s" : "")
        }
        if(seconds > 0){
          string += (string ? " " : "") + seconds + " Second" + (seconds > 0 ? "s" : "")
        }
        return string
      }

      $scope.needToShowCount = function(type){
        switch(type){
          case "bar":
            return true;
          default:
            return false;
        }
      }

      $scope.refreshTextLog = function () {
        $scope.textLog = L.tsq("Loading...");

        EngineService.loadTextLog($scope.projectName, function (data) {
          $scope.textLog = data.text;
          tryDigest();
        });
      };

      $scope.hideDropdown = function () {
        $(".sqn-settings .sqn-title > .dropdown").removeClass("open");
      };

      $scope.onFileSelect = function (fileName, sample) {
        if (!checkProject()) return;

        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        loadConfig(fileName, sample);
      };

      $scope.onLoad = function () {
        if (!checkProject()) return;

        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        var pathName = "loadConfigFile";
        var defaultPath = SQConstants.getSettings()["configsPath"];
        var fileExtension = {
          name: "cfx,xml,sqb",
          description: L.tsq("SQ Config Files"),
        };

        openFilePicker(
          pathName,
          defaultPath,
          fileExtension,
          function (filePath) {
            loadConfig(filePath);
          }
        );
      };

      $scope.onReset = function (configName) {
        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        AppService.resetTaskXML(configName);
      };

      $scope.isProjectRunning = function() {
        return $rootScope.project.state == $scope.projectStates.running;
      }

      function checkProject() {
        var elProject = AppService.getProjectConfig();
        var elTasks = getChildElement(elProject, "Tasks");
        var taskElems = getChildElements(elTasks, "Task");

        if (taskElems.length == 0) {
          $rootScope.showError(
            L.tsq(
              "Project doesn't contain any tasks. Please create some task first"
            )
          );
          return false;
        }

        return true;
      }

      function loadConfig(filePath, sampleName) {
        $rootScope.showConfirm(
          L.tsq("Load SQ config"),
          L.tsq(
            "Do you want to load config from selected file? <br>Any unsaved changes will be lost."
          ),
          function (confirmed) {
            if (confirmed) {
              AppService.loadXMLTemplate(filePath, sampleName);
            }
          },
          true
        );
      }

      $scope.onSave = function () {
        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        //avoid overwriting sample files
        var taskObj = getTask(AppService.getProjectConfig(), AppService.getTask());
        var templateFile = getAttrStringValue(taskObj, "templateFile");

        if (!valueFilled(templateFile)) {
          //unknown template
          $scope.onSaveAs();
          return;
        }

        $rootScope.showConfirm(
          L.tsq("Save template config"),
          L.tsq(
            "Do you want to save this config into the template? <br>Original template config will be overwritten."
          ),
          function (confirmed) {
            if (confirmed) {
              AppService.updateTemplateConfig();
            }
          },
          true
        );
      };

      $scope.onSaveAs = function () {
        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        var fileExtension = {
          name: "cfx",
          description: L.tsq("SQ Config Files"),
        };

        var pathName = SQConstants.getSettings()["loadConfigFile"] ? "loadConfigFile" : "configsPath";
        var fileName = window.appConfig.task.name;
        
        let elProject = SQConstants.getProjectConfig(AppService.getProject(), true);
        let elTasks = getChildElement(elProject, "Tasks");
        let taskElems = getChildElements(elTasks, "Task");
        let elTask = null;

        for(let i=0; i<taskElems.length; i++){
          let elTaskCurrent = taskElems[i];
          if(getAttrStringValue(elTaskCurrent, "taskXMLFile") === AppService.getTask().taskXMLFile){
            elTask = elTaskCurrent;   
            break;
          }
        }

        if(!elTask){
          $rootScope.showError("Task not found in project config (" + AppService.getTask().taskXMLFile + ")");
          return;
        }

        elTask.appendChild(xmlToObject(xmlToString(AppService.getTaskConfig())).find("Settings")[0]); //append cloned settings element

        var fileContent = xmlToString(elTask);

        $rootScope.saveFile(
          L.tsq("Save config"),
          fileExtension,
          pathName,
          fileName,
          fileContent
        );
      };

      $scope.onStrategyTypeSelect = function (selectedType) {
        if (initialized) {
          SettingsService.applySimpleTemplate(selectedType);
        }
      };

      $scope.getStrategyTypeInfo = function (selectedType) {
        return EngineService.getStrategyTypeInfo(selectedType);
      };

      function openFilePicker(pathName, defaultPath, extension, callback) {
        var path = SQConstants.getSettings()[pathName];

        if (path) {
          $rootScope.showFilePicker(
            L.tsq("Select config file to load"),
            pathName,
            true,
            false,
            null,
            extension,
            null,
            function (paths) {
              if (paths) {
                callback(paths[0]);
              }
            }
          );
        } else {
          $rootScope.showFilePicker(
            L.tsq("Select config file to load"),
            pathName,
            true,
            false,
            defaultPath,
            extension,
            null,
            function (paths) {
              if (paths) {
                callback(paths[0]);
              }
            }
          );
        }
      }

      function disabledForSpecialTrial(license) {
          var plugin = getItem($scope.appPlugins, 'appCode', appConfig.product);
          if (plugin) {
              $scope.disabledForSpecialTrial = (plugin.disabledForSpecialTrial && license?.isSpecialTrial);
          }
      }

      function init() {
        if(!AppService.getProject()) return;
        
        EngineService.getTypes($scope.projectName, function (data) {
          loadToArray($scope.types, data.types);

          for (let i = 0; i < 2; i++) {
            let item = getItem($scope.types, "type", data.settings[i]);
            let selectedType = item ? data.settings[i] : $scope.types[0].type;

            $scope.instances.push({
              number: i,
              selectedType: selectedType,
            });
          }

          $timeout(
            function () {
              loaded = true;
            },
            0,
            false
          );
        });

        initAcceptedStatsGrid();
        initDismissalStatsGrid();
        initDurationStatsGrid();

        ProjectControlPanelService.addListener(getCallbackId(), onEngineUpdate);

        var engineData = ProjectControlPanelService.getEngineData();
        if (engineData) onEngineUpdate(engineData);

        EngineService.loadSettings(
          {
            product: AppService.getProduct(),
          },
          function (data) {
            $scope.deleteLogOnStart = data.deleteLogOnStart;
          }
        );
        // engineProgressBar = new sqProgressBar($($element).find(".progress-bar-canvas")[0]);
      }

      function initAcceptedStatsGrid() {
        if ($("#acceptedStatsGrid").length == 0) {
          $timeout(function () {
            initAcceptedStatsGrid();
          }, 200);
          return;
        }
        var columns = [
          {
            title: L.tsq("Reason to accept"),
            type: "text",
            align: "left",
          },
          {
            title: L.tsq("Count"),
            type: "text",
            align: "right",
            sort: "number"
          },
          {
            title: L.tsq("% of all"),
            type: "text",
            align: "right",
            sort: "number"
          },
        ];

        acceptedStatsGrid = new sqGrid("acceptedStatsGrid");
        acceptedStatsGrid.setFirstColumnAsId(true, false);
        acceptedStatsGrid.enableCheckboxes(false);
        acceptedStatsGrid.setColumns(columns);
        acceptedStatsGrid.setWidths(["*", 100, 100]);
        acceptedStatsGrid.enableCellTitles(true, ", ");
        acceptedStatsGrid.setEmptyGridText("N/A");
        acceptedStatsGrid.setSort(1, true);
      }

      function initDismissalStatsGrid() {
        if ($("#dismissalStatsGrid").length == 0) {
          $timeout(function () {
            initDismissalStatsGrid();
          }, 200);
          return;
        }
        var columns = [
          {
            title: L.tsq("Reason to dismiss"),
            type: "text",
            align: "left",
          },
          {
            title: L.tsq("Count"),
            type: "text",
            align: "right",
            sort: "number"
          },
          {
            title: L.tsq("% of all"),
            type: "text",
            align: "right",
            sort: "number"
          },
        ];

        dismissalStatsGrid = new sqGrid("dismissalStatsGrid");
        dismissalStatsGrid.setFirstColumnAsId(true, false);
        dismissalStatsGrid.enableCheckboxes(false);
        dismissalStatsGrid.setColumns(columns);
        dismissalStatsGrid.setWidths(["*", 100, 100]);
        dismissalStatsGrid.enableCellTitles(true, ", ");
        dismissalStatsGrid.setEmptyGridText("N/A");
        dismissalStatsGrid.setSort(1, true);
      }

      function refreshDismissalStats() {
        dismissalStatsGrid.removeAllRows(false, true, true);
        if ($scope.dismissalStats.lastData) {
          var data = JSON.parse($scope.dismissalStats.lastData);
          dismissalStatsGrid.appendData(data);
        }
      }

      function refreshAcceptedStats() {
        acceptedStatsGrid.removeAllRows(false, true, true);
        if ($scope.acceptedStats.lastData) {
          var data = JSON.parse($scope.acceptedStats.lastData);
          acceptedStatsGrid.appendData(data);
        }
      }

      function initDurationStatsGrid() {
        if ($("#durationStatsGrid").length == 0) {
          $timeout(function () {
            initDurationStatsGrid();
          }, 200);
          return;
        }
        var columns = [
          {
            title: L.tsq("Name"),
            type: "text",
            align: "left",
          },
          {
            title: L.tsq("Count"),
            type: "text",
            align: "right",
            sort: "number"
          },
          {
            title: L.tsq("Avg. time"),
            type: "text",
            align: "right",
            sort: "number"
          },
          {
            title: L.tsq("Total time"),
            type: "text",
            align: "right",
            sort: "number"
          },
          {
            title: L.tsq("% of all"),
            type: "text",
            align: "right",
            sort: "number"
          },
        ];

        durationStatsGrid = new sqGrid("durationStatsGrid");
        durationStatsGrid.setFirstColumnAsId(true, false);
        durationStatsGrid.enableCheckboxes(false);
        durationStatsGrid.setColumns(columns);
        durationStatsGrid.setWidths(["*", 100, 100, 100, 100]);
        durationStatsGrid.enableCellTitles(true, ", ");
        durationStatsGrid.setEmptyGridText("N/A");
        durationStatsGrid.setSort(1, true);
      }

      function refreshDurationStats() {
        durationStatsGrid.removeAllRows(false, true, true);

        if ($scope.durationStats.lastData) {
          var data = JSON.parse($scope.durationStats.lastData);
          durationStatsGrid.appendData(data);
        }
      }

      function findChart(type, charts) {
        for (var i = 0; i < charts.length; i++) {
          var chart = charts[i];

          if (chart.type == type) {
            return chart;
          }
        }

        return null;
      }

      function getCallbackId() {
        return "EnginePanel";
      }

      function onEngineUpdate(channelData) {
        if (channelData.projectName != $scope.projectName) return;

        $scope.projectStats.backtestsPerformed = formatIntNumber(
          channelData.backtestsPerformed
        );

        $scope.projectStats.totalJobsDone = formatIntNumber(
          channelData.totalJobsDone
        );
        $scope.projectStats.strategiesRejected =
          formatIntNumber(channelData.strategiesRejected) +
          " / " +
          channelData.strategiesRejectedPct +
          " %";
        $scope.projectStats.strategiesAccepted =
          formatIntNumber(channelData.strategiesAccepted) +
          " / " +
          channelData.strategiesAcceptedPct +
          " %";

        $scope.projectStats.timePerStrategy = channelData.timePerStrategy;
        $scope.projectStats.timePerAcceptedStrategy =
          channelData.timePerAcceptedStrategy;
        $scope.projectStats.strategiesPerHour = formatIntNumber(
          channelData.strategiesPerHour
        );
        $scope.projectStats.acceptedStrategiesPerHour = formatIntNumber(
          channelData.acceptedStrategiesPerHour
        );

        $scope.projectStats.totalRunningTime = channelData.totalRunningTime;
        $scope.projectStats.taskRunningTime = channelData.taskRunningTime;
        $scope.projectStats.strategies = formatIntNumber(
          channelData.strategies
        );
        $scope.projectStats.totalSteps = channelData.totalSteps;
        $scope.projectStats.step = channelData.step;

        $scope.projectStats.timeToFinish = channelData.timeToFinish;
        $scope.projectStats.infiniteProgress = AppService.isTaskManager()
          ? true
          : channelData.infiniteProgress;
        $scope.projectStats.progressPercent = channelData.progressPercent;

        $scope.projectStats.strategiesFailed = channelData.strategiesFailed;
        $scope.projectStats.strategiesPassed = channelData.strategiesPassed;

        $scope.projectStats.runningStatus = channelData.runningStatus;
        if (
          $scope.projectStats.runningStatus == $scope.projectStates.running ||
          $scope.projectStats.runningStatus == $scope.projectStates.paused ||
          $scope.projectStats.runningStatus == $scope.projectStates.loading
        ) {
          // engineProgressBar.start();
        } else {
          // engineProgressBar.stop();
        }

        if ($scope.projectStats.runningStatus == $scope.projectStates.running) {
          $scope.projectStats.lastEvent = channelData.lastEvent;
        } else {
          $scope.projectStats.lastEvent = "";
        }

        if (channelData.dismissalStats) {
          $scope.dismissalStats.lastData = channelData.dismissalStats;
          if ($scope.dismissalStats.open) {
            refreshDismissalStats();
          }
        }

        if (channelData.acceptedStats) {
          $scope.acceptedStats.lastData = channelData.acceptedStats;
          if ($scope.acceptedStats.open) {
            refreshAcceptedStats();
          }
        }

        if (channelData.durationStats) {
          $scope.durationStats.lastData = channelData.durationStats;
          if ($scope.durationStats.open) {
            refreshDurationStats();
          }
        }

        tryDigest();
      }

      function onEngineChartsUpdate(channelData) {
        if (!loaded) {
          return;
        }

        for (var j = 0; j < $scope.instances.length; j++) {
          var type = $scope.instances[j].getSelectedType();

          var chart = findChart(type, channelData.charts);
          if (!chart) {
            continue;
          }

          $scope.instances[j].setData(chart.data);
        }

        if (channelData.fitnessEvolution)
          $scope.fitnessEvolution.setData(channelData.fitnessEvolution);

        tryDigest();
      }

      function onLogUpdate(channelData) {
        $scope.totalLog = channelData;
        tryDigest();
      }

      function onEvent(event, data) {
        if (event == SQEvents.get("APP_PROJECT_CHANGED")) {
          SQWebSocketService.unsubscribe(
            $scope.projectName,
            channels.engineCharts,
            getCallbackId()
          );
          SQWebSocketService.unsubscribe(
            $scope.projectName,
            channels.log,
            getCallbackId()
          );

          $scope.projectName = data;

          resetStats();
          clearLogUI();

          if (data != null) {
            SQWebSocketService.subscribe(
              $scope.projectName,
              channels.engineCharts,
              getCallbackId(),
              onEngineChartsUpdate
            );
            SQWebSocketService.subscribe(
              $scope.projectName,
              channels.log,
              getCallbackId(),
              onLogUpdate
            );
          }
        } else if (event == SQEvents.get("APP_PANEL_CHANGED")) {
          //TODO: subscribe/unsubscribe based on active panel
        } else if (event == SQEvents.get("PROJECT_BEFORE_START")) {
          cssAnimationSync("progress-sq");
          if ($scope.deleteLogOnStart) {
            $scope.clearLog();
          }
        } else if (event == SQEvents.get("TASK_TEMPLATES_CHANGED")) {
          $scope.taskConfigs = SQConstants.getConstants().taskConfigs;
        } else if (event == SQEvents.get('LICENSE_CHANGED')) {
            disabledForSpecialTrial(data);
        } else return;

        tryDigest();
      }

      function tryDigest() {
        if ($scope.$$phase || digestPlanned) return;

        $timeout(
          function () {
            $scope.$digest();
            digestPlanned = false;
          },
          500,
          false
        );

        digestPlanned = true;
      }

      $scope.$on("$destroy", function () {
        ProjectControlPanelService.removeListener(getCallbackId());
        SQWebSocketService.unsubscribe(
          AppService.getProject(),
          channels.engineCharts,
          getCallbackId()
        );
        SQWebSocketService.unsubscribe(
          AppService.getProject(),
          channels.log,
          getCallbackId()
        );
        SQEvents.removeListener(getCallbackId());
      });

      $scope.openHelp = function (help) {
        AppService.openHelp(help);
      };

      $scope.instances = [];
      $scope.types = [];

      $scope.projectStates = SQConstants.getConstants().runningStatuses;

      $scope.projectName = AppService.getProject();
      $scope.taskType = AppService.getTask().type;

      $scope.isPortfolioMaster = AppService.isPortfolioMaster();
      $scope.isTaskManager = AppService.isTaskManager();

      $scope.autoScroll = true;
      $scope.deleteLogOnStart = false;

      $scope.projectStats = {
        backtestsPerformed: 0,
        totalJobsDone: 0,
        strategiesRejected: "0 / 0 %",
        strategiesAccepted: "0 / 0 %",
        timePerStrategy: 0,
        timePerAcceptedStrategy: 0,
        strategiesPerHour: 0,
        acceptedStrategiesPerHour: 0,
        taskRunningTime: 0,
        totalRunningTime: 0,
        strategies: 0,
        totalSteps: 0,
        step: 0,
        strategiesFailed: 0,
        strategiesPassed: 0,
        runningStatus: $scope.projectStates.beforeStart,
        lastEvent: "",
      };

      function resetStats() {
        $scope.projectStats.backtestsPerformed = 0;
        $scope.projectStats.totalJobsDone = 0;
        $scope.projectStats.strategiesRejected = "0 / 0 %";
        $scope.projectStats.strategiesAccepted = "0 / 0 %";
        $scope.projectStats.timePerStrategy = 0;
        $scope.projectStats.timePerAcceptedStrategy = 0;
        $scope.projectStats.strategiesPerHour = 0;
        $scope.projectStats.acceptedStrategiesPerHour = 0;
        $scope.projectStats.taskRunningTime = 0;
        $scope.projectStats.totalRunningTime = 0;
        $scope.projectStats.strategies = 0;
        $scope.projectStats.totalSteps = 0;
        $scope.projectStats.step = 0;
        $scope.projectStats.strategiesFailed = 0;
        $scope.projectStats.strategiesPassed = 0;
        $scope.projectStats.runningStatus = $scope.projectStates.beforeStart;
        $scope.projectStats.lastEvent = "";
      }

      $scope.totalStrLabel =
        $scope.taskType == "Retest"
          ? L.tsq("Total tested")
          : L.tsq("Strategies generated");

      $scope.acceptedStats = {
        open: false,
        lastData: null,
      };
      $scope.dismissalStats = {
        open: false,
        lastData: null,
      };
      $scope.durationStats = {
        open: false,
        lastData: null,
      };

      $scope.strategyTypes = EngineService.strategyTypes;

      var acceptedStatsGrid = null;
      var dismissalStatsGrid = null;
      var durationStatsGrid = null;
      var loaded = false;

      $scope.fitnessEvolution = {};

      $scope.applyMassConfigDialog = {};

      // var engineProgressBar = null;
      $scope.totalLog = "";

      $scope.textLog = null;
      $scope.visualLog = {
        type: "global",
        tasks: [],
      };

      $scope.taskConfigs = SQConstants.getConstants().taskConfigs;

      var channels = SQConstants.getConstants().channels;
      var digestPlanned = false;

      init();

      SQWebSocketService.subscribe(
        AppService.getProject(),
        channels.engineCharts,
        getCallbackId(),
        onEngineChartsUpdate
      );

      SQWebSocketService.subscribe(
        AppService.getProject(),
        channels.log,
        getCallbackId(),
        onLogUpdate
      );

      SQEvents.addListener(
        getCallbackId(),
        [
          SQEvents.get("APP_PROJECT_CHANGED"),
          SQEvents.get("APP_PANEL_CHANGED"),
          SQEvents.get("PROJECT_BEFORE_START"),
          SQEvents.get("TASK_TEMPLATES_CHANGED"),
          SQEvents.get("LICENSE_CHANGED")
        ],
        onEvent
      );

      $(".sq-app-taskmanager").removeClass("sqn-hide-header");
    }
  );
