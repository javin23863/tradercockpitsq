angular.module('app.tmtasks').directive('tmTask', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/TaskManagerTasks/directives/tmtask/tmtask.html',
        scope : {
            instance: '=',
        },
        controller: 'TMTaskCtrl',
        link: function (scope, element){
            scope.element = element;
        },
    }
});