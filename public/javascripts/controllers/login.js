/**
 * Manages all functionality for the Login page
 */
angular.module('icon2014').controller('LoginCtrl', function($scope, $rootScope, $window, $location, $http, UserService) {

    /**
     * Fetch the OAuth URL and open the login pop-up.
     *
     */
    $scope.letTheOAuthBegin = function () {
        $http.get("https://mattlmbp:9443/signin-url")
            .success(function(data, status, headers, config) {
                $scope.handleOAuth(data);
            }).
            error(function(data, status, headers, config) {
                /* awesome error handling */
                $location.path('/');
            }
        );
    };

    /**
     * Open the login pop-up and handle both 'allow' and 'deny' actions.
     *
     * @param signUrl
     */
    $scope.handleOAuth = function(signUrl) {
        var popupOptions = {
            name: 'AuthPopup',
            openParams: {
                width: 650,
                height: 300,
                resizable: true,
                scrollbars: true,
                status: true
            }
        };

        /* turn the map into a nicely formatted string */
        var formatPopupOptions = function(options) {
            var pairs = [];
            angular.forEach(options, function(value, key) {
                if (value || value === 0) {
                    value = value === true ? 'yes' : value;
                    pairs.push(key + '=' + value);
                }
            });
            return pairs.join(',');
        };

        /* open the authentication pop-up */
        var popup = window.open(signUrl, popupOptions.name, formatPopupOptions(popupOptions.openParams));

        /* add a message event listener, which will be fired by the redirect_uri */
        window.addEventListener("message", function(event) {
            if (event.source == popup && event.origin == window.location.origin) {

                /* close that pop-up, we don't need him anymore */
                popup.close();

                /* move forward only if there was no error */
                if(!event.data.error) {
                    $http.get("https://mattlmbp:9443/token?code=" + event.data.code)
                        .success(function(data, status, headers, config) {
                            /* yay! let's save this guy for later */
                            UserService.setAuthToken(data.icon2014Token);

                            /* ...and then go have some fun with the api */
                            $location.path('/main');

                        }).
                        error(function(data, status, headers, config) {
                            /* awesome error handling */
                            $location.path('/');
                        }
                    );
                }
            }
        }, false);
    }

});