angular
  .module("app")
  .service(
    "SQResultsData",
    function ($q, BackendService, SQConstants, $timeout) {
      function getLatestData(handler, attributes) {
        if (isAlgoWizardCloud || isAlgoWizard) {
          return null;
        }

        if (isMTAnalyzer()) {
          return null;
        }

        if (
          handler.content !== undefined &&
          handler.content !== null &&
          handler.content.length != 0
        ) {
          var attributeNames = handler.dataAttributes;
          var content = getItem(handler.content, "projectName", currentProject);

          console.debug("Checking data...", content, attributes);

          //check handler content
          if (!content || !content.data || !content.data.attributes)
            return null;

          //check databank and strategy name
          if (
            content.data.databankName != currentDatabank ||
            content.data.strategyName != currentStrategy
          )
            return null;

          var passed = true;

          //check data attributes
          for (var i = 0; i < attributeNames.length; i++) {
            var attributeName = attributeNames[i];
            if (content.data.attributes[attributeName] != attributes[i]) {
              /*
              console.debug(
                "Attribute '" +
                  attributeName +
                  "' differs. " +
                  content.data.attributes[attributeName] +
                  " != " +
                  attributes[i]
              );*/
              passed = false;
              break;
            }
          }

          //check last change time
          passed =
            passed && content.data.lastFetched >= content.data.lastChanged;

          return passed ? content.data.value : null;
        }

        return null;
      }

      function getParams(handler, attributeValues) {
        var params = getAttributes(handler.dataAttributes, attributeValues);
        params.projectName = currentProject;
        params.databankName = currentDatabank;
        params.strategyName = currentStrategy;

        if (isAlgoWizardCloud || isAlgoWizard) {
          params.isAlgoWizardCloud = isAlgoWizardCloud;
          params.backtestID = backtestID;
          params.nodeID = nodeID;
        }

        return params;
      }

      function getAttributes(attributeNames, attributeValues) {
        var attributes = {};

        for (var i = 0; i < attributeNames.length; i++) {
          var attributeName = attributeNames[i];
          attributes[attributeName] = attributeValues[i];
        }

        return attributes;
      }

      function loadDataItems(callback) {
        currentDataItems = [];

        if (currentStrategy) {
          var data = {
            projectName: currentProject,
            databankName: currentDatabank,
            reportName: currentStrategy,
          };

          BackendService.getPromise("project/getDataItems", data).then(
            function (result) {
              if (result.data.success) {
                currentDataItems = result.data.dataItems;
              }

              if (callback) callback();
            }
          );
        } else {
          if (callback) callback();
        }
      }

      this.getDataInfo = function () {
        return {
          projectName: currentProject,
          databankName: currentDatabank,
          strategyName: currentStrategy,
          fileName: currentFileName,
          dataItems: currentDataItems,
          resultTypes: currentResultTypes,
          isPortfolio: isPortfolio,
          isStockpicker: isStockpicker,
          taskType: taskType,
          taskName: taskName,
          product: product,
          isTaskManager: isTaskManager,
          resultInfo: resultInfo,
          noResultItems: noResultItems,

          isAlgoWizard: isAlgoWizard,
          isAlgoWizardCloud: isAlgoWizardCloud,
          nodeID: nodeID,
          backtestID: backtestID,
        };
      };

      this.addHandler = function (
        handlerId,
        fetchDataCallback,
        refreshViewCallback,
        dataAttributes
      ) {
        instance.removeHandler(handlerId);

        var handler = {
          id: handlerId,
          fetchData: fetchDataCallback,
          refreshView: refreshViewCallback,
          dataAttributes: dataAttributes,
          content: [],
        };

        handlers.push(handler);

        return handler;
      };

      this.removeHandler = function (handlerId) {
        removeItem(handlers, "id", handlerId);
      };

      this.getData = function (handlerId, attributeValues, force) {
        var handler = getItem(handlers, "id", handlerId);
        if (!handler) return "N/A";

        var deferred = $q.defer();
        var latestData = getLatestData(handler, attributeValues);

        if (latestData !== null && !force) {
          deferred.resolve({ changed: false, data: latestData });
        } else {
          instance.showLoadingOverlay();

          handler
            .fetchData(getParams(handler, attributeValues))
            .then(function (value) {
              removeItem(handler.content, "projectName", currentProject);

              handler.content.push({
                projectName: currentProject,
                data: {
                  value: value,
                  attributes: getAttributes(
                    handler.dataAttributes,
                    attributeValues
                  ),
                  databankName: currentDatabank,
                  strategyName: currentStrategy,
                  lastFetched: new Date().getTime(),
                  lastChanged: new Date().getTime(),
                },
              });

              instance.hideLoadingOverlay();

              deferred.resolve({ changed: true, data: value });
            });
        }

        return deferred.promise;
      };

      this.refreshContent = function (params) {
        if (params && params.projectName) {
          currentProject = params.projectName;
          currentDatabank = params.databankName;
          currentStrategy = params.strategyName;
          currentFileName = params.fileName;

          currentDataItems = params.dataItems;
          currentResultTypes = params.resultTypes;

          isPortfolio = params.isPortfolio;
          isStockpicker = params.isStockpicker;
          isAlgoWizard = params.isAlgoWizard;

          taskType = params.taskType;
          taskName = params.taskName;
          product = params.product;
          isTaskManager = params.isTaskManager;
          resultInfo = params.resultInfo;
          noResultItems = params.noResultItems;

          nodeID = params.nodeID;
          backtestID = params.backtestID;

          if (window?.top?.isCloudVersion) {
            window.top.nodeID = nodeID;
          }
        } else {
          //reset
          currentProject = null;
          currentDatabank = null;
          currentStrategy = null;
          currentFileName = null;

          isPortfolio = false;
          isStockpicker = false;
          isAlgoWizard = false;

          taskType = null;
          taskName = null;
          product = null;
          isTaskManager = false;
          resultInfo = SQConstants.NO_RESULT_CHOSEN;
          noResultItems = [];

          currentDataItems = [];
          currentResultTypes = [];

          nodeID = null;
          backtestID = null;
        }

        for (var i = 0; i < handlers.length; i++) {
          var handler = handlers[i];
          if (handler.refreshView) {
            if (handler.id == "strategy-changed") {
              handler.refreshView(true, params);
            } else {
              handler.refreshView(true, getCurrentHandlerContent(handler));
            }
          }
        }
      };

      this.refreshTab = function (handlerId, strategyChanged) {
        var handler = getItem(handlers, "id", handlerId);
        if (handler && handler.refreshView)
          handler.refreshView(
            strategyChanged,
            getCurrentHandlerContent(handler)
          );
      };

      function getCurrentHandlerContent(handler) {
        return getItem(handler.content, "projectName", currentProject);
      }

      this.setDataChanged = function (
        projectName,
        databankName,
        change,
        resultsDisplayed
      ) {
        var reload = false;

        var strategyName = change.id;

        for (let i = 0; i < handlers.length; i++) {
          let handler = handlers[i];

          if (handler.content) {
            for (var a = 0; a < handler.content.length; a++) {
              var content = handler.content[a];
              if (content.projectName == projectName) {
                var data = content.data;
                if (
                  data &&
                  data.databankName == databankName &&
                  data.strategyName == strategyName
                ) {
                  if (
                    change.type ==
                    SQConstants.getConstants().databankChange.remove
                  ) {
                    data.lastFetched = 0;
                  } else {
                    data.lastChanged = new Date().getTime();
                  }

                  reload = true;

                  break;
                }
              }
            }
          }
        }

        if (reload && resultsDisplayed) {
          loadDataItems(function () {
            for (let i = 0; i < handlers.length; i++) {
              let handler = handlers[i];
              if (handler.refreshView)
                handler.refreshView(true, getCurrentHandlerContent(handler));
            }
          });
        }
      };

      this.isAlgoWizard = function () {
        return isAlgoWizard;
      };

      this.isAlgoWizardCloud = function () {
        return isAlgoWizardCloud;
      };

      this.resultsTabLoaded = function (controllerName) {
        switch (controllerName) {
          case "ResultsOverviewCtrl":
            overviewLoaded = true;
            overviewReadyDeferred.resolve();
            break;
          case "ResultsTradelistCtrl":
            tradelistLoaded = true;
            tradelistReadyDeferred.resolve();
            break;
          case "EquityChartCtrl":
            equityChartLoaded = true;
            equityChartReadyDeferred.resolve();
            break;
          case "TradeAnalysisCtrl":
            tradeAnalysisLoaded = true;
            tradeAnalysisReadyDeferred.resolve();
            break;
          case "StrategyConfigCtrl":
            strategyConfigLoaded = true;
            strategyConfigReadyDeferred.resolve();
            break;
          case "SourceCodeCtrl":
            sourceCodeLoaded = true;
            sourceCodeReadyDeferred.resolve();
            break;
          default:
            console.error(
              "ResultsTabLoaded - Unknown controller name '" +
                controllerName +
                "'"
            );
        }
      };

      var instance = this;

      var handlers = [];

      //strategy name for AlgoWizard contains timestamp to refresh the results, file name is used while saving strategy
      var currentProject, currentDatabank, currentStrategy, currentFileName;
      var currentDataItems = [];
      var currentResultTypes = [];

      var isAlgoWizard = false;
      var isPortfolio = false;
      var isStockpicker = false;
      var taskType = null;
      var taskName = null;
      var product = null;
      var isTaskManager = false;
      var resultInfo = null;
      var noResultItems = [];

      var isAlgoWizardCloud = window?.top?.isCloudVersion;
      var nodeID = null;
      var backtestID = null;

      var overviewReadyDeferred = $q.defer();
      var overviewLoaded = false;
      var tradelistReadyDeferred = $q.defer();
      var tradelistLoaded = false;
      var equityChartReadyDeferred = $q.defer();
      var equityChartLoaded = false;
      var tradeAnalysisReadyDeferred = $q.defer();
      var tradeAnalysisLoaded = false;
      var strategyConfigReadyDeferred = $q.defer();
      var strategyConfigLoaded = false;
      var sourceCodeReadyDeferred = $q.defer();
      var sourceCodeLoaded = false;

      this.uiLoadedPromise = $q.all([
        overviewReadyDeferred.promise,
        tradelistReadyDeferred.promise,
        equityChartReadyDeferred.promise,
        tradeAnalysisReadyDeferred.promise,
        strategyConfigReadyDeferred.promise,
        sourceCodeReadyDeferred.promise,
      ]);

      this.uiLoadedPromise.then(function () {
        console.info("- Results tabs loaded --------------------------");

        window.setStrategyXML = function (strategyXML, settingsXML) {
          //console.error("setStrategyXML");
          if(strategyXML){
            try {
              let elStrategyFile = xmlToObject(strategyXML).find('StrategyFile')[0];
              let elOptions = getChildElement(elStrategyFile, "options");
              currentFileName = getNodeValue(elOptions, "StrategyName", "New strategy");
            }
            catch(err){
              console.error("Parsing strategy name failed", err);
            }
          }

          for (var i = 0; i < handlers.length; i++) {
            var handler = handlers[i];
            if (handler.setStrategyXML) {
              handler.setStrategyXML(strategyXML, settingsXML);
              if (handler.refreshView) {
                handler.refreshView(true, getCurrentHandlerContent(handler));
              }
            }
          }
        };

        if (window.waitingSourceCode) {
          window.setStrategyXML(
            window.waitingSourceCode.strategyXML,
            window.waitingSourceCode.settingsXML
          );
        }
      });

      window.dumpResultsTabsStates = function () {
        console.error(
          "* Results tabs states **********************************"
        );
        console.error(
          "Overview tab: " + (overviewLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "Tradelist tab: " + (tradelistLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "Equity chart tab: " + (equityChartLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "Trade analysis tab: " +
            (tradeAnalysisLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "Strategy config tab: " +
            (strategyConfigLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "Source code tab: " + (sourceCodeLoaded ? "Loaded" : "Not loaded")
        );
        console.error(
          "********************************************************"
        );
      };

      this.showLoadingOverlay = null; //inicializovane v RESULTS/LayoutCtrl.js
      this.hideLoadingOverlay = null;
    }
  );
