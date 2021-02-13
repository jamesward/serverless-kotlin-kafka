const websocket = new WebSocket("ws://localhost:8080/websocket");

websocket.onmessage = function(event) {
    console.log(event);
}
