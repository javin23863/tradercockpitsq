angular
  .module("app.databanks")
  .controller(
    "DatabanksCtrl",
    function (
      $rootScope,
      $scope,
      $element,
      $timeout,
      sqPlugin,
      BackendService,
      DatabankService,
      SQEvents,
      SQConstants,
      SQWebSocketService,
      $state,
      AppService,
      L,
      ProjectControlPanelService,
      UniqueIdGenerator
    ) {
      console.log("DatabanksCtrl initialized");

      $scope.customDatabanksEnabled = AppService.isTaskManager() || window.appConfig.product == "RETESTER" || AppService.isPortfolioMaster();

      function init(callback) {
        $scope.databankTabs.records = 0;
        $scope.databankTabs.tabs.length = 0;
        $scope.databankTabs.clickedTab = null;
        $scope.databankTabs.activeTab = null;
        $scope.dropdownMenu = null;
        $scope.databankTabs.dropdownMenuId = UniqueIdGenerator.generateId();
        $scope.actionButtons.length = 0;
        $scope.databankTabs.idPrefix = "databank-tabs",

        // $scope.databankTabs.tabsAfterButtons.length = 0;

        DatabankService.databankList(function () {
          loadToArray($scope.databankTabs.tabs, getDatabankTabs());
          loadToArray($scope.actionButtons, getActionButtons());
          $scope.dropdownMenu = createDatabankMenu();

          $rootScope.progress = $rootScope.progress || {};

          ProjectControlPanelService.removeListener(listenerId);
          ProjectControlPanelService.addListener(listenerId, onEngineUpdate);

          $scope.databankTabs.activeTab = $scope.databankTabs.tabs[0];

          $timeout(
            function () {
              redrawDatabankMenu();
            },
            0,
            false
          );

          if (callback) callback();
        });
      }

      $scope.hideMenu = function () {
        $($scope.databankTabs.dropdownMenuId).removeClass("open");
      };

      $scope.onOpenManageViews = function () {
        if ($scope.databankTabs.activeTab) {
          window.parent.broadcastEvent(
            SQEvents.get("DB_VIEW_EDIT"),
            $scope.databankTabs.activeTab.view
          );
        }

        window.parent.showPopup("#manageViewsDatabank");
        hideDropdown();
      };

      function hideDropdown() {
        $("div.databanks-settings-menu").removeClass("open");
      }

      $scope.addDatabank = function () {
        window.parent.callAction("createDatabank", function (databankName) {
          var defaultSyncType = SQConstants.getSettings()["databankSyncType"];

          var defaultPosition = ($scope.databankTabs.tabs.length + 1) * 100;
          var position = defaultPosition;

          //set the new position in between current databank and the one on the right side

          if (
            $rootScope.activeDatabankTab &&
            $rootScope.activeDatabankTab.title
          ) {
            var min = -1;
            var max = 10000000;

            for (let a = 0; a < $scope.databankTabs.tabs.length; a++) {
              let dbTab = $scope.databankTabs.tabs[a];

              if (dbTab.title == $rootScope.activeDatabankTab.title) {
                if (DatabankService.isDefaultDatabank(dbTab.title)) {
                  min = 10;
                } else {
                  min = dbTab.position;
                }
                break;
              }
            }

            for (let a = 0; a < $scope.databankTabs.tabs.length; a++) {
              let dbTab = $scope.databankTabs.tabs[a];

              if (dbTab.position > min && dbTab.position < max) {
                max = dbTab.position;
              }
            }

            if (min > 0 && max < 10000000) {
              position = parseInt(min + (max - min) / 2);
            }
          }

          DatabankService.createDatabank(
            databankName,
            position,
            function (data) {
              var newTab = {
                title: databankName,
                dataItem: getDataItem(databankName),
                templateUrl:
                  "../../../plugins/ProjectDatabanks/views/databank.html",
                controller: "DatabankCtrl",
                active: false,
                syncType: defaultSyncType ? defaultSyncType : syncTypes.never,
                view: SQConstants.getConstants().defaultDatabankViews.main,
                position: position,
                records: 0,
                hasMenu: hasDatabankMenu(databankName),
                onSaveSettings: saveSettings,
                triggerDigest: function () {
                  tryDigest();
                },
              };

              $scope.databankTabs.tabs.push(newTab);

              $rootScope.showSuccess(data.success);

              SQEvents.notifyListeners(SQEvents.get("DATABANK_CHANGED"), {});

              $timeout(
                function () {
                  window.dispatchEvent(new Event("resize"));
                },
                0,
                false
              );
            }
          );
        });
      };

      $scope.clearAllDatabanks = function () {
        if (
          $rootScope.project.state == $scope.projectStates.loading ||
          $rootScope.project.state == $scope.projectStates.running
        ) {
          $rootScope.showError(
            L.tsq("Cannot clear databanks when project is running")
          );
          return;
        }

        $rootScope.showConfirm(
          L.tsq("Clear all databanks"),
          L.tsq(
            "Are you sure you want to clear all databanks?<br>Please note - this will clear ALL databanks in your project!"
          ),
          function (confirmed) {
            if (!confirmed) {
              return;
            }

            DatabankService.clearAllDatabanks();
          }, false, null, null, true
        );
      };

      function getDataItem(title) {
        return cssId("db-" + title);
      }

      function getActionButtons() {
        var plugins = [];
        var buttons = [];

        var comboPlugins = sqPlugin.getCombos("ResultsDatabankAction");
        var buttonPlugins = sqPlugin.getButtons("ResultsDatabankAction");

        var broker = SQConstants.getConstants().broker;

        for (let i = 0; i < comboPlugins.length; i++) {
          var comboPlugin = comboPlugins[i];
          comboPlugin.isCombo = true;

          //disable Source code option for white-label versions
          if (broker && broker.encryption && comboPlugin.title == "Save") {
            for (var a = comboPlugin.options.length - 1; a >= 0; a--) {
              if (comboPlugin.options[a].title == "Source code") {
                comboPlugin.options.splice(a, 1);
              }
            }
          }

          plugins.push(comboPlugin);
        }

        for (let i = 0; i < buttonPlugins.length; i++) {
          var buttonPlugin = buttonPlugins[i];
          plugins.push(buttonPlugin);
        }

        for (let i = 0; i < plugins.length; i++) {
          var plugin = plugins[i];

          var excludeTasks = plugin.excludeTasks;
          if (
            !excludeTasks ||
            excludeTasks.split(",").indexOf(AppService.getTask().type) < 0
          ) {
            buttons.push(plugin);
          }
        }

        return buttons;
      }

      //Loads the databanks that are contained in the current state object
      function getDatabankTabs() {
        var tabs = [];
        var databanks = DatabankService.databanks;

        for (var i = 0; i < databanks.length; i++) {
          let dataItem = getDataItem(databanks[i].name);

          var databank = {
            title: databanks[i].name,
            dataItem: dataItem,
            templateUrl:
              "../../../plugins/ProjectDatabanks/views/databank.html",
            controller: "DatabankCtrl",
            active: false,
            view: databanks[i].view,
            syncType: databanks[i].syncType,
            position: databanks[i].position,
            display: databanks[i].display,
            records: 0,
            hasMenu: hasDatabankMenu(databanks[i].name),
            tooltipHelp: getDatabankHelp(databanks[i]),
            onSaveSettings: saveSettings,
            triggerDigest: function () {
              tryDigest();
            },
            htmlId: dataItem
          };

          if (i == 0) {
            databank.active = true;
            $rootScope.activeDatabankTab = angular.copy(databank);
            AppService.setDatabank(databank);
          }

          tabs.push(databank);
        }

        return tabs;
      }

      function getDatabankHelp(databank) {
        var helpObject = false;
        // available databanks: "Last generation","Initial population","Strategies to improve","Strategies to optimize","Last generation",
        switch (databank.name) {
          case "Existing portfolio":
            helpObject = {
              text: L.tsq("Fit strategy to existing portfolio."),
              link: "fit-strategy-to-existing-portfolio",
            };
            break;

          default:
            helpObject = false;
            break;
        }

        if (!helpObject) {
          return false;
        }
        return (
          helpObject.text +
          ' <a data-helpurl="' +
          helpObject.link +
          '">' +
          L.tsq("Learn more") +
          "</a>"
        );
      }

      function createDatabankMenu() {
        if ($scope.customDatabanksEnabled) {
          return sqPlugin.getPlugins("CustomDatabankAction");
        } else return null;
      }

      function hasDatabankMenu(name) {
        if (
          ($scope.customDatabanksEnabled) &&
          !DatabankService.isDefaultDatabank(name)
        ) {
          return true;
        } else return false;
      }

      function onDatabanksInit() {
        $(".tabs-header div").show();
        $(".tabs-menu").hide();

        $scope.databankTabs.activeTab = $scope.databankTabs.getActiveTab();

        SQEvents.notifyListeners(
          SQEvents.get("DATABANKS_LOADED"),
          $scope.databankTabs.activeTab
        );
      }

      function onDatabankChange() {
        $scope.databankTabs.activeTab = $scope.databankTabs.getActiveTab();

        if ($scope.databankTabs.activeTab) {
          var activeTab = $scope.databankTabs.activeTab;
          $rootScope.activeDatabankTab.title = activeTab.title;

          var selectedStrategies = activeTab.getSelectedStrategies
            ? activeTab.getSelectedStrategies()
            : "";

          AppService.setDatabank(activeTab);
          AppService.setSelectedStrategies(selectedStrategies);

          fakeGridFocus();

          SQEvents.notifyListeners(SQEvents.get("DATABANK_CHANGED"), activeTab);
          SQEvents.notifyListeners(
            SQEvents.get("STRATEGY_SELECTED"),
            selectedStrategies
          );

          $timeout(
            function () {
              window.dispatchEvent(new Event("resize"));
            },
            0,
            false
          );
        }
      }

      function fakeGridFocus() {
        var activeTab = $scope.databankTabs.activeTab;
        if (
          activeTab.getGrid == undefined ||
          activeTab.getGrid() == undefined
        ) {
          $timeout(function () {
            fakeGridFocus();
          }, 200);
          return;
        }
        activeTab.getGrid().fakeGridFocus();
      }

      function saveSettings() {
        DatabankService.saveConfig($scope.databankTabs.tabs);
      }

      function displayDatabank(name, show, displayFirst) {
        var databank = getItem($scope.databankTabs.tabs, "title", name);

        if (databank) {
          if (
            (databank.display === true && show) ||
            (databank.display === false && !show)
          )
            return; //no need to change anything

          databank.display = show;

          if (databank.active && !show) {
            //select first visible tab
            for (let i = 0; i < $scope.databankTabs.tabs.length; i++) {
              var tab = $scope.databankTabs.tabs[i];
              if (tab.display !== false) {
                $scope.databankTabs.selectTab(tab.dataItem);
                break;
              }
            }
          } else if (show && displayFirst) {
            for (let i = 0; i < $scope.databankTabs.tabs.length; i++) {
              $scope.databankTabs.tabs[i].position = 100;
            }
            databank.position = 1;
            $scope.databankTabs.selectTab(databank.dataItem);
          }
        } else {
          console.warn(
            "Displaying databank - Databank '" + name + "' doesn't exist"
          );
        }
      }

      $scope.getDatabanksCount = function () {
        var count = 0;

        for (var i = 0; i < $scope.databankTabs.tabs.length; i++) {
          var tab = $scope.databankTabs.tabs[i];
          if (tab.display !== false) {
            count++;
          }
        }

        return count;
      };

      $scope.getStrategiesCount = function () {
        var count = 0;

        for (var i = 0; i < $scope.databankTabs.tabs.length; i++) {
          var tab = $scope.databankTabs.tabs[i];
          if (tab.display !== false) {
            count += tab.records;
          }
        }

        return count;
      };

      function removeDatabank(tab) {
        removeItem($scope.databankTabs.tabs, "title", tab.title);
        var firstTab = $scope.databankTabs.tabs[0];
        $scope.databankTabs.selectTab(firstTab.dataItem);
      }

      function renameDatabank(oldName, newName) {
        var databank = getItem($scope.databankTabs.tabs, "title", oldName);
        if (databank) {
          databank.title = newName;
        } else {
          console.error(
            "Cannot rename databank. Databank with name '" +
              oldName +
              "' not found"
          );
        }
      }

      function updatePositions() {
        for (var i = 0; i < $scope.databankTabs.tabs.length; i++) {
          var databankTab = $scope.databankTabs.tabs[i];
          var updatedDB = getItem(
            DatabankService.databanks,
            "name",
            databankTab.title
          );
          if (updatedDB) {
            databankTab.position = updatedDB.position;
          }
        }
      }

      function tryDigest() {
        $scope.$evalAsync();
      }

      function changeDatabankRecordsCount(newValue) {
        var oldValue = $scope.databankTabs.records;
        if (oldValue != newValue) {
          $scope.databankTabs.records = newValue;
          tryDigest();
        }
      }

      //- Event Handlers and Watches --------------------------------------------------------------

      function onWebSocketData(args) {
        if (args && args.customProjectStats && $scope.databankTabs) {
          for (var i = 0; i < args.customProjectStats.length; i++) {
            var projectStats = args.customProjectStats[i];

            if (projectStats.projectName == $scope.projectName) {
              changeDatabankRecordsCount(projectStats.strategies);
              break;
            }
          }
        }
      }

      function onEngineUpdate(channelData) {
        if (channelData.projectName == $scope.projectName) {
          if ($scope.databankTabs) {
            changeDatabankRecordsCount(channelData.strategies);
          }
        }
      }

      function onEvent(event, data) {
        if (event == SQEvents.get("DATABANK_DISPLAY")) {
          displayDatabank(data.name, data.show, data.displayFirst);
        } else if (event == SQEvents.get("APP_PROJECT_CHANGED")) {
          SQWebSocketService.unsubscribe(
            $scope.projectName,
            SQConstants.getConstants().channels.databanks
          );

          //subscription is done afterwards in init method called after receiving APP_PANEL_CHANGED event
          DatabankService.projectName = data;
          $scope.projectName = DatabankService.projectName;

          if (data == null) {
            ProjectControlPanelService.removeListener(listenerId);
            return;
          }

          init(onDatabanksInit);
        } else if (event == SQEvents.get("TASK_ADDED")) {
          //we must reload databanks, because some databanks may need to be created (e.g. Initial population, Strategies to improve...)
          init(onDatabanksInit);
        } else if (event == SQEvents.get("PROJECT_STATUS_CHANGED")) {
          if (data.projectName == AppService.getProject()) {
            projectRunning = data.status == $scope.projectStates.running;
          }
        } else if (event == SQEvents.get("CUSTOM_DATABANK_ACTION")) {
          switch (data.name) {
            case "delete":
              removeDatabank(data.tab);
              break;
            case "move":
              updatePositions();
              break;
            case "rename":
              renameDatabank(data.oldName, data.newName);
              onDatabankChange();
              break;
          }
        } else return;

        tryDigest();
      }

      $scope.$on("$destroy", function () {
        ProjectControlPanelService.removeListener(listenerId);
        SQEvents.removeListener(listenerId);
        SQEvents.removeListener(listenerId2);
      });

      //- Initialization & variables --------------------------------------------------------------

      var syncTypes = SQConstants.getConstants().databankSyncTypes;

      function getCallbackId() {
        return AppService.getProject() + "-databankCtrl";
      }

      var listenerId = getCallbackId();
      var projectRunning = false;

      $rootScope.activeDatabankTab = $rootScope.activeDatabankTab || {
        title: "Results",
      };

      $scope.project = $scope.$parent.project;

      $scope.projectStates = SQConstants.getConstants().runningStatuses;
      $scope.isTaskManager = AppService.isTaskManager();

      DatabankService.projectName = AppService.getProject();
      $scope.projectName = DatabankService.projectName;

      $scope.databankTabs = DatabankService.databankTabs;
      $scope.databankTabs.onSelectTab = onDatabankChange;
      $scope.databankTabs.afterInit = onDatabanksInit;

      $scope.actionButtons = [];
      $scope.actionOptionsCombos = sqPlugin.getCombos(
        "ResultsDatabankOptionsAction"
      );

      if ($scope.customDatabanksEnabled) {
        $scope.databankTabs.controls = [
          {
            title: L.tsq("+ New databank"),
            class: "databanks-add-btn",
            iconClass: "",
            onClick: function () {
              $scope.addDatabank();
            },
          },
          {
            title: L.tsq("Clear all databanks"),
            class: "databanks-add-btn clear-all",
            iconClass: "",
            onClick: function () {
              $scope.clearAllDatabanks();
            },
          },
        ];
      }

      SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);
      SQEvents.addListener(
        listenerId,
        [
          SQEvents.get("DATABANK_DISPLAY"),
          SQEvents.get("APP_PROJECT_CHANGED"),
          SQEvents.get("PROJECT_STATUS_CHANGED"),
          SQEvents.get("TASK_ADDED"),
          SQEvents.get("CUSTOM_DATABANK_ACTION"),
        ],
        onEvent
      );

      if (!$scope.isTaskManager) {
        init();
      }

      function redrawDatabankMenu() {
        if ($scope.customDatabanksEnabled) {
          var tabsBodyDiv = $(".sqn-databanks .tabs-body");
          if (
            tabsBodyDiv.length > 0 &&
            $(".sqn-container").hasClass("active")
          ) {
            $(".databank-toolbar").css(
              "top",
              tabsBodyDiv.position().top + 11 + "px"
            );
          }
        }
      }

      $(window).on("resize", function () {
        redrawDatabankMenu();
      });

      var listenerId2 = getCallbackId() + "-sync";
      window.parent.addListener(
        listenerId2,
        [SQEvents.get("CHANGE_AUTOSYNC_GLOBAL")],
        function (event, data) {
          for (var i = 0; i < $scope.databankTabs.tabs.length; i++) {
            var tab = $scope.databankTabs.tabs[i];
            tab.syncType = data;
          }
        }
      );
    }
  );
