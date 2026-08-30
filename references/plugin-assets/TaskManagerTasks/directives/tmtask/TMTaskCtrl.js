angular
  .module("app.tmtasks")
  .controller(
    "TMTaskCtrl",
    function (
      $scope,
      $rootScope,
      $state,
      AppService,
      TMTasksService,
      SQEvents,
      $timeout,
      SQConstants
    ) {
      console.log("TMTask controller initialized");
      $scope.projectStates = SQConstants.getConstants().runningStatuses;

      $(".sq-app-taskmanager").removeClass("sqn-hide-header");

      $scope.onTaskSelect = function () {
        if ($rootScope.project.state == $scope.projectStates.running) {
          $scope.instance.selectTask(
            $scope.instance.taskType,
            $scope.instance.taskName
          );
          return;
        }

        AppService.updateTaskXML().then(() => {
          $scope.instance.selectTask(
            $scope.instance.taskType,
            $scope.instance.taskName
          );

          $timeout(
            function () {
              window.dispatchEvent(new Event("resize"));
            },
            0,
            false
          );
        });
      };

      $scope.showMenu = function (event) {
        if ($rootScope.project.state == $scope.projectStates.running) {
          return;
        }

        var dropdownMenu = $(".dropdown-custom-tasks");
        var thisOpened = dropdownMenu.hasClass("open");
        if (!thisOpened) {
          TMTasksService.clickedTask = $scope.instance;
          var parentElement = $($scope.element).parent().offset();
          dropdownMenu.addClass("open");
          let vh = Math.max(
            document.documentElement.clientHeight || 0,
            window.innerHeight || 0
          );
          let top = parentElement.top + 16;
          let height = $(".dropdown-custom-tasks .dropdown-menu").height();
          if (vh - top - height < 0) {
            top = top - height;
          }
          dropdownMenu
            .css("top", top + "px")
            .css(
              "left",
              parentElement.left + $scope.element.width() + 14 + "px"
            );
        } else {
          dropdownMenu.removeClass("open");
        }
      };

      $scope.hideMenu = function () {
        $($scope.element).find(".dropdown").removeClass("open");
      };

      $scope.activateTask = function () {
        $scope.instance.active = !$scope.instance.active;
        $scope.instance.activateTask($scope.instance);
      };
    }
  );
