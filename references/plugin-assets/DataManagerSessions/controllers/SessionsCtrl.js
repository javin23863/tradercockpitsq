angular
  .module("app.sessions")
  .controller(
    "SessionsController",
    function (
      $scope,
      $rootScope,
      $timeout,
      $q,
      SessionService,
      SQEvents,
      SQConstants,
      L
    ) {
      console.log("Sessions controller initialized");

      function initGrids() {
        $timeout(function () {
          initSessionsGrid();
          initElementsGrid();

          $("#startTimePicker").timepicker();
          $("#endTimePicker").timepicker();
        });
      }

      function initSessionsGrid() {
        if ($("#sessionsGrid").length == 0) {
          $timeout(function () {
            initSessionsGrid();
          }, 200);
          return;
        }
        var columns = [
          {
            title: L.tsq("Session Name"),
            type: "text",
            sort: "text",
          },
          {
            title: L.tsq("Broker profile"),
            type: "text",
            sort: "text",
          },
          {
            title: "",
            type: "text",
            sort: "text",
          },
          {
            title: "",
            type: "text",
            sort: "text",
          },
        ];

        sessionsGrid = new sqGrid("sessionsGrid");
        sessionsGrid.setFirstColumnAsId(true, false);
        sessionsGrid.enableCheckboxes(true, false);
        sessionsGrid.setColumns(columns, !!sessionsGrid);
        sessionsGrid.setWidths([500, 150, "*", 20], !!sessionsGrid);
        sessionsGrid.setEmptyGridText("No sessions defined.");

        sessionsGrid.onCellDoubleClick = function (row) {
          $scope.selectedSessionRow = row;
          $scope.onEdit();
        };

        sessionsGrid.onSelectionChanged = function () {
          $scope.selectedSessionRow = sessionsGrid.getSelectedRows().selectedRows[0];
        };

        sessionsGrid.cellEventHandler = function (
          rowIndex,
          cellIndex,
          eventName,
          args
        ) {
          $scope.tab.callAction(eventName, rowIndex);
        };

        loadSessions();
        updateGridData();
      }

      function initElementsGrid() {
        if ($("#elementsGrid").length == 0) {
          $timeout(function () {
            initElementsGrid();
          }, 200);
          return;
        }
        var columns = [
          {
            title: "#",
            type: "float2",
            sort: "number",
          },
          {
            title: L.tsq("Start day"),
            type: "text",
            sort: "text",
          },
          {
            title: L.tsq("Start time"),
            type: "text",
            sort: "text",
          },
          {
            title: L.tsq("End day"),
            type: "text",
            sort: "text",
          },
          {
            title: L.tsq("End time"),
            type: "text",
            sort: "text",
          },
          {
            title: L.tsq("SEOC"),
            type: "text",
            sort: "text",
          },
        ];

        elementsGrid = new sqGrid("elementsGrid");
        elementsGrid.setFirstColumnAsId(true, false);
        elementsGrid.enableCheckboxes(false);
        elementsGrid.setColumns(columns, !!elementsGrid);
        elementsGrid.setWidths([40, 80, 80, 80, 80, 63], !!elementsGrid);
        elementsGrid.setEmptyGridText(L.tsq("No session times defined."));

        elementsGrid.onCellDoubleClick = function (row) {
          $scope.selectedElementRow = row;
          $scope.onEditElement();
        };

        elementsGrid.onSelectionChanged = function () {
          $scope.selectedElementRow = elementsGrid.getSelectedRows().selectedRows[0];
        };
      }

      function updateGridData() {
        var selectedSession;
        var realScrollPosition;
        var originalRowsCount = sessionsGrid.rows.length;

        var selection = sessionsGrid.getSelectedRows();
        if (selection.selectedRowsData.length>0){
            selectedSession = selection.selectedRowsData[0][0];
            realScrollPosition=sessionsGrid.scrollerBoxOuter.scrollTop;
        }

        sessionsGrid.removeAllRows();

        var broker = $scope.filter.brokerId;
        var search = $scope.filter.text;

        if ($scope.sessions.length) {
          for (var i = 0; i < $scope.sessions.length; i++) {
            var session = $scope.sessions[i];

            if (broker && broker!=session.broker){
              continue;
            }

            // Apply text search filter
            if (search && search.trim() !== "") {
              var searchLower = search.toLowerCase();
              var nameMatch = session.name.toLowerCase().indexOf(searchLower) >= 0;
              var brokerMatch = session.brokerName && session.brokerName.toLowerCase().indexOf(searchLower) >= 0;
              
              if (!nameMatch && !brokerMatch) {
                continue;
              }
            }

            sessionsGrid.appendData([
              [
                session.name,
                session.brokerName,
                "",
                createActionLink(
                  '<i class="fa fa-times" aria-hidden="true"></i>',
                  "action-link",
                  "delete",
                  i
                ),
              ],
            ]);
          }
        }

        if (selectedSession){
              const sessionInGrid = sessionsGrid.rows.find(r=>r.cells[0]==selectedSession);
              if (sessionInGrid){
                  if (originalRowsCount==sessionsGrid.rows.length){
                      sessionsGrid.selectRow(sessionInGrid.index, true, true);
                      sessionsGrid.scrollerBoxOuter.scrollTop = realScrollPosition;
                  }else{
                      sessionsGrid.scrollToRowIndex(sessionInGrid.index, false);
                  }
              }
          }

        if (!sessionsGrid.getSelectedRows().selectedRowsData[0]) {
          $scope.selectedSessionRow = null;
        }
      }

    function reloadBrokers(){
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.mtUse);
        brokerProfiles.unshift({
            id:-1,
            name:'SQ default'
        });
        $scope.brokerProfiles = brokerProfiles;
    }

      function loadSessions() {
        $scope.sessions.length = 0;
        var sessions = angular.copy(SQConstants.getConstants().sessions);

        for (var i = 0; i < sessions.length; i++) {
          var session = sessions[i];
          if (session.elements && session.elements.length) {
            $scope.sessions.push(session);
          }
        }
      }

      $scope.tab.getSelectedRows = function () {
        return {
          rows: getSelectedSessions(),
          grid: sessionsGrid,
        };
      };

      function getSelectedSessions() {
        var selectedSessions = [];
        var rowData = sessionsGrid.getSelectedRows().selectedRowsData;

        for (var i = 0; i < rowData.length; i++) {
          var data = rowData[i];

          selectedSessions.push({
            name: data[0],
          });
        }

        return selectedSessions;
      }

      $scope.onEdit = function () {
        if (!valueFilled($scope.selectedSessionRow)) {
          $rootScope.showError(L.tsq("You must select at least one row"));
          return;
        }

        $scope.templateAction = "Edit";
        $scope.selectedElementRow = null;

        var sessionName = sessionsGrid.rows[$scope.selectedSessionRow].cells[0];
        $scope.template.name = sessionName;

        elementsGrid.removeAllRows();
        for (var s = 0; s < $scope.sessions.length; s++) {
          var session = $scope.sessions[s];
          if (session.name == sessionName) {
            $scope.template.broker = session.broker;
            for (var e = 0; e < session.elements.length; e++) {
              var element = session.elements[e];
              elementsGrid.appendData([
                [
                  e + 1,
                  element.dayFrom,
                  element.timeFrom,
                  element.dayTo,
                  element.timeTo,
                  element.eod ? L.tsq("Yes") : L.tsq("No"),
                ],
              ]);
            }
            break;
          }
        }

        templateChanged = false;

        try {
          $scope.$digest();
        } catch (err) {}

        showPopup("#sessionTemplateModal");
        $timeout(function () {
          window.dispatchEvent(new Event("resize"));
        });
      };

      $scope.onAddElement = function () {
        $scope.elementAction = "Add";
        $scope.element = {
          startDay: "Mon",
          startTime: "00:00",
          endDay: "Mon",
          endTime: "23:59",
        };

        showPopup("#sessionElementModal");
      };

      $scope.onAddElementMonFri = function () {
        if (elementsGrid.getNumberOfRows() == 0) {
          $rootScope.showError(
            L.tsq(
              "No session defined. Please define Monday session and try again"
            )
          );
          return;
        }

        var mondayFound = false;
        var startDay, endDay, seoc;
        var elementRow = -1;

        for (var i = 0; i < elementsGrid.getNumberOfRows(); i++) {
          startDay = elementsGrid.rows[i].cells[1];
          endDay = elementsGrid.rows[i].cells[3];
          seoc = elementsGrid.rows[i].cells[5];

          if (startDay == "Mon") {
            mondayFound = true;
            if (endDay == "Mon" || endDay == "Tue") {
              elementRow = i;
              break;
            }
          }
        }

        if (elementRow < 0) {
          if (mondayFound) {
            $rootScope.showError(
              L.tsq(
                "No suitable Monday session found. Monday session must end on Monday or Tuesday"
              )
            );
            return;
          } else {
            $rootScope.showError(
              L.tsq("You have to create Monday session element")
            );
            return;
          }
        }

        var startTime = elementsGrid.rows[elementRow].cells[2];
        var endTime = elementsGrid.rows[elementRow].cells[4];

        //remove old session elements
        for (var i = elementsGrid.getNumberOfRows() - 1; i >= 0; i--) {
          startDay = elementsGrid.rows[i].cells[1];

          if (startDay != "Sat" && startDay != "Sun") {
            elementsGrid.removeRowByIndex(i);
          }
        }

        generateSessionElements(startTime, endTime, endDay == "Tue", seoc);

        //correction of rows # numbers
        for (var i = 0; i < elementsGrid.getNumberOfRows(); i++) {
          elementsGrid.rows[i].cells[0] = i + 1;
        }
      };

      function generateSessionElements(startTime, endTime, crossDay, seoc) {
        for (var a = 0; a < 5; a++) {
          elementsGrid.appendData([
            [
              elementsGrid.getNumberOfRows() + 1,
              $scope.days[a],
              startTime,
              crossDay ? $scope.days[a + 1] : $scope.days[a],
              endTime,
              seoc,
            ],
          ]);
        }
      }

      $scope.onEditElement = function () {
        $scope.elementAction = "Edit";
        if (!valueFilled($scope.selectedElementRow)) {
          $rootScope.showError(L.tsq("You must select at least one row"));
          return;
        }

        $scope.element.startDay =
          elementsGrid.rows[$scope.selectedElementRow].cells[1];
        $scope.element.startTime =
          elementsGrid.rows[$scope.selectedElementRow].cells[2];
        $scope.element.endDay =
          elementsGrid.rows[$scope.selectedElementRow].cells[3];
        $scope.element.endTime =
          elementsGrid.rows[$scope.selectedElementRow].cells[4];
        $scope.element.eod =
          elementsGrid.rows[$scope.selectedElementRow].cells[5] == "Yes";

        try {
          $scope.$digest();
        } catch (err) {}

        showPopup("#sessionElementModal");
      };

      $scope.onBrokerChange = function(){
        $scope.brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.template.broker);
      }

      $scope.onRemoveElement = function () {
        if (!valueFilled($scope.selectedElementRow)) {
          $rootScope.showError("You must select at least one row");
          return;
        }

        templateChanged = true;

        elementsGrid.removeRowByIndex($scope.selectedElementRow);
      };

      $scope.onCloseTemplate = function () {
        if (templateChanged) {
          $rootScope.showConfirm(
            L.tsq("Changes not saved"),
            L.tsq("Do you really want to discard changes?"),
            function (confirmed) {
              if (confirmed) {
                hidePopup("#sessionTemplateModal");
              }
            },
            true
          );
        } else {
          hidePopup("#sessionTemplateModal");
        }
      };

      $scope.onSaveTemplate = function () {
        if (!valueFilled($scope.template.name)) {
          $rootScope.showError(L.tsq("Session template name not filled"));
          return;
        }

        if (!isSessionNameValid($scope.template.name)) {
          $rootScope.showError(
            L.tsq("Session name cannot contain any special characters!")
          );

          return;
        }

        if (elementsGrid.getNumberOfRows() == 0) {
          $rootScope.showError(L.tsq("No sessions defined"));
          return;
        }

        $scope.template.sessions = [];

        for (var i = 0; i < elementsGrid.getNumberOfRows(); i++) {
          var data =
            elementsGrid.rows[i].cells[1] +
            "," +
            elementsGrid.rows[i].cells[2] +
            "," +
            elementsGrid.rows[i].cells[3] +
            "," +
            elementsGrid.rows[i].cells[4] +
            "," +
            (elementsGrid.rows[i].cells[5] || "No");

          $scope.template.sessions.push(data);
        }

        if ($scope.templateAction == "Add") {
          SessionService.addSession($scope.template).then(function (result) {
            if (!result.data.success) {
              $rootScope.showError(result.data.error);
            }
          });
        } else {
          SessionService.updateSession($scope.template).then(function (result) {
            if (!result.data.success) {
              $rootScope.showError(result.data.error);
            }
          });
        }

        hidePopup("#sessionTemplateModal");
      };

      $scope.getBrokerProfileLabel = function(){
        if (!$scope.filter.brokerId) return L.tsq('All broker profiles');

        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.filter.brokerId);
        return brokerProfile ? brokerProfile.name : '';
      }      

      $scope.getBrokerProfileLabelInEdit = function(){
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.template.broker);
        return brokerProfile ? brokerProfile.name : '';
     }      

      $scope.onSaveElement = function () {
        // This $timeout wrapper ensures proper execution order between blur and click events.
        // The input field's onBlur handler also uses $timeout(..., 0), and is triggered
        // before the button's click event when the user clicks the button after editing the input.
        // Without this $timeout here, the click handler could run before the onBlur $timeout,
        // causing inconsistent or outdated state.
        // Wrapping the click logic in $timeout(..., 0) ensures it runs after the blur logic.
         $timeout(function () {
            var eod = $scope.element.eod ? "Yes" : "No";

            if (
              $scope.element.startDay == $scope.element.endDay &&
              getIntTime($scope.element.startTime) >
                getIntTime($scope.element.endTime)
            ) {
              $rootScope.showError(
                L.tsq(
                  "Invalid times set. End time must be later than Start time and all times must be in 24h format"
                )
              );
              return;
            }

            if ($scope.elementAction == "Add") {
              var rowId = "e" + elementsGrid.getNumberOfRows();
              var no = elementsGrid.getNumberOfRows() + 1;

              elementsGrid.appendData([
                [
                  no,
                  $scope.element.startDay,
                  $scope.element.startTime,
                  $scope.element.endDay,
                  $scope.element.endTime,
                  eod,
                ],
              ]);
            } else {
              var rowData = elementsGrid.rows[$scope.selectedElementRow].cells;
              rowData[1] = $scope.element.startDay;
              rowData[2] = $scope.element.startTime;
              rowData[3] = $scope.element.endDay;
              rowData[4] = $scope.element.endTime;
              rowData[5] = eod;

              elementsGrid.modifyRowById(rowData, rowData[0]);
            }

            templateChanged = true;
            hidePopup("#sessionElementModal");
         }, 0, false);
      };

      function getIntTime(time) {
        var parts = time.split(":");
        return parseInt(parts[0]) * 100 + parseInt(parts[1]);
      }

      $scope.tab.callAction = function (actionName, rowIndex) {
        switch (actionName) {
          //----------------------------------------------------------------------------
          case "add":
            $scope.templateAction = "Add";

            $scope.template.name = "";
            $scope.template.broker = -1;
            
            $scope.brokerProfile = $scope.brokerProfiles[0];

            elementsGrid.removeAllRows();
            $scope.selectedElementRow = null;

            templateChanged = false;
            showPopup("#sessionTemplateModal");
            $timeout(function () {
              window.dispatchEvent(new Event("resize"));
            });

            break;

          //----------------------------------------------------------------------------
          case "delete":
            if (!valueFilled($scope.selectedSessionRow) && rowIndex < 0) {
              $rootScope.showError(L.tsq("No session selected"));
              return;
            }

            if (!valueFilled($scope.selectedSessionRow) && rowIndex >= 0) {
              var selectedData = [sessionsGrid.rows[rowIndex].cells];
              var names = selectedData[0][0];
              var name = names;
            } else {
              var selectedData = sessionsGrid.getSelectedRows()
                .selectedRowsData;
              var names = "";

              for (var i = 0; i < selectedData.length; i++) {
                var name = selectedData[i][0];
                names += (i > 0 ? "," : "") + name;
              }

              var name = sessionsGrid.rows[$scope.selectedSessionRow].cells[0];
            }

            $rootScope.showConfirm(
              L.tsq("Removing sessions"),
              "Are you sure you want to remove selected sessions (" +
                selectedData.length +
                ")?",
              function (confirmed) {
                if (confirmed) {
                  SessionService.removeSession(names);
                }
              },
              true
            );
            break;

          //----------------------------------------------------------------------------
          case "save":
            console.log("Save sessions");

            if (!valueFilled($scope.selectedSessionRow) && rowIndex < 0) {
              $rootScope.showError(
                L.tsq("You have to select at least one session.")
              );
              return;
            }

            if (!valueFilled($scope.selectedSessionRow) && rowIndex >= 0) {
              var selectedData = [sessionsGrid.rows[rowIndex].cells];
              var names = selectedData[0][0];
              var name = names;
            } else {
              var selectedData = sessionsGrid.getSelectedRows()
                .selectedRowsData;
              var names = "";

              for (var i = 0; i < selectedData.length; i++) {
                var name = selectedData[i][0];
                names += (i > 0 ? "," : "") + name;
              }

              var name = sessionsGrid.rows[$scope.selectedSessionRow].cells[0];
            }

            var data = {
              names: names,
            };

            $rootScope.saveFile(
              L.tsq("Select file"), 
              { name: "xml", description: "XML Files" }, 
              "SaveSession", 
              "Sessions.xml", 
              null, 
              function(targetPath) {
                  data.filePath = targetPath;
                  SessionService.save(data);
              }
            );

            break;

          //----------------------------------------------------------------------------
          case "load":
            console.log("Load sessions");

            var data = {};

            $rootScope.showFilePicker(
              L.tsq("Select file"),
              "LoadSession",
              true,
              false,
              null,
              { name: "xml", description: "XML Files" },
              null,
              function (paths) {
                if (paths) {
                  data.filePath = paths[0];
                  SessionService.load(data);
                }
            });

            break;
        }
      };

      //- Event Handlers --------------------------------------------------------------

      function onEvent(event, data) {
        if (event == SQEvents.get('BROKERS_CHANGED')) {
          reloadBrokers();
        }else if (event == SQEvents.get("SESSIONS_CHANGED")) {
          loadSessions();
          updateGridData();
        }
      }

      $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
      });

      function onFilterSearch() {
        updateGridData();
      }

      function onFilterReset() {
        $scope.filter.text = '';
        updateGridData();
      }

      //- Initialization --------------------------------------------------------------

      var sessionsGrid, elementsGrid;
      var templateChanged = false;
      var listenerId = "SessionsCtrl";

      reloadBrokers();
      $scope.days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
      $scope.template = {};
      $scope.element = {};
      $scope.sessions = [];

      $scope.filter = {
        text: '',
        onSearch: onFilterSearch,
        onReset: onFilterReset,
        brokerId: ""
    };

      initGrids();

      SQEvents.addListener(
        listenerId,
        [SQEvents.get("SESSIONS_CHANGED"),
          SQEvents.get("BROKERS_CHANGED")
        ],
        onEvent
      );
    }
  );
