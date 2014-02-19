angular.module('TodoApp', [])
    .controller('TodoCtrl', function ($scope) {
        $scope.foo = 'hi';
    });

angular.bootstrap(document, ['TodoApp']);


function createWork(msg) {

    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }

    function guid() {
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    }

    var workId = guid();

    socket.sendIfOpen(JSON.stringify({workid: workId, msg: msg}));

    var subscription = messages
        .filter(function (msg) {
            return msg.workid == workId;
        })
        .subscribe(
        function (x) {
            if (x.end) {
                console.log('Last work result: ' + x);
                subscription.dispose
            } else {
                console.log('Next work result: ' + x);
            }
        },
        function (err) {
            console.log('Error: ' + err);
        },
        function () {
            console.log('Completed');
        });
}


var messages = new Rx.Subject();

var socket = new SockJS('http://localhost/bus/sock');

socket.onopen = function () {
    console.log('open');
};

socket.onmessage = function (event) {
    //console.log('Next message: ', event.data);
    messages.onNext(JSON.parse(event.data));
};

socket.onclose = function () {
    console.log('close');
};

socket.sendIfOpen = function (message) {
    if (socket.readyState === SockJS.OPEN) {
        socket.send(message);
    } else {
        console.log("The socket is not open.");
    }
}

var subscription = messages.subscribe(
    function (x) {
        //console.log('Next message: ',x);
        var txt = $("textarea#response");
        txt.val(txt.val() + "\n" + JSON.stringify(x));
    },
    function (err) {
        console.log('Error: ' + err);
    },
    function () {
        console.log('Completed');
    });

function ajaxsend(jsonBody) {
    $.ajax(
        {
            url: "/api",
            type: "POST",
            data: jsonBody,
            dataType: "json",
            error: function (jqXHR, textStatus, errorThrown) {
                console.log(jqXHR, textStatus, errorThrown)
            }
        }).done(function (data) {
            messages.onNext(data);
        });
}


//
//    var socket = new WebSocket("ws://localhost/bus/ws");
//
//    socket.onmessage = function (event) {
//        //console.log("Received data from websocket: ", convert(event.data));
//        messages.onNext( event.data);
//    }
//
//    socket.onopen = function (event) {
//        console.log("Web Socket opened");
//    };
//
//    socket.onclose = function (event) {
//        console.log("Web Socket closed");
//    };
//
//    socket.sendIfOpen = function(message) {
//
//        if (socket.readyState === WebSocket.OPEN) {
//            console.log("sending message")
//            socket.send(message);
//        } else {
//            console.log("The socket is not open.");
//        }
//    }
